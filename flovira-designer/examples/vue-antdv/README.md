# Flovira Ant Design Vue 4 example

A consuming application for `@luokuiai/flovira-vue-designer` using Ant Design Vue 4.

The application shell and designer controls use Ant Design Vue. The adapter maps neutral `wf-*` components and imperative messages, notifications, dialogs, and loading feedback to Ant Design Vue.

Select the UI adapter with `setUiAdapter(antdvAdapter)` before rendering the designer.

## Run

From the repository root:

```bash
cd flovira-designer
bun install
cd vue
bun run build:lib
cd ../examples/vue-antdv
bun run dev
```

Open http://localhost:5181.

The example consumes the library's `dist-lib` output through a `workspace:*` dependency. After changing the library, run `bun run build:lib` in `flovira-designer/vue`, or run `bun run dev` there in another terminal to watch and rebuild.
