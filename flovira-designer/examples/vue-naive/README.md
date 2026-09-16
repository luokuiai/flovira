# Flovira Naive UI example

A consuming application for `@luokuiai/flovira-vue-designer` using Naive UI.

The example uses a localStorage-backed `demoProvider` and includes integration and extension examples. The adapter uses `createDiscreteApi` for context-independent messages, dialogs, and notifications. The application configures `n-config-provider` with the `zhCN` locale.

Select the UI adapter with `setUiAdapter(naiveAdapter)` before rendering the designer.

## Run

From the repository root:

```bash
cd flovira-designer
bun install
cd vue
bun run build:lib
cd ../examples/vue-naive
bun run dev
```

Open http://localhost:5182.

The example consumes the library's `dist-lib` output through a `workspace:*` dependency. After changing the library, run `bun run build:lib` in `flovira-designer/vue`, or run `bun run dev` there in another terminal to watch and rebuild.
