## Why

Vue 设计器当前采用双层头部、浮动节点栏和覆盖画布的属性抽屉，与新 React 设计器的紧凑工作台体验不一致，也降低了大屏编辑时的画布可见性。需要在保留 Vue 既有双模式、LogicFlow 能力和公共契约的前提下，统一两套设计器的工作台信息层级与交互。

## What Changes

- 将 Vue 设计器默认工作台改为 React 风格：单行紧凑头部、固定节点工具栏、中央画布、桌面右侧属性面板和贴近画布的视图控制。
- 保留基础信息 / 流程设计步骤、经典 / 仿钉钉双模式、节点拖拽、导入导出、撤销重做及现有插槽和事件。
- 属性编辑在桌面端以内联右侧面板呈现，在窄屏端自动切换为覆盖式面板，避免压缩画布与表单。
- 新工作台成为 Vue 设计器唯一布局，不再保留旧版双层头部和浮动属性抽屉分支。
- 补齐亮色、暗黑和移动端响应式状态，不改变流程 JSON 或后端接口。

## Capabilities

### New Capabilities

- `vue-designer-workbench`: Vue 设计器提供与 React 设计器一致的工作台布局、响应式属性面板和可兼容回退的外观配置。

### Modified Capabilities


## Impact

- 影响 `flovira-vue-designer` 的 `FlowDesigner`、头部、工具栏、属性设置组件、公共 props 类型和样式。
- 影响 Vue demo 的视觉验证，但不新增运行时依赖，也不修改 `flovira-ui` webjar 源工程。
- npm 公共 API 仅做加法；现有流程 JSON、DataProvider、UiAdapter、事件、插槽和命令式 API 保持兼容。
