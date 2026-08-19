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
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.NodeTimeoutConfig;
import com.luokuiai.flovira.core.dto.WaitConfig;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.TimeoutAction;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.NodeService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 等待节点和超时配置测试
 *
 * @author warm
 */
public class WaitTimeoutConfigTest {

    @Test
    public void shouldAppendWaitNodeTypeWithoutChangingExistingKeys() {
        assertEquals(Integer.valueOf(6), NodeType.SUB_PROCESS.getKey());
        assertEquals(Integer.valueOf(7), NodeType.WAIT.getKey());
        assertTrue(NodeType.isWorkNode(NodeType.WAIT.getKey()));
    }

    @Test
    public void shouldAcceptValidWaitKey() {
        WaitConfig config = new WaitConfig();
        config.setWaitKey("order.paid:retry_1");
        WaitConfigUtil.validate(config);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectInvalidWaitKey() {
        WaitConfig config = new WaitConfig();
        config.setWaitKey("1 invalid");
        WaitConfigUtil.validate(config);
    }

    @Test
    public void shouldAcceptCompatibleTimeoutActions() {
        TimeoutConfigUtil.validate(node(NodeType.BETWEEN), timeout(TimeoutAction.AUTO_PASS));
        TimeoutConfigUtil.validate(node(NodeType.BETWEEN), timeout(TimeoutAction.AUTO_REJECT));
        TimeoutConfigUtil.validate(node(NodeType.WAIT), timeout(TimeoutAction.RESUME_WAIT));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectWaitActionOnApproval() {
        TimeoutConfigUtil.validate(node(NodeType.BETWEEN), timeout(TimeoutAction.RESUME_WAIT));
    }

    @Test
    public void shouldFreezeSnapshotOnlyWhenBackendTimeoutIsEnabled() {
        final NodeTimeoutConfig config = timeout(TimeoutAction.AUTO_PASS);
        final Node node = node(NodeType.BETWEEN);
        NodeService nodeService = (NodeService) Proxy.newProxyInstance(NodeService.class.getClassLoader(),
            new Class<?>[]{NodeService.class}, (proxy, method, args) -> "getExt".equals(method.getName())
                ? Collections.singletonMap(TimeoutConfigUtil.EXT_CONFIG, "timeout") : null);
        FrameInvoker.setBeanFunction(type -> NodeService.class.equals(type) ? nodeService : null);
        FlowEngine.jsonConvert = new JsonConvert() {
            public Map<String, Object> strToMap(String jsonStr) { return Collections.emptyMap(); }
            public <T> T strToBean(String jsonStr, Class<T> clazz) { return clazz.cast(config); }
            public <T> List<T> strToList(String jsonStr) { return Collections.emptyList(); }
            public String objToStr(Object variable) { return "snapshot"; }
        };

        Flovira flovira = new Flovira();
        FlowEngine.setFlowConfig(flovira);
        Task disabledTask = TestEntityFactory.create(Task.class);
        TimeoutConfigUtil.applySnapshot(node, disabledTask, new Date(1000L));
        assertEquals(null, disabledTask.getTimeoutAt());

        flovira.getTimeout().setEnabled(true);
        Task enabledTask = TestEntityFactory.create(Task.class);
        TimeoutConfigUtil.applySnapshot(node, enabledTask, new Date(1000L));
        assertEquals(new Date(301000L), enabledTask.getTimeoutAt());
        assertEquals(TimeoutAction.AUTO_PASS.name(), enabledTask.getTimeoutAction());
        assertEquals(TimeoutConfigUtil.STATUS_PENDING, enabledTask.getTimeoutStatus());
    }

    private Node node(NodeType type) {
        return TestEntityFactory.create(Node.class).setNodeType(type.getKey()).setNodeCode(type.getValue());
    }

    private NodeTimeoutConfig timeout(TimeoutAction action) {
        NodeTimeoutConfig config = new NodeTimeoutConfig();
        config.setEnabled(true);
        config.setDuration(5L);
        config.setDurationUnit("MINUTES");
        config.setAction(action.name());
        return config;
    }
}
