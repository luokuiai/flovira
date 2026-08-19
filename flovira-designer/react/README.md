# @luokuiai/flovira-react-designer

Flovira 的 React + Tailwind 审批流程设计器。组件直接读写 Flovira `nodeList/skipList` JSON，不依赖路由、Vue、LogicFlow 或固定后端地址。

```bash
bun add @luokuiai/flovira-react-designer react react-dom
```

```tsx
import { useRef } from 'react'
import {
  ReactFlowDesigner,
  type ReactFlowDesignerRef,
} from '@luokuiai/flovira-react-designer'
import '@luokuiai/flovira-react-designer/style.css'

export function ProcessEditor() {
  const designer = useRef<ReactFlowDesignerRef>(null)

  return (
    <ReactFlowDesigner
      ref={designer}
      defaultValue={{ flowCode: 'leave', flowName: '请假审批', nodeList: [] }}
      dataProvider={{
        capabilities: async () => fetch('/api/flovira/integration/capabilities')
          .then((response) => response.json()),
        queryResources: async (query) => {
          const search = new URLSearchParams(
            Object.entries(query).filter(([, value]) => value != null) as [string, string][],
          )
          return fetch(`/api/flovira/integration/resources?${search}`)
            .then((response) => response.json())
        },
      }}
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
- `dataProvider`: 实例级统一数据源，用于能力发现、通用资源查询和业务关系解析。
- `renderNode`: 自定义节点卡片渲染器。
- ref: `getDefinition`、`getFlowJson`、`importJson`、`validate`、`undo`、`redo`、缩放和定位命令。

Tailwind 已在发布构建中编译，消费方只需引入 `style.css`，不需要安装或配置 Tailwind。

## Scope Migration

本 fork 的前端包从 1.0.0 开始统一使用 `@luokuiai` scope。Vue 包的新名称是 `@luokuiai/flovira-vue-designer`，旧的 `@luokui/*` import 不再使用。
