## Why

Flovira 目前只有 Vue 组件库，React 项目只能通过内置网页或自行重写设计器接入。新增 React + Tailwind 设计器可覆盖 React 技术栈，同时把前端 npm scope 与已经启用的 `com.luokuiai` 品牌命名统一为 `@luokuiaiai`。

## What Changes

- 新增 `@luokuiaiai/flovira-react-designer` React 组件库，提供面向审批流的纵向流程树画布、分支布局、节点插入/删除、属性编辑、缩放、撤销/重做和校验。
- 使用 Tailwind CSS 构建设计器样式，参考 Intelliconf 的流程画布信息层级和交互，但仅复用设计语言，不引入其会议业务代码或接口契约。
- React 设计器支持 Flovira 流程 JSON 导入/导出，并提供可注入的数据 Provider，以便宿主控制流程定义和子流程定义来源。
- 新增 React 演示应用，用真实组件包验证嵌入、编辑、子流程选择和 JSON 输出。
- **BREAKING** 将现有前端 workspace 包名和文档引用从 `@luokuiai/*` 迁移为 `@luokuiaiai/*`；Java/Maven 坐标不受本变更影响。

## Capabilities

### New Capabilities

- `react-process-designer`: React 宿主可嵌入、配置和扩展的 Tailwind 流程设计器及其公共 API。
- `frontend-package-scope`: Flovira 前端包统一使用 `@luokuiaiai` npm scope，并由 workspace 演示工程验证本地消费。

### Modified Capabilities

- `subprocess-designer`: 子流程节点在 React 设计器中也能选择固定子流程定义并进行结构校验。

## Impact

- 新增 `flovira-react-designer/` 和 React demo，并纳入根 Bun workspace 与构建脚本。
- 修改现有 Vue 设计器及 demo 的 npm 包名、依赖引用、文档示例和 lockfile。
- 新增 React、Tailwind、Lucide React 和相关构建/测试依赖；不改动核心引擎、数据库或后端 API。
