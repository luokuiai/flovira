# subprocess-designer Specification

## Purpose
Define consistent fixed-flow subprocess configuration, validation, and presentation across supported designers.

## Requirements

### Requirement: Designers shall configure a fixed subprocess template
The classic and mimic designers SHALL allow insertion of a `SUB_PROCESS` node and SHALL require selection of exactly one fixed child flow. Collection paths, item mappings, per-item flow selection and completion policy SHALL NOT be editable.

#### Scenario: Configure a subprocess node
- **WHEN** a designer inserts a subprocess node and selects a published child flow
- **THEN** the serialized node contains the fixed child flow code and engine-owned defaults
- **AND** reopening the definition restores the same configuration

### Requirement: Subprocess nodes shall have distinct consistent presentation
Both designer modes SHALL render subprocess nodes with the same node identity and a light purple primary background with readable white title text. The node SHALL remain distinguishable in normal, selected and dark modes.

#### Scenario: Render in both design modes
- **WHEN** the same definition is opened in classic and mimic modes
- **THEN** each mode displays a subprocess node with equivalent label and fixed-flow summary

### Requirement: Publication shall validate subprocess dependencies
Publishing SHALL reject a missing, disabled, cross-tenant or recursive fixed child reference. Validation SHALL traverse indirect fixed subprocess references and SHALL preserve the previously runnable definition when validation fails.

#### Scenario: Reject an indirect cycle
- **WHEN** flow A references flow B and B directly or indirectly references A
- **THEN** publishing the new definition fails with a dependency-cycle error
- **AND** the previous runnable definition remains unchanged

### Requirement: Existing definitions shall remain compatible
Definitions without subprocess nodes SHALL serialize, publish and execute unchanged. Unknown legacy subprocess extension fields SHALL remain readable for diagnostics but unsupported dynamic paths or variable-selection modes SHALL fail publication.

#### Scenario: Open an ordinary legacy definition
- **WHEN** a definition contains only existing node types
- **THEN** the designer and engine preserve its JSON and execution behavior

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
