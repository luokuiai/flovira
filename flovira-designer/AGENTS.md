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
- Forms use opaque string `formId` references and may come from Flovira-managed forms or host resource callbacks. Keep form rendering host-integrated and do not restore `form_custom` or designer mode switching.

## Verification and publishing

From `flovira-designer` run relevant tests, `rtk bun run build:designer` and `rtk bun run build:demos`. Verify affected UI interactions in a consuming example.

npm publishing uses Lerna fixed versioning and GitHub Actions Trusted Publishing. Follow the release and tagging section in root `AGENTS.md`: manually align workspace versions on `release-<VERSION>`, validate, merge into `main`, create an annotated tag, and merge back into `develop`. Do not use `bun run release` for this process or publish individual packages manually. See `docs/releasing.md` for the publishing prerequisites and exact push order.
