/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.FlowCombine;
import com.luokuiai.flovira.core.dto.ProgressResult;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Skip;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.DefService;
import com.luokuiai.flovira.core.service.InsService;
import com.luokuiai.flovira.core.service.NodeService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 流程节点审批人预计算测试。
 *
 * @author warm
 */
public class ProgressServiceImplTest {

    private final Node start = node("start", NodeType.START, null);
    private final Node first = node("first", NodeType.BETWEEN, "userA");
    private final Node second = node("second", NodeType.BETWEEN, "userB");
    private final Node end = node("end", NodeType.END, null);
    private final Map<String, Object> capturedVariables = new HashMap<>();

    @Before
    public void setUp() {
        FlowEngine.setNewDef(() -> TestEntityFactory.create(Definition.class));
        FlowEngine.setNewTask(() -> TestEntityFactory.create(Task.class));
        FlowEngine.jsonConvert = new VariableJsonConvert();
    }

    @Test
    public void shouldPreviewDefinitionNodesAndHandlersWithoutPersistingTasks() {
        Definition definition = TestEntityFactory.create(Definition.class).setId(1L).setFlowCode("LEAVE");
        configureEngine(definition, null);

        ProgressResult result = new ProgressServiceImpl()
            .previewByDefinitionId(1L, Collections.<String, Object>emptyMap());

        assertEquals(Long.valueOf(1L), result.getDefinitionId());
        assertNull(result.getInstanceId());
        assertEquals("start", result.getSourceNodeCode());
        assertEquals(Arrays.asList("first", "second"), Arrays.asList(
            result.getNodes().get(0).getNodeCode(), result.getNodes().get(1).getNodeCode()));
        assertEquals(Collections.singletonList("userA"), result.getNodes().get(0).getHandlers());
        assertEquals(Collections.singletonList("userB"), result.getNodes().get(1).getHandlers());
    }

    @Test
    public void shouldStartAfterCurrentNodeAndOverlayInstanceVariables() {
        Definition definition = TestEntityFactory.create(Definition.class).setId(1L).setFlowCode("LEAVE");
        Instance instance = TestEntityFactory.create(Instance.class).setId(2L).setDefinitionId(1L)
            .setNodeCode("first").setVariable("instance");
        Map<String, Object> instanceVariables = new HashMap<>();
        instanceVariables.put("amount", 10);
        instanceVariables.put("retained", "kept");
        TestEntityFactory.put(instance, "VariableMap", instanceVariables);
        configureEngine(definition, instance);
        Map<String, Object> formData = Collections.<String, Object>singletonMap("days", 3);
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 20);
        variables.put("formData", formData);

        ProgressResult result = new ProgressServiceImpl().previewByInstanceId(2L, variables);

