## 1. Core contracts and model

- [x] 1.1 Add core tests for `SUB_PROCESS` node type compatibility, versioned `Node.ext` configuration parsing, fixed-flow validation and preservation of existing node enum keys.
- [x] 1.2 Add Java 8-compatible subprocess configuration, runtime item, run/child/event entity interfaces, statuses, outcomes, commands and result DTOs without framework or ORM imports.
- [x] 1.3 Extend `FlowEngine` with subprocess entity suppliers and a `SubprocessService` accessor while preserving all existing public methods.
- [x] 1.4 Add dedicated subprocess DAO contracts for unique lookup, row locking, paging, counter updates and reconciliation queries.
- [x] 1.5 Add a framework-neutral transaction executor contract with after-commit registration and fail-fast validation for missing transaction support or an invalid `subprocessMaxChildren` value.

## 2. Definition validation and runtime orchestration

- [x] 2.1 Add tests for the standard `subprocessItems` contract, reserved variables, duplicate/blank keys, empty behavior, deterministic fingerprints, the configurable per-run limit and the default limit of 128.
- [x] 2.2 Implement fixed child definition resolution, same-tenant checks, an exact-definition `InsService` start entry point, definition ID/version pinning, deterministic child business keys and reserved parent-link variables.
- [x] 2.3 Add publication tests for missing/disabled child flows and direct or indirect fixed-reference cycles, including preservation of the previous runnable definition.
- [x] 2.4 Implement subprocess dependency graph validation in definition publication/import paths.
- [x] 2.5 Add lifecycle tests for atomic initialization, repeated initialization, interrupted initialization and reconciliation without duplicate runs, children or instances.
- [x] 2.6 Implement synchronous run initialization and reconciliation using ordinary `InsService` child instances and explicit run state transitions.
- [x] 2.7 Add aggregation tests for partial completion, final success, child-local rejection, failed/cancelled children, duplicate terminal notifications and failed parent-resume retry.
- [x] 2.8 Implement child terminal notification, ALL counter aggregation, `READY_TO_RESUME` recovery and exactly-once passage of the stored parent task.
- [x] 2.9 Add cancellation tests for parent termination, withdrawal, node-local rollback, repeated cancellation and subprocess-node re-entry.
- [x] 2.10 Integrate stable-order child cancellation into parent lifecycle operations before the parent task/instance changes state.

## 3. ORM and framework adapters

- [x] 3.1 Implement subprocess entities, mappers and DAOs in `flovira-mybatis-core`, including locking and indexed paging queries.
- [x] 3.2 Implement equivalent subprocess entities and DAOs in `flovira-mybatis-plus-core`.
- [x] 3.4 Wire subprocess suppliers, DAOs and services through all Spring Boot 2/3/4 starters and Solon plugins for each ORM family.
- [x] 3.5 Implement Spring and Solon transaction executors and verify atomic rollback of partial child initialization/cancellation.
- [x] 3.6 Run one shared subprocess persistence/lifecycle contract suite against all three ORM adapters.

## 4. Database schemas

- [x] 4.1 Add `flow_subprocess_run`, `flow_subprocess_child` and `flow_subprocess_event` with tenant, uniqueness, reconciliation and paging indexes to the MySQL 1.0.0 initialization schema.
- [x] 4.2 Provide complete MySQL and PostgreSQL `flovira-v1.sql` fresh-install scripts without inheriting the historical upgrade chain.
- [x] 4.3 Add equivalent table/index definitions to Oracle and SQL Server full schemas with documented dialect differences.
- [x] 4.4 Add schema contract tests verifying required columns, uniqueness and indexes for each supported database script.

## 5. Designer support

- [x] 5.1 Add component tests for inserting, editing, serializing, reopening and validating a fixed-template subprocess node in classic and mimic modes.
- [x] 5.2 Add `SUB_PROCESS` to shared designer types, node factories, import/export conversion and insertion menus.
- [x] 5.3 Implement the minimal configuration panel containing only fixed child-flow selection and read-only engine defaults.
- [x] 5.4 Render the subprocess node in both modes using a consistent light-purple background, white title and accessible selected/dark states.
- [x] 5.5 Add backend UI-plugin DTO/endpoint support for listing eligible child definitions and returning publication validation errors.

## 6. Hierarchical progress and history

- [x] 6.1 Add core tests for optional parent subprocess summaries without recursively loading child history.
- [x] 6.2 Implement run summaries and tenant-scoped paged child queries with current child node and pinned definition identity.
- [x] 6.3 Add tests proving one completed child appears immediately in history while sibling children remain active.
- [x] 6.4 Implement combined chronological history from parent history, orchestration events and lazily requested child history, excluding workflow variables/form payloads.
- [x] 6.5 Add UI-plugin APIs and designer/runtime components for expandable subprocess progress and logs with loading, empty, failed and cancelled states.

## 7. Compatibility and verification

- [x] 7.1 Add regression tests proving ordinary definitions, running instances, chart output and history remain unchanged when subprocess support is unused.
- [x] 7.2 Verify core source remains Java 8-compatible and contains no Spring, Solon or concrete ORM imports.
- [x] 7.3 Run focused Gradle tests and compilation for core, all ORM cores, all starter/plugin variants and UI plugin modules.
- [x] 7.4 Run Bun component tests, type checks and production builds for `flovira-vue-designer`, `flovira-ui` and all designer demos.
- [x] 7.5 Run `./gradlew clean build`, `bun install --frozen-lockfile`, `bun run build`, SQL contract checks and `openspec validate add-native-dynamic-subprocess --strict`.
