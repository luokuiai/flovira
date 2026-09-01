# wait-node-runtime Specification

## Purpose
Define WAIT node persistence, validation, precise resumption, history, and cross-designer behavior.

## Requirements

### Requirement: WAIT is a first-class executable node
The engine SHALL append a WAIT node type without changing existing node type keys. Reaching a WAIT node MUST persist a current task and MUST NOT require a human assignee.

#### Scenario: Enter a wait node
- **WHEN** execution reaches a valid WAIT node
- **THEN** one current WAIT task is persisted and the instance remains active at that node

#### Scenario: Existing definition remains compatible
- **WHEN** a definition contains no WAIT node
- **THEN** its task creation, passage and history behavior remains unchanged

### Requirement: WAIT configuration has a stable event key
A WAIT node SHALL store a versioned configuration with one required normalized wait key in `Node.ext`, and invalid configuration MUST be rejected before publication or execution.

#### Scenario: Save a valid wait key
- **WHEN** a designer saves a WAIT node with a syntactically valid wait key
- **THEN** import and export preserve the same key and schema version

#### Scenario: Reject a missing wait key
- **WHEN** a WAIT node has no wait key
- **THEN** definition validation fails with the affected node identified

### Requirement: WAIT resume is precise and idempotent
The engine SHALL resume a current WAIT task by task ID and SHALL provide an instance ID plus wait key convenience operation. Every resume source MUST atomically claim the WAIT task before passing through the normal task transition pipeline, and wait-key matching MUST use the immutable definition snapshot stored by the running instance.

#### Scenario: Resume by task ID
- **WHEN** a caller resumes an active WAIT task ID with variables
- **THEN** only that task is atomically claimed and advanced, and the variables participate in downstream routing

#### Scenario: Definition changes after instance start
- **WHEN** a WAIT node's live definition is edited after an instance has reached that node
- **THEN** resuming the instance uses the wait key stored in that instance's definition snapshot

#### Scenario: Resume an already completed wait
- **WHEN** the same wait is resumed after its current task has left the active table or was claimed by another source
- **THEN** the operation reports `NOT_FOUND_OR_ALREADY_RESUMED` without advancing another task

#### Scenario: Duplicate wait key is active
- **WHEN** an instance/key lookup matches more than one current WAIT task
- **THEN** the engine rejects the operation and advances none of the matches

#### Scenario: Snapshot configuration is invalid
- **WHEN** the instance definition snapshot is missing, malformed or does not contain the task's WAIT node configuration
- **THEN** the resume fails explicitly and does not fall back to the mutable live definition

### Requirement: WAIT history identifies system resume
WAIT completion SHALL create ordinary task history with system resume metadata while preserving listeners and chart progression. History metadata MUST distinguish an external WAIT resume from a timeout-triggered WAIT resume.

#### Scenario: Inspect externally resumed wait history
- **WHEN** a WAIT task is resumed successfully
- **THEN** its history identifies the wait key and `WAIT_RESUME` action without recording sensitive workflow variable values in the action metadata

#### Scenario: Inspect timeout-resumed wait history
- **WHEN** a WAIT task is resumed successfully by its configured timeout
- **THEN** its history identifies the wait key and `WAIT_TIMEOUT` action

### Requirement: Designers support WAIT consistently
The React and Vue designers SHALL insert, edit, validate, serialize and reopen WAIT nodes using the shared core extension contract.

#### Scenario: Configure a wait in a designer
- **WHEN** a designer user inserts WAIT and supplies a valid wait key
- **THEN** validation succeeds and reopening the exported definition shows the same configuration
