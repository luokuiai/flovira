# @luokuiai/flovira-react-adapter-lumen

`@luokuiai/flovira-react-designer` 的 Lumen UI 适配器。

Lumen UI 通过宿主依赖接入，版本范围为 `^1.0.0-beta.1`，包含 `1.0.0-beta.1`；本地开发与示例使用 `1.0.0-beta.1`。

```bash
bun add @luokuiai/flovira-react-designer \
  @luokuiai/flovira-react-adapter-lumen \
  @luokuiai/lumen-ui@1.0.0-beta.1 \
  @luokuiai/lumen-theme-clarity@1.0.0-beta.1
```

```tsx
import { ReactFlowDesigner } from '@luokuiai/flovira-react-designer'
import { lumenDesignerUi } from '@luokuiai/flovira-react-adapter-lumen'
import '@luokuiai/lumen-ui/styles.css'
import '@luokuiai/lumen-theme-clarity'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-lumen/style.css'

export function ProcessEditor() {
  return <ReactFlowDesigner ui={lumenDesignerUi} />
}
```

Set `data-lumen-theme` and `data-color-scheme` on `document.documentElement` or another ancestor. Document-level configuration is recommended because Lumen Select, DropdownMenu, and Drawer render through portals.
