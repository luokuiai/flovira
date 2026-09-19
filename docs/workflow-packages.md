# 流程包导入、导出与前端预览

完整迁移使用后端 `DefService.exportPackage` / `importPackage`。Vue 和 React
设计器不再内置 JSON 导入、导出按钮，入口和权限由宿主系统负责。图片下载、
设计器读取/载入设计 JSON 的已有编程接口保留，用于编辑和预览，不代表完整迁移。

## 后端

```java
WorkflowPackage bundle = FlowEngine.defService().exportPackage(definitionId);
String json = FlowEngine.jsonConvert.objToStr(bundle);

WorkflowPackage input = FlowEngine.jsonConvert.strToBean(json, WorkflowPackage.class);
Map<String, String> externalForms = new HashMap<>();
// 仅当包声明了宿主表单时，填写源引用到目标引用的映射。
// externalForms.put("host:old-expense", "host:new-expense");
WorkflowImportResult result = FlowEngine.defService().importPackage(input, externalForms);
Long newRootId = result.getRootDefinitionId();
```

默认设计器 API 提供统一导入导出接口（前缀可配置）：

统一入口为 `GET /flovira/export?type=...&id=...` 和
`POST /flovira/import?type=...`。`type` 必填且只接受以下三种值；导入请求体直接传
对应的导出对象，不增加外层包装，未知类型明确报错。

| type | 导出的 data 格式 | ID 含义 | 导入行为 |
| --- | --- | --- | --- |
| `design` | `DefJson`，只含流程设计及 `formId` | 流程定义 ID | 新建未发布版本；所引用表单须在当前租户存在 |
| `form` | `WorkflowPackage.PackagedForm`，含名称、编码、源版本、完整内容和 ext | 表单 ID | 新建未发布表单版本，返回 `formReferences` 新旧 ID 映射 |
| `package` | `WorkflowPackage`，流程、固定子流程及表单 | 根流程定义 ID | 自动恢复包内表单并重建关联 |

三种导入均返回 `ApiResult<WorkflowImportResult>`，三种导出均返回 `ApiResult`，其
`data` 对应上表。单独表单导入的 `rootDefinitionId` 为空、`definitionIds` 为空。
单独设计导入保留提供的 `formId`：先导入表单时，使用返回映射替换设计的 `formId`，
再导入设计。默认接口只能验证当前租户的托管表单，缺失、跨租户或无法验证的引用均报错。
完整迁移直接使用 `package`，不需要人工修改包内引用。

完整流程包包含引用的 `flow_form.form_content`，自定义字段、对象和数组内容一并保留。
业务导出按钮应下载 `export?type=package` 响应的 `data`，导入按钮把同一份 JSON 提交给
`import?type=package`。不要用 `query-def`、`exportJson()` 或设计器 `getFlowJson()` 的结果
代替完整流程包。先保存表单和流程，再导出；尚未保存的编辑内容不会包含在包内。
这两个 HTTP 接口针对完整的包内表单；确实仅有外部引用、未存入 `flow_form` 的包，
仍需通过 SDK 的 `importPackage` 显式提供外部映射，缺少映射会报错。

Vue / React 接入方的导出按钮可复用自己的鉴权请求封装：

```ts
// api 是接入方已有的请求封装，返回解析后的 ApiResult。
const response = await api.get(`/flovira/export?type=package&id=${definitionId}`)
const bundle = parseWorkflowPackage(response.data)
const url = URL.createObjectURL(new Blob([JSON.stringify(bundle, null, 2)], {
  type: 'application/json',
}))
const link = document.createElement('a')
link.href = url
link.download = `${bundle.rootFlowCode}.workflow.json`
document.body.appendChild(link)
link.click()
link.remove()
setTimeout(() => URL.revokeObjectURL(url), 0)

// 导入上传的同一文件，后端会恢复 forms 并关联新的 formId。
const result = await api.post('/flovira/import?type=package', parseWorkflowPackage(fileText))
const newDefinitionId = result.data.rootDefinitionId
```

