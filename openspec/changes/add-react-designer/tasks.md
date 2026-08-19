## 1. Workspace And Package Scope

- [x] 1.1 Rename active frontend package references from `@luokuiai` to `@luokuiaiai`
- [x] 1.2 Add the React designer library and demo to the Bun workspace and root build scripts
- [x] 1.3 Regenerate the Bun lockfile and verify no active old-scope references remain

## 2. React Designer Core

- [x] 2.1 Define public Flovira definition, node, skip, provider, validation and imperative API types
- [x] 2.2 Implement lossless definition normalization, sequence/branch editing and Flovira JSON serialization
- [x] 2.3 Implement controlled/uncontrolled state, dirty tracking and bounded undo/redo history
- [x] 2.4 Implement structure and fixed-subprocess validation including 128-node definitions

## 3. React User Interface

- [x] 3.1 Implement the Tailwind process-tree canvas, semantic node cards, connectors and branch layout
- [x] 3.2 Implement node insertion/deletion, selection and property editing for supported node types
- [x] 3.3 Implement toolbar commands, import/export actions, viewport controls and responsive/dark styles
- [x] 3.4 Implement subprocess option loading with visible loading, empty and error states

## 4. Demo And Documentation

- [x] 4.1 Add a React demo that consumes `@luokuiaiai/flovira-react-designer` through `workspace:*`
- [x] 4.2 Document React installation, public API, provider injection, Tailwind stylesheet and npm scope migration

## 5. Verification

- [x] 5.1 Add unit tests for JSON round-trip, editing, validation, history and provider isolation
- [x] 5.2 Run React and Vue type checks, tests, library builds and all workspace demo builds
- [x] 5.3 Run OpenSpec strict validation and repository diff checks
- [x] 5.4 Start the React demo and verify desktop/mobile screenshots and non-overlapping canvas controls
