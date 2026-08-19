## MODIFIED Requirements

### Requirement: Frontend packages shall use the Luokuiai npm scope
Every publishable Flovira frontend package SHALL use the `@luokuiai` npm scope and an explicit framework suffix, and every workspace dependency, source import and active documentation example SHALL reference the same package names.

#### Scenario: Build workspace consumers
- **WHEN** dependencies are installed from the root Bun workspace
- **THEN** Vue demos resolve `@luokuiai/flovira-vue-designer` and the React demo resolves `@luokuiai/flovira-react-designer`
- **AND** no active `@luokuiai/flovira-designer` package reference remains

### Requirement: Framework packages shall remain independently consumable
The Vue and React designer packages SHALL NOT depend on one another and SHALL declare their own framework runtimes as peer dependencies.

#### Scenario: Install only the React designer
- **WHEN** a consumer installs the React designer with its React peer dependencies
- **THEN** Vue, LogicFlow and Vue UI libraries are not required by the React package

#### Scenario: Install only the Vue designer
- **WHEN** a consumer installs the Vue designer with its Vue and LogicFlow peer dependencies
- **THEN** React, React DOM and React-only libraries are not required by the Vue package
