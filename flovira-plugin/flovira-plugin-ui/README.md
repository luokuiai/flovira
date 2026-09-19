# Flovira Designer Integration

`flovira-plugin-ui-core` 定义设计器与业务系统的稳定契约，`flovira-plugin-ui-sb-web` 仅把契约桥接为可选 REST API，不包含页面或静态资源。

## 业务接入

审批策略仅来自业务注册的 `ApproverResolver`，能力接口自动返回对应的
`ApproverStrategyDefinition`。未注册任何解析器时，前端没有可选人员策略。

Flovira 提供 `INITIATOR`、`USER`、`ROLE`、`DEPARTMENT_LEADER`、
`SUPERVISING_LEADER` 五个标准 code 及对应的 `Abstract*Resolver`。
这些抽象类固定 code 和默认编辑描述，不查询人员、不自动注册。业务也可以直接实现
`ApproverResolver` 定义其他稳定 code。

业务实现 `validate(ApproverRule)` 校验配置，以及
`resolve(ApproverContext)` 返回最终用户 ID。配置保存和发布只校验，不解析人员；
到达节点创建任务时才解析。预览使用同一个解析器并设置 `preview=true`，业务实现必须只读。
提交人身份和关系数据均由业务确定，不能依赖引擎从当前登录人推断。

`strategy`、`strategyVersion` 和业务 `config` 随节点扩展保存，必须与注册描述一致。
未知 code、重复注册、不支持的版本或空解析结果会明确报错，不会回退到其他策略。
已有任务使用其分配记录，未来节点才按当前业务数据解析。数据库表结构不变。

`selectionType` 可为 `RESOURCE`、`RELATION` 或 `EXPRESSION`，仅描述配置形式，
不触发引擎内置人员解析。`editorType` 控制 React 编辑方式：
`NONE` 不显示配置，`INLINE` 内联显示，`DIALOG` 打开弹窗。
`resourceType` 指定资源查询类型；`multiple` 控制资源选择数量；
`resultCardinality` 描述解析结果范围；`editorKey` 定位业务编辑器。
可选的 `options` 和自定义结构保存在 `ApproverRule.config`，由业务解析器校验和消费。

`DesignerDataProvider` 只负责 `queryResources`，不再承担运行时关系解析。
`DesignerCapabilityProvider` 可配置节点等其他能力，不能声明没有 Resolver 的人员策略。

1.0.0 只提供上述统一契约，不包含旧的办理人、字典、分类、节点扩展或监听器 Service。未注册 `DesignerDataProvider` 时，`FORM` 查询返回 Flovira 管理的已发布表单，其它资源查询返回空结果。宿主 Provider 返回非空分页结果时由宿主管理该类资源。

## Spring Web

默认 API 前缀为 `/flovira`：

- `POST /flovira/save-json`：完整保存流程定义、节点和连线，请求体为流程 JSON。
- `GET /flovira/export?type=design|form|package&id={id}`：统一导出流程设计、完整表单或流程包；`type` 必填。
- `POST /flovira/import?type=design|form|package`：直接提交对应导出对象；单独设计校验目标表单，完整包自动恢复表单并重建关联。
- `GET /flovira/integration/capabilities`
- `GET /flovira/integration/resources`
- `GET /flovira/form-content/{id}`
- `POST /flovira/form-content`

业务系统可配置自己的前缀：

```yaml
flovira:
  ui: true
  ui-api-prefix: /admin/v1/flovira
```

接口鉴权由宿主 Spring Security 负责。关闭 `flovira.ui` 后不会注册这些 Controller。

### 扩展内置 Controller

内置 `FloviraController` 是默认实现。业务声明其子类 Bean 后，Flovira 会通过
`@ConditionalOnMissingBean` 自动停止注册默认 Controller，因此可以在子类增加权限、关系授权、审计、限流、接口文档等任意注解，同时继续复用未覆盖的接口。

```java
@RestController
@PreAuthorize("hasAuthority('workflow:designer')")
@Tag(name = "流程设计器")
public class BusinessFloviraController extends FloviraController {

    @Override
    @PostMapping("/save-json")
    @PreAuthorize("@workflowAuth.canSave(#defJson)")
    public ApiResult<Void> saveJson(
            @RequestBody DefJson defJson) throws Exception {
        return super.saveJson(defJson);
    }
}
```

子类未声明类级 `@RequestMapping` 时继续使用 `flovira.ui-api-prefix`；需要完全自定义路径时可在子类声明自己的类级映射。覆盖方法时应重新声明该方法需要的 Spring MVC 参数和映射注解，避免依赖不同 Spring 版本对继承注解的解析差异。

## 前端

Vue 使用 `setDataProvider(...)`。React 本体不调用 HTTP 接口，宿主加载后通过 `capabilities`
属性传入能力清单，并通过 `queryResources` 回调提供按需资源查询。两个包导出的
`DesignerCapabilities`、`DesignerApproverStrategy`、`ApproverRule`、`DesignerResourceQuery`、`DesignerResourcePage`
字段一致；设计器不依赖 Intelliconf 或其他业务系统 DTO。
