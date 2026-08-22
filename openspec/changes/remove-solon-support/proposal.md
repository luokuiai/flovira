## Why

Flovira 当前实际集成目标集中在 Spring Boot，继续维护 Solon 的 ORM、表达式和 Web 适配会扩大构建矩阵与发布成本。移除未采用的 Solon 生态可以收敛依赖和维护边界。

## What Changes

- **BREAKING** 删除 MyBatis、MyBatis-Plus 的 Solon plugin 发布模块。
- **BREAKING** 删除 SnEL 表达式、Solon 事务、调度器和框架桥接实现。
- **BREAKING** 删除设计器 API 的 Solon Web 适配模块。
- 清理 Solon 依赖版本、Gradle 工程注册、自动装配资源和项目文档。
- 保留 Spring Boot 2、3、4，且不改变 core、数据库结构、JSON SPI 和前端设计器契约。

## Capabilities

### New Capabilities

- `spring-framework-support`: 规定发行模块仅提供 Spring Boot 2、3、4 框架适配，不再提供 Solon 集成。

### Modified Capabilities

无。

## Impact

影响 `settings.gradle`、Gradle version catalog、三个 ORM 模块、`flovira-plugin-modes`、`flovira-plugin-ui` 及对应说明文档。依赖 Solon artifact 或 `org.noear.solon` 自动装配的下游应用无法升级到此版本，属于明确的破坏性生态收缩。
