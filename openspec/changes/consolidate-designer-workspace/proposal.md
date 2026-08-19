## Why

Vue、React 设计器及示例目前分散在三个顶层目录，且 Vue npm 包名没有体现框架，长期会造成发现、构建和发布语义不一致。项目也已决定不再提供可通过 URL 打开的内置设计器页面，因此旧 SPA 与静态资源 WebJar 应从发布矩阵中移除。

## What Changes

- 将前端设计器统一组织到 `flovira-designer/`，下设 `vue/`、`react/` 和 `examples/`。
- **BREAKING** 将 Vue npm 包从 `@luokuiai/flovira-designer` 更名为 `@luokuiai/flovira-vue-designer`；React 包继续使用 `@luokuiai/flovira-react-designer`，两者独立构建和发布。
- 删除顶层 `flovira-ui` SPA 工程以及 `flovira-plugin-vue3-ui` 静态资源 jar，不再提供内置网页/WebJar 设计器。
- 删除 Spring/Solon 的设计器静态资源映射和相关 Gradle/workspace 构建入口。
- 保留 `flovira-plugin-ui-core`、`flovira-plugin-ui-sb-web`、`flovira-plugin-ui-solon-web` 提供的后端 API，供 npm 设计器默认 HTTP Provider 使用。
- 更新 README、AGENTS、构建脚本、demo 依赖及所有有效路径引用。

## Capabilities

### New Capabilities

- `designer-workspace-layout`: Vue、React npm 设计器在统一目录下独立构建、独立发布，示例集中维护。

### Modified Capabilities

- `frontend-package-scope`: Vue npm 包采用明确的框架后缀，消费方迁移到新包名。

## Impact

- 影响根 Bun workspace、设计器及 demo 目录、Vue npm 包名、lockfile、README 和项目规则。
- 移除 `flovira-ui/`、`flovira-plugin-vue3-ui`、对应 Gradle include/dependency 及静态资源映射。
- 不改变 Vue/React 设计器的流程 JSON、组件 API、后端 API 路径或核心引擎行为。
