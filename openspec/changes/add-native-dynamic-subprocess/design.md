## Context

Flovira 1.0.0 executes a definition as one independent instance. Nodes, tasks and history are persisted through the framework-neutral `FloviraDao` abstraction; Spring Boot and Solon starters wire concrete ORM implementations into `FlowEngine`. A node already has an `ext` JSON field, so subprocess configuration can be added without changing `flow_node`, but the engine has no parent-child aggregate, transaction abstraction, terminal callback or hierarchical query contract.

The implementation must remain Java 8 compatible, framework/ORM neutral in core, additive for existing users, and aligned across MyBatis, MyBatis-Plus, Easy-Query, Spring Boot 2/3/4 and Solon.

## Goals / Non-Goals

**Goals:**

- Make `SUB_PROCESS` a first-class executable node rather than a business-side WAIT convention.
- Dynamically create ordinary child instances from a standard runtime collection, bounded by a configurable per-run limit that defaults to 128.
- Pin one fixed child definition version per run and resume the exact parent task once under `ALL` completion.
- Provide idempotent creation, terminal aggregation, cancellation, retry, audit and hierarchical read APIs.
- Preserve tenant boundaries and existing ordinary-flow behavior across every supported ORM/database.
- Keep designer configuration minimal: select the fixed child flow; engine-owned defaults are not editable.

**Non-Goals:**

- Recursive subprocess definitions, per-item definition selection, arbitrary designer field mappings, or `ANY`/threshold completion.
- Distributed child workflows, message brokers or cross-database transactions.
- Rewriting existing definitions, instances, tasks or historical records.

## Decisions

### 1. Add node type 6 and store configuration in `Node.ext`

Append `SUB_PROCESS(6, "subProcess")` to `NodeType`. The persisted node remains a normal `flow_node`; its `ext` JSON stores a versioned configuration containing `fixedChildFlowCode`, `completionPolicy=ALL`, `allowEmpty`, and optional output variable names. Existing enum keys are unchanged.

Using a dedicated node type lets task creation and rollback distinguish subprocess semantics without parsing business conventions. Adding columns to `flow_node` was rejected because the configuration is sparse, versioned and already fits the established extension mechanism.

### 2. Use ordinary Flovira instances as children

Every dynamic item starts an ordinary child instance through an additive `InsService` entry point that accepts the exact resolved definition ID. The runnable child definition is resolved when the parent task is reached and the started instance is therefore genuinely pinned to that definition ID/version. Child execution, rejection, history, handlers and listeners reuse the existing engine.

Copying child topology into the parent definition was rejected because it destroys definition-version identity and makes history and cancellation ambiguous.

### 3. Define one engine-owned runtime input contract

The parent variable `subprocessItems` is a collection of maps with:

- `itemKey`: required, unique, stable string.
- `itemLabel`: optional display snapshot.
- `variables`: optional map copied into the child instance.

The engine derives child business keys and injects reserved linkage variables. Designer-authored collection paths and mappings are not supported. Keys beginning with `flovira.subprocess.` and engine identity variables cannot be overridden. Items exceeding the configured limit fail before any child starts.

`Flovira.subprocessMaxChildren` controls the per-run limit, defaults to 128 and must be greater than zero. Exceeding the configured limit fails before any run, relationship or child instance is persisted. The engine does not impose a second hard-coded ceiling because deployments may deliberately tune this trusted configuration for their database capacity.

### 4. Persist an explicit run aggregate

Add tenant-scoped tables:

- `flow_subprocess_run`: one run per parent task/node entry, including pinned child definition, fingerprint, counters, status and optimistic version.
- `flow_subprocess_child`: one row per item, including item snapshot, child business key, definition identity, instance identity, status and outcome.
- `flow_subprocess_event`: append-only lifecycle events containing identifiers and classifications, never workflow variable values.

Uniqueness on parent task, run/item and child instance makes retries idempotent. A new parent task created after re-entry creates a new run while preserving previous history.

### 5. Add a dedicated subprocess persistence/service seam

