## MODIFIED Requirements

### Requirement: Unified business resource queries
Flovira SHALL define a host-neutral provider for querying users, roles, organizations, form fields, dictionaries, and subprocess definitions with stable string identifiers and paged results. Form selection SHALL support Flovira-managed forms and MAY be supplemented or replaced by host-provided form resources.

#### Scenario: Query organization-scoped users
- **WHEN** the designer requests user resources with an organization scope and search text
- **THEN** the host provider receives the resource type, scope, search text, and paging values without any Intelliconf-specific DTO dependency

#### Scenario: Query managed forms without a host form provider
- **WHEN** the designer requests form resources and the host does not provide them
- **THEN** Flovira returns selectable published managed forms for the current tenant

#### Scenario: Query host-provided forms
- **WHEN** the host supplies form resources
- **THEN** the designer can select their stable string identifiers without enabling a `form_custom` mode

