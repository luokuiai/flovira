## Context

根 Bun workspace 同时管理后端仓库中的 Vue npm 设计器、React npm 设计器、四个 demo 和旧 `flovira-ui` SPA。旧 SPA 的构建结果又被复制进 `flovira-plugin-vue3-ui`，由 Spring/Solon 静态映射暴露为内置页面。新的集成方向是业务方直接安装 Vue/React npm 包，后端只保留可选 API 适配，不再托管前端页面。

## Goals / Non-Goals

**Goals:**

- 让所有可发布设计器和示例在一个清晰的前端目录下维护。
- Vue、React 包名对称且继续独立发布、独立声明 peer dependencies。
- 完整移除内置 SPA/WebJar 的源码、构建和运行时静态映射。
- 保留 npm 设计器默认 HTTP Provider 所需的后端接口。

**Non-Goals:**

- 不合并 Vue 与 React npm 包或运行时依赖。
- 不统一两套设计器内部画布实现。
- 不重命名现有 `/flovira-ui/*` 后端 API 路径或 `flovira-plugin-ui-*` API 适配模块；该公共契约另行评估。

## Decisions

1. 使用 `flovira-designer/vue`、`flovira-designer/react`、`flovira-designer/examples/*` 的单层目录结构，由 `flovira-designer` Bun workspace 直接管理。
2. Vue 包更名为 `@luokuiai/flovira-vue-designer`，React 保持 `@luokuiai/flovira-react-designer`。当前 fork 从 1.0.0 重新发布，优先在首次正式发布前完成明确命名。
3. 删除 `flovira-ui` 和 `flovira-plugin-vue3-ui`。`flovira-plugin-ui-core` 不再通过 `api` 依赖静态资源 jar；Spring/Solon 仅保留 controller，不再注册 classpath 静态目录。
4. 后端 API 继续使用 `/flovira-ui/*`，避免把前端目录整理扩大为跨语言 API 破坏性改名。Vue 默认 HTTP Provider 与自定义 DataProvider 均保持可用。
5. 目录迁移采用机械移动并集中更新根 workspace、demo 依赖、文档和 AGENTS。生成目录与 node_modules 不作为源代码迁移依据。

## Risks / Trade-offs

- [Vue npm 包改名会破坏旧安装声明] → 当前从 1.0.0 重新起版，并同步全部 demo、README、lockfile和导入示例。
- [删除静态资源 jar 后依赖旧 URL 页面会失效] → 明确作为预期移除能力；README 只保留 npm 组件集成方式。
- [保留 `/flovira-ui` API 名称与“无内置 UI”语义不完全一致] → 先保护后端契约，未来单独提供弃用迁移而不是本次顺手改名。
- [大范围目录移动容易遗漏相对路径] → 通过全仓文字扫描、根 workspace 安装、两个库构建、全部 demo 构建和相关 Gradle 编译验证。

## Migration Plan

1. 消费 Vue 包的项目将依赖和 import 从 `@luokuiai/flovira-designer` 改为 `@luokuiai/flovira-vue-designer`。
2. 使用内置 `/flovira-ui/index.html` 的项目改为自行部署或嵌入 Vue/React npm 设计器。
3. 后端若仍需要默认 HTTP Provider，可继续引入 UI core + 对应 Spring/Solon web 适配模块。

## Open Questions

- 无。后端模块和 API 前缀重命名不纳入本变更。