        assertEquals("first", result.getSourceNodeCode());
        assertEquals(1, result.getNodes().size());
        assertEquals("second", result.getNodes().get(0).getNodeCode());
        assertEquals(20, capturedVariables.get("amount"));
        assertEquals("kept", capturedVariables.get("retained"));
        assertEquals(formData, capturedVariables.get("formData"));
    }

    @Test
    public void shouldSelectConditionalBranchFromBusinessVariables() {
        Node gateway = node("amountGateway", NodeType.SERIAL, null);
        Node manager = node("manager", NodeType.BETWEEN, "managerUser");
        Node finance = node("finance", NodeType.BETWEEN, "financeUser");
        FlowCombine flowCombine = new FlowCombine();
        flowCombine.setAllNodes(Arrays.asList(start, gateway, manager, finance, end));
        flowCombine.setAllSkips(Arrays.asList(
            skip("start", "amountGateway", SkipType.PASS.getKey(), null),
            skip("amountGateway", "manager", null, "ge@@amount|1000"),
            skip("amountGateway", "finance", null, null),
            skip("manager", "end", SkipType.PASS.getKey(), null),
            skip("finance", "end", SkipType.PASS.getKey(), null)
        ));
        configureConditionalEngine(flowCombine);
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 1500);

        ProgressResult result = new ProgressServiceImpl().previewByDefinitionId(1L, variables);

        assertEquals(1, result.getNodes().size());
        assertEquals("manager", result.getNodes().get(0).getNodeCode());
        assertEquals(Collections.singletonList("managerUser"), result.getNodes().get(0).getHandlers());

        variables.put("amount", 500);
        ProgressResult defaultResult = new ProgressServiceImpl().previewByDefinitionId(1L, variables);
        assertEquals("finance", defaultResult.getNodes().get(0).getNodeCode());
    }

    @SuppressWarnings("unchecked")
    private void configureEngine(Definition definition, Instance instance) {
        FlowCombine flowCombine = new FlowCombine();
        flowCombine.setAllNodes(Arrays.asList(start, first, second, end));
        DefService defService = proxy(DefService.class, (method, args) -> {
            if ("getById".equals(method.getName()) || "getPublishByFlowCode".equals(method.getName())) {
                return definition;
            }
            if ("getFlowCombineNoDef".equals(method.getName())) {
                return flowCombine;
            }
            return defaultValue(method.getReturnType());
        });
        NodeService nodeService = proxy(NodeService.class, (method, args) -> {
            if ("getStartNode".equals(method.getName())) {
                return start;
            }
            if ("getByDefIdAndNodeCode".equals(method.getName())) {
                return first;
            }
            if ("getExt".equals(method.getName())) {
                return Collections.emptyMap();
            }
            if ("getNextNodeList".equals(method.getName()) && args[0] instanceof Node) {
                capturedVariables.putAll((Map<String, Object>) args[3]);
                return next((Node) args[0]);
            }
            return defaultValue(method.getReturnType());
        });
        InsService insService = proxy(InsService.class, (method, args) ->
            "getById".equals(method.getName()) ? instance : defaultValue(method.getReturnType()));
        FrameInvoker.setBeanFunction(type -> {
            if (DefService.class.equals(type)) return defService;
            if (NodeService.class.equals(type)) return nodeService;
            if (InsService.class.equals(type)) return insService;
            return null;
        });
        FlowEngine.initPermissionHandler(null);
    }

    @SuppressWarnings("unchecked")
    private void configureConditionalEngine(FlowCombine flowCombine) {
        Definition definition = TestEntityFactory.create(Definition.class).setId(1L).setFlowCode("PURCHASE");
        DefService defService = proxy(DefService.class, (method, args) -> {
            if ("getById".equals(method.getName())) return definition;
            if ("getFlowCombineNoDef".equals(method.getName())) return flowCombine;
            return defaultValue(method.getReturnType());
        });
        NodeServiceImpl delegate = new NodeServiceImpl();
        NodeService nodeService = proxy(NodeService.class, (method, args) -> {
            if ("getStartNode".equals(method.getName())) return start;
            if ("getExt".equals(method.getName())) return Collections.emptyMap();
            if ("getNextNodeList".equals(method.getName()) && args[0] instanceof Node) {
                return delegate.getNextNodeList((Node) args[0], (String) args[1], (String) args[2],
                    (Map<String, Object>) args[3], null, (FlowCombine) args[5]);
            }
            return defaultValue(method.getReturnType());
        });
        FrameInvoker.setBeanFunction(type -> {
            if (DefService.class.equals(type)) return defService;
            if (NodeService.class.equals(type)) return nodeService;
            return null;
        });
        FlowEngine.initPermissionHandler(null);
    }

    private List<Node> next(Node node) {
        if ("start".equals(node.getNodeCode())) return Collections.singletonList(first);
        if ("first".equals(node.getNodeCode())) return Collections.singletonList(second);
        if ("second".equals(node.getNodeCode())) return Collections.singletonList(end);
        return Collections.emptyList();
    }

    private static Node node(String code, NodeType type, String permissions) {
        return TestEntityFactory.create(Node.class).setDefinitionId(1L).setNodeCode(code)
            .setNodeName(code).setNodeType(type.getKey()).setPermissionFlag(permissions);
    }

    private static Skip skip(String source, String target, String skipType, String condition) {
        return TestEntityFactory.create(Skip.class).setDefinitionId(1L).setNowNodeCode(source)
            .setNextNodeCode(target).setSkipType(skipType).setSkipCondition(condition);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (proxy, method, args) -> invocation.invoke(method, args));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private interface Invocation {
        Object invoke(Method method, Object[] args);
    }

    private static final class VariableJsonConvert implements JsonConvert {
        @Override
        public Map<String, Object> strToMap(String jsonStr) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("amount", 10);
            variables.put("retained", "kept");
            return variables;
        }

        @Override
        public <T> T strToBean(String jsonStr, Class<T> clazz) {
            return null;
        }

        @Override
        public <T> List<T> strToList(String jsonStr) {
            return Collections.emptyList();
        }

        @Override
        public String objToStr(Object variable) {
            return String.valueOf(variable);
        }
    }
}
