# task-timeout Specification

## Purpose
Define optional node timeout configuration and cluster-safe execution across supported framework adapters.

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
The backend SHALL expose `flovira.timeout.enabled`, default it to false, and preserve node timeout configuration regardless of the switch.

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
- **THEN** the claim is released for retry and the failure is returned or logged explicitly

#### Scenario: WAIT timeout action fails
- **WHEN** a WAIT workflow transition throws after its WAIT claim
- **THEN** the transaction rolls back the claim so a later signal or timeout scan can retry

#### Scenario: Ordinary claim owner terminates
- **WHEN** a non-WAIT timeout claim remains running past the configured claim timeout
- **THEN** a later scan makes the task eligible for recovery

### Requirement: Redis scheduler locking is optional and preferred
The timeout service SHALL support a framework-neutral scheduler lock. When a supported Redis client is available, the framework adapter SHALL register a Redis implementation automatically and the timeout service SHALL acquire it before querying due tasks. Database task claims SHALL remain mandatory.

#### Scenario: Multiple instances share Redis
- **WHEN** two instances start the same timeout scan and one instance holds the Redis scheduler lock
- **THEN** the other instance skips that scan without querying due tasks

#### Scenario: Redis is not configured
- **WHEN** no scheduler lock implementation is available
- **THEN** timeout scanning continues and database atomic claims prevent duplicate task execution

#### Scenario: Redis is temporarily unavailable
- **WHEN** acquiring or releasing the Redis scheduler lock fails
- **THEN** the failure is logged and scanning falls back to database atomic claims

#### Scenario: Host supplies a custom lock
- **WHEN** the host registers a scheduler lock implementation
- **THEN** it takes precedence over an automatically detected Redis implementation

### Requirement: Framework scheduling follows timeout execution
Core SHALL expose a framework-neutral due-task execution API. Spring and Solon adapters SHALL invoke it at the `flovira.timeout.scan-interval-seconds` interval while timeout execution is enabled, without requiring a second scheduler switch. The interval SHALL default to 60 seconds and SHALL be at least 1 second.

#### Scenario: Timeout execution is enabled
- **WHEN** `flovira.timeout.enabled` is true in a Spring or Solon integration
- **THEN** the corresponding scheduling adapter scans due tasks at the configured interval

#### Scenario: Core is integrated without a framework scheduling adapter
- **WHEN** a host embeds core without the Spring or Solon adapter
- **THEN** the host can call the same timeout service without importing a framework into core

### Requirement: Designers configure supported timeout actions
React and Vue designers SHALL expose timeout enablement, positive duration, duration unit and node-compatible actions, and SHALL preserve configuration while backend execution is disabled.

#### Scenario: Reopen timeout configuration
- **WHEN** a definition with timeout configuration is exported and reopened
- **THEN** all timeout values are restored without requiring the backend scheduler to be enabled
