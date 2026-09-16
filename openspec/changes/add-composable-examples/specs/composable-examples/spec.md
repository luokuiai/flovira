## Purpose

Define a composable example matrix whose database backends and framework/UI frontends can run independently in any supported pairing while demonstrating the same Flovira workflow lifecycle.

## ADDED Requirements

### Requirement: Interchangeable backend variants
The examples SHALL provide PostgreSQL and MySQL backend variants that expose the same versioned HTTP contract and observable workflow behavior.

#### Scenario: Run the PostgreSQL backend
- **WHEN** a developer starts the PostgreSQL variant with its documented database service
- **THEN** the backend initializes from the authoritative PostgreSQL Flovira schema and exposes the example health and API endpoints

#### Scenario: Run the MySQL backend
- **WHEN** a developer starts the MySQL variant with its documented database service
- **THEN** the backend initializes from the authoritative MySQL Flovira schema and exposes the same example health and API endpoints

#### Scenario: Database is unavailable
- **WHEN** either backend starts without its configured database being reachable
- **THEN** startup fails visibly rather than simulating a working workflow

### Requirement: Interchangeable frontend variants
The examples SHALL provide React + Lumen, React + Ant Design, and Vue + Ant Design Vue frontends, and each frontend SHALL accept a configurable backend target without database-specific behavior.

#### Scenario: Select a backend target
- **WHEN** a developer starts any frontend with the documented PostgreSQL or MySQL backend target
- **THEN** all designer and lifecycle requests are sent to that backend using the shared HTTP contract

#### Scenario: Backend request fails
- **WHEN** a frontend operation receives an unsuccessful backend response
- **THEN** the frontend presents an actionable failure and does not report success

### Requirement: All backend and frontend pairings work
Every supported frontend SHALL complete the same user journey against either supported backend, yielding a six-combination compatibility matrix.

#### Scenario: Verify a matrix pairing
- **WHEN** one of the three frontends is configured for either backend
- **THEN** the user can load resources, save and publish a definition, start a process, handle tasks, and view progress without changing source code

#### Scenario: Switch database implementations
- **WHEN** a frontend is redirected from a running PostgreSQL backend to a running MySQL backend
- **THEN** its screens and user operations remain available with only the backend target configuration changed

### Requirement: Persistent workflow lifecycle
Both backends SHALL persist definitions, instances, current tasks, and history and SHALL expose a coherent lifecycle for design, save, publish, start, approve, reject, and progress inspection.

#### Scenario: Approve a process to completion
- **WHEN** selected demo users approve every pending task in a published sample process
- **THEN** the process completes and its progress and history identify completed nodes and handlers

#### Scenario: Reject a pending task
- **WHEN** an authorized demo user rejects a pending task with a comment
- **THEN** the resulting process state and history are visible through the same API and each frontend

#### Scenario: State survives restart
- **WHEN** a backend restarts without resetting its database
- **THEN** previously saved definitions, instances, tasks, and history remain available

#### Scenario: Start an unpublished definition
- **WHEN** a user attempts to start a process using an unpublished definition
- **THEN** the backend rejects the request with an explicit validation error

### Requirement: Shared host integration semantics
Both backends SHALL provide the same deterministic development identities, organizations, roles, condition fields, sample business records, designer resources, and task authorization behavior.

#### Scenario: Switch active demo user
- **WHEN** a frontend selects a different stable demo identity
- **THEN** the task list reflects tasks that identity may handle regardless of the selected backend

#### Scenario: Configure approvers
- **WHEN** a user opens an approver resource picker in any frontend
- **THEN** it displays resources supplied by the backend through Flovira's designer provider contract using stable identifiers

### Requirement: Host-owned business data remains separate
The examples SHALL store sample business records in host-owned tables and SHALL associate workflow records through stable string business and form identifiers.

#### Scenario: View process business details
- **WHEN** a user opens a sample process instance
- **THEN** the frontend combines host-owned business details with Flovira progress data
- **AND** Flovira does not own the form definition or business record

### Requirement: Reproducible independent operation
The repository SHALL document commands to start each database, backend, and frontend independently, including distinct default ports where simultaneous variants are supported and a deliberate reset procedure for each database.

#### Scenario: Start a chosen combination
- **WHEN** a developer follows the matrix documentation from a clean checkout
- **THEN** only the selected database, backend, and frontend are required to complete the workflow journey
- **AND** no unpublished external Flovira artifact is required

### Requirement: Development boundary is explicit
Documentation SHALL identify sample credentials, demo identity propagation, seed data, proxy or CORS behavior, and reset commands as development-only mechanisms.

#### Scenario: Evaluate production reuse
- **WHEN** a developer reads the example documentation
- **THEN** reusable Flovira integration code is distinguished from demo-only security, data, credentials, and local networking choices
