## ADDED Requirements

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
The engine SHALL resume a current WAIT task by task ID and SHALL provide an instance ID plus wait key convenience operation. Resume MUST pass through the normal task transition pipeline.

#### Scenario: Resume by task ID
- **WHEN** a caller resumes an active WAIT task ID with variables
- **THEN** only that task advances and the variables participate in downstream routing

#### Scenario: Resume an already completed wait
- **WHEN** the same wait is resumed after its current task has left the active table
- **THEN** the operation reports `NOT_FOUND_OR_ALREADY_RESUMED` without advancing another task

#### Scenario: Duplicate wait key is active
- **WHEN** an instance/key lookup matches more than one current WAIT task
- **THEN** the engine rejects the operation and advances none of the matches

### Requirement: WAIT history identifies system resume
WAIT completion SHALL create ordinary task history with system resume metadata while preserving listeners and chart progression.

#### Scenario: Inspect resumed wait history
- **WHEN** a WAIT task is resumed successfully
- **THEN** its history identifies the wait key and system resume action without recording sensitive workflow variable values in the action metadata

### Requirement: Designers support WAIT consistently
The React and Vue designers SHALL insert, edit, validate, serialize and reopen WAIT nodes using the shared core extension contract.

#### Scenario: Configure a wait in a designer
- **WHEN** a designer user inserts WAIT and supplies a valid wait key
- **THEN** validation succeeds and reopening the exported definition shows the same configuration
