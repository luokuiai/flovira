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
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.HisTask;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.orm.dao.FlowHisTaskDao;
import com.luokuiai.flovira.core.orm.dao.FlowInstanceDao;
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
import com.luokuiai.flovira.core.service.InsService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * 业务关联查询测试
 *
 * @author warm
 */
public class BusinessCorrelationServiceTest {

    @Before
    public void setUp() {
        FrameInvoker.setBeanFunction(type -> null);
        FlowEngine.setFlowConfig(new Flovira());
        FlowEngine.setNewIns(() -> TestEntityFactory.create(Instance.class));
        FlowEngine.initDataFillHandler(null);
        FlowEngine.jsonConvert = new EmptyJsonConvert();
    }

    @Test
    public void shouldUseFlowCodeAsDefaultBusinessType() {
        Definition definition = TestEntityFactory.create(Definition.class).setFlowCode("PURCHASE");

        assertEquals("PURCHASE", InsServiceImpl.defaultBusinessType(definition));
    }

    @Test
    public void shouldPersistExplicitBusinessType() throws Exception {
        Node node = TestEntityFactory.create(Node.class).setDefinitionId(1L).setNodeType(1)
            .setNodeCode("APPROVE").setNodeName("审批");
        Method method = InsServiceImpl.class.getDeclaredMethod("setStartInstance", Node.class,
            String.class, String.class, FlowParams.class);
        method.setAccessible(true);

        Instance instance = (Instance) method.invoke(new InsServiceImpl(), node, "PURCHASE_ORDER", "1001",
            new FlowParams().handler("starter"));

        assertEquals("PURCHASE_ORDER", instance.getBusinessType());
        assertEquals("1001", instance.getBusinessId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryInstancesWithStructuredBusinessKey() {
        final Instance[] criteria = new Instance[1];
        final Instance expected = TestEntityFactory.create(Instance.class).setId(11L);
        FlowInstanceDao<Instance> dao = proxy(FlowInstanceDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                criteria[0] = (Instance) args[0];
                return Collections.singletonList(expected);
            }
            return defaultValue(method.getReturnType());
        });

        List<Instance> result = new InsServiceImpl().setDao(dao).listByBusinessKey("PURCHASE_ORDER", "1001");

        assertSame(expected, result.get(0));
        assertEquals("PURCHASE_ORDER", criteria[0].getBusinessType());
        assertEquals("1001", criteria[0].getBusinessId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBatchQueryCurrentAndHistoricalTasksByInstanceIds() {
        final List<Long>[] taskIds = new List[1];
        final List<Long>[] historyIds = new List[1];
        final Task task = TestEntityFactory.create(Task.class).setId(21L);
        final HisTask hisTask = TestEntityFactory.create(HisTask.class).setId(31L);
        final List<Instance> instances = Arrays.asList(
            TestEntityFactory.create(Instance.class).setId(11L),
            TestEntityFactory.create(Instance.class).setId(12L));
        InsService insService = proxy(InsService.class, (method, args) ->
            "listByBusinessKey".equals(method.getName()) ? instances : defaultValue(method.getReturnType()));
        FrameInvoker.setBeanFunction(type -> InsService.class.equals(type) ? insService : null);
        FlowTaskDao<Task> taskDao = proxy(FlowTaskDao.class, (method, args) -> {
            if ("listByInsIds".equals(method.getName())) {
                taskIds[0] = (List<Long>) args[0];
                return Collections.singletonList(task);
            }
            return defaultValue(method.getReturnType());
        });
        FlowHisTaskDao<HisTask> historyDao = proxy(FlowHisTaskDao.class, (method, args) -> {
            if ("listByInsIds".equals(method.getName())) {
                historyIds[0] = (List<Long>) args[0];
                return Collections.singletonList(hisTask);
            }
            return defaultValue(method.getReturnType());
        });

        List<Task> tasks = new TaskServiceImpl().setDao(taskDao)
            .listByBusinessKey("PURCHASE_ORDER", "1001");
        List<HisTask> history = new HisTaskServiceImpl().setDao(historyDao)
            .listByBusinessKey("PURCHASE_ORDER", "1001");

        assertSame(task, tasks.get(0));
        assertSame(hisTask, history.get(0));
        assertEquals(Arrays.asList(11L, 12L), taskIds[0]);
        assertEquals(Arrays.asList(11L, 12L), historyIds[0]);
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

    private static final class EmptyJsonConvert implements JsonConvert {
        public Map<String, Object> strToMap(String jsonStr) { return Collections.emptyMap(); }
        public <T> T strToBean(String jsonStr, Class<T> clazz) { return null; }
        public <T> List<T> strToList(String jsonStr) { return Collections.emptyList(); }
        public String objToStr(Object variable) { return null; }
    }
}
