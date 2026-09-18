## Context

See proposal.md for motivation and ../../../docs/lifecycle-transition-audit.md for the original call sites and test gaps. The old start callback can run before instance creation or authorization. The old finish callback also runs for transfers and signer changes that do not leave a node. Gateway convergence currently uses graph state keyed by nodeCode rather than independent join batches; this change preserves that model.

Hosts can synchronize process portals on start/resubmission, create work items and update business stages on node entry, mark individual work items complete on approval actions, and update recipients on assignment changes. Pre-operation hooks support host validation and business locking. Engine code does not depend on a particular host implementation.

## Goals / Non-Goals

**Goals:**

- Distinguish instance lifecycle, one node execution, one participant action, and assignment changes, with sufficient context for business and work-item synchronization.
- Preserve countersignature, routing, timeout, and subprocess semantics. Emit facts only for accepted, successfully executed transitions.
- Use persistent identities, transactions, and conditional transitions to prevent duplicate closure; state external-delivery limits explicitly.

**Non-Goals:**

- Public TASK_CREATED, TASK_COMPLETED, TASK_CANCELLED, or TASK_ACTION_APPLIED events. Pre-operation handling uses separate methods on the same listener.
- Gateway business callbacks, join batches, routing fixes, or new guarantees for concurrent re-entry into the same node.
- Built-in work-item services, process portals, notification channels, timeout schedulers, business locks, or outbox delivery services; additional pause/resume lifecycle events.
- Executing development-database migrations or publishing artifacts.

## Decisions

### 1. Exactly eight fact events

| Group | Event | Trigger and purpose |
| --- | --- | --- |
| Process | PROCESS_STARTED | After a new instance has a valid identity and initial state, before start-node entry; establish business correlation |
| Process | PROCESS_WITHDRAWN | The same instance is withdrawn into a resumable state; make business data editable |
| Process | PROCESS_RESUBMITTED | The same instance resumes from awaiting resubmission; do not emit STARTED again |
| Process | PROCESS_ENDED | The instance actually becomes terminal; reason is COMPLETED, TERMINATED, or CANCELLED |
| Node | NODE_ENTERED | A supported execution starts with its final assignment snapshot; create work items and synchronize stages |
| Node | NODE_LEFT | The execution closes with COMPLETED, REJECTED, WITHDRAWN, or CANCELLED; clean up remaining work items |
| Participant | APPROVAL_ACTION_COMPLETED | One approval/rejection succeeds, including incomplete countersignature votes and delegation completion; complete that participant's work item |
| Participant | ASSIGNEES_CHANGED | Transfer, delegation/return, or signer changes actually alter relationships on an active execution; update recipients |

Separate task CRUD events would duplicate node entry/exit. Participant facts must remain independent: an individual can finish before the countersignature threshold is reached, and a transfer changes recipients without leaving the node.

APPROVAL_ACTION_COMPLETED distinguishes APPROVE and REJECT and includes participant identity, representation/delegation relationships, opinion, target node, source, and whether the execution closes. Transfer is not approval. Initial assignment is included in NODE_ENTERED, without ASSIGNEES_CHANGED. Routine approval consumption and closure cleanup do not produce additional assignee-change events. No-op, unauthorized, invalid, or rolled-back operations produce no committed success notification.

Delegation completion that restores the principal emits approval completion before assignee changes, followed by node departure only if the node actually closes. Consumers separately handle individual completion, assignment differences, and remaining-node cleanup. Closure must be idempotent; consumers must not clear an entire node for every event.

### 2. Supported nodes and persistent execution identity

Supported types are approval, wait, carbon-copy, start, end, initiator, and subprocess. Conditional, parallel, and inclusive gateways retain their routing role without execution records or callbacks. Previews, condition evaluation, and unselected branches emit no events. Subprocess entry activates the parent execution; departure follows existing aggregation rules. One child becoming terminal does not necessarily complete its parent node.

