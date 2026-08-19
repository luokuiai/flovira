## ADDED Requirements

### Requirement: Optional Spring REST bridge
The Spring Web module SHALL expose capability and business data provider operations as an optional REST bridge and SHALL contain no frontend rendering or static assets.

#### Scenario: Bridge enabled
- **WHEN** `flovira.ui` is enabled
- **THEN** Spring registers the designer contract endpoints

#### Scenario: Bridge disabled
- **WHEN** `flovira.ui` is disabled
- **THEN** Spring does not register the designer contract endpoints

### Requirement: Configurable API prefix
The Spring Web bridge SHALL use `flovira.ui-api-prefix` as its route prefix and SHALL default to `/flovira`.

#### Scenario: Host configures an admin prefix
- **WHEN** `flovira.ui-api-prefix=/admin/v1/flovira`
- **THEN** capability and business data endpoints are available below `/admin/v1/flovira`

#### Scenario: Host does not configure a prefix
- **WHEN** `flovira.ui-api-prefix` is absent
- **THEN** existing `/flovira` endpoints continue to work

### Requirement: Host security ownership
The REST bridge SHALL delegate authentication and authorization to the host application's Spring Web security configuration.

#### Scenario: Protected host route
- **WHEN** the host protects its configured designer prefix
- **THEN** Flovira does not bypass or replace the host security policy
