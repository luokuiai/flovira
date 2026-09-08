# Core module instructions

Follow [root AGENTS.md](../AGENTS.md). This file contains only core-specific rules.

## Scope

`com.luokuiai.flovira.core` is the framework-, ORM- and JSON-independent engine used by all adapters and consuming applications. Public behavior changes are L2.

- `FlowEngine` exposes services, entity suppliers, handlers, listeners and JSON conversion.
- `config` and `invoker/FrameInvoker` bridge configuration and framework services.
- `entity`, `service`, `service.impl` and `orm` define engine contracts and abstract persistence.
- `handler` provides data fill, tenant, permission, business relationships and external form field labels.
- `listener`, `strategy` and `condition` provide workflow callbacks, approver / condition / voting expressions and comparison operations.
- `keygen`, `json` and `utils` provide IDs, serialization SPI and Java 8 utilities.

## Before editing

Read relevant service implementations, strategies, handlers, listeners and enums before changing workflow behavior. Optional `.qoder/repowiki` documentation is not required.

Keep Java 8 source compatibility and zero concrete framework / ORM dependencies. Preserve `FlowEngine` factories and existing extension points. Synchronize entity changes across DTOs, both ORMs, serialization and all three SQL schemas.

Form definitions belong to the host application. `formId` is an external string; do not parse it as an engine-owned numeric key. Preserve task and history reference snapshots. `FormFieldProvider` is optional and framework-independent.

## Verification

Run relevant core tests and `rtk proxy ./gradlew :flovira-core:compileJava`. Compile at least one affected downstream ORM / plugin. State-machine changes require focused behavior tests, not compilation alone.
