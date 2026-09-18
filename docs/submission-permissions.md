# 提交范围

开始、结束节点直接展示配置，不使用 Tab，也不展示审批配置和表单权限。开始节点增加“可提交人员”，默认全员；回调配置继续保留。

开始节点通过 `ext.submitterRule` 保存提交范围，结构复用 `ApproverRule`：

```json
{
  "schemaVersion": 1,
  "strategyVersion": 1,
  "strategy": "USER",
  "selectionType": "RESOURCE",
  "subjects": [{ "id": "user-1", "type": "USER", "name": "张三" }]
}
```

- `ALL`：全员，内置默认；`selectionType` 为 `RELATION`，`subjects` 为空。
- `USER`：指定人员，使用业务注册的 `USER` 解析器。
- `ROLE`：指定角色，保存角色标识，提交时由业务解析器返回当前角色成员的用户 ID。

宿主继续通过 `ApproverResolver` 的 `validate` 和 `resolve` 分别校验配置和解析用户。`USER`、`ROLE` 默认支持提交范围；业务自定义策略覆盖 `supportsSubmission()` 返回 `true` 后，可出现在 `capabilities.submitterStrategies`。未注册的策略不能使用。`ALL` 是保留编码，不由业务解析器覆盖。

提交范围不使用审批的空人跳过、转交、会签等策略。解析结果为空表示无人可提交；当前 `FlowParams.handler` 必须在解析出的最终用户 ID 中。宿主必须从认证信息设置 `handler`，不能直接信任前端传入的身份。

执行顺序是：读取开始节点配置 → `beforeOperation` 初始化可信业务上下文 → 提交范围校验 → 审批人解析和实例/任务持久化。`start`、`startByDefinitionId` 及经过这些入口的子流程启动都执行校验；失败抛出异常，事务回滚。已有任务的办理、退回后的重新提交仍使用已有任务的权限规则。

React 复用 `onSelectApprover` / `renderApproverEditor` 和资源查询；回调可通过 `node.nodeType === '0'` 区分提交范围。Vue 复用设计器资源提供接口和人员/角色选择器。业务自定义配置项仅在 `nodeTypes` 显式包含 `"0"` 时显示。

未配置 `submitterRule` 的已有流程保持全员可提交，无需数据库迁移。存在但格式错误、版本不支持或解析器未注册的规则会报错，不能隐式退回全员。版本回退前应移除或在业务入口自行执行新增的提交限制；旧引擎不会识别这一权限规则。
