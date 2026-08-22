# AGENTS.md — flovira-plugin 模块规则

> 本文件只写 `flovira-plugin` 的差异化规则。通用工程与编码规范以仓库根 [`../AGENTS.md`](../AGENTS.md) 为准；规则优先级见根 `AGENTS.md`「规则优先级」。

## 模块职责

引擎的可插拔扩展实现，按扩展点分三类：

- `flovira-plugin-modes`：Spring 模式与 SpEL 表达式实现。`*-sb` 包含 `ConditionStrategySpel`、`VoteSignStrategySpel`、`HandlerStrategySpel`、`SpelHelper` 和安全方法解析，包根 `com.luokuiai.flovira.plugin.modes.sb`。
- `flovira-plugin-json`：`JsonConvert` 的独立 SPI 实现。`*-json-jackson`、`*-json-jackson3`、`*-json-gson` 各自只注册一个 provider，使用方只选择一个。包根 `com.luokuiai.flovira.plugin.json`。
- `flovira-plugin-ui`：设计器 / 流程图后端 API。`*-ui-core`（`service` / `dto` / `vo`）、`*-ui-sb-web`（Spring controller）。不再打包或映射前端静态资源，包根 `com.luokuiai.flovira.ui`。

## 改动前必读

- 根 [`../AGENTS.md`](../AGENTS.md)「架构与扩展机制」「兼容性红线」「扩展开发指引」。
- core 的 `condition` / `strategy` / `listener` / `json.JsonConvert` 接口；改表达式策略前先读 core 默认实现与 Spring 实现。

## 高风险点（按 L2）

- **SPI 注册一致**：新增 / 改 JSON 实现必须同步 `META-INF/services/com.luokuiai.flovira.core.json.JsonConvert`；`FlowEngine` 通过 `ServiceLoaderUtil.loadFirst` 取首个实现，注意实现优先级与 classpath 唯一性。
- **表达式安全**：SpEL / SnEL 涉及脚本执行，沿用现有 `SafeMethodResolver` / `SafeTypeLocator` 等安全约束，**不要放开任意方法 / 类型调用**，避免表达式注入风险。
- **设计器 API**：保留 `/flovira-ui/*` API 前缀兼容已有客户端，但后端模块不提供设计器页面。
- **Spring 版本对齐**：modes / ui 的公共能力需兼容 Spring Boot 2、3、4，不要引入只适用于单一版本的 API。
- **JSON 兼容**：不同 JSON 库对日期 / null / 泛型 / 多态序列化差异要与引擎实体约定一致，避免下游切换 JSON 库后行为变化。

## 聚焦验证

```bash
# 按被改子模块替换路径
./gradlew :flovira-plugin:flovira-plugin-json:flovira-plugin-json-jackson:compileJava
./gradlew :flovira-plugin:flovira-plugin-modes:flovira-plugin-modes-sb:compileJava
```

涉及 SPI / 表达式 / UI 契约时，确认注册文件与 Spring 实现同步。
