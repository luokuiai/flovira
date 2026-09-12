<p align="center">
	<img alt="Flovira 标志" src="https://foruda.gitee.com/images/1726820610127990120/c8c5f3a4_2218307.png" width="100">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Flovira Vue 流程设计器</h1>
<p align="center">
	<a href="https://gitee.com/luokui/flovira/stargazers"><img src="https://gitee.com/luokui/flovira/badge/star.svg?theme=dark"></a>
</p>

## 介绍

`@luokuiai/flovira-vue-designer` 是 Flovira 的 Vue 3 流程设计器组件库，可嵌入业务页面，支持经典拖拽与仿钉钉两种设计模式。

- **流程编辑**：配置流程节点、连线、审批办理人、条件和监听器。
- **界面适配**：按需选择 Element Plus、Ant Design Vue 或 Naive UI，也可实现自定义适配器。
- **数据接入**：通过数据源接口连接后端，也可直接传入流程 JSON，使用模拟数据独立运行。
- **扩展能力**：支持自定义节点、画布扩展、插槽和流程结构校验。
- **程序控制**：提供保存、缩放、撤销、重做、导出等方法，以及流程变更事件。

## 作为 npm 组件库使用

设计器作为 Vue 3 组件库由业务方直接 `import` 使用，实现数据层与具体后端解耦。后端不再内嵌设计器页面。

### 构建库产物

```bash
bun run build:lib   # 输出到 dist-lib/（标准模块格式、类型声明 .d.ts 和合并样式）
```

### 安装与初始化

> 主入口 **界面组件库无关**，不内置任何 界面组件库。渲染 `FlowDesigner` 前必须先 `setUiAdapter(...)` 选择一个 界面适配器（element-plus / antdv / naive），否则设计器内的中性组件 `wf-*` 无法映射到具体 界面组件库、画布不渲染。

```ts
import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import { FloviraDesigner, setUiAdapter, setDataProvider, createMockProvider } from '@luokuiai/flovira-vue-designer'
// 界面适配器从子入口按需引入；antdv 用 `@luokuiai/flovira-vue-designer/antdv` 的 `antdvAdapter`
import { elementPlusAdapter } from '@luokuiai/flovira-vue-designer/element-plus'
import '@luokuiai/flovira-vue-designer/style'

const app = createApp(App)

// ① 选择 界面适配器（必须在渲染 FlowDesigner 之前调用）
setUiAdapter(elementPlusAdapter)
// ② 注册插件：全局 svg-icon 组件 + 中性组件 wf-*（图标零配置）
app.use(ElementPlus)
app.use(FloviraDesigner)
// ③ 可选：注入自定义数据源或模拟数据（无需后端）
setDataProvider(createMockProvider())
app.mount('#app')
```

### 组件用法

```vue
<template>
  <!-- 后端驱动：传入 definitionId，组件调用 queryDef 加载 -->
  <FlowDesigner :definition-id="defId" :only-design-show="true" :disabled="false" @close="onClose" @saved="onSaved" />

  <!-- 本地数据驱动：直接传入流程 JSON（initialJson 优先于 definitionId / queryDef） -->
  <FlowDesigner :initial-json="flowJson" :only-design-show="true" @saved="onSaved" />
</template>
<script setup>
import { FlowDesigner } from '@luokuiai/flovira-vue-designer'
</script>
```

#### FlowDesigner 组件属性

