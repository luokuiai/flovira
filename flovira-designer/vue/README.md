# Flovira Vue Designer

`@luokuiai/flovira-vue-designer` is an embeddable Vue 3 workflow designer for Flovira. It provides a unified approval-flow canvas for editing nodes, connections, approvers, conditions, and listeners.

- UI adapters for Element Plus, Ant Design Vue, and Naive UI.
- Host-provided data access or direct workflow JSON.
- Custom nodes, LogicFlow extensions, slots, and structure validation.
- Imperative validation, data access, zoom, undo, redo, and export utilities.

The backend provides APIs; host applications build and deploy the frontend.

## Setup

The main entry is UI-library independent. Call `setUiAdapter(...)` before rendering `FlowDesigner` so neutral `wf-*` components can resolve their UI implementations.

```ts
import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import {
  FloviraDesigner,
  setUiAdapter,
  setDataProvider,
  createMockProvider,
} from '@luokuiai/flovira-vue-designer'
import { elementPlusAdapter } from '@luokuiai/flovira-vue-designer/element-plus'
import '@luokuiai/flovira-vue-designer/style'

const app = createApp(App)
setUiAdapter(elementPlusAdapter)
app.use(ElementPlus)
app.use(FloviraDesigner)
// Optional: run without a backend.
setDataProvider(createMockProvider())
app.mount('#app')
```

Other adapter entries are `@luokuiai/flovira-vue-designer/antdv` (`antdvAdapter`) and `@luokuiai/flovira-vue-designer/naive` (`naiveAdapter`).

### Host dependencies

Provide `vue`, `vue-router`, `pinia`, `@logicflow/core`, and `@logicflow/extension`, plus the UI library selected by your adapter: `element-plus`, `ant-design-vue@4`, or `naive-ui`. See [package.json](./package.json) for peer dependency ranges. Icons are bundled and need no separate icon package.

## Component usage

```vue
<template>
  <!-- Backend data: queryDef loads the requested definition. -->
  <FlowDesigner :definition-id="defId" :only-design-show="true" />

  <!-- Local data: initialJson takes precedence over definitionId. -->
  <FlowDesigner :initial-json="flowJson" :only-design-show="true" />
</template>

<script setup>
import { FlowDesigner } from '@luokuiai/flovira-vue-designer'
// Supply defId and flowJson from your application.
</script>
```

### Props

| Name | Type | Default | Description |
| --- | --- | --- | --- |
| definitionId | String | null | Definition identifier passed to `queryDef`; omit to create a definition |
| initialJson | String \| Object | null | Initial Flovira definition; takes precedence over `definitionId` and avoids loading it from the backend |
| disabled | Boolean | false | Read-only mode |
| toolbar | Boolean | true | Show the top toolbar; does not disable editing or imperative APIs |
| appearance | 'standalone' \| 'embedded' | standalone | Rounded card or embedded content without outer border, radius, or shadow; internal controls and toolbar are unchanged |
| onlyDesignShow | Boolean | false | Show the canvas directly, skipping the basic-information step |
| showGrid | Boolean | false | Show the canvas grid |
| customNodes | Array | [] | Register LogicFlow nodes after built-in nodes; may add types or override matching types |
| extraExtensions | Array | [] | Additional LogicFlow extensions, such as MiniMap, Control, or Group |
| lfOptions | Object | {} | Top-level LogicFlow initialization overrides; do not provide the component-managed `container` |
| onBeforeUse | Function | - | Receives the LogicFlow class after extension registration and before construction; supports `LF.use(Ext, options)` |
| onRegister | Function | - | Receives the LogicFlow instance after custom node registration and before rendering |
| json | String \| Object | - | Initial input plus `update:json` output for `v-model:json`; takes initial precedence over `initialJson` and `definitionId`. External replacement requires remounting with `:key`; runtime changes do not re-render the canvas |
| structureValidator | Function | - | `(graph: { nodes, edges }) => string[] \| void`; adds checks after built-in structure validation |
| conditionFields | Array | [] | Readable form fields for visual connection conditions |

The designer fills its parent. Provide an explicit container height; in flex layouts, give the content area `flex: 1; min-height: 0` within a height-constrained ancestor.

### Events

