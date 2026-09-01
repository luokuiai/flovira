## Why

Flovira can currently pause only for human task handling or subprocess aggregation, so a definition cannot explicitly wait for an external business event without disguising that wait as an approval task. Node-level timeout behavior is also left entirely to applications, which makes automatic handling inconsistent and difficult for designers to express.

## What Changes

- Add a first-class `WAIT` node that persists an ordinary current task without creating a human work item and resumes through a precise, idempotent engine API.
- Add node-level timeout configuration for ordinary handling nodes and WAIT nodes, supporting a fixed duration and a node-specific timeout action.
- Persist immutable timeout snapshots when configured tasks are created so running instances are not changed by later definition edits.
- Add an engine-wide timeout execution switch that defaults to disabled; designer configuration remains serializable regardless of the runtime switch.
- Expose framework-neutral timeout scanning/execution APIs and Spring Boot/Solon scheduling adapters that start with timeout execution.
- Prefer an optional Redis scheduler lock when a supported Redis client is available, while retaining database task claims as the correctness boundary.
- Add WAIT and timeout configuration to the React and Vue designers and preserve the configuration through import/export.
- Extend all ORM adapters and database initialization scripts with timeout snapshot persistence.

## Capabilities

### New Capabilities

- `wait-node-runtime`: Definition, execution, event resume, history and designer behavior for first-class WAIT nodes.
- `task-timeout`: Optional node timeout configuration, immutable runtime snapshots, scanning, execution and framework scheduling.

### Modified Capabilities

None.

## Impact

The change adds public core APIs, a new append-only node type, timeout entity/DAO/service contracts, one runtime table, ORM implementations, starter/plugin wiring, configuration properties, and designer model/UI changes. Existing definitions remain unchanged, timeout execution is off by default, and core remains Java 8, framework-neutral, ORM-neutral and JSON-library-neutral.
