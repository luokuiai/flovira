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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.SubprocessConfig;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.ActivityStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.DefService;
import com.luokuiai.flovira.core.service.NodeService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * 子流程定义发布校验测试
 *
 * @author warm
 */
public class SubprocessDefinitionValidatorTest {

    private final Map<Long, List<Node>> nodes = new HashMap<Long, List<Node>>();
    private final Map<String, Definition> definitions = new HashMap<String, Definition>();

    @Before
    public void setUp() {
        final NodeService nodeService = service(NodeService.class);
        final DefService defService = service(DefService.class);
        FrameInvoker.setBeanFunction(type -> {
            if (NodeService.class.equals(type)) return nodeService;
            if (DefService.class.equals(type)) return defService;
            return null;
        });
        FlowEngine.jsonConvert = new ConfigJsonConvert();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectMissingChildDefinition() {
        Definition parent = definition(1L, "parent", "0", ActivityStatus.ACTIVITY.getKey());
        nodes.put(1L, Collections.singletonList(subprocessNode("missing")));
        SubprocessDefinitionValidator.validateForPublish(parent);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectSuspendedChildDefinition() {
        Definition parent = definition(1L, "parent", "0", ActivityStatus.ACTIVITY.getKey());
        Definition child = definition(2L, "child", "0", ActivityStatus.SUSPENDED.getKey());
        definitions.put("child", child);
        nodes.put(1L, Collections.singletonList(subprocessNode("child")));
        SubprocessDefinitionValidator.validateForPublish(parent);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectDirectCycle() {
        Definition parent = definition(1L, "parent", "0", ActivityStatus.ACTIVITY.getKey());
        definitions.put("parent", parent);
        nodes.put(1L, Collections.singletonList(subprocessNode("parent")));
        SubprocessDefinitionValidator.validateForPublish(parent);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectIndirectCycle() {
        Definition parent = definition(1L, "parent", "0", ActivityStatus.ACTIVITY.getKey());
        Definition child = definition(2L, "child", "0", ActivityStatus.ACTIVITY.getKey());
        definitions.put("parent", parent);
        definitions.put("child", child);
        nodes.put(1L, Collections.singletonList(subprocessNode("child")));
        nodes.put(2L, Collections.singletonList(subprocessNode("parent")));
        SubprocessDefinitionValidator.validateForPublish(parent);
    }

    @Test
    public void shouldValidateAcyclicSameTenantGraphWithoutMutation() {
        Definition parent = definition(1L, "parent", "tenant", ActivityStatus.ACTIVITY.getKey());
        Definition child = definition(2L, "child", "tenant", ActivityStatus.ACTIVITY.getKey());
        definitions.put("child", child);
        nodes.put(1L, Collections.singletonList(subprocessNode("child")));
        nodes.put(2L, new ArrayList<Node>());
        SubprocessDefinitionValidator.validateForPublish(parent);
        assertEquals("parent", parent.getFlowCode());
        assertEquals(ActivityStatus.ACTIVITY.getKey(), parent.getActivityStatus());
    }

    private Definition definition(Long id, String code, String tenantId, Integer activityStatus) {
        return TestEntityFactory.create(Definition.class).setId(id).setFlowCode(code)
            .setTenantId(tenantId).setActivityStatus(activityStatus);
    }

    private Node subprocessNode(String childCode) {
        return TestEntityFactory.create(Node.class).setNodeType(NodeType.SUB_PROCESS.getKey()).setExt(childCode);
    }

    @SuppressWarnings("unchecked")
    private <T> T service(final Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (Object proxy, Method method, Object[] args) -> {
                if (type == NodeService.class && "getByDefId".equals(method.getName())) {
                    List<Node> result = nodes.get(args[0]);
                    return result == null ? Collections.emptyList() : result;
                }
                if (type == NodeService.class && "getExt".equals(method.getName())) {
                    return Collections.singletonMap(SubprocessConfigUtil.EXT_CONFIG, ((Node) args[0]).getExt());
                }
                if (type == DefService.class && "getPublishByFlowCode".equals(method.getName())) {
                    return definitions.get(args[0]);
                }
                return null;
            });
    }

    private static final class ConfigJsonConvert implements JsonConvert {
        public Map<String, Object> strToMap(String jsonStr) { throw new UnsupportedOperationException(); }
        public <T> T strToBean(String jsonStr, Class<T> clazz) {
            SubprocessConfig config = new SubprocessConfig();
            config.setFixedChildFlowCode(jsonStr);
            return clazz.cast(config);
        }
        public <T> List<T> strToList(String jsonStr) { throw new UnsupportedOperationException(); }
        public String objToStr(Object variable) { throw new UnsupportedOperationException(); }
    }
}
