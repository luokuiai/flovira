# @luokuiai/flovira-react-designer

Flovira 的 React 审批流程设计器。组件直接读写 Flovira `nodeList/skipList` JSON，不依赖路由、Vue、Tailwind、LogicFlow 或固定后端地址。

```bash
bun add @luokuiai/flovira-react-designer react react-dom
```

```tsx
import { useRef, type ReactNode } from 'react'
import {
  ReactFlowDesigner,
  type DesignerCapabilities,
  type ApproverEditorRenderContext,
  type DesignerResourceLoader,
  type ReactFlowDesignerRef,
} from '@luokuiai/flovira-react-designer'
import '@luokuiai/flovira-react-designer/style.css'

export function ProcessEditor({
  capabilities,
  queryResources,
  renderApproverEditor,
}: {
  capabilities?: DesignerCapabilities
  queryResources?: DesignerResourceLoader
  renderApproverEditor?: (context: ApproverEditorRenderContext) => ReactNode
}) {
  const designer = useRef<ReactFlowDesignerRef>(null)

  return (
    <ReactFlowDesigner
      ref={designer}
      defaultValue={{ flowCode: 'leave', flowName: '请假审批', nodeList: [] }}
      capabilities={capabilities}
      queryResources={queryResources}
      renderApproverEditor={renderApproverEditor}
      onSave={async (_definition, json) => {
        await fetch('/api/flows/leave', { method: 'PUT', body: json })
      }}
    />
  )
}
```

## API

- `value` / `defaultValue`: Flovira 定义对象或 JSON 字符串，分别用于受控和非受控模式。
- `onChange`: 返回最新定义、JSON 和 dirty 状态。
- `onSave`: 可选保存回调；组件不绑定具体 HTTP 客户端。
- `capabilities`: 宿主从后端加载后传入的设计器能力和办理人策略；不传时使用内置默认值。
- `queryResources`: 宿主提供的资源查询函数，用于默认列表选择器和子流程等数据；提供自定义选择器时可自行加载业务数据。
- `renderApproverEditor`: 注入业务办理人编辑器。回调会收到当前节点、策略、完整 `rule`、只读状态以及 `onChange` / `onRuleChange`；可渲染组织树、表格或任意业务配置。
- `renderNode`: 自定义节点卡片渲染器。
- `ui`: 可选 UI Adapter，按需替换 Button、Input、Select、Checkbox、RadioGroup、Field、Tooltip、DropdownMenu、Drawer 和 Dialog；未传入的控件继续使用默认实现。
- ref: `getDefinition`、`getFlowJson`、`importJson`、`validate`、`undo`、`redo`、缩放和定位命令。

默认 UI 使用包内作用域 CSS，消费方只需引入 `style.css`，不需要安装或配置 Tailwind。UI Adapter 使用 `onPress`、`onValueChange` 和 `onCheckedChange` 等语义事件，便于对接 Ant Design、Arco Design、MUI 或业务组件库。

办理人类型不在 React 包中枚举。设计器直接渲染宿主传入的
`approverStrategies`。`selectionType` 描述运行时解析语义；`editorType` 独立描述设计器交互：
`NONE` 不展示配置、`INLINE` 在 Drawer 内展示、`DIALOG` 展示 `+` 并打开适配器 Dialog。
`multiple` 控制单选或多选，`editorKey` 用于让宿主定位对应业务编辑器。自定义编辑器可以通过
`ApproverRule.config` 保存供业务解析器消费的结构化配置。

`resultCardinality` 描述策略解析为具体人员后的数量范围，而不是选择器选择数量：
`EXACTLY_ONE`、`ONE_OR_MORE`、`ZERO_OR_ONE` 或 `ZERO_OR_MORE`。附加选项可用
`condition: MULTIPLE | EMPTY | ALWAYS` 声明展示条件。设计器因此会对直接单选的具体人员隐藏
多人和无人策略，对直接多选人员只展示多人策略，对可能解析出 0 到多人的分组同时展示两者。
内置能力还提供“审批人与提交人为同一人时”选项，默认值包括本人审批、跳过或由其他人审批、
转交部门负责人；接入方可在自己的策略 `options` 中替换或移除它。