Add flow_node_execution with id, tenant_id, instance_id, definition_id, node_code, node_type, state, entry/closure timestamps, closure reason, concurrency version, and deleted. Tasks and history reference nodeExecutionId. Start/end nodes have identities even without persistent work items. Every actual re-entry receives a new ID; instanceId + nodeCode is not a unique key. Countersignature participants share one execution.

Terminal instance transitions, execution closure, and participant consumption use transactional locks or conditional updates. Only the successful transition emits facts. Independent operations may each emit events; matching nodeCode values do not merge them. These constraints protect actual executions without claiming to fix existing gateway concurrency limitations.

### 3. Resumable states and causal ordering

Lifecycle cannot be inferred from configurable flowStatus strings such as CANCEL or RE_START. Persist a separate state with ACTIVE, AWAITING_RESUBMISSION, and ENDED. Withdrawal awaits resubmission; rejection to an ordinary approval node remains ACTIVE.

Provide an explicit same-instance resubmission entry point accepting only AWAITING_RESUBMISSION. Restore ACTIVE and follow the captured submission path without creating an instance or replaying the start node. start always creates a new instance. Ordinary approval cannot bypass the resubmission gate. State and entry-point behavior are public contracts rather than host-assembled events.

Typical sequences:

- Start: PROCESS_STARTED → start NODE_ENTERED / NODE_LEFT → approval NODE_ENTERED.
- Serial approval: APPROVAL_ACTION_COMPLETED → current NODE_LEFT → downstream NODE_ENTERED.
- Incomplete countersignature: APPROVAL_ACTION_COMPLETED only; execution stays active.
- Transfer / signer changes: ASSIGNEES_CHANGED only; execution identity remains unchanged.
- Rejection: APPROVAL_ACTION_COMPLETED(REJECT) → current NODE_LEFT(REJECTED) → target NODE_ENTERED with a new identity; this does not imply process completion.
- Withdrawal: old active executions NODE_LEFT(WITHDRAWN) → PROCESS_WITHDRAWN → actual replacement executions NODE_ENTERED. Snapshots indicate awaiting resubmission so hosts do not create ordinary approval work items.
- Resubmission: PROCESS_RESUBMITTED → actual node transitions caused by the operation, without fabricated entry or another PROCESS_STARTED.
- Normal completion: actual end-node entry/exit and closure of remaining active executions → PROCESS_ENDED(COMPLETED).
- Forced termination: all active executions NODE_LEFT(CANCELLED) → PROCESS_ENDED(TERMINATED), without a fabricated end-node visit.

NODE_ENTERED is created after approver resolution and assignment hooks. Its task and participant snapshots remain usable after automatic advancement deletes the tasks. NODE_LEFT preserves affected tasks and outstanding participants for cleanup. Nodes without tasks use empty lists.

Guarantee causal order within an operation/execution, not a global order across unrelated parallel branches. Complete active-execution cleanup before withdrawal or terminal process events. CRUD deletion does not automatically mean business cancellation.

### 4. Automatic advancement, correlation, and context

Manual approval, timeout approval/rejection, and automatic skipping use the same facts with distinct sources. System actors must not impersonate human approvers. Wait signal/timeout resumption emits actual node transitions without fabricated approval actions. Carbon-copy advancement produces paired entry/exit events. Timeout failure/retry audit remains separate from success events; hosts must provide scheduling.

Events are immutable, Java 8 compatible snapshots detached from entities. They include eventId, type, operationId, occurredAt, tenantId, instanceId, definitionId/version, businessType/businessId, applicable nodeExecutionId, nodeCode/type, taskId, actor, action, reason, source, target node, opinion, and before/after state. Child events include parent instance, task, and execution references.

Participant snapshots include stable user IDs, participant identities, and delegation relationships. Change events include before/after data sufficient to derive additions/removals, not merely counts. Node entry/exit includes applicable task and participant snapshots. Copy variables and approval data defensively; never expose mutable ORM entities. Hosts supply business page URLs and external-system fields.