| 名称 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| definitionId | String | null | 流程定义标识，传入则 `queryDef` 加载该定义，否则新建 |
| initialJson | String \| Object | null | 初始流程 JSON（flovira 定义对象或其字符串）。**有值时优先于 `definitionId`/`queryDef`**，组件不再请求后端，直接渲染 / 编辑，实现纯组件用法 |
| disabled | Boolean | false | 只读模式（隐藏保存等编辑操作） |
| onlyDesignShow | Boolean | false | 仅显示画布（跳过基础信息步骤，直达流程设计） |
| showGrid | Boolean | false | 画布显示网格 |
| customNodes | Array | [] | 追加自定义 LogicFlow 节点（`lf.register`），在内置节点之后注册，可新增节点类型或覆盖内置同名 type |
| extraExtensions | Array | [] | 追加自定义 LogicFlow 扩展（`LogicFlow.use`），如 MiniMap / Control / Group |
| lfOptions | Object | {} | 透传合并到 LogicFlow 初始化选项（顶层覆盖内置 grid / keyboard / 交互开关等；`container` 由组件管理，请勿传入） |
| onBeforeUse | Function | - | 命令式扩展钩子：在 `extraExtensions` 之后、`new LogicFlow()` 之前调用，透出 LogicFlow 类，可注册**带配置**的扩展 `LF.use(Ext, { ...options })` |
| onRegister | Function | - | 命令式节点钩子：在 `customNodes` 之后、`render` 之前调用，透出 lf 实例，可批量 / 条件注册节点、注册自定义边或做渲染前设置 |
| json | String \| Object | - | 配合 `v-model:json` 的受控流程 JSON。语义：**初始注入（优先级高于 initialJson/definitionId）+ 变更回写**（`update:json`）。不做运行时反向重渲染（外部重载请配合 `:key` 重挂载，规避回环与撤销历史丢失） |
| structureValidator | Function | - | 自定义流程结构校验器 `(graph: { nodes, edges }) => string[] \| void`：在内置结构校验（≥1 开始 / ≥1 结束 / 无孤立节点）之后追加调用，返回错误信息数组。通过命令式 `validateStructure()` 触发；不自动拦截保存，可在 `before-save` 里据结果 `preventDefault()` |

#### FlowDesigner 组件事件

| 名称 | 回传 | 说明 |
| --- | --- | --- |
| @close | - | 宿主关闭回调（替代 iframe postMessage）；保存成功后也会自动触发 |
| @saved | `{ id, data, json }` | 保存成功：当前定义标识、后端返回 data（如新建后的 definitionId）、本次提交的流程 JSON |
| @ready | `{ lf }` | 画布初始化完成，透出底层 LogicFlow 实例，便于高级定制 |
| @before-save | `{ id, json, onlyDesignShow, setJson, preventDefault }` | 保存提交前（**同步**）：可 `setJson(next)` 改写提交内容，或 `preventDefault()` 取消本次保存（异步逻辑不会被等待） |
| @change | `{ dirty, getJson, getGraphData }` | 画布图数据变更（基于 LogicFlow `history:change`，初次渲染不触发）；取值函数按需执行，可获取 JSON 或图数据 |
| @dirty | `boolean` | 未保存状态翻转：首次变更 `false→true`，保存成功 / `resetDirty()` 后 `true→false`（仅画布图数据，不含基础信息表单字段） |
| @validate-error | `{ source, fields? }` | 基础信息校验未通过：`source` = save / step / api；`fields` 为无效字段明细（Element Plus 提供，Ant Design Vue 暂为空）。onlyDesignShow 模式无校验、不触发 |
| @node-click | `{ id, type, data, lf }` | 画布节点被点击 |
| @update:json | `string` | 配合 `v-model:json`：画布变更时回写最新流程 JSON（仅绑定 `json` 时派发） |

#### 插槽（未提供时使用默认内容）

| 名称 | 透出 | 说明 |
| --- | --- | --- |
| header-left | `{ flowName }` | 流程名区自定义 |
| header-actions | `{ save, disabled }` | 保存按钮区追加 |
| toolbar-extra | `{ lf, disabled }` | 工具栏追加自定义按钮 |
| logo | - | 画布水印 |
| node-form-extra | `{ form, disabled }` | 节点属性抽屉扩展点，可向任意节点注入自定义配置项 |

业务表单使用字符串 `formId` 引用，可选择 Flovira 管理的版本化表单，也可由接入方提供。基础信息设置流程表单，节点可覆盖，留空则继承。数据提供者的资源查询通过 `resourceType: 'FORM'` 返回 `{ id, name }` 选项；没有选项时可填写业务表单标识。旧 `formCustom` 与模式切换逻辑不再使用。
| header-center | `{ activeStep, steps, goToStep }` | 替换顶部中间的步骤切换区；`goToStep(index)` 跳转步骤 |
| loading | - | 初始流程定义加载中的覆盖层（默认「加载中…」） |
| empty | - | 加载完成但无可用定义（如 definitionId 失效）的覆盖层（默认「暂无流程定义」） |

