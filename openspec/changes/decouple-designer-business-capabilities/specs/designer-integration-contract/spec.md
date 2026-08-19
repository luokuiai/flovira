## ADDED Requirements

### Requirement: Versioned capability manifest
Flovira SHALL expose a versioned manifest describing the node types, approver strategies, approval modes, return policies, timeout support, operations, and business resource types available to a designer.

#### Scenario: Host narrows available features
- **WHEN** a host declares only user and role approver strategies and excludes wait nodes
- **THEN** a consuming designer displays only those strategies and omits the wait-node creation control

#### Scenario: No host capability provider exists
- **WHEN** no host capability provider is registered
- **THEN** Flovira returns its built-in default capability manifest

### Requirement: Unified business resource queries
Flovira SHALL define a host-neutral provider for querying users, roles, organizations, form fields, dictionaries, and subprocess definitions with stable string identifiers and paged results.

#### Scenario: Query organization-scoped users
- **WHEN** the designer requests user resources with an organization scope and search text
- **THEN** the host provider receives the resource type, scope, search text, and paging values without any Intelliconf-specific DTO dependency

### Requirement: Relationship resolution contract
Flovira SHALL define semantic relationship queries for department leaders, supervising leaders, role members, and organization chains without coupling the engine to a host persistence model.

#### Scenario: Resolve department leader
- **WHEN** an approval strategy resolves the department leader for a process participant
- **THEN** Flovira delegates the semantic relationship query to the registered host provider and receives stable subject references

### Requirement: Engine-owned execution semantics
Capability and data providers MUST NOT execute workflow transitions, countersign calculations, empty-approver behavior, timeout actions, or definition serialization.

#### Scenario: Host supplies role members
- **WHEN** a host returns the members of a role
- **THEN** Flovira creates and advances tasks using its own runtime rules

### Requirement: Shared frontend contract
The Vue and React packages SHALL export equivalent TypeScript contracts for capability discovery and business data access.

#### Scenario: Reuse a host adapter
- **WHEN** a host implements the shared provider shape for one designer package
- **THEN** the same response models and capability codes can be used by the other package

