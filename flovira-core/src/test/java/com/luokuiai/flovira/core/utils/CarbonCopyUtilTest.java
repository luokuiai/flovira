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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.TaskService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 抄送节点自动流转测试。
 *
 * @author warm
 */
public class CarbonCopyUtilTest {

    @Test
    public void shouldAdvanceOnlyCarbonCopyTasksAndRecordRecipients() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<FlowParams> capturedParams = new AtomicReference<FlowParams>();
        final AtomicReference<Task> capturedTask = new AtomicReference<Task>();
        TaskService taskService = proxy(TaskService.class, new Invocation() {
            @Override
            public Object invoke(Method method, Object[] args) {
                if ("skipSystemTask".equals(method.getName())) {
                    calls.incrementAndGet();
                    capturedParams.set((FlowParams) args[0]);
                    capturedTask.set((Task) args[1]);
                }
                return defaultValue(method.getReturnType());
            }
        });
        FrameInvoker.setBeanFunction(type -> TaskService.class.equals(type) ? taskService : null);
        FlowEngine.jsonConvert = new StringJsonConvert();
        Task carbonCopy = TestEntityFactory.create(Task.class).setNodeType(NodeType.CARBON_COPY.getKey())
            .setPermissionList(Arrays.asList("userA", "userB"));
        Task approval = TestEntityFactory.create(Task.class).setNodeType(NodeType.BETWEEN.getKey())
            .setPermissionList(Collections.singletonList("approver"));

        CarbonCopyUtil.advanceTasks(Arrays.asList(carbonCopy, approval), Collections.<String, Object>emptyMap());

        assertEquals(1, calls.get());
        assertSame(carbonCopy, capturedTask.get());
        assertEquals(CarbonCopyUtil.SYSTEM_HANDLER, capturedParams.get().getHandler());
        assertEquals(FlowStatus.AUTO_PASS.getKey(), capturedParams.get().getFlowStatus());
        assertTrue(capturedParams.get().getHisTaskExt().contains("userA"));
        assertTrue(capturedParams.get().getHisTaskExt().contains("userB"));
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

    private static final class StringJsonConvert implements JsonConvert {
        @Override
        public Map<String, Object> strToMap(String jsonStr) {
            return Collections.emptyMap();
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
