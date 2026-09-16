## Purpose

Provide versioned workflow form metadata and content that Flovira can manage while workflow definitions and task snapshots reference forms through a single stable identifier.

## ADDED Requirements

### Requirement: Versioned managed forms
Flovira SHALL persist forms with stable identity, code, name, version, publication status, renderable content, extension data, tenant identity, audit data, and logical deletion. Managed forms SHALL NOT store a form-mode discriminator or host page address.

#### Scenario: Create a managed form version
- **WHEN** a client saves a form with its code, name, version, and content
- **THEN** Flovira persists the form version and returns its stable identifier

#### Scenario: Isolate tenant forms
- **WHEN** two tenants use the same form code and version
- **THEN** each tenant reads and modifies only its own form record

#### Scenario: Keep host pages outside managed forms
- **WHEN** a host supplies an external form resource
- **THEN** Flovira stores its opaque `formId` reference without adding `form_type` or `form_path` to `flow_form`

### Requirement: Publish managed forms
Flovira SHALL support publication and invalidation of managed form versions without a `form_custom` discriminator.

#### Scenario: Publish a form version
- **WHEN** a draft form version is published
- **THEN** that version becomes available for workflow selection under its tenant

### Requirement: Reference forms by formId
Workflow definitions and nodes SHALL reference a managed form through `formId`; a non-empty node reference SHALL override the definition reference, and an empty node reference SHALL inherit it.

#### Scenario: Approval node inherits workflow form
- **WHEN** an approval node has no `formId`
- **THEN** its task stores the definition's effective `formId`

#### Scenario: Approval node overrides workflow form
- **WHEN** an approval node has its own `formId`
- **THEN** its task and history snapshot store the node's `formId`

### Requirement: Managed form service API
Flovira SHALL expose framework-independent form creation, query, update, publication, and deletion contracts through the engine service facade.

#### Scenario: Obtain form service
- **WHEN** an application obtains the form service from the engine facade
- **THEN** it can manage forms without depending on a concrete ORM implementation

### Requirement: Designer backend form access
The designer backend SHALL expose managed form metadata/content operations while preserving host authorization and tenant isolation.

#### Scenario: Load managed form content
- **WHEN** an authorized designer requests a managed form by identifier
- **THEN** the backend returns that tenant's form content or an explicit not-found result

#### Scenario: Save managed form content
- **WHEN** an authorized designer saves valid managed form content
- **THEN** the backend persists it through the form service

### Requirement: Supported database parity
MySQL, PostgreSQL, and Oracle fresh-install schemas SHALL define equivalent `flow_form` columns, logical-deletion behavior, tenant-aware lookup indexes, and no foreign keys.

#### Scenario: Compare supported schemas
- **WHEN** supported schema definitions are statically compared
- **THEN** all three contain equivalent form-management fields and constraints appropriate to their dialect
