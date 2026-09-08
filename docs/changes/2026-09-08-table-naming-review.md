# 数据库命名审查清单（2026-09-08）

范围：MySQL、PostgreSQL、Oracle 三套初始化 SQL，共 10 张表。SQL Server 支持及初始化脚本已按用户要求移除。用户最终决定：除已授权移除 model_value 外，其他字段保留当前名称，剩余改名建议全部撤回。此前已完成的审计字段、关联 ID、协作类型及连线字段统一不回退。

三库字段集合检查：MySQL、PostgreSQL、Oracle 一致。

本轮复审原则：表名提供领域上下文，字段仅补充必要语义；不重复添加 subprocess 等前缀，也不为了展开缩写、替换同义词或改变词序而重命名。保留明确的业务角色、来源/目标和关联对象区分；确实承载多个值的字段考虑复数。

通用字段 id、created_by、created_at、updated_by、updated_at、deleted、tenant_id 建议保持当前命名。下面逐表列出全部实际字段；历史任务没有 created_by/updated_by，不补造字段。deleted 保持现有存储和值，命名审查不改变类型。

## flow_definition — 流程定义

全部字段：

```text
id, flow_code, flow_name, category, version, publish_status, form_custom, form_path, activity_status, listener_type, listener_path, ext, created_at, created_by, updated_at, updated_by, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| model_value | 已移除 | 已授权 | 统一设计器，不再按流程保存设计模式。 |
| form_custom | 保留 | 保留 | 与 form_path 成组，现有上下文可理解；仅换词序不值得修改契约。 |
| activity_status | 保留 | 保留 | 激活状态语义已有明确约定；不做同义词替换。 |
| listener_type / listener_path | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| ext | 保留 | 保留 | 通用扩展字段，缩写不构成错误。 |

未单列的字段暂未发现明确命名问题。

## flow_node — 流程节点

全部字段：

```text
id, node_type, definition_id, node_code, node_name, permission_flag, node_ratio, coordinate, any_node_skip, listener_type, listener_path, form_custom, form_path, version, created_at, created_by, updated_at, updated_by, ext, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| permission_flag | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| node_ratio | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| any_node_skip | 保留 | 保留 | 当前用途已核实为默认退回目标；澄清注释即可，无需强制改名。 |
| listener_type / listener_path | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| form_custom | 保留 | 保留 | 同流程定义表。 |
| coordinate | 保留 | 保留 | 节点坐标；无需为了复数而修改。 |

未单列的字段暂未发现明确命名问题。

## flow_skip — 节点连线

全部字段：

```text
id, definition_id, source_node_code, source_node_type, target_node_code, target_node_type, skip_name, skip_type, skip_condition, coordinate, created_at, created_by, updated_at, updated_by, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| 表名 flow_skip；skip_name/type/condition | 保留 | 保留 | skip 是引擎现有统一流转术语；不为更换英文同义词修改整个领域 API。 |
| source_node_code/type；target_node_code/type | 保留 | 保留 | 表示静态连线的来源与目标，适用于通过和退回。 |
| coordinate | 保留 | 保留 | 表名已区分连线与节点，不因另一张表也有坐标就改名。 |

未单列的字段暂未发现明确命名问题。

## flow_instance — 流程实例

全部字段：

```text
id, definition_id, business_type, business_id, node_type, node_code, node_name, variables, flow_status, activity_status, def_json, created_at, created_by, updated_at, updated_by, ext, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| def_json | 保留 | 保留 | 在流程实例上下文中含义可识别，单纯展开缩写不是必要调整。 |
| activity_status | 保留 | 保留 | 与定义表保持一致。 |
| node_code/name/type | 保留 | 保留 | 运行状态记录；不改成连线 source/target，也不将其宣称为并行状态下完整的当前节点集合。 |
| variables / flow_status | 保留 | 保留 | 含义明确。 |

未单列的字段暂未发现明确命名问题。

## flow_task — 待办任务

全部字段：

