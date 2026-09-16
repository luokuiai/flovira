## MODIFIED Requirements

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
- **WHEN** a WAIT task is resumed successfully by an external signal
- **THEN** its history identifies the wait key and `WAIT_RESUME` action without recording sensitive workflow variable values in the action metadata

#### Scenario: Inspect timeout-resumed wait history
- **WHEN** a WAIT task is resumed successfully by its configured timeout
- **THEN** its history identifies the wait key and `WAIT_TIMEOUT` action

## REMOVED Requirements

### Requirement: Pre-claimed WAIT tasks can be resumed publicly

**Reason**: A public pre-claimed resume operation permits callers to bypass the WAIT task's atomic state transition and can advance the same workflow more than once.

**Migration**: Call `resumeTask(taskId, variables)` for direct recovery or `resume(instanceId, waitKey, variables)` for event-key recovery; timeout execution uses the engine's internal WAIT recovery path.
