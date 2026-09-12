# Vue designer instructions

Follow [workspace instructions](../AGENTS.md) and [root instructions](../../AGENTS.md). This is a frontend library; Java / ORM compilation rules do not apply to its implementation.

## Library and dependencies

`@luokuiai/flovira-vue-designer` is a Vue 3 component library, with no standalone application shell or pages bundled into backend jars. Consuming examples are `../examples/vue-element-plus`, `vue-antdv` and `vue-naive` (development ports 5180, 5181, 5182).

- Inspect `package.json` for actual versions. The library uses Vue, Vite, LogicFlow, Pinia, axios, Sass and file-saver.
- Icons use offline Iconify through `src/icons`; do not restore Element Plus icon imports or SVG sprites.
- Framework / LogicFlow dependencies are peers. UI libraries are optional peers. Bundled helpers belong in dev dependencies. Do not move framework or UI libraries into runtime dependencies.
- Use TypeScript under `src`; incrementally use `lang="ts"` in Vue script setup. Public designer / data / UI entry points must emit declarations.
- This frontend package uses MIT licensing; do not replace its notices with backend Apache headers.
- Follow the unified designer contract. Legacy internal rendering files do not authorize restoring a public mode switch.

## Commands and verification

From `flovira-designer`, use Bun workspace installation. From this package:

```bash
rtk bun run build:lib
rtk bun run dev
rtk bun run mock
rtk bun run test
```

`build:lib` builds the main bundle and all three adapter entry points into `dist-lib`. `dev` watches library builds; it is not an app server. Run a consuming example's `bun run dev` alongside it. The mock HTTP server uses port 8080.

After library changes, build it and at least one consuming example. Check affected graph / configuration / adapter interactions through a demo or browser tests. After adapter changes, verify feedback, dialogs and loading with the affected adapter. Report failures honestly. LogicFlow upgrades and public entry-point changes require particular care.

## Data and UI adapters

- Read backend DTO / VO contracts before changing definition JSON, node properties, approvers, conditions or listeners.
- Components use `@/api`, which delegates to `getDataProvider()`. Consumers may inject `setDataProvider(...)`; do not hardcode business endpoints in components.
- Business forms use opaque `formId` and `FORM` resource options backed by Flovira-managed forms or host resources. Keep rendering host-integrated; do not restore `form_custom`, form-create dependencies or designer mode switching.
- Imperative messages, notifications, dialogs, loading and click-outside behavior go through `getUiAdapter()`.
- The main designer entry registers neutral `wf-*` components and icons only. No static UI framework imports or default adapter registration. Consumers must call `setUiAdapter(...)` before rendering.
- Element Plus, Ant Design Vue and Naive UI adapters are separate `/element-plus`, `/antdv`, `/naive` exports, built with their own Vite configs. Each externalizes Vue and its framework peer.
- Naive UI imperative APIs use lazy `createDiscreteApi` inside its adapter.
- New controls use neutral `wf-*` components. Framework imports belong only in matching adapters. New adapters implement the entire existing `UiAdapter` contract and get independent entry points; do not add framework conditionals to core components.
- Existing `.el-*` CSS selectors are style compatibility concerns, not permission to add runtime coupling.
- Public exports, styles, `DataProvider`, `UiAdapter` and package names follow root contract rules.

## Design system

Use `common/vue/baseInfo.vue` as the visual reference and reuse existing theme tokens / `_common.scss` mixins. Do not invent a separate panel style.

- Colors come from `src/config/themeConfig.ts` and `--wf-*` CSS variables, with fallbacks. Primary family: `--wf-primary` #409eff, dark #2b7de9, light #ecf5ff, lighter #f0f7ff. Danger #f56c6c, success #67c23a, listener purple #8960dc.
- Radius tokens: 8px default, 12px large, 4px small. Use the existing system font stack (`-apple-system`, BlinkMacSystemFont, SF Pro Text, PingFang SC, Helvetica Neue, Microsoft YaHei, sans-serif) and antialiasing.
- Reuse `modern-tabs`, `base-settings-card`, `section-card`, `table-form-align` and `responsive-adaption` mixins. Prefer shared token / mixin changes over per-component overrides.
- Standalone inputs: white background even when empty, light 1px border, approximately 10px radius, no default shadow. Hover uses primary border; focus adds the existing soft blue ring. Validation errors override blue focus with red border / ring.
- Use default control sizes (~32px height), not small controls. Empty fields must not look disabled through gray fills. Table controls may remain transparent until hover / focus.
- Labels are right-aligned, 110px wide, weight 600 and primary text color. Use flat sections with a bold 15px title and light bottom separator; do not restore bordered section cards with gradient headers and shadows. Keep roughly 24px form-row spacing.
- Tabs use flat underline styling: icon plus label, neutral inactive text, primary active text with 2px underline. No filled pills, outer white boxes or floating shadows. Icons use 16px SVG with currentColor.
- Avoid duplicate headings inside a titled drawer. Tab names supply section headings; single-section edge / branch / end panels do not need another repeated title.
- Tables use white headers, subtle separators and existing inline-edit styling; follow `table-form-align` for vertical alignment. Use established card radios where appropriate; ordinary boolean settings can use neutral switches.
- Add-row buttons use a dashed primary border and transparent background, becoming solid with a light primary background / shadow on hover. Primary actions use solid primary fill and ~10px radius. Apply icon / text gap to the actual inline-flex wrapper.
- Teleported drawers need global styles scoped under `.property-drawer-modern`. Dialog roots and bodies need opaque backgrounds and the established overlay. Use flat complete `:global(...)` selectors; do not nest children inside `:global()`.
- Drawer headings use primary text, weight 600, 16px. Preserve the existing compact header/body spacing and dark-mode overrides.
- Workflow nodes use the established node-type palette and soft shadows. Existing mimic rendering uses white cards and blue header gradients; legacy classic rendering keeps its semantic type colors where still used. Keep sidebar icons and their matching renderer consistent.
- Preserve runtime status colors and backend `chartStatusColor` semantics. Design colors must not overwrite runtime pending / completed / unreached / returned states. Avoid default black dashed selection outlines; use the established soft blue outline.
- Add `html.dark` and `--wf-*` support for new inputs, tables, drawers and cards; do not implement only light mode.

Current explicit user design instructions override these defaults.
