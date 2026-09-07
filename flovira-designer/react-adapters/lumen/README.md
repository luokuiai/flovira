# @luokuiai/flovira-react-adapter-lumen

Lumen UI adapter for `@luokuiai/flovira-react-designer`.

```bash
bun add @luokuiai/flovira-react-designer \
  @luokuiai/flovira-react-adapter-lumen \
  @luokuiai/lumen-ui@1.0.0-alpha.7 \
  @luokuiai/lumen-theme-clarity@1.0.0-alpha.7
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
