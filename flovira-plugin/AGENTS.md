# Plugin module instructions

Follow [root AGENTS.md](../AGENTS.md). This file contains only plugin-specific rules.

## Scope

- `flovira-plugin-modes`: Spring integration and SpEL expression implementations.
- `flovira-plugin-json`: independent Jackson, Jackson 3 and Gson providers, each with its own SPI registration. Consumers choose one.
- `flovira-plugin-ui`: designer backend services / DTOs and Spring controllers. Do not restore bundled frontend assets or WebJar packaging.

## Before editing

Read the corresponding core condition, strategy, listener or JSON interface. Preserve expression safety through existing method / type resolvers; never broaden arbitrary type or method access to make expressions work.

- Match SPI registration files to implementation classes and preserve provider selection behavior.
- Shared modes / UI behavior must work with supported Spring Boot generations; respect each module's Java convention.
- Keep backend and Vue / React data contracts aligned. Preserve configured API prefixes and inspect controller mappings for actual routes; do not assume a hardcoded prefix.
- Form choices may come from Flovira-managed forms or host providers; condition fields may use the existing host callback. Preserve managed form-content endpoints, opaque `formId` values and tenant isolation; do not restore `form_custom`.
- Check date, null, generic and polymorphic serialization behavior across JSON providers when changing contracts.

## Verification

Compile affected plugins. Test relevant expressions, registrations or API contracts. For shared DTO changes, verify round-trip behavior with each supported JSON provider.
