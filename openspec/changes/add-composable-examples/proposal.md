## Why

Flovira has frontend-only designer demos but no runnable host examples, and a single fixed full-stack example would couple learning the engine to one database and one UI stack. A composable example matrix lets developers choose a supported backend and frontend independently while still exercising the same complete workflow lifecycle.

## What Changes

- Add a top-level `flovira-example` area with PostgreSQL and MySQL Spring Boot backends that expose the same REST contract.
- Extract shared backend workflow, business-data, provider, identity, and lifecycle code so database variants contain only database-specific dependencies, configuration, schema bootstrap, and startup wiring.
- Add React + Lumen, React + Ant Design, and Vue + Ant Design Vue frontends that can each connect to either backend.
- Extract framework-neutral frontend API contracts/client code and shared React behavior so UI variants contain only framework or component-library-specific presentation and bootstrapping.
- Demonstrate definition design/save/publish, process start, task listing, approval, rejection, progress, history, deterministic users/resources, and a host-owned sample business record.
- Provide independent startup commands, configurable backend targets, database containers, matrix documentation, and focused verification for every supported combination.

## Capabilities

### New Capabilities

- `composable-examples`: Independently runnable and interchangeable database backend and frontend examples covering a complete Flovira workflow lifecycle.

### Modified Capabilities

None.

## Impact

- Adds new example-only Gradle modules, a Bun frontend workspace, database orchestration, development schemas/seeds, tests, and documentation under `flovira-example`.
- Reuses the supported Spring Boot 3, MyBatis-Plus, JSON, designer REST bridge, React/Vue designer, Lumen, Ant Design, and Ant Design Vue integrations without changing their public contracts.
- Uses the authoritative MySQL and PostgreSQL fresh-install schemas under `sql/`; Oracle remains supported by Flovira but is outside this example matrix.
- Updates root Gradle settings, frontend build metadata where needed, and repository documentation so all variants are discoverable and verifiable.
