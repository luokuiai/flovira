# 恢复 Flovira 表单管理

此前删除 `form_custom` 时误删了 `flow_form` 及表单服务。本次恢复 Flovira 管理的版本化表单，同时继续使用单一字符串 `formId` 引用；不会恢复 `form_custom`、`form_type`、`form_path` 或表单模式切换逻辑。宿主系统仍可通过资源 provider 提供自己的表单。

## 新建数据库

MySQL、PostgreSQL、Oracle 的 `flovira-v1.sql` 已包含完整 `flow_form`。新建库直接执行对应基线脚本。

## 已有开发数据库

不要重新执行完整 V1 脚本。先备份，再从对应 V1 脚本提取 `flow_form` 建表及索引语句单独执行。该操作是新增表，不应删除或改写现有流程、任务及历史数据。

若旧环境保留过 `flow_form`：

1. 比较列、类型、非空约束、默认值和索引，不要直接覆盖表。
2. 保留已有主键以及被 `form_id` 引用的稳定标识。
3. 补齐租户、审计、逻辑删除和发布状态字段后，再切换应用版本；`form_type`、`form_path` 不属于当前结构。
4. 验证同租户下表单编码和版本查询、发布、失效、逻辑删除、内容读取。

`flow_definition.form_id`、`flow_node.form_id`、`flow_task.form_id`、`flow_his_task.form_id` 保持字符串语义。节点非空值覆盖流程默认值；任务与历史任务保存办理时引用快照。不要把宿主表单编码强制转换为数字。

若开发库已存在 `form_type`、`form_path`，应用升级后不再读取它们。删除旧列属于破坏性 DDL，应在备份并单独授权后执行，本次不自动执行。

## 回滚

先回滚应用版本。为避免丢失已创建的表单，默认保留新增 `flow_form`；如确需删除，必须另行备份并获得明确授权。MySQL 和 Oracle DDL 可能隐式提交，不能依赖事务回滚。