```text
id, definition_id, instance_id, node_code, node_name, node_type, flow_status, form_custom, form_path, created_at, created_by, updated_at, updated_by, deleted, tenant_id, timeout_at, timeout_action, timeout_config, timeout_status, timeout_claimed_at
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| form_custom | 保留 | 保留 | 与其他表保持一致。 |
| timeout_at/action/config/status/claimed_at | 保留 | 保留 | 一组超时调度字段，命名一致。 |
| flow_status | 保留 | 保留 | 当前复用流程状态语义，不仅凭所在表改成 task_status。 |

未单列的字段暂未发现明确命名问题。

## flow_his_task — 历史任务

全部字段：

```text
id, definition_id, instance_id, task_id, node_code, node_name, node_type, target_node_code, target_node_name, approver, cooperation_type, collaborator, skip_type, flow_status, form_custom, form_path, message, variables, ext, created_at, updated_at, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| 表名 flow_his_task | 保留 | 保留 | his 与现有 HisTask 术语一致，不为展开缩写整体改名。 |
| target_node_code / target_node_name | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| collaborator | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| message | 保留 | 保留 | 历史任务上下文已有含义，也用于协作操作记录，无需增加 approval 限定。 |
| form_custom | 保留 | 保留 | 与其他表保持一致。 |
| created_at / updated_at | 审计时间与 started_at/completed_at 分开设计 | 语义问题 | 当前存任务开始/审批完成时间；不能当作普通拼写替换，也不直接撤销已统一的审计命名。 |
| approver | 保留 | 保留 | 实际处理记录中的审批人；不盲目与 flow_user 的权限主体统一。 |
| node_code/name/type 注释 | 办理节点编码/名称/类型 | 注释问题 | 现在写开始节点，容易误解为流程起始节点；target_node_name 的结束节点注释也应改为目标节点名称。 |

未单列的字段暂未发现明确命名问题。

## flow_user — 流程权限关联

全部字段：

```text
id, type, processed_by, associated_id, created_at, created_by, updated_at, updated_by, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| type | 保留 | 保留 | 在人员关联记录中已有单一类别含义，不为完整英文补前缀；注释写清枚举含义即可。 |
| processed_by | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
| associated_id | 保留 | 保留 | 公共接口允许关联任务、实例、历史、节点；不应直接收窄为 task_id。 |
| associated_id 注释 | 关联记录 ID | 注释问题 | 当前任务表 ID 注释比接口约定窄。 |
| updated_by 注释 | 更新人 | 注释问题 | MySQL 当前误写创建人。 |
| user_processed_type / user_associated 索引名 | 保留 | 保留 | 本轮聚焦字段语义，不为格式统一增加索引重命名。 |

未单列的字段暂未发现明确命名问题。

## flow_subprocess_run — 子流程运行聚合

全部字段：

```text
id, parent_instance_id, parent_task_id, parent_definition_id, parent_node_code, child_flow_code, child_definition_id, child_definition_version, completion_policy, collection_fingerprint, expected_count, pending_count, running_count, completed_count, failed_count, cancelled_count, run_status, failure_code, lock_version, initialized_at, completed_at, created_at, created_by, updated_at, updated_by, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| 现有字段 | 保留 | 保留 | parent/child、状态、计数、时间和锁版本命名基本清晰。 |
| completion_policy | 保留 | 保留 | 完成策略，与执行状态不同。 |

未单列的字段暂未发现明确命名问题。

## flow_subprocess_child — 子流程实例关系

全部字段：

```text
id, run_id, item_key, item_label, child_business_key, child_flow_code, child_definition_id, child_definition_version, child_instance_id, child_status, outcome, started_at, completed_at, created_at, created_by, updated_at, updated_by, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| run_id | 保留 | 保留 | 表名已有子流程上下文，不重复添加 subprocess。 |
| child_business_key | 保留 | 保留 | 不因为 instance 使用 business_id 就直接改名；业务键与业务 ID 可能承担不同约束。 |
| child_status / outcome | 保留 | 保留 | 状态与结果分别表达，不机械合并。 |

未单列的字段暂未发现明确命名问题。

## flow_subprocess_event — 子流程事件

全部字段：

```text
id, run_id, child_id, parent_instance_id, child_instance_id, parent_node_code, event_type, event_result, reason, occurred_at, created_at, created_by, updated_at, updated_by, deleted, tenant_id
```

| 当前项 | 保留 | 已撤回 | 按用户最终决定，不再改名。 |
|---|---|---|---|
| run_id / child_id | 保留 | 保留 | 表名已有子流程上下文；child_id 指关系记录，child_instance_id 指流程实例，已经能区分。 |
| event_type / event_result / reason / occurred_at | 保留 | 保留 | 事件类别、结果、原因和发生时间，含义清晰。 |

未单列的字段暂未发现明确命名问题。

## 修改边界

本轮不再选择其他字段重命名。此前已授权的修改已同步 Java 属性/访问器、参数、枚举与相关方法、两套 ORM、Mapper XML、MySQL、PostgreSQL、Oracle 三套 SQL、JSON 和前端契约、示例及迁移说明。旧命名仅在明确的迁移对照中保留。

本次清单不涉及数据库执行。历史时间拆分、集合字段存储改造及业务主体模型调整均需作为语义变更单独处理。