Core introduces entity interfaces, DAO interfaces and `SubprocessService`; `FlowEngine` receives corresponding suppliers and service accessors. The three ORM core modules implement these contracts using their native query/locking facilities. Generic `FloviraDao` remains unchanged because row locking, counter updates and reconciliation queries are subprocess-specific.

### 6. Execute orchestration synchronously and expose recovery explicitly

When task creation reaches `SUB_PROCESS`, the engine initializes the run synchronously before returning. When a child reaches a terminal state, the engine updates the child/run aggregate and resumes the stored parent task when all children succeeded. Unique constraints and state preconditions make repeated calls harmless.

Core does not introduce Spring events or a scheduler. It exposes `initialize`, `notifyChildTerminal`, `resumeReadyRun`, `cancelByParent` and `reconcile` APIs so host applications can retry after crashes. Framework adapters provide a small transaction executor with transaction execution and after-commit registration. Initialization and parent resumption are triggered after the task or terminal transition commits; reconciliation remains the durable fallback. Direct core usage without an adapter fails fast when subprocesses are enabled.

### 7. Treat cancellation scope as an engine invariant

Parent termination and withdrawal cancel every active run under the parent instance. Rejection or rollback leaving one subprocess node cancels the active run for that parent task/node. Active children are terminated in stable child-ID order before the parent action completes. Completed/cancelled rows and history are retained.

### 8. Validate fixed references at definition publication

Publishing a definition containing `SUB_PROCESS` requires a same-tenant enabled child definition and validates the complete fixed-reference graph. Direct/indirect cycles and a child definition containing a subprocess that reaches an ancestor are rejected. Runtime repeats the runnable-definition check and pins its ID/version to protect against publication races.

### 9. Compose progress and history instead of copying it

Parent chart nodes gain an optional subprocess summary. Child rows are paged and child chart/history is queried lazily from existing instance services. Combined history merges parent history, orchestration events and referenced child history in timestamp order with source and relationship metadata. No task history is copied to the parent.

### 10. Ship a fresh 1.0.0 schema and gate node use

This fork starts a new version line at 1.0.0. MySQL and PostgreSQL ship complete `flovira-v1.sql` initialization scripts containing all ten tables; no Warm-Flow or earlier Flovira upgrade chain is supported. Oracle and SQL Server retain equivalent complete schemas. Definitions without `SUB_PROCESS` run exactly as before. Enabling a subprocess definition before applying the schema fails with an explicit persistence error.

## Risks / Trade-offs

- [A host process crashes after child instances start but before the run is marked running] -> Persist deterministic rows first, use unique business keys, and make `reconcile` finish or classify the incomplete run.
- [The last child completion races with parent cancellation] -> Lock the run, use optimistic status transitions, and acquire child locks in stable order.
- [Framework-neutral core lacks a transaction manager] -> Require a starter-provided transaction executor for subprocess mutations and fail fast when absent.
- [Three ORM implementations drift] -> Define shared contract tests and run the same lifecycle suite against each ORM adapter.
- [Hierarchical logs become unbounded] -> Page child lists/history, return aggregate counts by default, and load child details only on expansion.
- [Adding three tables weakens the lightweight footprint] -> Keep the feature optional; ordinary workflows do not read or write subprocess tables.
- [Fixed child definitions are republished while a parent is waiting] -> Resolve once and persist exact definition ID/version before child creation.

## Migration Plan

1. Install the matching 1.0.0 initialization schema and deploy the core/ORM support while no published definition uses `SUB_PROCESS`.
2. Verify starter initialization for each ORM/framework combination.
3. Deploy the designer and publish new subprocess-enabled definitions; do not mutate running definitions.
4. Monitor initialization failures, active runs, failed children and ready-to-resume runs through the query/recovery APIs.
5. Application rollback may disable new subprocess definitions, but a compatible engine must remain available until active subprocess runs are completed or cancelled. Runtime tables and audit data are not dropped during production rollback.

Because 1.0.0 is a fresh-install baseline, database reset uses backup/restore or database recreation rather than a product upgrade rollback script.

## Open Questions

- Whether a later release should expose an SPI for additional completion policies while keeping `ALL` as the only built-in policy initially.
