## 1. Core contracts and validation

- [x] 1.1 Append WAIT node type and add Java 8-compatible wait/timeout configuration DTOs, enums and parsers.
- [x] 1.2 Add definition validation for WAIT keys, timeout duration/action combinations and unsupported node types.
- [x] 1.3 Add `WaitService` resume APIs/results and expose the service through `FlowEngine`.
- [x] 1.4 Extend task entities and DAO contracts with immutable timeout snapshot and atomic claim operations.
- [x] 1.5 Add `TimeoutService` due-scan, stale-claim recovery and built-in action execution APIs.

## 2. Runtime integration

- [x] 2.1 Create durable assignee-free WAIT tasks and preserve ordinary listeners, variables, charts and history.
- [x] 2.2 Freeze timeout fields when enabled configured tasks are created and leave them empty when the backend switch is disabled.
- [x] 2.3 Record system WAIT resume and timeout action metadata in ordinary task history.
- [x] 2.4 Add focused core tests for WAIT entry/resume/idempotency and timeout configuration/execution/races.

## 3. ORM and database support

- [x] 3.1 Add timeout fields and claim queries to MyBatis task entity, mapper and DAO.
- [x] 3.2 Add equivalent timeout fields and claim operations to MyBatis-Plus.
- [x] 3.3 Add equivalent timeout fields and claim operations to Easy-Query.
- [x] 3.4 Update MySQL, PostgreSQL, Oracle and SQL Server initialization schemas with timeout columns and due-scan indexes.

## 4. Framework configuration and scheduling

- [x] 4.1 Add default-off timeout configuration, second-based scan interval, batch size and claim timeout to `Flovira`.
- [x] 4.2 Wire WAIT/timeout services and task suppliers consistently through all Spring Boot and Solon ORM variants.
- [x] 4.3 Add Spring and Solon periodic invokers that follow the timeout execution switch and delegate to the framework-neutral timeout service.
- [x] 4.4 Add an optional scheduler-lock contract, automatic Spring Redis and Solon Redisson adapters, custom-provider priority and database-claim fallback.

## 5. Designer support

- [x] 5.1 Add WAIT insertion, configuration, serialization and validation to the React designer.
- [x] 5.2 Add node timeout controls and compatible action choices to the React designer.
- [x] 5.3 Add WAIT node rendering/configuration to both Vue designer modes.
- [x] 5.4 Add node timeout controls and import/export preservation to the Vue designer.
- [x] 5.5 Add or update frontend tests for WAIT and timeout round trips and validation.

## 6. Verification

- [x] 6.1 Compile and test core plus all three ORM core modules.
- [x] 6.2 Compile representative Spring Boot 2/3/4 and Solon adapters and verify no framework imports entered core.
- [x] 6.3 Run React and Vue tests, type checks and production builds.
- [x] 6.4 Validate four database schemas and run `openspec validate add-wait-node-and-task-timeout --strict`.
- [x] 6.5 Test scheduler lock contention/failure behavior and compile the Redis-aware Spring/Solon adapters.
