# JSON extensions

The `ext` fields on definitions, forms, nodes, instances and history contain JSON
objects. Java APIs retain `String`; database columns use native JSON storage.
Configuration readers preserve unrelated host values. Native storage may normalize
whitespace and object key order, so compare parsed data rather than JSON text.
Absent extensions use SQL NULL; an empty object is `{}`. Empty strings, plain text
and array roots are not valid extension values.

```json
{
  "business": { "tags": ["finance", "review"], "enabled": false },
  "approverRule": {
    "schemaVersion": 1,
    "strategyVersion": 1,
    "strategy": "USER",
    "subjects": [{ "id": "reviewer", "type": "USER" }]
  }
}
```

Lifecycle callbacks are registered in code. The engine does not interpret an
`ext.lifecycle` property, and neither designer offers callback selection.

Both designers write object extensions and preserve nested host values. Core
configuration readers reject array roots; there is no runtime conversion or
fallback to the former `[{"code": ..., "value": ...}]` representation.

## Host JSON adapters

A JSON string does not itself require a special type handler. This handler exists
because the database columns are native JSON: JDBC binding (including SQL NULL)
and JSON equality differ from ordinary VARCHAR binding and text equality. Reads
still use JDBC getString; the handler does not deserialize JSON into business objects.

Both ORM integrations delegate these database-specific operations to
`com.luokuiai.flovira.orm.type.ExtJsonAdapter`. Implement it in a host module and
register the implementation's fully qualified class name in:

```text
META-INF/services/com.luokuiai.flovira.orm.type.ExtJsonAdapter
```

The provider must have a public no-argument constructor and be thread-safe. It is
loaded once through Java SPI; no Spring annotation or mapper replacement is needed.
Both MyBatis and MyBatis-Plus, including the Boot 2/3/4 starters, use this contract.

- `supportsDatabase(productName)` matches the actual JDBC database product name.
- `supportsDialect(dataSourceType)` matches the configured Flovira SQL dialect.
  These two names may differ; implement both deliberately.
- `bind(statement, index, json)` handles both JSON object strings and null values.
  The host can bind a driver-specific JSON object without adding that driver to Flovira.
- `comparison(column, parameter)` returns an equality predicate. The default is
  `column = parameter`. The parameter is already a MyBatis bound placeholder with
  the ext handler. Preserve it; never concatenate JSON values into SQL.

Host providers take precedence over built-ins for matching databases/dialects.
Two host providers matching the same name fail explicitly, as do unregistered
names and invalid SPI registrations. Built-in MySQL, PostgreSQL and Oracle
binding/comparison behavior remains available without configuration. Unknown
products are not silently treated as PostgreSQL or text columns.

For Kingbase or another database, use the actual driver product name and the
binding required by that driver, column type and compatibility mode. This SPI
opens JSON persistence to host adapters; it is not a claim that Kingbase's driver,
schemas, pagination or full workflow behavior has been verified. SQL pagination
and other dialect configuration remain separate from the JSON adapter.

This extension adds no schema/data migration. Remove the host SPI registration
to restore built-in handling for a supported database; an unsupported database
requires its provider and will fail explicitly if the provider is removed.

## Existing development data

This pre-1.0 correction changes the extension representation and SQL column types,
without changing workflow execution state. Before using an existing development database, stop
writes and back up definitions, nodes and instance definition snapshots. Convert
each reviewed array entry into an object property named by its `code`. Preserve
ordinary business values exactly. Parse the JSON value of known engine settings
such as `approverRule`, `waitConfig` or `nodeControlConfig` into a nested object.
Resolve duplicate or missing codes explicitly rather than discarding entries.

Apply the same mapping to exported workflow packages, host-generated definitions,
and `nodeList[*].ext` in stored instance `defJson` snapshots. Move any previously
configured lifecycle behavior into host callback code and remove the obsolete
configuration from reviewed development data. No automatic data migration runs.

Follow [native JSON migration](native-json-storage.md) for database-specific
column conversion. Do not rerun fresh-install schemas. Rolling back requires restoring the matching code and
backed-up extension/snapshot data together. No database data was changed by this
source correction.
