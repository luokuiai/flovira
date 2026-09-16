## Why

The removal of `form_custom` was incorrectly expanded into removal of Flovira's entire form-management capability. Flovira must retain versioned form metadata and content while representing workflow-to-form selection with a single `formId` reference.

## What Changes

- Restore the `flow_form` table to all supported fresh-install schemas.
- Restore the core `Form` contract, form service, DAO, ORM entities, mappers, and framework registration for both MyBatis implementations.
- Restore designer-backend form content APIs needed by Flovira-managed forms.
- Keep `form_custom` and its branching semantics removed; definitions and nodes select forms solely through `formId`.
- Keep unused `form_type` and `form_path` fields removed; host page routing stays behind the host resource provider.
- Keep task and history `formId` snapshots so running and historical approvals retain the selected form version.
- Correct project documentation and agent rules that incorrectly require forms to be host-owned.
- Document development-database restoration without executing destructive migration SQL.

## Capabilities

### New Capabilities

- `form-management`: Versioned workflow form metadata/content management, publication lifecycle, workflow references, and designer-backend access.

### Modified Capabilities

- `designer-integration-contract`: Form resources may be supplied by the host or backed by Flovira-managed forms without restoring `form_custom`.

## Impact

Affected areas include public core APIs, both ORM implementations, Spring Boot starter wiring, designer backend endpoints, Vue/React integration contracts, MySQL/PostgreSQL/Oracle schemas, persistence tests, documentation, and migration guidance. This restores APIs removed during pre-1.0 stabilization and does not restore the obsolete `form_custom` contract.