### 5. Delivery and registration

- IN_TRANSACTION: Invoke synchronously after the corresponding transition and before commit. Failure rolls back the operation. Recursive advancement of the same instance is prohibited. These are fact callbacks, separate from pre-operation validation.
- AFTER_COMMIT: Notify in recorded order only after the host's outermost transaction commits. Report failures through the error handler and logs, without claiming rollback or blocking other listeners.

Every lifecycle write entry requires a real transaction adapter and fails explicitly when one is missing. Reuse and verify transactionExecutor / afterCommit without a Spring dependency in core. Boot 2 / 3 / 4 must behave consistently. Transaction retries can repeat synchronous callbacks; a crash after commit can lose notifications. Hosts requiring durable external delivery must use a transactional outbox and idempotent consumers.

Register stable codes with deterministic order. Store versioned lifecycle configuration in definition/node ext. Process events allow definition/global scope; node events allow supported-node/definition/global scope; participant events allow approval-node/definition/global scope. Reject duplicate codes, unknown codes/events, unsupported scopes/versions, and conflicting parameters. A registration matching multiple scopes receives an event once per delivery phase.

### 6. Host integration and legacy listeners

| Host scenario | Integration |
| --- | --- |
| Create/restore externally initiated process records | PROCESS_STARTED / PROCESS_RESUBMITTED |
| Synchronize terminal and withdrawn business state | PROCESS_ENDED / PROCESS_WITHDRAWN; use NODE_LEFT reason/target for resumable rejection |
| Current stage, initial work items, timeout snapshots | NODE_ENTERED; inspect lifecycle state and tasks before creating approval work items |
| Individual completion during countersignature | APPROVAL_ACTION_COMPLETED for the specific participant |
| Transfer, delegation, and signer updates | ASSIGNEES_CHANGED relationship differences, without fabricated re-entry |
| Remaining work-item cleanup after termination or branch cancellation | NODE_LEFT reason and affected-task snapshots |
| Material validation, business locking, empty-approver handling | Optional pre-operation/assignment hooks; resolvers still provide approver sources |

Remove GlobalListener and old start/assignment/finish/create dispatch. Register WorkflowLifecycleListener without a legacy switch. Reject old configuration during import/publication/execution validation rather than silently ignoring it. Map old start validation/variable changes to beforeOperation, assignment to beforeAssignment, and finish/create facts to the eight events according to their actual meaning. FORM_LOAD remains a separate extension. Arbitrary mutation of identities, tenants, or graph structures is not retained; use the supported context operations.

## Risks / Trade-offs

- Duplicate personal/node cleanup: Provide participant and remaining-task snapshots, idempotent examples, and countersignature/transfer tests.
- Existing gateway limits: Do not add batch guarantees. Record existing routes and verify that callbacks preserve them; handle independent routing defects separately.
- Resubmission changes old call patterns: Use explicit state and entry points; do not infer migration state from a single flowStatus.
- Execution/snapshot overhead: Persist supported nodes and index tenant/instance/state queries; do not add a built-in delivery table.
- Transaction side effects and notification loss: Document phases, error reporting, and host outbox requirements without promising exactly-once external delivery.
- Compatibility coverage gaps: Verify both ORMs, three SQL dialects, JSON providers, Boot 2 / 3 / 4, and Vue / React.

## Migration Plan

The project has no existing users requiring migration. New integrations use the current API and fresh-install schemas. The following records migration tooling included in this implementation, not a prerequisite for new installations.

1. Establish the eight-event contract and host mapping with sequence and negative tests.
2. Add execution references and lifecycle state; synchronize three fresh-install schemas and document separate development-database migration/rollback. Never run installation scripts over an existing database.
3. For active-instance tooling, derive execution references from actual tasks/subprocess runs without replaying historical start/entry events. Explicitly reject ambiguous state or associations rather than guessing.
4. Map old subscriptions to supported hooks/facts and verify manual and automatic paths. Preserve existing resolution and assignment capabilities.
5. Before a rollback involving existing development data, stop writes and back up data. Assess old-version readability or restore matching backups; downgrading jars alone is insufficient.