#### 实例方法

通过组合式 `useFlowDesigner()`（推荐，带空安全包装）或模板 `ref` 获取实例，程序化操控设计器：

```ts
import { useFlowDesigner } from '@luokuiai/flovira-vue-designer'

const { designerRef, isReady, save, getFlowJson, getLogicFlow, zoom, undo, redo, clear } = useFlowDesigner()
// 模板：<FlowDesigner ref="designerRef" ... />
```

可用方法：`save / validate / getGraphData / getFlowJson / getFlowName / getLogicFlow / zoom / zoomIn / zoomOut / fitView / resetZoom / undo / redo / clear / downloadImage / downloadJson / isDirty / resetDirty / validateStructure`。

> `validateStructure()` 返回 `{ valid: boolean, errors: string[] }`：内置校验 ≥1 开始节点 / ≥1 结束节点 / 无孤立节点，并追加 `props.structureValidator` 的输出。不自动拦截保存，可在 `before-save` 里据 `valid` 决定是否 `preventDefault()`。

#### useFlowJson（流程 JSON 响应式只读视图）

把设计器当前 JSON 同步成响应式 `json` / `data`，便于实时预览、外部展示、脏检测（**单向读**：写入仍走 `initialJson` + `:key` 重挂载，不做反向写回画布，规避双向绑定回环）：

```ts
import { ref } from 'vue'
import { FlowDesigner, useFlowJson } from '@luokuiai/flovira-vue-designer'
import type { FlowDesignerInstance } from '@luokuiai/flovira-vue-designer'

const designerRef = ref<FlowDesignerInstance | null>(null)
const { json, data, dirty } = useFlowJson(designerRef)
// 模板：<FlowDesigner ref="designerRef" :initial-json="initial" v-on="useFlowJson(designerRef).bind" />
// 或在自己的 @ready/@change 里调用 flowJson.sync() 手动刷新
```

返回：`json`（字符串，响应式）、`data`（解析对象）、`dirty`（未保存标记）、`sync()`（手动拉取）、`bind`（可 `v-on` 展开的 ready/change/saved/dirty 监听集合；若你已单独绑定这些事件，请改用 `sync()` 以免事件被覆盖）。

### 自定义界面适配器（UiAdapter）

内置 `element-plus` / `antdv` / `naive` 三个适配器已覆盖主流场景。若要接入其它 界面组件库（Arco / TDesign…），实现 `UiAdapter` 契约并 `setUiAdapter(yourAdapter)` 即可——核心逻辑与 界面组件库解耦，无需改库源码。

适配器需提供命令式反馈（`message` / `notify` / `alert` / `confirm` / `prompt` / `loading`）、两个可选指令（`clickOutside` / `loadingDirective`），以及中性组件注册表 `components`（语义名 → 具体 界面组件库组件，供 `Wf*` 中性组件解析渲染目标）：

```ts
import { setUiAdapter } from '@luokuiai/flovira-vue-designer'
import type { UiAdapter } from '@luokuiai/flovira-vue-designer'

const myAdapter: UiAdapter = {
  name: 'your-ui-lib',
  // type: 'info' | 'success' | 'warning' | 'error'；content 可能是 string 或 { message, title, duration, ... }
  message: (type, content) => MyMessage[type](typeof content === 'string' ? content : content.message ?? ''),
  notify:  (type, content) => MyNotification[type](typeof content === 'string' ? { message: content } : content),
  alert:   (content, options) => MyModal.alert({ content, ...options }),          // 返回 Promise
  confirm: (content, options) => MyModal.confirm({ content, ...options }),         // 确认时兑现，取消时拒绝
  prompt:  (content, options) => MyModal.prompt({ content, ...options }).then((value) => ({ value })),
  loading: (options) => { const inst = MyLoading.open(options); return { close: () => inst.close() } },
  // 可选：自定义指令；不提供则相关交互降级（仍可用）
  clickOutside: myClickOutsideDirective,
  loadingDirective: myLoadingDirective,
  // 中性组件映射（按需补全，未注册的语义名对应组件不渲染，调用方已容错）
  components: { button: MyButton, input: MyInput }
}
setUiAdapter(myAdapter)
```

