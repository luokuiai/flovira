# Definition business type

`flow_definition.business_type` configures the business type. Instances retain
`business_type` as a snapshot taken when the workflow starts. Both startup paths,
including subprocess startup, require a nonempty definition business type. There
is no fallback to the flow code and callers cannot override it at startup.

The column is nullable so drafts and existing definitions can be loaded before
configuration. Configure it before starting an instance. Definition copies and
JSON import/export preserve it. Existing instances are not rewritten when a
definition changes; their business-key queries continue to use their snapshots.

## Code migration

Set `Definition.businessType` or `DefJson.businessType` when configuring a flow.
Replace `start(businessType, businessId, params)` with
`start(businessId, params)`, and replace
`startByDefinitionId(businessType, businessId, definitionId, params)` with
`startByDefinitionId(businessId, definitionId, params)`.
The business-key query APIs still accept a business type.
Custom `Definition` implementations must implement the new accessors.

## Existing development databases

Back up the database and stop application writes before applying the matching
statement. These are manual migration instructions, not an automatic migration.
Do not rerun the V1 fresh-install scripts over an existing database.

```sql
-- MySQL
ALTER TABLE flow_definition ADD COLUMN business_type varchar(64) NULL COMMENT '业务类型';

-- PostgreSQL
ALTER TABLE flow_definition ADD COLUMN business_type varchar(64);
COMMENT ON COLUMN flow_definition.business_type IS '业务类型';

-- Oracle
ALTER TABLE FLOW_DEFINITION ADD (BUSINESS_TYPE VARCHAR2(64));
COMMENT ON COLUMN FLOW_DEFINITION.BUSINESS_TYPE IS '业务类型';
```

Map each definition version and tenant to its intended business type. Inspect
existing instance business types grouped by `tenant_id` and `definition_id` as
evidence, but do not blindly use `flow_code` or select an arbitrary historical
value. A definition previously used with multiple business types requires an
explicit choice or separate definitions for future starts. Definitions with no
instances require explicit configuration too. Backfill approved values using
both tenant and definition ID; leave existing instance snapshots unchanged.

Verify configured types, lengths (64), all callable definition versions and
subprocess targets before resuming starts. MySQL/PostgreSQL use `varchar(64)`;
Oracle uses `VARCHAR2(64)` and treats an empty string as null. No new foreign keys
or indexes are required; instance business-key indexes remain unchanged.

## Rollback

Roll back application code and caller changes together. The previous application
can leave the nullable extra column in place. Preserve a backup of configured
values; removing the column is optional destructive cleanup requiring separate
approval. Do not alter historical instance business types during rollback.
