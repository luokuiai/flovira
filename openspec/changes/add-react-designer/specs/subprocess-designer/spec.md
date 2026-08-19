## ADDED Requirements

### Requirement: React designers shall configure a fixed subprocess template
The React designer SHALL allow insertion of a `SUB_PROCESS` node, SHALL load published child definitions through its instance data provider, and SHALL require selection of exactly one fixed child flow. Collection paths, item mappings, per-item flow selection and completion policy SHALL NOT be editable.

#### Scenario: Configure a React subprocess node
- **WHEN** a user inserts a subprocess node and selects a published child flow
- **THEN** the serialized node contains the fixed child flow code and engine-owned defaults
- **AND** reopening the definition restores the same selection

#### Scenario: Provider is unavailable
- **WHEN** no subprocess provider is supplied or loading fails
- **THEN** the designer exposes the loading error without inventing fallback definitions
- **AND** existing subprocess values remain readable and exportable
