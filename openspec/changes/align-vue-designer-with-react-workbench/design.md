## Context

Vue 设计器以 LogicFlow 承载经典和仿钉钉两套流程图，现有页面由导航头、独立工具栏、浮动拖拽栏、画布和 teleport 到 body 的属性抽屉组成。React 设计器已经形成更紧凑的单行头部、画布内视图控制和固定右侧属性区。Vue npm 包是主动演进工作区，但其 props、事件、插槽、命令式 API、UiAdapter 以及流程 JSON 均属于外部契约。

## Goals / Non-Goals

**Goals:**

- 统一 Vue 与 React 设计器的工作台信息层级和视觉节奏。
- 桌面端编辑节点时保持画布与属性同时可见，移动端避免面板挤压可用空间。
- 保持经典 / 仿钉钉、LogicFlow、UI 适配器和全部现有公共契约可用。
- 提供旧布局回退入口，降低宿主升级风险。

**Non-Goals:**

- 不把 Vue 画布改成 React 设计器的纵向 DOM 流程树。
- 不修改流程 JSON、后端 API、节点属性语义或 `flovira-ui` webjar。
- 不新增 UI 框架、图标库或运行时依赖。

## Decisions

1. React 风格工作台作为唯一布局，不增加外观切换 prop，也不保留旧版结构分支，避免长期维护两套 DOM 和样式路径。
2. `FlowDesigner` 继续持有全部状态和 LogicFlow 操作，仅重排展示组件。工作台头部承载流程身份、步骤切换和编辑命令；缩放/适应命令作为画布左下角控制组。
3. `PropertySetting` 增加 `panel` / `drawer` 展示模式。两种模式共享同一表单、watcher、校验和 LogicFlow 写回逻辑；桌面工作台使用内联右侧面板，窄屏自动使用抽屉。
4. 经典模式拖拽栏由浮动卡片调整为工作台左侧工具区；仿钉钉模式不显示空工具区。保留 `#sidebar` 和 `paletteNodes` 契约。
5. 工作台尺寸使用 flex/grid 和固定边栏约束，节点选择导致右栏出现时调用 LogicFlow `resize`/`fitView`，防止画布内容被遮挡。样式继续使用 `--wf-*` token，并补齐 `html.dark` 与窄屏覆盖。

## Risks / Trade-offs

- [属性面板从 teleport 改为内联后，既有全局抽屉样式不能完全复用] → 为 panel 增加独立根类，表单内部仍沿用现有 token 和组件样式，drawer 路径保持不变。
- [右侧面板出现会改变 LogicFlow 容器宽度] → 监听面板可见性，在 DOM 更新后调用 `resize`，仅移动端使用覆盖布局。
- [唯一布局会改变宿主既有截图和自定义 CSS] → 保持 props、事件、插槽、尺寸约束和流程 JSON 契约不变，并通过 demo 做桌面/移动端视觉验证。
- [三套 UiAdapter 对 drawer/button 的 DOM 不同] → 工作台结构只依赖中性 `wf-*` 组件，属性 panel 使用原生语义容器，不读取具体 UI 库 DOM。

## Migration Plan

1. 发布后所有宿主直接使用新工作台，无需传入外观配置。
2. 对宿主自定义 CSS 做桌面、移动端和暗黑模式验证；稳定后再评估同步到 webjar。

## Open Questions

- 无。若后续产品要求完全统一两端的画布交互，应另立变更评估 LogicFlow 与 React 树模型的能力差异。
