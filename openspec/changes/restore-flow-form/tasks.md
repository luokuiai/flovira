## 1. Contract and project rules

- [x] 1.1 Correct root/module guidance and migration documentation to retain managed forms while forbidding `form_custom`; verify literal searches no longer mandate removal of `flow_form`
- [x] 1.2 Restore core form entity/service/DAO contracts and `FlowEngine` factory/service access; verify core form tests and compilation pass

## 2. Persistence and schemas

- [x] 2.1 Restore MyBatis and MyBatis-Plus form entities, DAOs, mappers, and starter wiring with tenant/logical-delete behavior; verify both ORM contract suites pass
- [x] 2.2 Add equivalent `flow_form` definitions and tenant-aware indexes to MySQL, PostgreSQL, Oracle, and contract-test schemas; verify schema parity tests pass
- [x] 2.3 Remove unused `form_type` and `form_path` contracts from core, ORM, schemas, designer metadata, tests, and documentation; rerun focused verification

## 3. Designer integration

- [x] 3.1 Restore designer-backend managed-form metadata/content operations and fallback form resource discovery; verify focused plugin UI tests and compilation pass
- [x] 3.2 Align Vue/React documentation and contracts with managed-or-host form selection without restoring `form_custom`; verify frontend tests and package builds pass

## 4. Verification

- [x] 4.1 Add regression coverage for managed-form lifecycle, opaque `formId` snapshots, tenant isolation, and absence of `form_custom`; run focused backend tests
- [x] 4.2 Run OpenSpec strict validation, relevant Gradle builds, frontend tests/builds, and `git diff --check`; document any environment-limited checks
