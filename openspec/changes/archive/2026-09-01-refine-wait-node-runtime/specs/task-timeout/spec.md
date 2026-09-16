## MODIFIED Requirements

### Requirement: Due timeout execution is cluster-safe
The timeout service SHALL atomically claim due tasks before execution, recover stale claims and reuse normal workflow transition services. A WAIT timeout MUST compete through the same atomic WAIT claim as external resume signals instead of using a separate pre-claim operation.

#### Scenario: Two scanners observe one ordinary due task
- **WHEN** two backend instances scan the same due non-WAIT task concurrently
- **THEN** at most one instance claims and executes its timeout action

#### Scenario: Scanner and signal observe one due WAIT task
- **WHEN** a timeout scanner and an external signal concurrently attempt to resume the same WAIT task
- **THEN** at most one source atomically claims and advances the WAIT task

#### Scenario: Ordinary claimed action fails
- **WHEN** a non-WAIT workflow transition throws after a task is claimed
- **THEN** the claim is released for retry and the failure is returned or logged explicitly

#### Scenario: WAIT timeout action fails
- **WHEN** a WAIT workflow transition throws after its WAIT claim
- **THEN** the transaction rolls back the claim so a later signal or timeout scan can retry

#### Scenario: Ordinary claim owner terminates
- **WHEN** a non-WAIT timeout claim remains running past the configured claim timeout
- **THEN** a later scan makes the task eligible for recovery

### Requirement: Timeout snapshots belong to active tasks
The engine SHALL freeze timeout deadline, action, configuration, status and claim time on the current task and SHALL scan only active due tasks. WAIT timeout execution MUST use the instance definition snapshot for WAIT configuration while retaining the task's immutable timeout deadline and action.

#### Scenario: Definition changes after task creation
- **WHEN** a definition's timeout or WAIT configuration changes after a task was created
- **THEN** that task retains its original timeout deadline and action and resolves its wait key from the instance definition snapshot

#### Scenario: Task finishes before deadline
- **WHEN** a task is completed manually before its deadline
- **THEN** it leaves the current task table and cannot be executed by the timeout scanner