每种策略还可以声明 `options`。设计器会将这些选项渲染为 RadioGroup，并按 `code` 写入
`ApproverRule.config`，适合配置仅对部分类型有意义的多人或无人审批策略：

```ts
{
  code: 'GROUP',
  name: '分组',
  selectionType: 'RESOURCE',
  resourceType: 'GROUP',
  multiple: false,
  editorType: 'DIALOG',
  options: [
    {
      code: 'emptyPolicy',
      name: '无人审批策略',
      defaultValue: 'FAIL',
      nodeTypes: ['1'],
      condition: 'EMPTY',
      choices: [
        { value: 'FAIL', label: '阻止提交' },
        { value: 'TO_ADMIN', label: '转交管理员' },
      ],
    },
  ],
  resultCardinality: 'ZERO_OR_MORE',
}
```

提交人、指定人员、指定角色等业务策略及对应的人员解析器均由接入方后端配置。

官方适配包：

- `@luokuiai/flovira-react-adapter-lumen`
- `@luokuiai/flovira-react-adapter-antd`

Adapter 可以完整传入，也可以只覆盖需要统一的控件，其余控件自动回退到默认实现：

```tsx
import { antdDesignerUi } from '@luokuiai/flovira-react-adapter-antd'
import '@luokuiai/flovira-react-adapter-antd/style.css'

<ReactFlowDesigner ui={antdDesignerUi} />
```

## 条件分支配置

条件分支使用独立条件卡片：点击卡片配置规则，卡片下方的「＋」添加审批、等待、子流程或嵌套分支，不会为了表示条件自动创建审批任务。组内条件为「且」，条件组之间为「或」；条件分支的「其他条件」为兜底，多选分支的「始终进入」为无条件执行，并行分支无需条件。

流程通过字符串 `formId` 引用业务表单，表单结构和页面由接入方维护。开始节点配置流程表单，审批节点可覆盖表单，留空则继承。提供 `queryResources({ resourceType: 'FORM', ... })` 可加载业务表单选项；未提供资源回调时可直接填写表单标识。

业务字段来自接入方的表单。通过 `queryConditionFields` 回调获取，可读取 `definition.formId`；组件不绑定表单接口。打开条件配置时加载，支持加载提示、失败重试，并忽略关闭或切换分支后的过期响应。例如：

```tsx
<ReactFlowDesigner
  queryConditionFields={async ({ definition, node, branch }) => {
    const fields = await loadYourFormFields(definition)
    return fields.map((field) => ({
      code: field.code,
      label: field.label,
      type: field.type, // STRING、NUMBER 或 BOOLEAN
    }))
  }}
/>
```

可视化规则默认编译为 `spel@@#{...}`，执行端需要启用已有的 Spring SpEL 条件策略；使用其他条件引擎时，通过 `compileBranchConditions(groups)` 属性返回对应的条件表达式。字段编码默认支持字母、数字和下划线，且不能以数字开头。规则元数据保存在分支父节点的 `ext.branchConditions` 条目，运行表达式仍保存在连线 `skipCondition`，不增加数据库字段。已有表达式不自动转换或覆盖，可继续通过「表达式」模式编辑；回调成功返回空字段时仍可使用表达式模式；也可通过 `conditionFields` 直接传入已有字段，回调优先。尚未配置完整的新增条件分支会阻止界面保存。

## 只读流程预览

`FlowPreview` 用于业务详情页或弹窗：节点使用统一 40 像素的类型色纯色圆角方块与白色图标，当前节点以小圆点标记，名称统一显示在卡片顶部。所有节点均可悬浮或键盘聚焦查看完整名称与办理人。支持并行分支、汇合、自动适应容器、缩放和拖动画布空白处平移。

