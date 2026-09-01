## 1. Instance Snapshot Runtime

- [x] 1.1 Extend WAIT configuration parsing to resolve a node from `Instance.defJson` without consulting the live definition
- [x] 1.2 Refactor task-ID and instance/key recovery to share snapshot-based matching and one atomic WAIT claim
- [x] 1.3 Remove the public pre-claimed recovery API and add distinct external/timeout WAIT history metadata

## 2. Task Progression And Query

- [x] 2.1 Add a system-task progression entry that centralizes internal skip flags while reusing the normal transition pipeline
- [x] 2.2 Add an instance-and-node-type task query across the core DAO contract and MyBatis and MyBatis-Plus implementations
- [x] 2.3 Route WAIT and ordinary timeout actions through the appropriate atomic claim and shared system progression behavior

## 3. Database Performance

- [x] 3.1 Add the `(tenant_id, instance_id, node_type)` task index to MySQL and PostgreSQL V1 initialization scripts
- [x] 3.2 Add the same task index to Oracle and SQL Server initialization scripts using each database's syntax

## 4. Verification

- [x] 4.1 Add or update focused tests for snapshot stability, invalid snapshots, WAIT signal/timeout races, idempotency and history action metadata
- [x] 4.2 Compile core and all three ORM core modules, run focused core tests, and validate OpenSpec plus SQL/index consistency
