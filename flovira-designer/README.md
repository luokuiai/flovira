# Flovira Designer

Flovira 的前端设计器工作区。Vue 与 React 实现共享仓库目录和示例管理，但作为两个独立 npm 包发布，不合并运行时依赖或公共 API。

| 目录 | npm 包 / 用途 |
| --- | --- |
| [`vue`](./vue) | `@luokuiai/flovira-vue-designer` |
| [`react`](./react) | `@luokuiai/flovira-react-designer` |
| [`examples/vue-element-plus`](./examples/vue-element-plus) | Vue + Element Plus 示例，端口 5180 |
| [`examples/vue-antdv`](./examples/vue-antdv) | Vue + Ant Design Vue 示例，端口 5181 |
| [`examples/vue-naive`](./examples/vue-naive) | Vue + Naive UI 示例，端口 5182 |
| [`examples/react`](./examples/react) | React + Tailwind CSS 示例，端口 5183 |

后端 `flovira-plugin-ui-*` 模块仅提供设计器 API，不再打包设计器网页或静态资源。宿主应用应安装对应 npm 包并自行集成、构建和部署。

## 构建

在仓库根目录执行：

```bash
bun install
bun run build:designer
bun run build:demos
```

也可以使用 `bun run build` 一次构建两个 npm 包和全部示例。
