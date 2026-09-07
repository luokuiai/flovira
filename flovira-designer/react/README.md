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

## Scope Migration

本 fork 的前端包从 1.0.0 开始统一使用 `@luokuiai` scope。Vue 包的新名称是 `@luokuiai/flovira-vue-designer`，旧的 `@luokui/*` import 不再使用。
