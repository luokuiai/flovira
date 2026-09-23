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
import java.util.HashMap;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

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
        config.setWaitKey("ORDER_PAID_RETRY_1");
        WaitConfigUtil.validate(config);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectInvalidWaitKey() {
        WaitConfig config = new WaitConfig();
        config.setWaitKey("order.paid");
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

        FlowEngine.setTimeoutEnabled(false);
        Task disabledTask = TestEntityFactory.create(Task.class);
        TimeoutConfigUtil.applySnapshot(node, disabledTask, new Date(1000L));
        assertEquals(null, disabledTask.getTimeoutAt());

        FlowEngine.setTimeoutEnabled(true);
        Task enabledTask = TestEntityFactory.create(Task.class);
        TimeoutConfigUtil.applySnapshot(node, enabledTask, new Date(1000L));
        assertEquals(new Date(301000L), enabledTask.getTimeoutAt());
        assertEquals(TimeoutAction.AUTO_PASS.name(), enabledTask.getTimeoutAction());
        assertEquals(TimeoutConfigUtil.STATUS_PENDING, enabledTask.getTimeoutStatus());

        config.setSource("FORM_FIELD");
        config.setFieldCode("schedule.deadline");
        Map<String, Object> schedule = new HashMap<String, Object>();
        schedule.put("deadline", "2026-09-17T18:00:00+08:00");
        Map<String, Object> form = new HashMap<String, Object>();
        form.put("schedule", schedule);
        Map<String, Object> variables = Collections.<String, Object>singletonMap("formData", form);
        Task fieldTask = TestEntityFactory.create(Task.class);
        TimeoutConfigUtil.applySnapshot(node, fieldTask, new Date(1000L), variables);
        assertEquals(Date.from(Instant.parse("2026-09-17T10:00:00Z")), fieldTask.getTimeoutAt());
        schedule.put("deadline", "2026-09-18T10:00:00Z");
        Task laterTask = TestEntityFactory.create(Task.class);
        TimeoutConfigUtil.applySnapshot(node, laterTask, new Date(1000L), variables);
        assertEquals(Date.from(Instant.parse("2026-09-18T10:00:00Z")), laterTask.getTimeoutAt());
        assertEquals(Date.from(Instant.parse("2026-09-17T10:00:00Z")), fieldTask.getTimeoutAt());
        schedule.clear();
        assertThrows(IllegalStateException.class, () -> TimeoutConfigUtil.applySnapshot(node,
            TestEntityFactory.create(Task.class), new Date(), variables));
        FlowEngine.setTimeoutEnabled(false);
    }

    @Test
    public void shouldValidateFormFieldSourceWithoutRequiringDuration() {
        NodeTimeoutConfig config = timeout(TimeoutAction.AUTO_PASS);
        config.setSource("FORM_FIELD");
        config.setDuration(0);
        config.setDurationUnit(null);
        config.setFieldCode("schedule.deadline");
        TimeoutConfigUtil.validate(node(NodeType.BETWEEN), config);
        config.setFieldCode("rows[].deadline");
        assertThrows(IllegalStateException.class, () -> TimeoutConfigUtil.validate(node(NodeType.BETWEEN), config));
        config.setFieldCode("");
        assertThrows(IllegalStateException.class, () -> TimeoutConfigUtil.validate(node(NodeType.BETWEEN), config));
        config.setSource("UNKNOWN");
        assertThrows(IllegalStateException.class, () -> TimeoutConfigUtil.validate(node(NodeType.BETWEEN), config));
    }

    @Test
    public void shouldAcceptDateAndEpochAndLocalDatetimeButRejectInvalidValues() {
        NodeTimeoutConfig config = timeout(TimeoutAction.AUTO_PASS);
        config.setSource("FORM_FIELD");
        config.setFieldCode("deadline");
        Map<String, Object> form = new HashMap<String, Object>();
        Map<String, Object> variables = Collections.<String, Object>singletonMap("formData", form);
        Date date = new Date(123456L);
        form.put("deadline", date);
        assertEquals(date, TimeoutConfigUtil.formDeadline(config, variables));
        form.put("deadline", 123456L);
        assertEquals(date, TimeoutConfigUtil.formDeadline(config, variables));
        form.put("deadline", "2026-09-17 12:30:00");
        assertEquals(Date.from(LocalDateTime.of(2026, 9, 17, 12, 30).atZone(ZoneId.systemDefault()).toInstant()),
            TimeoutConfigUtil.formDeadline(config, variables));
        for (Object invalid : new Object[]{null, "", "2026-02-30T12:00:00", "not-a-date", true, 1.5D}) {
            form.put("deadline", invalid);
            assertThrows(IllegalStateException.class, () -> TimeoutConfigUtil.formDeadline(config, variables));
        }
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
