# 审批人与提交人相同、审批人为空

系统自动流转统一使用系统处理人：审批策略自动跳过为 `SYSTEM_AUTO_APPROVE`，
等待节点恢复（含超时恢复）为 `SYSTEM_WAIT`，审批任务超时处理为 `SYSTEM_TIMEOUT`，
抄送节点自动完成为 `SYSTEM_CARBON_COPY`。历史扩展继续记录具体动作，例如
`WAIT_RESUME`、`WAIT_TIMEOUT`，以区分触发原因。宿主展示这些处理人时应识别为系统身份。
已有历史记录中的 `flovira:approver-policy`、`flovira:wait`、`flovira:timeout`、
`flovira:carbon-copy` 保持原样，宿主读取历史时应兼容旧标识；无需修改数据库结构。

审批节点的 `ext.approverRule.config` 支持以下配置。提交人取流程实例的
`createdBy`，即启动流程时的 `FlowParams.handler`，不会使用后续任务的当前办理人。

| 配置 | 值 | 行为 |
| --- | --- | --- |
| `sameAsStarterAction` | `SELF_APPROVE`（默认） | 保留提交人，由本人正常审批 |
| `sameAsStarterAction` | `AUTO_SKIP_OR_TRANSFER` | 从解析结果移除提交人；其余人员继续审批，无其他人则自动通过 |
| `sameAsStarterAction` | `TRANSFER_TO_USER` | 用指定人员替换提交人，保留其他审批人 |
| `emptyPolicy` | `ERROR`（默认） | 报错，阻止本次流转 |
| `emptyPolicy` | `SKIP` | 自动通过当前节点，继续流转 |
| `emptyPolicy` | `TRANSFER_TO_USER` | 交给指定人员审批 |

转交人员分别保存在 `sameAsStarterSubjects`、`emptyPolicySubjects`，格式为
`[{"id":"user-id","type":"USER","name":"姓名"}]`。宿主必须注册 `USER`
解析器；保存或发布时校验配置，到达节点时才解析最终用户 ID。解析器抛出的异常
不会被当成“无人审批”。转交目标解析为空、包含无效 ID，或同人转交仍返回提交人时，
引擎报错，不再递归转交或自动放行。

先处理原始解析结果为空的策略，再处理与提交人相同的策略。多人处理后的最终结果去重，
节点现有或签、会签和票签规则继续生效。抄送节点不应用这两项审批策略。

预览使用相同规则且不会创建或自动办理任务。没有实例的预览若启用同人跳过或转交，
须通过 `FlowParams.handler` 提供提交人。运行时在任务保存、创建监听器执行后自动流转，
历史状态为 `AUTO_PASS`，办理人为 `SYSTEM_AUTO_APPROVE`，历史扩展的 `action`
记录 `EMPTY_APPROVER` 或 `SAME_AS_STARTER`。自动跳过遇到循环会报错；宿主应将整个
启动或办理操作置于事务内，使解析或流转失败能够整体回滚。

业务显式设置 `nextHandler` 或通过分派监听器为待跳过任务指定人员后，任务正常等待审批。
既有任务使用已保存的人员快照，不重新应用策略。

标准审批策略基类通过能力描述提供这些选项；自行实现 `ApproverResolver` 的宿主可复用
`ApproverPolicyUtil.options()`，或在自己的描述中提供同名配置。

无需修改数据库结构或重建开发数据库。旧规则未配置无人策略时仍报错，未配置同人策略时仍由
本人审批。已有前端保存的上述配置升级后开始生效，发布前应复核这些规则；回滚代码后，空人员
恢复报错，同人跳过和转交配置不再执行，已有任务人员快照保持原样。
