# Definition business type

`flow_definition.business_type` configures the business type. Instances retain
`business_type` as a snapshot taken when the workflow starts. Both startup paths,
including subprocess startup, require a nonempty definition business type. There
is no fallback to the flow code and callers cannot override it at startup.

Both definition and instance columns have length 128, are `NOT NULL`, and have
no default. Configure the type before saving a definition, including a draft.
Saving a definition rejects null or whitespace-only types. Partial updates may
omit the field to preserve its stored value. Definition copies and JSON
import/export preserve it. Existing instance snapshots are not rewritten when
a definition changes; business-key queries continue to use those snapshots.

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
ALTER TABLE flow_definition ADD COLUMN business_type varchar(128) NULL COMMENT '业务类型';

-- PostgreSQL
ALTER TABLE flow_definition ADD COLUMN business_type varchar(128);
COMMENT ON COLUMN flow_definition.business_type IS '业务类型';

-- Oracle
ALTER TABLE FLOW_DEFINITION ADD (BUSINESS_TYPE VARCHAR2(128));
COMMENT ON COLUMN FLOW_DEFINITION.BUSINESS_TYPE IS '业务类型';
```

The statements above are only for databases missing the definition column;
skip them if the previous 64-length column already exists. Before backfilling,
widen existing columns (preserving any existing NOT NULL constraint):

```sql
-- MySQL: the definition column is temporarily nullable during backfill.
ALTER TABLE flow_definition MODIFY COLUMN business_type varchar(128) NULL COMMENT '业务类型';
ALTER TABLE flow_instance MODIFY COLUMN business_type varchar(128) NOT NULL COMMENT '业务类型';

-- PostgreSQL
ALTER TABLE flow_definition ALTER COLUMN business_type TYPE varchar(128);
ALTER TABLE flow_instance ALTER COLUMN business_type TYPE varchar(128);

-- Oracle
ALTER TABLE FLOW_DEFINITION MODIFY (BUSINESS_TYPE VARCHAR2(128));
ALTER TABLE FLOW_INSTANCE MODIFY (BUSINESS_TYPE VARCHAR2(128));
```

Map each definition version and tenant to its intended business type. Inspect
existing instance business types grouped by `tenant_id` and `definition_id` as
evidence, but do not blindly use `flow_code` or select an arbitrary historical
value. A definition previously used with multiple business types requires an
explicit choice or separate definitions for future starts. Definitions with no
instances require explicit configuration too. Backfill approved values using
both tenant and definition ID; leave existing instance snapshots unchanged.

After backfilling, verify every definition, including drafts and logically
deleted versions, has a nonblank type. Then enforce the definition constraint:

```sql
-- MySQL
ALTER TABLE flow_definition MODIFY COLUMN business_type varchar(128) NOT NULL COMMENT '业务类型';

-- PostgreSQL
ALTER TABLE flow_definition ALTER COLUMN business_type SET NOT NULL;

-- Oracle: run only if the column is currently nullable.
ALTER TABLE FLOW_DEFINITION MODIFY (BUSINESS_TYPE NOT NULL);
```

Verify all definition versions and subprocess targets before resuming starts.
MySQL/PostgreSQL use `varchar(128)`; Oracle uses `VARCHAR2(128)` with the database
length semantics and treats an empty string as null. Verify any pre-existing
column defaults are absent. No new foreign keys or indexes are introduced;
instance business-key indexes remain unchanged. Live PostgreSQL contract tests
cover 128-character definition/instance values and the definition NOT NULL
constraint; MySQL and Oracle receive static schema checks.

## Rollback

Roll back application code and caller changes together. Keep widened columns
to preserve values longer than 64; do not truncate data to restore the old size.
If the old application saves definitions without a business type, explicitly
relax the definition NOT NULL constraint as part of the rollback. Preserve a
backup of configured values. Removing the column is optional destructive
cleanup requiring separate approval. Do not alter historical instance types.
