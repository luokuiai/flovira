## Why

等待节点已经具备人工恢复和超时恢复能力，但运行时仍读取可变的流程定义，且超时恢复可以绕过等待节点的原子抢占入口。这会让已启动实例受到后续定义编辑影响，并增加多实例并发下重复推进流程的风险。

## What Changes

- 等待节点的运行配置改为从流程实例保存的定义快照读取，确保实例启动后的行为稳定。
- 外部信号与超时恢复统一竞争同一个原子抢占操作，任一来源成功后其他来源只能得到幂等结果。
- 增加内部系统任务推进入口，由等待、超时等系统节点复用完整流转管线，不再各自拼装审批跳过参数。
- 按实例和节点类型查询待办任务，避免按等待标识恢复时扫描实例全部任务和逐节点查询。
- 在四套数据库初始化脚本中为任务表增加 `(tenant_id, instance_id, node_type)` 组合索引，不增加表、字段或迁移脚本。
- **BREAKING** 移除允许调用方跳过原子抢占的 `WaitService.resumeClaimedTask` 公共方法。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `wait-node-runtime`: 等待配置来源改为实例定义快照，恢复入口统一执行原子抢占并提供稳定的幂等语义。
- `task-timeout`: 等待任务超时不再预先走通用超时抢占，而是与外部恢复信号竞争等待节点自己的原子状态转换。

## Impact

- 核心服务：`WaitService`、`WaitServiceImpl`、`TimeoutServiceImpl`、`TaskService` 与 `TaskServiceImpl`。
- ORM 契约与实现：`FlowTaskDao` 以及 MyBatis、MyBatis-Plus 的任务查询实现。
- 数据库：MySQL、PostgreSQL、Oracle、SQL Server 的任务表索引。
- 测试：等待快照稳定性、信号与超时竞争、幂等恢复、实例等待任务查询及系统推进。
