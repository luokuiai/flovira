/*
 *    Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.luokuiai.flovira.core.handler;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.*;
import com.luokuiai.flovira.core.entity.*;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.NodeService;
import com.luokuiai.flovira.core.service.impl.TaskServiceImpl;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import com.luokuiai.flovira.core.utils.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.Assert.*;

public class ApproverResolverTest {
    private final Map<String, ApproverRule> rules = new HashMap<String, ApproverRule>();
    private final List<String> resolvedNodes = new ArrayList<String>();
    private List<String> members = Arrays.asList("old");
    private int validations;
    private JsonConvert previousJson;
    private final ApproverResolver role = new AbstractRoleResolver() {
        public void validate(ApproverRule rule) {
            validations++;
            if (!"valid".equals(rule.getConfig().get("key"))) throw new IllegalArgumentException("Invalid config");
        }
        public List<String> resolve(ApproverContext context) {
            resolvedNodes.add(context.getNode().getNodeCode());
            return members;
        }
    };

    @Before
    public void setup() {
        previousJson = FlowEngine.jsonConvert;
        register(role);
        NodeService nodes = (NodeService) Proxy.newProxyInstance(NodeService.class.getClassLoader(),
            new Class<?>[]{NodeService.class}, (proxy, method, args) -> {
                if ("getExt".equals(method.getName())) {
                    return Collections.singletonMap("approverRule", ((Node) args[0]).getNodeCode());
                }
                throw new AssertionError("Unexpected persistence call: " + method.getName());
            });
        FrameInvoker.setBeanFunction(type -> NodeService.class.equals(type) ? nodes : null);
        java.util.concurrent.atomic.AtomicLong taskIds = new java.util.concurrent.atomic.AtomicLong(100);
        FlowEngine.setNewTask(() -> TestEntityFactory.create(Task.class).setId(taskIds.incrementAndGet()));
        FlowEngine.initDataFillHandler(null);
        FlowEngine.jsonConvert = new JsonConvert() {
            public <T> T strToBean(String json, Class<T> type) { return type.cast(rules.get(json)); }
            public Map<String, Object> strToMap(String json) { return Collections.emptyMap(); }
            public <T> List<T> strToList(String json) { return Collections.emptyList(); }
            public String objToStr(Object value) { return String.valueOf(value); }
        };
    }

    @After
    public void reset() {
        FrameInvoker.setBeansFunction(type -> Collections.emptyList());
        FrameInvoker.setBeanFunction(type -> null);
        FlowEngine.jsonConvert = previousJson;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void register(ApproverResolver... resolvers) {
        FrameInvoker.setBeansFunction(type -> ApproverResolver.class.equals(type)
            ? (Collection) Arrays.asList(resolvers) : Collections.emptyList());
    }

    private Node node(String code) {
        rules.put(code, new ApproverRule().setStrategy("ROLE")
            .setConfig(Collections.<String, Object>singletonMap("key", "valid")));
        return TestEntityFactory.create(Node.class).setNodeType(1).setNodeCode(code).setDefinitionId(1L);
    }

    @Test
    public void exposesAllStandardAbstractResolverCodesWithoutDefaultImplementations() {
        assertEquals("INITIATOR", new AbstractInitiatorResolver() {
            public void validate(ApproverRule rule) {}
            public List<String> resolve(ApproverContext context) { throw new AssertionError(); }
        }.getDefinition().getCode());
        assertEquals("USER", new AbstractUserResolver() {
            public void validate(ApproverRule rule) {}
            public List<String> resolve(ApproverContext context) { throw new AssertionError(); }
        }.getDefinition().getCode());
        assertEquals("ROLE", role.getDefinition().getCode());
        assertEquals("DEPARTMENT_LEADER", new AbstractDepartmentLeaderResolver() {
            public void validate(ApproverRule rule) {}
            public List<String> resolve(ApproverContext context) { throw new AssertionError(); }
        }.getDefinition().getCode());
        assertEquals("SUPERVISING_LEADER", new AbstractSupervisingLeaderResolver() {
            public void validate(ApproverRule rule) {}
            public List<String> resolve(ApproverContext context) { throw new AssertionError(); }
        }.getDefinition().getCode());
    }

    @Test
    public void validatesConfigurationWithoutResolvingFutureUsers() {
        Node first = node("first");
        Node future = node("future");
        NodeConfigValidator.validate(Arrays.asList(first, future));
        assertEquals(2, validations);
        assertTrue(resolvedNodes.isEmpty());
    }

    @Test
    public void taskAlwaysSnapshotsWorkflowFormEvenWhenNodeHasAnotherReference() {
        Node node = node("first").setFormId("node-form");
        Instance instance = TestEntityFactory.create(Instance.class).setId(2L).setDefinitionId(1L);
        Definition definition = TestEntityFactory.create(Definition.class).setId(1L).setFormId("workflow-form");
        TaskServiceImpl service = new TaskServiceImpl();
        Task task = service.addTask(node, instance, definition, FlowParams.build());
        assertEquals("workflow-form", task.getFormId());
        definition.setFormId(null);
        assertNull(service.addTask(node, instance, definition, FlowParams.build()).getFormId());
        assertEquals("workflow-form", task.getFormId());
    }

    @Test
    public void resolvesOnlyEnteredNodeAndKeepsEarlierAssignmentSnapshot() {
        Node first = node("first");
        Node future = node("future");
        NodeConfigValidator.validate(Arrays.asList(first, future));
        Instance instance = TestEntityFactory.create(Instance.class).setId(2L).setDefinitionId(1L);
        Definition definition = TestEntityFactory.create(Definition.class).setId(1L);
        FlowParams params = FlowParams.build().variables(new HashMap<String, Object>());
        TaskServiceImpl service = new TaskServiceImpl();
        Task earlier = service.addTask(first, instance, definition, params);
        assertEquals(Collections.singletonList("first"), resolvedNodes);
        members = Arrays.asList("new", "new");
        Task later = service.addTask(future, instance, definition, params);
        assertEquals(Arrays.asList("first", "future"), resolvedNodes);
        assertEquals(Collections.singletonList("old"), earlier.getPermissionList());
        assertEquals(Collections.singletonList("new"), later.getPermissionList());
    }

    @Test
    public void resolvedUserIdsAreNotEvaluatedAsExpressions() {
        Node node = node("first");
        members = Collections.singletonList("${user}");
        Task task = TestEntityFactory.create(Task.class).setPermissionList(
            ApproverRuleUtil.resolve(node, null, FlowParams.build(), true));
        ExpressionUtil.evalVariable(Collections.singletonList(task),
            FlowParams.build().variables(Collections.<String, Object>singletonMap("user", "wrong")));
        assertEquals(Collections.singletonList("${user}"), task.getPermissionList());
    }

    @Test
    public void previewAndRuntimeUseTheSameResolverWithExplicitContext() {
        final List<Boolean> modes = new ArrayList<Boolean>();
        final Instance instance = TestEntityFactory.create(Instance.class).setId(2L);
        register(new AbstractInitiatorResolver() {
            public void validate(ApproverRule rule) {}
            public List<String> resolve(ApproverContext context) {
                assertSame(instance, context.getInstance());
                modes.add(context.isPreview());
                return Collections.singletonList("business-applicant");
            }
        });
        Node node = node("first");
        rules.get("first").setStrategy("INITIATOR");
        assertEquals(ApproverRuleUtil.resolve(node, instance, FlowParams.build(), true),
            ApproverRuleUtil.resolve(node, instance, FlowParams.build(), false));
        assertEquals(Arrays.asList(true, false), modes);
    }

    @Test
    public void rejectsUnknownDuplicateAndMismatchedStrategies() {
        assertThrows(IllegalStateException.class, () -> FlowEngine.approverResolver("USER"));
        register(role, role);
        assertThrows(IllegalStateException.class, () -> FlowEngine.approverResolvers());
        register(new AbstractRoleResolver() {
            public ApproverStrategyDefinition getDefinition() {
                return super.getDefinition().setCode("USER");
            }
            public void validate(ApproverRule rule) {}
            public List<String> resolve(ApproverContext context) { return members; }
        });
        assertThrows(IllegalStateException.class, () -> FlowEngine.approverResolvers());
    }

    @Test
    public void rejectsInvalidConfigurationAndVersionWithoutResolving() {
        Node node = node("first");
        rules.get("first").setStrategyVersion(2);
        assertThrows(IllegalStateException.class, () -> ApproverRuleUtil.read(node));
        rules.get("first").setStrategyVersion(1).setConfig(Collections.<String, Object>emptyMap());
        assertThrows(IllegalArgumentException.class, () -> ApproverRuleUtil.read(node));
        assertTrue(resolvedNodes.isEmpty());
    }

    @Test
    public void rejectsMissingRulesAndEmptyResultsInsteadOfFallingBack() {
        Node node = node("first").setPermissionFlag("legacy-user");
        rules.remove("first");
        assertThrows(IllegalStateException.class, () -> ApproverRuleUtil.resolve(node, null, FlowParams.build(), false));
        node("first");
        members = Collections.emptyList();
        assertThrows(IllegalStateException.class, () -> ApproverRuleUtil.resolve(node, null, FlowParams.build(), false));
        members = Collections.singletonList(" ");
        assertThrows(IllegalStateException.class, () -> ApproverRuleUtil.resolve(node, null, FlowParams.build(), false));
    }

    @Test
    public void hasNoImplicitStandardResolvers() {
        register();
        assertTrue(FlowEngine.approverResolvers().isEmpty());
        assertThrows(IllegalStateException.class, () -> FlowEngine.approverResolver("INITIATOR"));
    }
}
