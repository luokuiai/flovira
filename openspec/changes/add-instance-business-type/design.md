## Context

`flow_instance.business_id` 是当前唯一持久化的业务关联。`Task` 和 `HisTask` 暴露了派生的 `businessId` 属性，但任务表并不保存该字段，不能用它建立可靠业务查询。不同业务域可能产生相同业务 ID，因此需要独立的业务类型，而不是编码到业务 ID 中。

## Goals / Non-Goals

**Goals:**

- 用结构化、可索引的业务类型和业务 ID 定位流程实例。
- 业务系统无需保存流程实例 ID 或任务 ID。
- 当前任务和历史任务不复制实例级业务字段。
- 保持旧启动 API 源码兼容。

**Non-Goals:**

- 不强制一个业务对象只能启动一次流程。
- 不在任务或历史任务表增加 `business_type`、`business_id`。
- 不建立业务系统表与流程表的数据库外键。

## Decisions

### 1. 业务键只持久化在流程实例

`Instance` 增加 `businessType`，数据库列为 `business_type`。查询键为 `tenant_id + business_type + business_id`，建立普通组合索引而非唯一索引，从而允许撤销后重启或同一业务关联多个流程实例。

### 2. 新增显式启动重载并兼容旧入口

新增 `start(businessType, businessId, flowParams)` 和对应 definition ID 重载。旧入口使用所选流程定义的 `flowCode` 作为默认业务类型，使现有调用在新增非空实例字段后仍可运行。

### 3. 任务查询通过实例 ID 批量关联

业务键查询先通过实例表索引得到实例集合，再一次性按实例 ID 集合查询当前任务或历史任务。三套 ORM 增加批量查询实现，避免 N+1；任务和历史任务保持规范化。

### 4. 业务查询由 Core 服务提供

`InsService`、`TaskService`、`HisTaskService` 分别提供 `listByBusinessKey`，业务接入不需要理解内部表连接关系。租户过滤继续由现有 ORM/handler 机制施加。

## Risks / Trade-offs

- [同一业务键返回多个流程实例] -> API 明确返回列表，由调用方按状态或时间选择，不设置错误的全局唯一约束。
- [批量实例 ID 数量过大] -> 业务键通常只对应少量运行记录；跨大量业务对象的报表查询不走该点查 API。
- [旧入口业务类型语义不够精确] -> 默认使用稳定的 `flowCode`，新接入使用显式业务类型重载。

## Migration Plan

本 fork 从 1.0.0 起版，直接修改四种数据库完整初始化脚本，不创建升级链。已有本地试用库需自行增加 `business_type` 并以流程编码回填后创建组合索引。

## Open Questions

无。
