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
                "USER", "ROLE", "DEPARTMENT_LEADER", "SUPERVISING_LEADER"));
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
        // 按 relationType 查询部门负责人、分管领导、角色成员或组织链。
        return Collections.emptyList();
    }
}
```

业务 Provider 只返回能力和数据。节点校验、定义序列化、审批人策略执行、任务创建、会签计算、空审批人处理、超时及状态流转仍由 Flovira 负责。

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

## 前端

Vue 使用 `setDataProvider(...)`，React 使用组件实例的 `dataProvider` 属性。两个包导出的 `DesignerCapabilities`、`DesignerResourceQuery`、`DesignerResourcePage` 和 `DesignerRelationshipQuery` 字段一致；设计器不依赖 Intelliconf 或其他业务系统 DTO。
