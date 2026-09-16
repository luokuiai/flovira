# Flovira Element Plus example

A consuming application for `@luokuiai/flovira-vue-designer` using Element Plus.

The example uses a localStorage-backed `demoProvider` to demonstrate listing, creating, saving, editing, previewing, exporting, and deleting workflows without a backend.

Select the UI adapter with `setUiAdapter(elementPlusAdapter)` before rendering the designer.

## Run

From the repository root:

```bash
cd flovira-designer
bun install
cd vue
bun run build:lib
cd ../examples/vue-element-plus
bun run dev
```

Open http://localhost:5180.

The example consumes the library's `dist-lib` output through a `workspace:*` dependency. After changing the library, run `bun run build:lib` in `flovira-designer/vue`, or run `bun run dev` there in another terminal to watch and rebuild.