```tsx
import { FlowPreview, type FloviraDefinition } from '@luokuiai/flovira-react-designer'
import '@luokuiai/flovira-react-designer/style.css'

export function ProcessProgress({ definition }: { definition: FloviraDefinition }) {
  return (
    <FlowPreview
      value={definition}
      height={280}
      currentNodeCodes={['manager_review', 'finance_review']}
      completedNodeCodes={['start']}
      nodeHandlers={{ manager_review: ['张三'], finance_review: ['李四', '王五'] }}
    />
  )
}
```

示例中的节点编号和姓名需替换为实例的实际数据。组件不发起后端请求；传入新的属性即可更新流程和当前进度。

| 属性 | 说明 |
| --- | --- |
| `value` | Flovira 定义对象或 JSON 字符串；未传或节点为空时显示空状态 |
| `currentNodeCodes` | 当前办理节点编号数组，支持同时高亮多个并行节点；默认不高亮 |
| `completedNodeCodes` | 已办理节点编号；提供后启用三态配色：未办理类型浅色底与同色图标、办理中类型实色加圆点、已办理类型实色。当前状态优先；不传时保留纯流程预览的类型色 |
| `nodeHandlers` | 以节点编号为键的实际办理人姓名数组；未提供时不从设计配置推断人员 |
| `height` | 画布高度，默认 `320`，接受数字或 CSS 高度值；下方另有紧凑缩放栏 |
| `ui` | 可传入现有 Lumen / Ant Design 适配器，预览仅使用其 `Tooltip` |
| `renderTooltip` | 自定义提示内容，接收 `{ node, current, status, handlers }` |
| `renderNodeIcon` | 自定义节点图标，接收同样的上下文 |
| `className` / `style` | 容器样式，可使用 `--frp-*` 变量调整颜色 |

预览依据业务传入的当前和已办理节点着色，不推断已办理节点或高亮历史路径。未办理表示未出现在两组编号中；业务需提供准确的实例状态。`skipType: 'REJECT'` 的连线不展示，也不展示退回轨迹。节点采用紧凑自动布局，不使用设计器保存的坐标。当前不支持正向循环；遇到循环、重复编号、无效连线或无法解析的数据会明确显示错误。

Lumen 示例页面包含并行办理预览：`flovira-designer/examples/react-lumen`。

## Scope Migration

本 fork 的前端包从 1.0.0 开始统一使用 `@luokuiai` scope。Vue 包的新名称是 `@luokuiai/flovira-vue-designer`，旧的 `@luokui/*` import 不再使用。

## 审批节点控制策略

审批节点设置提供“允许退回、允许转办、允许加签、允许减签”开关。默认允许退回，其他操作默认关闭。关闭退回会隐藏退回配置，但保留之前填写的策略。

退回策略包括退回发起人、退回上一节点、设计时指定前置审批节点、办理时指定节点。除退回上一节点外，可配置重新提交后“重新顺序流转”或“回到执行退回的节点继续”。指定节点不可为自身、后置节点或并行兄弟节点；目标删除或失效后会阻止保存。

配置通过现有 `ext` 数组的 `nodeControlConfig` 项保存，不新增节点数据库字段：

```ts
const config = getNodeControlConfig(node)
// config.allowRollback / allowTransfer / allowAddSign / allowMinusSign
// config.rejectStrategy / rejectTargetNodeCode / resubmitStrategy
const updated = setNodeControlConfig(node, { allowTransfer: true })
```

旧 `returnPolicy` 的 `PREVIOUS`、`ANY` 分别回显为“退回上一节点”“退回时指定节点”；旧 `REJECT` 保留为“直接驳回（旧配置）”，不擅自转换语义。原字段和其他扩展数据均保留，新控制策略以 `nodeControlConfig` 为准。

**当前实现为设计器配置与序列化，Flovira 核心尚未自动读取这套控制策略。** 业务运行接口需要读取配置并校验操作许可、实际已流转节点，以及退回重提路径；办理时指定的候选节点必须依据实例历史提供。前端开关不能代替后端权限检查，也不会自动改变引擎行为。
