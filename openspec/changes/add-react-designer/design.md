## Context

Flovira currently ships a Vue 3 component library backed by LogicFlow and a separate Vue webjar application. React consumers need a native package, but the existing Vue implementation is tightly coupled to Vue component and UI-adapter conventions. Intelliconf demonstrates a React/Tailwind approval designer based on an automatically laid-out process tree rather than a free-coordinate graph.

The new package must preserve Flovira's serialized definition contract and subprocess fields. It must remain an embeddable SDK component: no router, application shell, global data store, or fixed backend URL.

## Goals / Non-Goals

**Goals:**

- Publish an independently consumable React component package under `@luokuiaiai`.
- Provide a focused approval-flow editing loop with automatic sequence/branch layout and Tailwind styling.
- Round-trip Flovira definition JSON without requiring a backend and expose a partial, injectable data Provider for hosted use.
- Keep React and Vue implementations independent while aligning their public concepts and npm scope.
- Verify the package through unit tests, a workspace demo, production builds, and browser screenshots.

**Non-Goals:**

- Reimplement the standalone `flovira-ui` webjar in React.
- Replace or internally wrap the Vue package.
- Add a new engine JSON schema, backend endpoint, database table, or dynamic subprocess selection mode.
- Copy Intelliconf workflow APIs, form model, assignee model, or publishing lifecycle.

## Decisions

### Use a controlled process-tree model

The component accepts and emits a normalized `FlowDefinition` containing nodes and skips. Internally it derives a tree of sequential and branching nodes for rendering, while serialization maps edits back to Flovira node/skip records. Fixed layout removes coordinate drift and makes hundreds of approval nodes cheaper to render than a general graph engine.

Alternative: reuse LogicFlow or add React Flow. This would preserve free-coordinate editing but adds a large peer dependency, duplicates Vue-specific graph behavior, and does not match the referenced approval-flow interaction.

### Keep data access host-injected

`ReactFlowDesigner` supports `value`/`defaultValue` and `onChange`; persistence remains the host's responsibility. A `DesignerDataProvider` supplies published subprocess definitions when present. No module-global provider is used, so multiple designer instances can safely target different tenants or services.

### Use Tailwind as a compiled implementation detail

Source components use Tailwind utilities and the package build emits one namespaced CSS asset. Tailwind is a development dependency rather than a consumer peer. Stable `flovira-react-*` root classes and CSS variables provide theme hooks while preventing the host from needing Tailwind configuration.

### Preserve unknown JSON fields

Normalization copies unknown definition, node, property and skip fields. Editing known fields updates only those keys. This is required for interoperability with current definitions and future engine extensions.

### Migrate npm scope in one workspace change

All workspace package names, dependencies, imports and documentation move from `@luokuiai` to `@luokuiaiai`. Since the fork restarts at 1.0.0 and packages are not treated as a legacy release line, no compatibility alias package is introduced.

## Risks / Trade-offs

- [Tree rendering cannot express arbitrary free-position diagrams] -> Keep the Vue/LogicFlow package available for classic free-layout use; React focuses on approval flows.
- [JSON mapping could discard fields] -> Preserve unknown fields and add round-trip tests with subprocess and gateway data.
- [Tailwind utilities could leak or be overridden] -> Compile a package stylesheet, scope component styles below the package root, and expose semantic CSS variables.
- [npm scope migration breaks old imports] -> Update every workspace reference and document the breaking rename before release.
- [Large flow trees can create excessive DOM] -> Collapse branch details by default where appropriate and avoid global state subscriptions; add a large-definition render test.

## Migration Plan

1. Rename existing workspace package references to `@luokuiaiai` and regenerate the lockfile.
2. Add the React library and demo to the workspace without changing Vue build outputs.
3. Build/test both designer packages and all demos.
4. Publish new 1.0.0 packages under `@luokuiaiai`; consumers update imports explicitly.

Rollback consists of removing the new workspaces and restoring package names before publication. No runtime or database migration is required.

## Open Questions

None for the first implementation. Additional node property editors can be added through additive renderer/editor hooks after the core contract is stable.
