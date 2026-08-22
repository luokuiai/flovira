# Flovira Designer Integration

`flovira-plugin-ui-core` 定义设计器与业务系统的稳定契约，`flovira-plugin-ui-sb-web` 仅把契约桥接为可选 REST API，不包含页面或静态资源。

## 业务接入

业务系统按需提供两个 Spring Bean：

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

`DesignerApproverStrategy` 的 `selectionType` 可为 `RESOURCE`、`RELATION` 或 `EXPRESSION`。`RESOURCE` 策略声明前端查询的 `resourceType`，需要展开成员时同时声明 `relationType`；`RELATION` 策略不要求用户选择资源，运行时直接按关系和流程变量查询。Vue 和 React 都会把结果保存为相同的 `approverRule` 节点扩展。

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

Vue 使用 `setDataProvider(...)`，React 使用组件实例的 `dataProvider` 属性。两个包导出的 `DesignerCapabilities`、`DesignerApproverStrategy`、`ApproverRule`、`DesignerResourceQuery`、`DesignerResourcePage` 和 `DesignerRelationshipQuery` 字段一致；设计器不依赖 Intelliconf 或其他业务系统 DTO。
