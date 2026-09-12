## 1. Matrix foundation

- [x] 1.1 Create the `flovira-example/backend/{common,postgres,mysql}` Gradle modules and register them in root settings; verify Gradle resolves all three projects and compiles the common module.
- [x] 1.2 Create the `flovira-example/frontend/{common,react-common,react-lumen,react-antd,vue-antd}` Bun workspace with one lockfile and repository-local designer dependencies; verify installation and workspace discovery succeed without child lockfiles.
- [x] 1.3 Add shared example naming, port, environment-variable, and API-prefix conventions to the root example README skeleton; verify each planned runnable variant has a unique default port and configurable backend/database target.

## 2. Shared backend implementation

- [x] 2.1 Configure `backend/common` with Spring Boot 3-compatible MyBatis-Plus, Jackson 2, and Flovira UI bridge dependencies while keeping database drivers out; verify dependency inspection shows no PostgreSQL or MySQL driver in common.
- [ ] 2.2 Implement shared example-owned users, organizations, roles, condition fields, and purchase-request persistence; verify unit tests return deterministic stable identifiers and host-owned business data.
- [x] 2.3 Implement shared designer capability/resource providers through Flovira extension contracts; verify contract tests cover approver, organization, role, and condition-field queries.
- [x] 2.4 Implement the demo-only `X-Demo-User` context and validation; verify tests reject unknown identities and propagate valid handlers to task operations.
- [ ] 2.5 Implement the versioned `/api/example/v1` contract for health, definitions, publication, business requests, process start, tasks, approval, rejection, progress, and history using public `FlowEngine` services/factories; verify controller and lifecycle tests cover success and explicit error responses.
- [x] 2.6 Add an idempotent sample-workflow initializer using public Flovira services; verify repeated initialization creates no duplicate definition.

## 3. Database backend variants

- [x] 3.1 Implement the PostgreSQL runtime module with only its application entry point, driver, configuration, and example schema/seed assets; verify its dependency graph and `compileJava` task succeed.
- [x] 3.2 Implement the MySQL runtime module with only its application entry point, driver, configuration, and example schema/seed assets; verify its dependency graph and `compileJava` task succeed.
- [x] 3.3 Add container services that initialize each database from its authoritative `sql/<dialect>/flovira-v1.sql` plus the matching example-owned schema; verify fresh PostgreSQL and MySQL volumes contain the expected Flovira and example tables.
- [ ] 3.4 Run the reusable backend contract suite against both real database variants; verify definition round-trip, publish/start, approve-to-completion, rejection, progress/history, tenant/deletion behavior, and persistence after backend restart.

## 4. Shared frontend implementation

- [x] 4.1 Implement `frontend/common` with framework-neutral API DTOs, client, backend-target configuration, error normalization, and display helpers; verify unit tests run without React, Vue, or UI-library dependencies.
- [ ] 4.2 Implement `frontend/react-common` with shared React hooks and lifecycle state for definitions, requests, tasks, demo identities, and progress; verify tests cover loading, backend switching, task actions, refresh, and error states without importing Lumen or Ant Design.
- [x] 4.3 Configure relative API routes and `EXAMPLE_BACKEND_URL`-driven Vite proxies for all runnable frontends; verify each app can switch between default PostgreSQL and MySQL targets without source changes.

## 5. Runnable frontend variants

- [ ] 5.1 Implement the React + Lumen application using the React designer and Lumen adapter over shared frontend packages; verify design/save/publish/start/task/progress component tests and its production build succeed.
- [ ] 5.2 Implement the React + Ant Design application using the React designer and Ant Design adapter over the same React/common packages; verify equivalent component tests and its production build succeed.
- [ ] 5.3 Implement the Vue + Ant Design Vue application using the Vue designer and framework-neutral common package; verify equivalent component tests and its production build succeed.
- [ ] 5.4 Verify all three UIs visibly label demo identity selection, surface backend failures, and keep business/form ownership outside Flovira; record the focused UI interaction checks.

## 6. Documentation and build integration

- [x] 6.1 Complete the matrix README with prerequisites, individual and aggregate startup commands, backend selection, default ports, lifecycle walkthrough, persistence/restart behavior, and per-database reset procedures; verify every documented command and path.
- [x] 6.2 Document shared versus variant-specific code and mark credentials, identity headers, seeds, proxy behavior, and reset operations as development-only; verify production replacements are stated without implying Flovira owns authentication or forms.
- [x] 6.3 Add aggregate backend test/build and frontend test/build commands and update root documentation so the example matrix is discoverable; verify existing backend modules and designer-only example commands remain valid.

## 7. Compatibility matrix verification

- [ ] 7.1 Run the full approve-to-completion and rejection journeys against PostgreSQL with one frontend and against MySQL with one frontend; verify persisted workflow state and UI progress/history agree.
- [ ] 7.2 Smoke-test React + Lumen, React + Ant Design, and Vue + Ant Design Vue against PostgreSQL; verify resource loading, definition round-trip, task action, and progress refresh for all three pairings.
- [ ] 7.3 Repeat the frontend smoke tests against MySQL; verify the same operations for all three pairings with only backend-target configuration changed.
- [x] 7.4 Run affected Gradle tests/compilations, Bun tests/builds, and `rtk git diff --check`; report Docker-dependent results separately from static/unit verification.
