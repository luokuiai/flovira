## 1. Backend Contract

- [x] 1.1 Add Java 8-compatible capability, resource query/result, and relationship query/result value objects.
- [x] 1.2 Add host capability and business data provider interfaces plus a facade with built-in defaults.
- [x] 1.3 Add contract endpoints to the Spring Web bridge and make the API prefix configurable with `/flovira` as its default.

## 2. Designer Packages

- [x] 2.1 Add shared capability and resource contracts to the Vue provider, HTTP provider, and mock provider.
- [x] 2.2 Filter the Vue node palette using the capability manifest while preserving explicit palette overrides.
- [x] 2.3 Add the equivalent React provider types and capability-driven node and approver-strategy controls.

## 3. Verification

- [x] 3.1 Add frontend contract and capability-filtering regression tests.
- [x] 3.2 Compile the UI core and Spring Web modules and build/test both designer packages.
- [x] 3.3 Validate the OpenSpec change and record final task completion.

## 4. Remove Legacy Integration

- [x] 4.1 Remove legacy business-data services, DTOs, VOs, REST endpoints, and fallback branches.
- [x] 4.2 Migrate all Vue business-data calls to capability and generic resource queries.
- [x] 4.3 Update mocks, documentation, and resource constants for the single 1.0 contract.
- [x] 4.4 Re-run backend compilation, frontend tests/builds, symbol checks, and OpenSpec validation.
