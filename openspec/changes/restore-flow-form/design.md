## Context

See `proposal.md` for motivation. Current workflow tables use string `form_id` references and task/history snapshots, but commit `c4bda051` removed the form contract, service, persistence, starter wiring, and designer-backend endpoints. Earlier baseline consolidation also omitted `flow_form` from fresh-install schemas. Java 8 compatibility, ORM parity, framework independence, tenant isolation, logical deletion, and three-database parity remain mandatory.

## Goals / Non-Goals

**Goals:**

- Restore managed form metadata/content as a first-class optional Flovira capability.
- Preserve the current single `formId` selection model and snapshot behavior.
- Restore public services through existing `FlowEngine`, DAO, SPI, and starter patterns.
- Restore schema and endpoint coverage consistently across supported stacks.

**Non-Goals:**

- Restore `form_custom`, conditional custom-form branches, or designer mode switching.
- Bundle a low-code renderer or frontend pages into backend artifacts.
- Add database foreign keys or execute migration SQL against existing databases.
- Treat `formId` as a numeric-only contract; string IDs remain valid for host-provided forms.

## Decisions

### Restore form management from the pre-removal implementation, then adapt it

Use the implementation immediately before `c4bda051` as behavioral reference because it contains the complete service/DAO/ORM surface. Adapt it to current naming, timestamps, tenant/deletion rules, starter matrix, and `formId` semantics instead of reverting the commit wholesale. A wholesale revert would incorrectly restore `form_custom`, `form_path` workflow fields, and obsolete designer behavior.

### Keep workflow references polymorphic

`formId` remains a string. A value may identify a managed `flow_form` record or a host-provided form. Managed-form lookup validates and parses only when the managed-form API is called; workflow execution stores the opaque reference. Numeric foreign keys were rejected because they would break host-provided identifiers and existing snapshots.

### Keep host page metadata outside managed forms

`flow_form` stores versioned definition content only. The restored `form_type` and `form_path` fields had no behavioral consumer and duplicated the host resource-provider boundary, so they remain removed together with form-mode switching. Host pages are resolved by their owner through the opaque `formId` reference.

### Use existing extension points

Core defines contracts only. Both ORM families provide entities and DAOs, starters attach suppliers, and designer backend delegates through `FlowEngine.formService()`. No Spring, JSON provider, or concrete ORM dependency enters core.

### Treat task/history references as immutable snapshots

Task creation resolves node override versus definition default and stores the effective `formId`; history copies it from the task. Updating or invalidating a form does not rewrite historical references.

## Risks / Trade-offs

- [Restored API diverges from current consumers] → Restore additively and retain current `formId`, provider, and snapshot APIs.
- [Historical source contains stale naming or SQL assumptions] → Adapt against current entities, schema conventions, and contract tests rather than copying blindly.
- [Managed and host form identifiers collide] → Keep resolution explicit at API/provider boundary; workflow runtime treats identifiers as opaque.
- [Schema drift across databases] → Add static parity tests and review dialect-specific types/indexes.
- [Existing databases lack `flow_form`] → Document additive creation and rollback; do not run destructive DDL automatically.

## Migration Plan

1. Deploy an additive `flow_form` table using the supported-database definition and tenant-aware indexes.
2. Restore/import managed form rows where applicable; retain stable IDs referenced by `form_id`.
3. Deploy backend and designer integrations. Existing host-provided `formId` values continue to work.
4. Roll back by reverting application use first. Keep the additive table during rollback to avoid data loss; remove it only through a separately authorized migration after backup and verification.
