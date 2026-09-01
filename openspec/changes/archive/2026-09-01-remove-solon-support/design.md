## Context

当前 Gradle 工程为三个 ORM、表达式模式和设计器 API 分别发布 Solon 模块，并在 version catalog 中维护 Solon 及其 ORM 插件依赖。用户已决定停止提供 Solon 生态，本 fork 尚从 1.0.0 起版，因此直接删除比保留废弃壳模块更符合当前发布策略。

## Goals / Non-Goals

**Goals:**

- 从源码树和 Gradle 工程中删除所有 Solon 专属发布模块。
- 删除不再使用的 Solon 依赖别名和项目说明。
- 保证 Spring Boot 2、3、4 模块仍能完整编译。

**Non-Goals:**

- JSON 插件按实现独立发布，不与框架 Starter 绑定。
- 不从 core 删除通用框架枚举值或已发布配置字段，避免无必要扩大公共 API 破坏面。
- 不修改流程状态机、SQL 或前端设计器行为。

## Decisions

- 物理删除五个 Solon 模块，而不是保留空 artifact。当前版本从 1.0.0 重新发布，无需提供无功能兼容壳。
- 使用方显式选择一个 JSON SPI 实现，避免引入无关序列化依赖。
- 删除 core 中 `FrameworkType.SOLON` 和相关配置说明。该 fork 从 1.0.0 重新发布，保留无法工作的枚举值会错误宣称框架能力。
- 验证覆盖全部 Spring ORM starter、Spring 表达式插件与 Spring UI Web 模块，确保移除不会破坏传递依赖。

## Risks / Trade-offs

- [依赖现有 Solon artifact 的应用无法升级] → 在提案和发布说明中明确标记为破坏性变更。
- [清理时误删 Snack JSON 支持] → 残留扫描区分 `org.noear.solon` 与 `org.noear.snack`。
- [Gradle 中残留项目引用导致配置失败] → 先清理 `settings.gradle` 与 version catalog，再执行全量 Spring 模块编译。
