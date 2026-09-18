# Native JSON storage for extensions

All five extension columns (`flow_definition.ext`, `flow_form.ext`, `flow_node.ext`,
`flow_instance.ext`, `flow_his_task.ext`) use native JSON storage. Other columns,
such as variables, form_content and def_json, are outside this change.

| Database | Column type | Requirement |
| --- | --- | --- |
| MySQL | JSON | Native JSON support (5.7.8+); integration checks use 8.4 |
| PostgreSQL | JSONB | Native JSONB support (9.4+); integration checks use 16 |
| Oracle | JSON | 21c+ with COMPATIBLE >= 20 and a JDBC driver supporting native JSON |

Java entities, DTOs and APIs still use String. Supply a JSON object such as `{}`,
or null when absent. The field-specific ExtJsonTypeHandler is compiled into both
ORM artifacts; it is not a global String handler and adds no database-driver or
JSON-library dependency. PostgreSQL binds Types.OTHER, MySQL binds JSON text, and
Oracle binds OracleTypes.JSON (2016). Reads use getString. Database validation
rejects malformed JSON; the handler rejects empty strings and non-object roots.
There is no fallback to text storage or automatic conversion of old arrays.

Database storage may normalize whitespace, key order and numeric formatting.
Do not compare serialized bytes or rely on duplicate keys. Entity-based ext
filters use JSON equality: PostgreSQL JSONB equality, MySQL CAST(parameter AS JSON),
and Oracle JSON_EQUAL. For host-written wrapper expressions, bind the ext handler
explicitly and use the corresponding database JSON comparison; ordinary String
wrapper parameters do not automatically inherit a field handler.

## Existing development databases

Fresh-install SQL files define the new types. Do not execute them over existing
data. No migration runs automatically and no live database was changed as part
of this source change.

1. Stop writers and host scheduling; back up all five tables and exported workflow
   packages. Keep the original schema and matching application artifact for rollback.
2. Inspect every non-null ext. Resolve malformed JSON, duplicate keys, arrays and
   plain text explicitly. Convert empty strings to SQL NULL only after confirming
   they mean absence. Map the former code/value representation as described in
   [extension JSON objects](extension-json-object.md). Do not JSON-quote invalid
   text to make a conversion succeed.
3. Apply the database-specific column conversion below to each of the five tables.
4. Deploy both the schema and matching ORM artifact. Verify single/batch inserts,
   updates, nullable values, JSON equality queries and tenant/deletion filters
   before reopening writers.

### PostgreSQL

After validation, run the following for each table in a controlled transaction:

```sql
ALTER TABLE flow_definition ALTER COLUMN ext TYPE jsonb USING ext::jsonb;
ALTER TABLE flow_form ALTER COLUMN ext TYPE jsonb USING ext::jsonb;
ALTER TABLE flow_node ALTER COLUMN ext TYPE jsonb USING ext::jsonb;
ALTER TABLE flow_instance ALTER COLUMN ext TYPE jsonb USING ext::jsonb;
ALTER TABLE flow_his_task ALTER COLUMN ext TYPE jsonb USING ext::jsonb;
```

The conversion takes table locks and fails on invalid JSON. Retain SQL NULL.

### MySQL

After validation, use `ALTER TABLE <table> MODIFY COLUMN ext JSON NULL` for each
of the five tables, preserving its existing column comment. DDL commits implicitly;
plan for table rebuild/locking and do not rely on transactional rollback.

### Oracle

Verify database compatibility and JDBC versions first. Do not retain a 19c/text
fallback. For each table, add a temporary JSON column, populate it from the validated
old column using the JSON constructor, and verify all converted objects and nulls.
For example, during the maintenance window:

```sql
ALTER TABLE flow_definition ADD (ext_json JSON);
UPDATE flow_definition SET ext_json = JSON(ext) WHERE ext IS NOT NULL;
```

After verifying the copy, replace the old column with the new column, restore its
name/comment and verify dependent objects. Repeat for form, node, instance and
history. Column replacement is destructive and Oracle DDL commits implicitly;
execute it only against a backed-up database with explicit operator authorization.

### Rollback

Restore the backed-up schema, data and matching application artifact together.
Casting normalized JSON back to text does not recover original whitespace, duplicate
keys or key order, and old varchar(500) columns may no longer fit newly stored data.
A blind reverse ALTER is not a complete rollback.

## References

- [PostgreSQL JSON types](https://www.postgresql.org/docs/16/datatype-json.html)
- [MySQL JSON type](https://dev.mysql.com/doc/refman/8.4/en/json.html)
- [Oracle JDBC JSON binding](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/sql/json/package-summary.html)
- [Oracle native JSON requirements and comparison](https://docs.oracle.com/en/database/oracle/oracle-database/21/adjsn/json-in-oracle-database.html)

## Validation performed

Both MyBatis and MyBatis-Plus passed 53 tests each, with no failures or skips.
The shared native-JSON integration test starts PostgreSQL 16 and MySQL 8.4,
installs the actual fresh-install scripts, and exercises all five ext columns:
large Unicode/nested objects, SQL NULL, batch persistence, reads, updates,
entity filtering/count/pagination, key-order-independent equality, invalid JSON
rejection and logical deletion. Existing workflow persistence tests also passed
against JSONB contract columns.

Core tests, both ORM Spring Boot 3/4 checks and the common backend example
compilation passed. Three-schema static checks confirmed five native JSON columns
per database; mapper inspection confirmed typed ext parameters and Oracle INSERT
ALL batches. Oracle binding types and JSON_EQUAL parameter generation passed JDBC
contract/SQL-generation tests only. No Oracle database was available, so Oracle
DDL and runtime execution have not been verified against a server.
