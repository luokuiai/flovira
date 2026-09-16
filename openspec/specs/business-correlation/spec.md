# business-correlation Specification

## Purpose
Define normalized, tenant-scoped business keys and indexed runtime lookup across process instances and tasks.

## Requirements

### Requirement: Process instances persist a structured business key
The engine SHALL persist business type and business ID as separate process-instance fields and MUST NOT require callers to concatenate them.

#### Scenario: Start with an explicit business type
- **WHEN** a caller starts a process with business type `PURCHASE_ORDER` and business ID `1001`
- **THEN** the instance persists both values independently

#### Scenario: Existing start API is used
- **WHEN** a caller uses the start API that only supplies a business ID
- **THEN** the instance uses the selected definition's flow code as its business type

### Requirement: Business keys locate process runtime data
Core services SHALL query instances, current tasks and historical tasks by business type plus business ID while applying the existing tenant boundary.

#### Scenario: Query current tasks
- **WHEN** a business key has active process instances
- **THEN** the task service returns current tasks belonging to those instance IDs

#### Scenario: Query history after task completion
- **WHEN** tasks for a business key have completed
- **THEN** the history service returns historical tasks belonging to those instance IDs

#### Scenario: Same business ID exists in different domains
- **WHEN** two instances share a business ID but have different business types
- **THEN** each business-key query returns only its matching instances and tasks

### Requirement: Task tables remain normalized
Current-task and historical-task tables SHALL reference business identity through `instance_id` and MUST NOT persist duplicate business type or business ID columns.

#### Scenario: A process creates multiple tasks
- **WHEN** one business instance creates sequential or parallel tasks
- **THEN** every task references the instance without copying its business key

### Requirement: Business-key queries are indexed
Database initialization scripts SHALL index tenant ID, business type and business ID on the instance table, and SHALL index instance ID access used for task history lookup.

#### Scenario: Locate a tenant-scoped business process
- **WHEN** the engine queries a business key inside a tenant
- **THEN** the database can use the instance business-key index before loading related tasks