SDK 方法使用现有 `TransactionExecutor`：Spring starter 自动注册，其他框架需提供
真实事务实现。导入失败整体回滚，不允许通过无事务实现伪装成功。导出需要稳定视图
的宿主应按自己的数据库配置合适的事务隔离级别，并避免同时编辑这些定义和表单。

### 包内容与引用规则

- `schemaVersion: 1` 标识包格式，不是应用或流程版本。
- `rootFlowCode` 指向根流程；`definitions` 包含完整设计及递归引用的固定子流程。
  子流程取导出时已发布、激活的版本；缺失依赖、循环依赖或同编码不同版本会报错。
- `forms` 包含流程定义引用的 Flovira 表单，按源引用去重，保存源版本和完整
  `formContent`。规范十进制 ID 且能在当前租户 `flow_form` 中找到的引用视为托管表单。
  包中的 `reference` 仅用于包内关联，不能作为目标数据库 ID 使用。
- 未能解析为当前租户托管表单的引用列入 `externalFormIds`，不会猜测或静默丢弃。
  导入时必须逐一显式映射；宿主负责确认目标外部表单可用。包外同名 ID 不会自动复用。
- 不携带实例、任务、审批数据、源租户或源审计身份。表单内容和设计扩展作为数据保存，
  导入不会执行监听器、审批人表达式或表单脚本。

### 导入策略

导入创建新的定义、节点、连线和表单 ID，并重建所有表单引用。相同流程/表单编码
沿用现有版本递增规则，创建新版本，不覆盖目标环境的旧版本。源版本用于展示和审核；
实际新版本由目标环境分配。

定义和表单均为未发布状态。返回的 `definitionIds` 按子流程在前的顺序排列，宿主可
先审核并发布表单和子流程，最后发布父流程。子流程运行时仍按流程编码查已发布版本，
因此发布前必须确认依赖版本，不能只发布父流程而意外使用目标环境旧子流程。

人员/角色标识、监听器类名、表达式和业务扩展原样保留，包不搬迁宿主账户、代码或
字典。宿主须在发布前审核这些依赖。`ext` / `formContent` 内私有协议中的其他 ID
不会被引擎猜测并替换。

## 前端离线解析与展示

两个 npm 包均导出以下纯解析方法，无需数据库、网络或文件输入控件：

```ts
import {
  parseWorkflowPackage,
  getPackageDefinition,
  getPackageForm,
  parsePackageFormContent,
} from '@luokuiai/flovira-react-designer'
// Vue 使用同名导出：@luokuiai/flovira-vue-designer

const bundle = parseWorkflowPackage(jsonText)
const root = getPackageDefinition(bundle)
const child = getPackageDefinition(bundle, 'child_flow_code')
const form = getPackageForm(bundle, root.formId as string)
const formData = form ? parsePackageFormContent(form) : undefined
```

React 可直接渲染 `<FlowPreview value={root} />`，或把 `root` 作为设计器 `value`。
Vue 可使用 `<FlowDesigner :initial-json="root" disabled only-design-show />`。
子流程切换同样使用 `getPackageDefinition`，无需查询目标数据库。

`getPackageForm` 返回表单名称、编码、源版本和原始内容；外部引用返回 `undefined`，
供宿主显示待映射状态。使用 `root.formId` 选择流程表单，节点不覆盖流程表单。
`parsePackageFormContent` 只调用 JSON 解析，
返回数据交给宿主已有表单渲染器。非 JSON 内容会明确报错，宿主也可自行展示原始内容；
不要将不可信包内容直接作为 HTML 或脚本执行。

解析器检查版本、根流程、重复编码、连线引用、子流程依赖和完整表单清单；不代替
后端导入校验、租户权限或宿主业务资源校验。返回独立副本，不修改传入对象。

## 验证

两套 ORM 的 PostgreSQL 契约测试覆盖包往返、共享表单去重、新 ID/版本、未发布状态、
外部表单映射、缺少子流程、跨租户拒绝和写入失败整体回滚。前端使用同一 JSON 样例，
验证离线解析、根流程/子流程预览数据及表单内容读取。无生产数据库迁移或发布操作。
