## ADDED Requirements

### Requirement: Frontend packages shall use the Luokuiai npm scope
Every publishable Flovira frontend package SHALL use the `@luokuiaiai` npm scope, and every workspace dependency, source import and documentation example SHALL reference the same scope.

#### Scenario: Build workspace consumers
- **WHEN** dependencies are installed from the root Bun workspace
- **THEN** Vue and React demos resolve their designer packages through `@luokuiaiai/*`
- **AND** no active `@luokuiai/*` package reference remains

### Requirement: Framework packages shall remain independently consumable
The Vue and React designer packages SHALL NOT depend on one another and SHALL declare their own framework runtimes as peer dependencies.

#### Scenario: Install only the React designer
- **WHEN** a consumer installs the React designer with its React peer dependencies
- **THEN** Vue, LogicFlow and Vue UI libraries are not required by the React package
