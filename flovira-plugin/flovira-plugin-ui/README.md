# Flovira Designer Integration

`flovira-plugin-ui-core` 定义设计器与业务系统的稳定契约，`flovira-plugin-ui-sb-web` 仅把契约桥接为可选 REST API，不包含页面或静态资源。

## 业务接入

业务系统按需提供设计器能力和资源 Spring Bean。下面的策略仅为配置示例，不是固定枚举：

```java
@Component
public class WorkflowCapabilities implements DesignerCapabilityProvider {
    @Override
    public DesignerCapabilities getCapabilities() {
        return DesignerCapabilities.defaults()
            .setApproverStrategies(Arrays.asList(
                DesignerApproverStrategy.resource("USER", "指定人员", "USER", null),
                DesignerApproverStrategy.resource("ROLE", "指定角色", "ROLE",
                    BusinessRelationProvider.ROLE_MEMBERS),
                DesignerApproverStrategy.relation("DEPARTMENT_LEADER", "部门负责人",
                    BusinessRelationProvider.DEPARTMENT_LEADER),
                DesignerApproverStrategy.relation("SUPERVISING_LEADER", "分管领导",
                    BusinessRelationProvider.SUPERVISING_LEADER),
                DesignerApproverStrategy.expression("EXPRESSION", "表达式")));
    }
}

@Component
public class WorkflowBusinessData implements DesignerDataProvider {
    @Override
    public DesignerResourcePage queryResources(DesignerResourceQuery query) {
        // 按 resourceType 查询 USER / ROLE / ORGANIZATION / FORM_FIELD /
        // DICTIONARY / SUBPROCESS，并转换为稳定字符串 ID。
        return new DesignerResourcePage();
    }

    @Override
    public List<BusinessSubject> resolveRelationship(BusinessRelationQuery query) {
        // 按 relationType 查询部门负责人、分管领导、角色成员、组织成员或组织链。
        return Collections.emptyList();
    }
}
```

接入方可以定义任意策略 `code`，并注册同 `code` 的 `ApproverResolver`。解析器接收设计器保存的
`ApproverRule` 和流程变量，返回最终用户 ID：

```java
@Component
public class ProjectOwnerApproverResolver implements ApproverResolver {
    @Override
    public String getStrategy() {
        return "PROJECT_OWNER";
    }

    @Override
    public List<String> resolve(Node node, ApproverRule rule, FlowParams flowParams) {
        // 由业务系统校验自定义配置，并根据 rule 与 flowParams 解析用户。
        return projectService.findOwnerIds(flowParams.getVariables());
    }
}
```

`DesignerCapabilityProvider` 未配置时返回 Flovira 内置默认能力。某策略注册了
`ApproverResolver` 时优先使用业务解析器，即使它与内置策略同名；没有匹配解析器时才使用
Flovira 对 `USER`、`ROLE`、`ORGANIZATION` 和 `EXPRESSION` 的默认解析逻辑。

`DesignerApproverStrategy` 的 `selectionType` 可为 `RESOURCE`、`RELATION` 或 `EXPRESSION`，只描述运行时解析语义。`editorType` 独立控制 React 设计器交互：`NONE` 不展示配置、`INLINE` 在 Drawer 内展示、`DIALOG` 通过 `+` 打开弹窗；`multiple` 只控制编辑器选择几个对象，不能用于判断解析结果人数；`resultCardinality` 使用 `EXACTLY_ONE`、`ONE_OR_MORE`、`ZERO_OR_ONE` 或 `ZERO_OR_MORE` 描述解析为具体人员后的数量范围；`editorKey` 供业务前端定位自定义编辑器。每个策略还可通过 `options` 声明任意附加单选项，并用 `condition=MULTIPLE` 或 `EMPTY` 按结果范围决定是否展示，例如直接单选具体人员不展示多人和无人策略，单选分组则可以同时展示两者。选项结果以及业务编辑器的额外结构化数据统一写入 `ApproverRule.config`，由同 `code` 的解析器消费。`RESOURCE` 策略声明默认列表查询的 `resourceType`，需要展开成员时同时声明 `relationType`。Vue 和 React 都会把结果保存为相同的 `approverRule` 节点扩展。

业务 Provider 只返回能力和数据。节点校验、定义序列化、审批人策略执行、结果去重、空审批人处理、任务创建、会签计算、超时及状态流转仍由 Flovira 负责。关系 Provider 缺失、返回非法主体或最终没有办理人时，任务创建会明确失败。

1.0.0 只提供上述统一契约，不包含旧的办理人、字典、分类、节点扩展或监听器 Service。未注册 `DesignerDataProvider` 时，资源查询返回空结果。

## Spring Web

默认 API 前缀为 `/flovira`：

- `GET /flovira/integration/capabilities`
- `GET /flovira/integration/resources`
- `POST /flovira/integration/relationships/resolve`

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
            @RequestBody DefJson defJson,
            @RequestHeader("onlyNodeSkip") boolean onlyNodeSkip) throws Exception {
        return super.saveJson(defJson, onlyNodeSkip);
    }
}
```

子类未声明类级 `@RequestMapping` 时继续使用 `flovira.ui-api-prefix`；需要完全自定义路径时可在子类声明自己的类级映射。覆盖方法时应重新声明该方法需要的 Spring MVC 参数和映射注解，避免依赖不同 Spring 版本对继承注解的解析差异。

## 前端

Vue 使用 `setDataProvider(...)`。React 本体不调用 HTTP 接口，宿主加载后通过 `capabilities`
属性传入能力清单，并通过 `queryResources` 回调提供按需资源查询。两个包导出的
`DesignerCapabilities`、`DesignerApproverStrategy`、`ApproverRule`、`DesignerResourceQuery`、`DesignerResourcePage`
和 `DesignerRelationshipQuery` 字段一致；设计器不依赖 Intelliconf 或其他业务系统 DTO。
