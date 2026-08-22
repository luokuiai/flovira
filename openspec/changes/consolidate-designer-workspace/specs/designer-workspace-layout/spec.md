## ADDED Requirements

### Requirement: Unified designer source layout
The repository SHALL place the Vue designer, React designer, and their examples under one `flovira-designer` source root while keeping each framework package independently buildable.

#### Scenario: Build designers from the designer workspace
- **WHEN** a maintainer runs the build commands from `flovira-designer`
- **THEN** the workspace resolves `flovira-designer/vue`, `flovira-designer/react`, and every `flovira-designer/examples/*` package

### Requirement: No embedded designer application
The project SHALL NOT publish a built-in designer SPA or a static-resource WebJar, while retaining backend API adapters used by externally hosted npm designers.

#### Scenario: Build backend UI adapters
- **WHEN** the Spring or Solon designer API adapter is built
- **THEN** it does not depend on `flovira-plugin-vue3-ui` and does not register classpath static designer resources
- **AND** its existing designer API controller remains available

#### Scenario: Inspect project modules
- **WHEN** a maintainer inspects Gradle and Bun workspace modules
- **THEN** neither `flovira-ui` nor `flovira-plugin-vue3-ui` is present
