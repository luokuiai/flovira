# AGENTS.md - flovira-designer 模块规则

> 本文件只写 `flovira-designer` 的差异化规则。通用规范以仓库根 [`../AGENTS.md`](../AGENTS.md) 为准。

## 模块职责

- `vue/`：Vue 3 设计器组件库，npm 包 `@luokuiai/flovira-vue-designer`。
- `react/`：React 设计器组件库，npm 包 `@luokuiai/flovira-react-designer`。
- `examples/`：通过 Bun `workspace:*` 消费上述包的集成示例。

两个组件库只共享仓库工作区，不合并 npm 包、框架依赖或公共 API。后端只提供设计器 API，本模块不得重新引入 WebJar、内嵌页面或后端静态资源打包链路。

## 改动规则

- 复用各框架现有实现和交互语义，不要求 Vue 与 React 内部代码同构。
- 变更流程定义 JSON、节点属性或 API 路径时，同时核对 `flovira-plugin-ui` 和 core 的数据契约。
- 公共导出、组件属性、事件、样式入口和 npm 包名属于对外契约，优先用兼容性增加方式演进。
- 包管理和工作区命令统一使用 Bun；不要在子目录新增独立 lockfile。

## 聚焦验证

```bash
cd flovira-designer
bun run build:designer
bun run build:demos
```

npm 发布使用 Lerna 固定版本模式：在 `main` 执行 `bun run release` 同步 Vue/React 包版本并推送 `vX.Y.Z` tag，GitHub Actions 通过 npm Trusted Publishing 发布两个包。不要手工只修改或发布其中一个包。