## Complete listener replacement contract

WorkflowLifecycleListener provides beforeOperation, beforeAssignment, and onEvent. The first two execute synchronously in the transaction; onEvent receives the eight immutable facts. Global/definition/node scopes share stable registration codes and deterministic ordering. Pre-hooks cannot use AFTER_COMMIT.

beforeOperation covers start, approval/rejection, withdrawal, resubmission, termination, transfer, delegation, signer changes, pending, wait resumption, and system advancement. Authorization and basic input validation precede the hook; routing and state writes follow it. Business variable changes affect expression evaluation, persistence, and downstream assignment. Instance identity, tenant, actor identity, and authorization-bypass flags are immutable. Failure rolls back the operation without success facts or after-commit notifications.

beforeAssignment runs after resolution and before task/participant persistence. It exposes an editable assignment draft containing participants and supported task business fields, without replaceable ORM identities. Validate participants, fields, and node constraints after all hooks, then persist participants and capture final assignment in NODE_ENTERED. Variable changes must not silently reroute an already selected path.

Each matching pre-hook registration runs once per engine operation, including automatic paths; internal service calls do not repeat it. Different operations have separate operationId values. Hosts define business lock ordering. Network side effects are not transactionally reversible.

Return to initiator (TO_INITIATOR) activates an initiator work step distinct from the start node, owned by the original initiator for editing and resubmission. Rejection produces APPROVAL_ACTION_COMPLETED(REJECT) → NODE_LEFT(REJECTED) → initiator NODE_ENTERED, without PROCESS_WITHDRAWN or PROCESS_ENDED. Resubmission emits PROCESS_RESUBMITTED before leaving the initiator step and continuing approval, without replaying the start node. Use consistent initiator terminology in interfaces, logs, and documentation.

## Resubmission strategy

Use the node control configuration's resubmitStrategy rather than adding a global choice. RESTART_FROM_BEGINNING restarts the approval sequence; CONTINUE_FROM_REJECTED_NODE resumes at the approval node that returned the process. Before this change, React exposed these settings with RESTART_FROM_BEGINNING as default, but the backend did not implement them. Persist the selected policy and originating node at return time; resubmission uses that captured context so later definition changes cannot alter the current path. Neither strategy replays process-start or start-node events. Re-entered approval nodes receive new execution identities. Legacy TO_DRAFT configuration maps to TO_INITIATOR.

## Global programmatic registration

FlowEngine exposes a framework-independent registry. Hosts can register listener objects and global subscriptions without Spring, FrameInvoker, or database configuration. Multiple listeners use the same interface. Spring is an optional discovery adapter; the database stores stable codes and subscriptions, never arbitrary executable class paths.

Registration maps a stable code to an instance. Global subscriptions select hook/event, phase, and ordering; definitions/nodes reference the same code. Reject duplicate codes. Deduplicate overlapping global/local subscriptions by registration, handling point, and phase, rejecting parameter conflicts rather than silently overwriting. Hosts register at startup; runtime reads immutable registry snapshots.

## Spring Bean discovery

After singleton initialization, discover WorkflowLifecycleListener Beans automatically and use their actual Bean names as subscription codes. Preserve proxies and dependency injection. Hosts need not call register. Database lifecycle configuration references Bean names; unknown names fail explicitly instead of instantiating arbitrary classes. Non-Spring hosts retain direct object registration.

Hosts may declare LifecycleSubscription Beans for global subscriptions. Validate registrations and subscriptions as a batch and install atomically. Name conflicts or unknown references fail startup without partial registration. Context shutdown removes only its own registrations/subscriptions, preserving unrelated programmatic registrations. Boot 2 / 3 / 4 adapters share this behavior.
