## ADDED Requirements

### Requirement: Versioned capability manifest
Flovira SHALL expose a versioned manifest describing the node types, approver strategies, approval modes, return policies, timeout support, operations, and business resource types available to a designer.

Each approver strategy SHALL declare its semantic code, display name, selection type, optional resource and relation types, and whether multiple resources may be selected.

#### Scenario: Host narrows available features
- **WHEN** a host declares only user and role approver strategies and excludes wait nodes
- **THEN** a consuming designer displays only those strategies and omits the wait-node creation control

#### Scenario: Host declares a relationship strategy
- **WHEN** a host declares a department-leader strategy with selection type `RELATION` and relation type `DEPARTMENT_LEADER`
- **THEN** both designers offer the strategy without a resource picker and serialize its relation type for runtime resolution

#### Scenario: No host capability provider exists
- **WHEN** no host capability provider is registered
- **THEN** Flovira returns its built-in default capability manifest

### Requirement: Unified business resource queries
Flovira SHALL define a host-neutral provider for querying users, roles, organizations, form fields, dictionaries, and subprocess definitions with stable string identifiers and paged results.

#### Scenario: Query organization-scoped users
- **WHEN** the designer requests user resources with an organization scope and search text
- **THEN** the host provider receives the resource type, scope, search text, and paging values without any Intelliconf-specific DTO dependency

### Requirement: Relationship resolution contract
Flovira SHALL define semantic relationship queries for department leaders, supervising leaders, role members, organization members, and organization chains without coupling the engine to a host persistence model.

#### Scenario: Resolve department leader
- **WHEN** an approval strategy resolves the department leader for a process participant
- **THEN** Flovira delegates the semantic relationship query to the registered host provider and receives stable subject references

### Requirement: Engine-owned execution semantics
Capability and data providers MUST NOT execute workflow transitions, countersign calculations, empty-approver behavior, timeout actions, or definition serialization.

#### Scenario: Host supplies role members
- **WHEN** a host returns the members of a role
- **THEN** Flovira creates and advances tasks using its own runtime rules

### Requirement: Versioned approver rule
Vue, React, and the core engine SHALL use the same versioned approver rule containing a semantic strategy, selection type, optional relation type, typed subject references, and an optional expression.

#### Scenario: Save selected role approvers
- **WHEN** a designer saves a role-based approval node
- **THEN** it serializes `approverRule` with strategy `ROLE` and typed role subject references without framework-specific fields

#### Scenario: Read a legacy definition
- **WHEN** an approval node has no `approverRule`
- **THEN** the engine uses its existing `permissionFlag` behavior

### Requirement: Runtime approver resolution
Flovira SHALL resolve a versioned approver rule to final unique task permission identifiers before task persistence.

#### Scenario: Resolve role members
- **WHEN** an approval node uses strategy `ROLE`
- **THEN** Flovira queries `ROLE_MEMBERS` for every selected role, validates the returned subjects, removes duplicate identifiers, and creates the task with the resolved identifiers

#### Scenario: Relationship provider is unavailable
- **WHEN** a relationship-based rule executes without a registered business relation provider
- **THEN** task creation fails explicitly instead of creating an unclaimable task

#### Scenario: Resolution returns no approvers
- **WHEN** a configured rule resolves to no valid identifiers
- **THEN** task creation fails explicitly

### Requirement: Shared frontend contract
The Vue and React packages SHALL export equivalent TypeScript contracts for capability discovery and business data access.

#### Scenario: Reuse a host adapter
- **WHEN** a host implements the shared provider shape for one designer package
- **THEN** the same response models and capability codes can be used by the other package

#### Scenario: Configure approvers in either designer
- **WHEN** a user selects the same strategy and business subjects in Vue or React
- **THEN** both packages query the same resource type and serialize the same `approverRule` shape
