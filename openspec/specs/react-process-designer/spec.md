# react-process-designer Specification

## Purpose
Define the embeddable React process designer's editing, integration, compatibility, and scale requirements.

## Requirements

### Requirement: React hosts shall embed a native process designer
The system SHALL provide a React component package named `@luokuiaiai/flovira-react-designer` that renders without a router, application shell, backend URL, or host Tailwind configuration.

#### Scenario: Embed the designer
- **WHEN** a React host imports the component and its stylesheet and supplies an initial definition
- **THEN** the designer renders the definition as an editable process canvas
- **AND** no Vue runtime or application-level provider is required

### Requirement: The designer shall support approval-flow editing
The designer SHALL support start, approval, subprocess, exclusive gateway, parallel gateway and end nodes with automatic sequence and branch layout. Users SHALL be able to insert, select, configure and delete eligible nodes.

#### Scenario: Insert and configure an approval node
- **WHEN** a user inserts an approval node between two existing nodes and edits its name and handler expression
- **THEN** the canvas immediately reflects the change
- **AND** exported Flovira JSON contains the inserted node and connecting skips

#### Scenario: Edit a branch
- **WHEN** a user adds or removes a gateway branch and configures its condition
- **THEN** all branches remain visibly joined to the same gateway continuation
- **AND** the exported skips retain each configured condition

### Requirement: Definitions shall round-trip through the public API
The component SHALL support controlled and uncontrolled definitions, preserve unknown JSON fields, emit changes, export a JSON string, import compatible JSON, and validate structural requirements.

#### Scenario: Round-trip an existing definition
- **WHEN** a host loads a definition containing known and unknown extension properties and exports it without editing those properties
- **THEN** the exported definition preserves those extension properties

#### Scenario: Reject an invalid structure
- **WHEN** validation finds a missing start or end node, an unconnected node, or an invalid subprocess reference
- **THEN** validation returns a failed result with actionable issue locations
- **AND** the host can prevent persistence

### Requirement: Editing history and viewport tools shall be available
The designer SHALL expose undo, redo, zoom in, zoom out, reset viewport and locate-start commands through both visible icon controls and an imperative ref API.

#### Scenario: Undo an edit
- **WHEN** a user changes a node and invokes undo
- **THEN** the previous definition and dirty state are restored
- **AND** redo can reapply the reverted change

### Requirement: Data access shall be injectable
The component SHALL accept an instance-scoped partial `DesignerDataProvider` and SHALL use it to load published subprocess definitions without requiring a global singleton.

#### Scenario: Load tenant-specific subprocess options
- **WHEN** two designer instances receive different providers
- **THEN** each instance displays only the subprocess definitions returned by its own provider

### Requirement: Large approval definitions shall remain operable
The canvas SHALL render a definition containing 128 sequential subprocess or approval nodes without truncating the definition or requiring all nodes to have stored coordinates.

#### Scenario: Open a 128-node definition
- **WHEN** a definition with 128 sequential business nodes is loaded
- **THEN** all nodes remain present in the exported JSON
- **AND** viewport controls and node selection remain available
