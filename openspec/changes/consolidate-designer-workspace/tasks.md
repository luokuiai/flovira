## 1. Workspace Consolidation

- [x] 1.1 Move Vue, React, and example projects under `flovira-designer/`
- [x] 1.2 Update root Bun workspaces and build scripts for the new paths

## 2. Package Naming

- [x] 2.1 Rename the Vue package and every active dependency/import to `@luokuiai/flovira-vue-designer`
- [x] 2.2 Refresh the Bun lockfile and verify Vue/React package independence

## 3. Embedded UI Removal

- [x] 3.1 Delete the `flovira-ui` SPA and `flovira-plugin-vue3-ui` static-resource module
- [x] 3.2 Remove Gradle dependencies, static mappings, and build entries while retaining backend API controllers

## 4. Documentation

- [x] 4.1 Update root/module AGENTS and README to describe npm-only designer delivery
- [x] 4.2 Remove or update active comments and documentation that reference old paths, package names, or embedded UI delivery

## 5. Verification

- [x] 5.1 Install the root Bun workspace and build both designer packages
- [x] 5.2 Build all designer examples
- [x] 5.3 Compile the retained UI core, Spring web, and Solon web backend modules
- [x] 5.4 Validate OpenSpec and confirm removed modules/package names have no active references