**使用建议**

- **脱上下文调用**：`message` / `notify` / `alert` 等由设计器内部命令式触发，需在「无组件上下文」下工作。优先用 界面组件库的全局 / 脱离上下文 API（如 naive 的 `createDiscreteApi`），不要依赖 `app.use` 注入到组件树的实例。
- **注册时机**：`setUiAdapter` 必须在渲染 `FlowDesigner` 之前调用；`app.use(FloviraDesigner)` 仅注册组件，不会默认选择适配器。
- **取消确认**：`confirm` 在用户取消时拒绝期约，请在调用处通过 `.catch()` 处理取消，避免未处理的异步拒绝。
- **参考实现**：`src/ui/{elementPlusAdapter,antdvAdapter,naiveAdapter}.ts`（含 28 组件映射 + 指令实现 + `createDiscreteApi` 脱上下文），照抄改名即可快速起步。

### 数据层（与后端解耦）

所有后端交互经由「数据源」(DataProvider)，默认使用 axios 发送请求；可注入自定义实现，仅覆盖关心的方法，其余自动回退：

```ts
import { setDataProvider } from '@luokuiai/flovira-vue-designer'

setDataProvider({
  queryDef: (id) => myHttp.get('/flow/def/' + id),
  saveJson: (data) => myHttp.post('/flow/save', data)
})
```

也可用 `createMockProvider()` 在无后端环境下运行，或运行期 URL 加 `?mock=true`。

**接口约定与使用建议**

- **方法清单**（均返回 `Promise`）：统一集成 `capabilities()` / `queryResources(query)` / `resolveRelationship(query)`；流程定义 `saveJson(data, onlyNodeSkip?)` / `queryDef(id?)` / `queryFlowChart(id)`；运行时查询 `subprocessSummary(...)` / `subprocessChildren(...)` / `subprocessEvents(...)` / `subprocessHistory(...)`；配置 `config()`。
- **部分覆盖**：`setDataProvider(partial)` 会与内置 HTTP 实现 `Object.assign` 合并，只需覆盖关心的方法，其余自动回退；传 `null` / 不传恢复为默认 HTTP 实现。
- **失败要抛**：方法内部失败请 `reject` / `throw`（而非静默返回空），以便设计器经 UiAdapter 反馈错误，避免「假成功」。
- **入参/出参宽松**：业务数据保持 `any`，不强约束后端响应结构，便于跨后端适配；按需在自己的实现里做强类型。
- **参考实现**：`src/data/{httpProvider,mockProvider}.ts`（默认 axios 实现 + 完整模拟数据，可作为自定义实现的对照）。

### 宿主依赖

宿主需提供：`vue`、`vue-router`、`pinia`、`@logicflow/core`、`@logicflow/extension`（必选）；界面组件库三选一——`element-plus`（子入口 `@luokuiai/flovira-vue-designer/element-plus`）/ `ant-design-vue@4`（子入口 `@luokuiai/flovira-vue-designer/antdv`）/ `naive-ui`（子入口 `@luokuiai/flovira-vue-designer/naive`），均为可选的宿主依赖，按所选适配器 `setUiAdapter(...)`。图标已内置（iconify 离线集 `ep` + `wf`，随库打包，无需额外依赖）。

### 本地开发与示例

在仓库根目录执行：

```bash
# 安装工作区依赖并构建组件库
cd flovira-designer
bun install
cd vue
bun run build:lib

# 启动 Element Plus 示例
cd ../examples/vue-element-plus
bun run dev
```

其它示例位于 `flovira-designer/examples/vue-antdv` 和 `flovira-designer/examples/vue-naive`，可在对应目录执行 `bun run dev`。

开发组件库时，在另一个终端进入 `flovira-designer/vue` 并执行 `bun run dev`，监听源码变更并重新构建。

包名为 `@luokuiai/flovira-vue-designer`；本地工作区通过 `workspace:*` 引用组件库的 `dist-lib` 构建产物。

设计器统一使用一套审批流程画布，不再提供经典/仿钉钉模式切换，流程定义无需 modelValue。
