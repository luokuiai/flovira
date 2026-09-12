## Context

See `proposal.md` for motivation and `specs/composable-examples/spec.md` for observable behavior. Existing examples under `flovira-designer/examples` demonstrate individual designer packages with browser-local data. The backend repository provides libraries and starters but no runnable host. The new examples must preserve framework/ORM/JSON independence in published modules, use public `FlowEngine` factories and services, reuse authoritative database schemas, and keep form/business data under host ownership.

## Goals / Non-Goals

**Goals:**

- Make database choice and frontend choice orthogonal and configuration-driven.
- Keep one backend feature implementation and one cross-frontend HTTP contract.
- Share frontend domain/API code and React behavior without forcing React and Vue into a common component model.
- Allow one selected combination to run cheaply while permitting variants to run simultaneously on distinct ports.
- Exercise repository projects directly before Flovira artifacts are published.

**Non-Goals:**

- Production authentication, authorization, secret management, or tenant administration.
- A built-in form designer or engine-owned business persistence abstraction.
- Oracle, Spring Boot 2/4, plain MyBatis, or every designer adapter in the first example matrix.
- Pixel-identical React and Vue applications or a framework-neutral UI component layer.
- Bundling frontend assets into a backend library or changing public Flovira contracts.

## Decisions

### Organize examples as a backend/frontend matrix

Use this top-level layout:

```text
flovira-example/
├── backend/
│   ├── common/
│   ├── postgres/
│   └── mysql/
├── frontend/
│   ├── common/
│   ├── react-common/
│   ├── react-lumen/
│   ├── react-antd/
│   └── vue-antd/
├── compose.yaml
└── README.md
```

`backend/common` is a Java library module; `backend/postgres` and `backend/mysql` are runnable Spring Boot application modules. `frontend` is one private Bun workspace with a single lockfile. The three runnable frontend packages depend on shared workspace packages and local Flovira designer builds.

A directory per complete backend/frontend pairing was rejected because six applications would duplicate almost all code and inevitably drift. Mixing these examples into `flovira-designer` was rejected because database lifecycle and host business APIs are outside the designer package's responsibility.

### Keep database variants as thin runtime modules

`backend/common` owns controllers, application services, DTOs, example-owned entities/mappers, designer providers, demo identity handling, sample workflow bootstrap, and all database-neutral configuration. Both runnable variants use Spring Boot 3, the MyBatis-Plus starter, Jackson 2 provider, and UI Web bridge. A variant adds only its JDBC driver, application configuration, database-specific example schema/seed asset, and a minimal application entry point.

The common module compiles with the Boot 3/Java 17 convention while published Java 8 modules remain unchanged. A single application with Spring profiles was rejected because the user-facing examples should make each database dependency and startup path explicit, while still sharing implementation code.

### Reuse authoritative schemas and isolate example data

The PostgreSQL and MySQL container services initialize from `sql/postgresql/flovira-v1.sql` and `sql/mysql/flovira-v1.sql` respectively. Each then applies a small dialect-specific example schema/seed script for host-owned users, organizations, roles, condition fields, and purchase requests. No Flovira table definition is copied into `flovira-example`.

Initialization occurs only for a fresh development volume. Reset is a documented deliberate operation, never an automatic backend-start action. The sample workflow is imported/published idempotently through public Flovira services instead of direct definition-table inserts.

### Define one versioned example HTTP contract

Both backends expose identical lifecycle endpoints under `/api/example/v1` and configure the Flovira designer bridge under the same prefix on their respective hosts. The contract covers health, identities, definitions, publication, business requests, process start, current-user tasks, approve/reject actions, progress, and history. Response/error envelopes, stable identifiers, paging, and validation behavior are shared code.

Backend defaults use separate ports (PostgreSQL `8081`, MySQL `8082`) so both may run simultaneously. Database containers also use distinct host ports. Frontends never inspect database type.

### Share frontend code at two appropriate levels

`frontend/common` is framework-neutral TypeScript containing HTTP DTOs, error normalization, the API client, endpoint configuration, and workflow display helpers. It has no React, Vue, Lumen, Ant Design, or Ant Design Vue dependency.

`frontend/react-common` contains React hooks, query/loading/error state, route-level feature models, and behavior shared by the Lumen and Ant Design applications. It does not import either UI library. The React apps provide library-specific shells, controls, feedback, and their respective Flovira designer adapters. The Vue app consumes `frontend/common` directly and implements Vue composables/components with Ant Design Vue.

Trying to share rendered components between React and Vue was rejected as false abstraction. Duplicating the API client and lifecycle state in all three apps was rejected because it would weaken the interchangeability guarantee.

### Select a backend through runtime startup configuration

Each frontend uses relative API routes and a Vite development proxy whose target comes from `EXAMPLE_BACKEND_URL`, defaulting to the PostgreSQL backend. Switching to MySQL changes only that environment value. Each frontend has a distinct default development port so variants can be compared simultaneously.

This avoids database-specific frontend builds and broad backend CORS settings. A compile-time database selector was rejected because frontend behavior must not depend on the persistence implementation.

### Keep demo identity explicit and host-owned data separate

The selected demo identity is sent as `X-Demo-User`; shared backend code validates it against deterministic example users before task operations. All UIs visibly label the selector as demo-only. Real authentication remains the host's responsibility.

Purchase requests and resource catalogs live in example-owned tables. Workflow records retain only stable `businessId`/`formId` references and normal Flovira approval snapshots. This demonstrates integration without reintroducing form management into the engine.

### Test shared contracts once and variants at their boundaries

Backend common tests cover lifecycle behavior, controllers, providers, and identity validation. A reusable backend contract suite runs against both PostgreSQL and MySQL variants to catch dialect/configuration differences. Frontend common tests cover DTO/client/error behavior; React common tests cover shared state; each UI app has adapter/render smoke tests and a production build.

The documented matrix smoke test runs the core lifecycle for all six pairings. Repeated lifecycle depth is concentrated in one pairing per backend, while the remaining pairings verify contract discovery, resource load, definition round-trip, task action, and progress refresh to keep the suite practical without reducing compatibility coverage.

## Risks / Trade-offs

- [Five shared/runnable modules per side increase build structure] → Keep ownership rules explicit and provide aggregate backend/frontend verification tasks.
- [Shared backend code may accidentally rely on one SQL dialect] → Run the same persistence contract suite against both real database engines.
- [Local frontend packages depend on unpublished designer builds] → Use repository-local dependencies and an aggregate setup/build command; keep one example workspace lockfile.
- [UI variants can drift despite a common client] → Define one acceptance journey and run matrix smoke coverage for every pairing.
- [Demo identity headers may be mistaken for real security] → Isolate and label them as development-only in code, UI, and documentation.
- [Two databases and three dev servers can contend for ports/resources] → Use distinct defaults and document starting only the selected combination.
- [Schema baseline changes can invalidate reused volumes] → Document schema expectations and explicit per-database reset commands; never reset automatically.

## Migration Plan

1. Add the top-level module/workspace skeleton and shared contracts.
2. Implement shared backend behavior, then PostgreSQL and MySQL runtime variants.
3. Implement shared frontend packages, then React Lumen, React Ant Design, and Vue Ant Design Vue applications.
4. Add database orchestration, documentation, aggregate checks, and six-pairing smoke verification.
5. Update root documentation without changing or removing existing designer-only examples.

Rollback removes `flovira-example` and its root build registrations. No published API or production schema migration is required.
