# 外部业务表单引用迁移

Flovira 只保存业务表单标识，不维护表单定义、页面地址或低代码渲染器。业务系统可自行建立表单表、版本表和关联表。

## 新契约

- `Definition`、`Node`、`Task`、`HisTask` 及对应 DTO / ORM 字段统一为字符串 `formId`；MySQL、PostgreSQL、Oracle 对应列为 `form_id`，长度 100。`Instance.formId` 仍为非持久化展示字段。
- 节点 `formId` 非空时覆盖流程默认表单；为空时继承。创建任务时保存实际引用，历史任务从任务复制引用。历史读取使用办理时快照，不依赖当前流程或节点配置。
- `FlowEngine.taskService().load(...)` / `hisLoad(...)` 返回 `{ formId, data }`。`data` 仍来自 `variables.formData`。业务系统根据 `formId` 选择页面、获取结构并渲染。
- 删除 `formCustom`、`FormCustomEnum`、内置 `Form` / `FormService` / DAO / Mapper / 自动装配，以及 `FlowEngine.formService()`、`newForm()`、`setNewForm()`。删除 `FormDefinition` / `FormFieldDefinition` 这套内置表单结构协议。
- 删除 `/flovira/form-content/{id}` GET 和 `/flovira/form-content` POST。原使用方改为调用自己的表单管理接口。任务数据加载、提交和历史数据变更查询保留。
- 设计器表单列表通过外部资源查询 `resourceType: 'FORM'` 提供，每项 `id` 是业务表单标识、`name` 是展示名称；旧 `FORM_PATH` 资源类型同步迁移为 `FORM`。React 开始节点配置流程默认表单，审批节点配置覆盖表单；Vue 基础信息及节点配置使用相同语义。
- 条件字段继续由业务回调提供。React `queryConditionFields` 获得的流程定义包含 `formId`，业务方据此获取字段；引擎不查询表单结构。
- 原 `DefJson.formPathList` 删除；选项属于资源接口，不保存到流程定义。

## 业务字段名称回调

历史表单变更保留。可选实现 `FormFieldProvider`，返回字段编码与名称的映射：

```java
public class BusinessFormFields implements FormFieldProvider {
    @Override
    public Map<String, String> getFieldLabels(String formId) {
        return businessFormService.getFieldLabels(formId);
    }
}
```

Spring 使用方将实现注册为 Bean；其它框架在现有 `FrameInvoker.setBeanFunction(...)` 中接入该类型。不要为表单回调重新覆盖整个 Bean 查询函数。

未提供实现、表单未找到或字段未命中时展示字段编码。业务回调执行失败会向上传递。一次历史查询内相同 `formId` 只查询一次，不做跨请求缓存。需保留旧版本字段名称时，业务方使用带版本的标识，例如 `expense:v2`。

`LISTENER_FORM_LOAD` 继续保留：配置节点表单时使用节点监听器，否则使用流程监听器。该监听器现在适用于外部表单，不再受旧自定义开关限制。

## 现有代码与 JSON

1. 业务代码将 `get/setFormPath` 改为 `get/setFormId`，移除 `get/setFormCustom`。DTO 消费方从 `FlowDto.form` / `formContent` 改为 `formId`，通过业务接口获取表单。不要将 `formId` 转为 `Long`。
2. 盘点旧表单引用。旧 `formPath` 可能是页面路径、内置表单 ID 或业务编码，必须先确定映射，不能无条件改字段名就完成数据迁移。
3. 准备映射文件，例如：

```json
[
  { "formCustom": "N", "formPath": "/expense/approve", "formId": "expense:v2" },
  { "formCustom": "Y", "formPath": "20", "formId": "finance:v1" }
]
```

4. 使用 Node 18+ 执行：

```bash
node scripts/migrate-form-references.mjs old-definition.json mappings.json migrated-definition.json
```

脚本拒绝覆盖已有输出文件。只迁移定义及 `nodeList` 的表单引用，不递归改业务变量、扩展配置或连线条件。缺少映射、重复映射和新旧引用冲突都会报错；旧字段为空时直接移除。新 JSON 可重复处理。此工具处理流程定义 JSON，不是数据库迁移工具；数据库中的历史快照需按下一节单独迁移。

## 已有数据库

当前三套 V1 脚本用于新建库，不要在已有数据库上重新执行。当前初始化脚本原本就不包含 `flow_form`；某些接入项目可能自行建过这张表，需要单独盘点。

建议在暂停流程写入并备份后，按各数据库方言执行以下阶段。这里不提供自动执行的破坏性脚本：

1. 在 `flow_definition`、`flow_node`、`flow_task`、`flow_his_task` 新增可空字符串列 `form_id`（MySQL / PostgreSQL `varchar(100)`，Oracle `VARCHAR2(100)`）。暂时保留原列。
2. 如使用过内置表单，将 `flow_form` 中的表单定义、版本、状态及页面配置迁移到业务系统自己的表，建立旧引用到新 `formId` 的映射。核对行数和内容后才切换读取方。
3. 按旧 `form_custom` 与 `form_path` 映射回填四张表；多租户系统的映射要包含租户范围。历史任务使用自身的旧引用，不能用当前流程表单覆盖所有历史记录。原任务引用缺失的情况单独核对，不能猜测绑定。
4. 迁移已存储的流程设计 JSON；`variables.formData` 审批数据快照保留，不做键名替换。
5. 核对所有非空旧引用均有新值，验证流程复制、导入导出、节点继承与覆盖、待办加载、历史加载和历史字段名称。部署后端与前端的新契约，并同步外部业务回调。
6. 完成验收后再安排删除旧 `form_custom` / `form_path` 列及业务已迁出的内置表。删除前确认没有旧版本应用继续访问。

回滚阶段保留旧列和表，恢复应用版本与备份；若切换后已经产生新任务，需要先停止写入并核对新增数据，不能只回滚应用。MySQL / Oracle 的 DDL 可能隐式提交，不能依赖事务回滚恢复结构；PostgreSQL 也需事先安排备份和维护窗口。

## 验证记录

- `./gradlew build --offline` 全量构建通过；两套 ORM 的 PostgreSQL 合同测试验证了字符串 `formId` 的实际写入和读取。
- `bun run test`：Vue 9、React 63、Lumen 8、Antd 7，共 87 项通过；`bun run build` 完成组件库、适配器及全部示例构建。
- JSON 迁移脚本两项测试通过；Gson、Jackson、Jackson 3 的 DTO / ORM 序列化及定义复制往返检查通过。
- 三套 SQL 的十张表字段集合一致；四张相关表均包含 `form_id`，无内置表单结构或旧表单列。Oracle 脚本统一为 `sql/oracle/flovira-v1.sql`。MySQL / Oracle 仅做静态检查，未连接实际数据库执行。
