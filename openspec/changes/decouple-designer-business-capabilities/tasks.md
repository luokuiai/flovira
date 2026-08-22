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

## 5. Unified Approver Contract

- [x] 5.1 Add the Java 8-compatible versioned approver rule and subject contracts plus semantic strategy/relation constants.
- [x] 5.2 Resolve user, role, organization, and expression rules inside core before task persistence, with legacy `permissionFlag` fallback and explicit invalid/empty failures.
- [x] 5.3 Align backend capability defaults and relationship contracts with the runtime resolver.
- [x] 5.4 Make Vue serialize typed approver rules and preserve subject types during resource selection and display lookup.
- [x] 5.5 Replace React's free-text approver fields with the shared capability-driven, searchable resource selector and serialize the same rule.
- [x] 5.6 Add contract/model regression tests for identical rule serialization and semantic strategy codes.
- [x] 5.7 Compile affected backend modules, test/build both designer packages, and validate the updated OpenSpec change.

## 6. Extensible Spring Controller

- [x] 6.1 Register the built-in `FloviraController` conditionally so a host-provided subclass Bean replaces it without duplicate mappings.
- [x] 6.2 Document class-level and method-level annotation customization using a controller subclass.
- [x] 6.3 Compile the Spring Web bridge and strictly validate the updated OpenSpec change.
