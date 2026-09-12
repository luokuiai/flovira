# ORM module instructions

Follow [root AGENTS.md](../AGENTS.md). This file contains only ORM-specific rules.

## Scope

Implement core entities and `FloviraDao` in the existing MyBatis / MyBatis-Plus matrix: ORM core plus Spring Boot 2 / 3 / 4 starters. Implementations use `com.luokuiai.flovira.orm`; starter integration uses `com.luokuiai.flovira.spring.boot`.

## Before editing

Read the affected core entity, DAO / query / service contracts, ORM implementation and starter registration. Public persistence contracts and cross-ORM behavior are L2.

- Keep pagination, batching, logical deletion and tenant isolation consistent across both ORMs.
- Register implementation suppliers with `FlowEngine` and framework access through `FrameInvoker`.
- Boot 2 uses `spring.factories`; Boot 3 / 4 use `AutoConfiguration.imports`. Keep class names and registration files aligned.
- Changes to ID generation, datasource types and pagination need dialect checks.
- Preserve both engine-managed and ORM-managed tenant / deletion paths.
- Synchronize schema changes across MySQL, PostgreSQL and Oracle V1 scripts. Keep shared ORM contract-test schemas aligned too.
- Workflow `formId` columns remain opaque strings and may reference a Flovira-managed `flow_form` row or a host form. Keep the managed form table and both ORM implementations aligned; do not restore `form_custom`, `form_type`, `form_path` or numeric-only workflow references.

## Verification

Compile affected ORM cores and starters. For changes shared by both ORMs, run both persistence contract suites:

```bash
rtk proxy ./gradlew :flovira-mybatis-sb-starter:test :flovira-mybatis-plus-sb-starter:test
```

Check the actual Gradle project names in settings; filesystem nesting does not define Gradle task paths.