| Name | Payload | Description |
| --- | --- | --- |
| @close | - | Host close notification; successful persistence does not automatically close the editor |
| @ready | `{ lf }` | Canvas initialization completed |
| @change | `{ dirty, getJson, getGraphData }` | Graph change from LogicFlow history; not emitted for the initial render; getters retrieve data on demand |
| @dirty | `boolean` | Graph dirty state changed; excludes basic-information fields |
| @validate-error | `{ source, fields? }` | Basic-information validation failed; source is `step` or `api`. Field details are supplied by Element Plus and currently empty for Ant Design Vue. Not emitted in `onlyDesignShow` mode |
| @node-click | `{ id, type, data, lf }` | Canvas node clicked |
| @update:json | `string` | Updated workflow JSON when the `json` prop is bound |

### Slots

Unspecified slots keep their default content.

| Name | Context | Description |
| --- | --- | --- |
| header-left | `{ flowName }` | Workflow title area |
| toolbar | `{ disabled, dirty, activeStep, steps, goToStep }` | Replace the entire top toolbar |
| header-actions | Same as toolbar | Replace right-side actions |
| toolbar-extra | `{ lf, disabled }` | Additional canvas toolbar actions |
| logo | - | Canvas watermark |
| node-form-extra | `{ form, disabled }` | Extend node property configuration |
| header-center | `{ activeStep, steps, goToStep }` | Replace step navigation; call `goToStep(index)` to navigate |
| loading | - | Overlay while the initial definition loads |
| empty | - | Overlay when loading completes without a usable definition |

### Imperative API and host actions

Use a template ref or the null-safe `useFlowDesigner()` wrapper:

```ts
import { useFlowDesigner } from '@luokuiai/flovira-vue-designer'

const {
  designerRef, isReady, validate, getFlowJson,
  getLogicFlow, zoom, undo, redo, clear,
} = useFlowDesigner()
// Bind with <FlowDesigner ref="designerRef" ... />.
```

Available methods: `validate`, `getGraphData`, `getFlowJson`, `getFlowName`, `getLogicFlow`, `zoom`, `zoomIn`, `zoomOut`, `fitView`, `resetZoom`, `undo`, `redo`, `clear`, `downloadImage`, `downloadJson`, `isDirty`, `resetDirty`, and `validateStructure`.

The designer has no save or publish buttons, methods, or callbacks. The host owns actions, requests, loading indicators, error handling, and navigation. Hide the toolbar with `:toolbar="false"` or customize the `toolbar` / `header-actions` slots.

Call `validate()` for basic information and `validateStructure()` for the graph, then read `getFlowJson()` and submit through your application. After success, call `resetDirty()`. If editing remains enabled during submission, first confirm the current JSON still matches the submitted snapshot so later edits stay dirty. The host can validate and submit basic information, form content, and workflow design together.

`validateStructure()` returns `{ valid: boolean, errors: string[] }`. Built-in checks require at least one start node, at least one end node, and no isolated nodes, followed by `structureValidator` checks. The host handles the result.

Combine `onlyDesignShow` and `:toolbar="false"` for canvas-only embedding. Use the slot's `activeStep`, `steps`, and `goToStep` when custom navigation is needed.

### Reactive JSON view

`useFlowJson` exposes a one-way reactive view for previews and dirty-state tracking. To replace input data, use `initialJson` and remount with `:key`.

```ts
import { ref } from 'vue'
import { useFlowJson, type FlowDesignerInstance } from '@luokuiai/flovira-vue-designer'

const designerRef = ref<FlowDesignerInstance | null>(null)
const flowJson = useFlowJson(designerRef)
const { json, data, dirty } = flowJson
// <FlowDesigner ref="designerRef" :initial-json="initial" v-on="flowJson.bind" />
// Alternatively, call flowJson.sync() from your own ready/change handlers.
```

Returns reactive `json`, parsed `data`, `dirty`, a manual `sync()` function, and a `bind` object for ready/change/dirty listeners. If those events already have host handlers, use `sync()` to avoid replacing them.

## Forms, conditions, and workflow packages

Forms use opaque string `formId` references and can be versioned Flovira-managed forms or host-provided forms. Basic information selects the workflow form; nodes may override it or inherit it by leaving the reference empty. Resource queries with `resourceType: 'FORM'` return `{ id, name }` options; a form identifier can also be entered when no options are available.

Use `getFormConditionFields` to turn standard form metadata into readable fields and pass them through `conditionFields`. Array detail groups support any-row or all-rows matching, with all conditions in a group applying to the same row. See [nested form fields and conditions](../../docs/form-field-conditions.md).

