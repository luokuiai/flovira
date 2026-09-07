# Flovira Designer

Flovira 的前端设计器工作区。Vue 与 React 实现共享仓库目录和示例管理，但作为独立 npm 包发布，不合并运行时依赖或公共 API；React 的具体 UI 框架集成由可选适配包提供。

| 目录 | npm 包 / 用途 |
| --- | --- |
| [`vue`](./vue) | `@luokuiai/flovira-vue-designer` |
| [`react`](./react) | `@luokuiai/flovira-react-designer` |
| [`react-adapters/lumen`](./react-adapters/lumen) | `@luokuiai/flovira-react-adapter-lumen` |
| [`react-adapters/antd`](./react-adapters/antd) | `@luokuiai/flovira-react-adapter-antd` |
| [`examples/vue-element-plus`](./examples/vue-element-plus) | Vue + Element Plus 示例，端口 5180 |
| [`examples/vue-antdv`](./examples/vue-antdv) | Vue + Ant Design Vue 示例，端口 5181 |
| [`examples/vue-naive`](./examples/vue-naive) | Vue + Naive UI 示例，端口 5182 |
| [`examples/react`](./examples/react) | React 默认 UI 示例，端口 5183 |
| [`examples/react-lumen`](./examples/react-lumen) | React + Lumen UI 示例，端口 5184 |
| [`examples/react-antd`](./examples/react-antd) | React + Ant Design 示例，端口 5185 |

后端 `flovira-plugin-ui-*` 模块仅提供设计器 API，不再打包设计器网页或静态资源。宿主应用应安装对应 npm 包并自行集成、构建和部署。

## 构建

在仓库根目录执行：

```bash
bun install
bun run build:designer
bun run build:demos
```

也可以使用 `bun run build` 一次构建全部 npm 包和示例。

## 发布

在 `main` 分支执行 `bun run release`，由 Lerna 同步公开包的版本、创建并推送 `vX.Y.Z` tag。tag 会触发 GitHub Actions，通过 npm Trusted Publishing 发布：

- `@luokuiai/flovira-vue-designer`
- `@luokuiai/flovira-react-designer`
- `@luokuiai/flovira-react-adapter-lumen`
- `@luokuiai/flovira-react-adapter-antd`

四个 npm 包需要在 npm 网站分别配置本仓库 `.github/workflows/publish-npm.yml` 为 Trusted Publisher。
