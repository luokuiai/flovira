## Why

流程实例目前只能保存 `business_id`，不同业务域出现相同 ID 时，接入方只能拼接字符串来避免冲突，缺少稳定、可索引的业务关联模型。业务侧保存短生命周期任务 ID 同样会造成耦合和一致性问题。

## What Changes

- 仅在流程实例增加持久化的 `business_type`，与租户和业务 ID 共同构成业务查询键。
- 增加显式接收业务类型的流程启动重载；旧启动方法继续可用并以流程编码作为默认业务类型。
- 增加按 `businessType + businessId` 查询流程实例、当前任务和历史任务的核心服务方法。
- 当前任务和历史任务不增加业务字段，通过 `instance_id` 关联实例。
- 三套 ORM 同步实例字段与业务键查询，并为批量实例任务查询补齐实现。
- 四种数据库初始化脚本为实例增加字段和组合索引，为历史任务增加实例查询索引。

## Capabilities

### New Capabilities

- `business-correlation`: 使用独立业务类型和业务 ID 启动、定位并查询流程运行数据。

### Modified Capabilities

无。

## Impact

- Core：`Instance`、`InsService`、`TaskService`、`HisTaskService` 及其实现和 DAO 契约。
- ORM：MyBatis、MyBatis-Plus 的实例实体、查询条件、任务与历史任务批量查询。
- SQL：MySQL、PostgreSQL、Oracle、SQL Server 完整初始化脚本。
- 公共 API 仅增加字段和方法重载，不删除现有入口。