Subprocess candidates load when the selector opens, with keyword search and pages of 20 items. The host's `queryResources` must handle `keyword`, `pageNum`, and `pageSize`, and return the matching `total`. Selected workflows remain visible by code even when absent from the loaded page or after a failed request.

Complete workflow transfer uses backend workflow-package methods. The designer has no built-in JSON import/export buttons; its imperative JSON utilities handle design data only. Use `parseWorkflowPackage` and `getPackageDefinition` to obtain a root workflow or subprocess and pass it as `initialJson`. `getPackageForm` and `parsePackageFormContent` expose form metadata and content to host renderers. See [workflow package integration](../../docs/workflow-packages.md).

## Custom UI adapters

Implement `UiAdapter` and call `setUiAdapter(yourAdapter)` to integrate another UI library. Adapters provide imperative feedback, optional directives, and a neutral component registry:

```ts
import { setUiAdapter, type UiAdapter } from '@luokuiai/flovira-vue-designer'

const myAdapter: UiAdapter = {
  name: 'your-ui-lib',
  // content may be a string or an object with message, title, duration, etc.
  message: (type, content) => MyMessage[type](typeof content === 'string' ? content : content.message ?? ''),
  notify: (type, content) => MyNotification[type](typeof content === 'string' ? { message: content } : content),
  alert: (content, options) => MyModal.alert({ content, ...options }),
  // Resolve on confirmation; reject on cancellation.
  confirm: (content, options) => MyModal.confirm({ content, ...options }),
  prompt: (content, options) => MyModal.prompt({ content, ...options }).then(value => ({ value })),
  loading: options => {
    const instance = MyLoading.open(options)
    return { close: () => instance.close() }
  },
  clickOutside: myClickOutsideDirective,
  loadingDirective: myLoadingDirective,
  // Complete the mappings required by your application.
  components: { button: MyButton, input: MyInput },
}
setUiAdapter(myAdapter)
```

Feedback APIs must work outside component context; prefer global or discrete APIs such as Naive UI's `createDiscreteApi`. Register the adapter before rendering; `app.use(FloviraDesigner)` registers components but does not select an adapter. Handle rejected confirmation promises on cancellation.

Reference implementations: `src/ui/elementPlusAdapter.ts`, `antdvAdapter.ts`, and `naiveAdapter.ts`.

## Data provider

Backend access goes through `DataProvider`, with an axios-based HTTP implementation by default. Override only the required methods:

```ts
import { setDataProvider } from '@luokuiai/flovira-vue-designer'

setDataProvider({
  queryDef: id => myHttp.get('/flow/def/' + id),
  saveJson: data => myHttp.post('/flow/save', data),
})
```

Use `createMockProvider()` or the runtime URL parameter `?mock=true` for backend-free development.

- Integration: `capabilities()`, `queryResources(query)`.
- Definitions: `saveJson(data)`, `queryDef(id?)`, `queryFlowChart(id)`.
- Runtime queries: `subprocessSummary`, `subprocessChildren`, `subprocessEvents`, `subprocessHistory`.
- Configuration: `config()`.

Methods return promises. Partial providers merge with the default HTTP implementation; pass `null` or omit the argument to restore it. Throw or reject on failures rather than returning a simulated success. Provider payloads are deliberately flexible; add application-specific typing in your implementation. See `src/data/httpProvider.ts` and `mockProvider.ts`.

## Local development

From the repository root:

```bash
cd flovira-designer
bun install
cd vue
bun run build:lib
cd ../examples/vue-element-plus
bun run dev
```

Library output is written to `dist-lib`, including modules, declarations, and styles. Run `bun run dev` in `flovira-designer/vue` in another terminal to watch and rebuild. Other examples are in `examples/vue-antdv` and `examples/vue-naive`; all consume the built library through `workspace:*`.

## Form designer background

`FormDesigner` accepts an optional `background` prop with any CSS background value, including colors, gradients, `transparent`, and CSS variables. Omit it to use the theme default. Both `standalone` and `embedded` retain a 12px corner radius; `embedded` removes only the outer border. This setting affects the container, not field controls or saved form metadata.

```vue
<FormDesigner
  v-model="form"
  appearance="embedded"
  background="var(--business-form-background, #f8fafc)"
/>
```

Vue root `style` attributes take precedence over `background` and can also customize `borderRadius`.
