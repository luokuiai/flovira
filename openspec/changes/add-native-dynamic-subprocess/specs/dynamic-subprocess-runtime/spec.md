## ADDED Requirements

### Requirement: A subprocess node shall create bounded dynamic child instances
The engine SHALL read `subprocessItems` when a parent reaches a `SUB_PROCESS` task and SHALL create one ordinary child instance per valid item using the node's fixed child flow. The collection SHALL contain no more items than `flovira.subprocess-max-children`, which defaults to 128 and SHALL be configured as a positive integer. Item keys SHALL be nonblank and unique.

#### Scenario: Start parallel child instances
- **WHEN** a parent subprocess task receives three valid items
- **THEN** the engine creates one run and three child relationships
- **AND** all three child instances use the configured fixed flow
- **AND** the parent task remains active

#### Scenario: Reject an oversized collection atomically
- **WHEN** `subprocessItems` exceeds the configured per-run limit
- **THEN** initialization fails before any child instance or relationship is created

#### Scenario: Reject an unsafe configured limit
- **WHEN** `flovira.subprocess-max-children` is configured below 1
- **THEN** engine initialization fails with an explicit configuration error

### Requirement: Child definitions shall be fixed and version-pinned
The engine SHALL resolve one same-tenant runnable child definition during run initialization and SHALL persist its definition ID and version for every child relationship. An initialized run SHALL NOT switch definitions after a newer child version is published.

#### Scenario: Publish a newer child version during execution
- **WHEN** a newer child definition is published after a run created its children
- **THEN** existing children continue using the persisted definition ID/version
- **AND** a later parent run may resolve the newer runnable version

### Requirement: Initialization shall be idempotent and recoverable
The engine SHALL uniquely identify a run by tenant, parent instance, parent node and parent task. Repeating initialization or reconciliation SHALL NOT create duplicate runs, relationships or child instances.

#### Scenario: Retry after an interrupted initialization
- **WHEN** reconciliation retries an incomplete run with the same parent task and item fingerprint
- **THEN** existing child relationships are reused
- **AND** only missing safe work is completed

### Requirement: ALL completion shall resume the exact parent task once
The engine SHALL keep the parent task active until every expected child succeeds. The final successful child SHALL make the run ready and SHALL pass the stored parent task at most once. Child rejection to an earlier child node SHALL remain internal to that child.

#### Scenario: One child remains active
- **WHEN** four of five children succeeded and one remains active
- **THEN** the run reports four completed of five
- **AND** the parent task remains active

#### Scenario: Final child succeeds
- **WHEN** the final child reaches the successful terminal outcome
- **THEN** the run becomes ready to resume
- **AND** the exact parent task is passed once
- **AND** duplicate terminal notifications do not pass it again

### Requirement: Parent lifecycle actions shall govern active children
Parent termination or withdrawal SHALL cancel every active run for the parent instance. A rejection or rollback leaving one subprocess node SHALL cancel the active run associated with the exited parent task. Active children SHALL be terminated in stable order and history SHALL remain queryable.

#### Scenario: Withdraw and re-enter a subprocess node
- **WHEN** a parent is withdrawn while children are active and later reaches the subprocess node again
- **THEN** the earlier run and active children remain cancelled
- **AND** the new parent task creates a distinct run
- **AND** earlier run history is preserved

### Requirement: Runtime persistence shall be tenant-isolated and portable
Run, child and event operations SHALL enforce tenant identity and SHALL be implemented for MyBatis, MyBatis-Plus and Easy-Query on every supported database schema. Events SHALL omit child variable values and business form content.

#### Scenario: Query another tenant's child
- **WHEN** a query uses a tenant that does not own the parent or child instance
- **THEN** no run, relationship, event or workflow content is returned
