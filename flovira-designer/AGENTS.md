# Designer workspace instructions

Follow [root AGENTS.md](../AGENTS.md). This file contains only frontend-specific rules.

## Scope

- `vue`: `@luokuiai/flovira-vue-designer`.
- `react`: `@luokuiai/flovira-react-designer`.
- `react-adapters`: optional UI framework adapters; do not import their framework dependencies into the React core.
- `examples`: consuming applications using Bun `workspace:*` dependencies.

Vue and React share a workspace, not framework dependencies or a combined package API. The backend exposes APIs only; do not restore bundled designer pages, WebJars or static-resource packaging.

## Changes

- Reuse each framework's established implementation and interactions. Internal Vue / React structures need not be identical.
- Use a single workflow design model; do not expose classic / mimic mode switching.
- Shared workflow JSON, node properties and API paths must match core and UI backend contracts.
- Public exports, props, events, styles and package names are contracts. Follow root compatibility rules and current user authorization.
- Use Bun and the workspace lockfile. Do not add independent child lockfiles.
- Forms are external business references (`formId`). Resource callbacks provide choices and condition fields; do not add a built-in form designer or form-content store.

## Verification and publishing

From `flovira-designer` run relevant tests, `rtk bun run build:designer` and `rtk bun run build:demos`. Verify affected UI interactions in a consuming example.

npm publishing uses Lerna fixed versioning. The authorized release flow runs `bun run release` on `main`, updates all designer and adapter versions, and pushes a version tag. GitHub Actions publishes public packages through npm Trusted Publishing. Do not manually release only one package or trigger publishing without authorization.
