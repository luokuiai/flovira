# @luokuiai/flovira-react-adapter-antd

Ant Design adapter for `@luokuiai/flovira-react-designer`.

```bash
bun add @luokuiai/flovira-react-designer \
  @luokuiai/flovira-react-adapter-antd \
  antd@6.6.2
```

```tsx
import { ConfigProvider } from 'antd'
import { ReactFlowDesigner } from '@luokuiai/flovira-react-designer'
import { antdDesignerUi } from '@luokuiai/flovira-react-adapter-antd'
import 'antd/dist/reset.css'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-antd/style.css'

export function ProcessEditor() {
  return (
    <ConfigProvider theme={{ cssVar: {} }}>
      <ReactFlowDesigner ui={antdDesignerUi} />
    </ConfigProvider>
  )
}
```

Enable Ant Design CSS variables on `ConfigProvider` so the designer canvas follows the host theme tokens.
