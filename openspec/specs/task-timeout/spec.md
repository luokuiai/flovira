# task-timeout Specification

## Purpose
Define optional node timeout configuration and cluster-safe execution triggered by host applications.

## Requirements

### Requirement: Node timeout configuration is optional
BETWEEN and WAIT nodes SHALL support optional fixed-duration timeout configuration. BETWEEN SHALL support `AUTO_PASS` and `AUTO_REJECT`; WAIT SHALL support `RESUME_WAIT`.

#### Scenario: Configure approval auto pass
- **WHEN** a BETWEEN node enables a positive fixed duration with `AUTO_PASS`
- **THEN** the definition preserves and validates that timeout configuration

#### Scenario: Configure wait resume
- **WHEN** a WAIT node enables a positive fixed duration with `RESUME_WAIT`
- **THEN** the timeout action uses that node's configured wait key

#### Scenario: Reject an invalid action combination
- **WHEN** a non-WAIT node selects `RESUME_WAIT` or a WAIT node selects an approval action
- **THEN** definition validation fails before publication

### Requirement: Backend timeout execution has a safe global switch
The backend SHALL expose a process-wide `FlowEngine.setTimeoutEnabled(boolean)` switch, default it to false, expose no `flovira.timeout.*` Spring Boot properties, and preserve node timeout configuration regardless of the switch.

#### Scenario: Timeout backend is disabled
- **WHEN** a configured node creates a task while the global switch is false
- **THEN** the task receives no executable timeout snapshot and no automatic transition occurs

#### Scenario: Timeout backend is enabled
- **WHEN** a configured node creates a task while the global switch is true
- **THEN** the task stores an immutable deadline and action snapshot

### Requirement: Timeout snapshots belong to active tasks
The engine SHALL freeze timeout deadline, action, configuration, status and claim time on the current task and SHALL scan only active due tasks. WAIT timeout execution MUST use the instance definition snapshot for WAIT configuration while retaining the task's immutable timeout deadline and action.

#### Scenario: Definition changes after task creation
- **WHEN** a definition's timeout or WAIT configuration changes after a task was created
- **THEN** that task retains its original timeout deadline and action and resolves its wait key from the instance definition snapshot

#### Scenario: Task finishes before deadline
- **WHEN** a task is completed manually before its deadline
- **THEN** it leaves the current task table and cannot be executed by the timeout scanner

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
- **THEN** the transaction rolls back the claim for retry and the failure is returned or logged explicitly

#### Scenario: WAIT timeout action fails
- **WHEN** a WAIT workflow transition throws after its WAIT claim
- **THEN** the transaction rolls back the claim so a later signal or timeout scan can retry

#### Scenario: Ordinary claim owner terminates
- **WHEN** a non-WAIT timeout claim remains running past the engine claim recovery window
- **THEN** a later scan makes the task eligible for recovery

### Requirement: Hosts must integrate timeout scheduling
Core SHALL expose batch `executeDue(now, batchSize)` and single-task `executeTimeout(taskId)` APIs. Flovira SHALL NOT register periodic invokers or Redis scheduler locks. Enabling the process-wide timeout switch enables snapshots and explicit execution, not scheduling. Hosts MUST supply scheduling or delayed-message delivery, tenant context, retries and monitoring. Documentation and both designers MUST display this requirement.

#### Scenario: Timeout execution is enabled without host scheduling
- **WHEN** a host enables timeout execution but never invokes the APIs
- **THEN** deadlines are persisted but no automatic transition occurs

#### Scenario: A host delivers an early or duplicate message
- **WHEN** a single-task timeout invocation targets a missing, completed, not-yet-due or already-claimed task
- **THEN** the API returns false without advancing the task

#### Scenario: A host invokes a due task
- **WHEN** a supported due task is available
- **THEN** the engine claims and advances it in one transaction, returns true on success, and propagates failures after rollback

#### Scenario: A slow ordinary timeout is still executing
- **WHEN** another worker tries to recover its claim
- **THEN** the uncommitted database claim remains locked until the first execution commits or rolls back

### Requirement: Designers configure supported timeout actions
React and Vue designers SHALL expose timeout enablement, positive duration, duration unit and node-compatible actions, and SHALL preserve configuration while backend execution is disabled.

#### Scenario: Reopen timeout configuration
- **WHEN** a definition with timeout configuration is exported and reopened
- **THEN** all timeout values are restored without requiring host scheduling to be active
