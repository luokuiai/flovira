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
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.dto.NodeJson;
import com.luokuiai.flovira.core.dto.TimeoutExecutionResult;
import com.luokuiai.flovira.core.dto.WaitConfig;
import com.luokuiai.flovira.core.dto.WaitResumeResult;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.TimeoutAction;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.lock.TimeoutSchedulerLock;
import com.luokuiai.flovira.core.service.InsService;
import com.luokuiai.flovira.core.service.TaskService;
import com.luokuiai.flovira.core.service.WaitService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 等待恢复和超时执行测试
 *
 * @author warm
 */
public class WaitTimeoutServiceTest {

    @Before
    public void setUp() {
        FlowEngine.setTransactionExecutor(new DirectTransactionExecutor());
        FlowEngine.setTimeoutSchedulerLock(null);
        FlowEngine.jsonConvert = new TestJsonConvert();
    }

    @Test
    public void shouldResumeWaitOnceAndReturnIdempotentResultAfterCompletion() {
        WaitFixture fixture = new WaitFixture();
        WaitResumeResult first = fixture.service.resumeTask(10L, Collections.<String, Object>singletonMap("paid", true));
        WaitResumeResult second = fixture.service.resumeTask(10L, Collections.<String, Object>emptyMap());

        assertEquals("RESUMED", first.getStatus());
        assertEquals("order.paid", first.getWaitKey());
        assertEquals("NOT_FOUND_OR_ALREADY_RESUMED", second.getStatus());
        assertEquals(1, fixture.passes);
        assertEquals(WaitServiceImpl.SYSTEM_HANDLER, fixture.params.getHandler());
        assertNotNull(fixture.params.getHisTaskExt());
        org.junit.Assert.assertTrue(fixture.params.getHisTaskExt().contains("WAIT_RESUME"));
    }

    @Test
    public void shouldResolveWaitKeyFromInstanceSnapshot() {
        WaitFixture fixture = new WaitFixture();

        WaitResumeResult result = fixture.service.resume(100L, "order.paid",
            Collections.<String, Object>emptyMap());

        assertEquals("RESUMED", result.getStatus());
        assertEquals(1, fixture.snapshotReads);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectMissingInstanceSnapshot() {
        WaitFixture fixture = new WaitFixture();
        fixture.instance.setDefJson(null);

        fixture.service.resumeTask(10L, Collections.<String, Object>emptyMap());
    }

    @Test
    public void shouldAllowOnlyOneSignalOrTimeoutResume() throws Exception {
        final WaitFixture fixture = new WaitFixture();
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final WaitResumeResult[] results = new WaitResumeResult[2];
        Thread signal = new Thread(() -> {
            ready.countDown();
            await(start);
            results[0] = fixture.service.resumeTask(10L, Collections.<String, Object>emptyMap());
        });
        Thread timeout = new Thread(() -> {
            ready.countDown();
            await(start);
            results[1] = fixture.service.resumeTimeoutTask(10L);
        });
        signal.start();
        timeout.start();
        ready.await();
        start.countDown();
        signal.join();
        timeout.join();

        int resumed = ("RESUMED".equals(results[0].getStatus()) ? 1 : 0)
            + ("RESUMED".equals(results[1].getStatus()) ? 1 : 0);
        assertEquals(1, resumed);
        assertEquals(1, fixture.passes);
    }

    @Test
    public void shouldRouteWaitTimeoutThroughWaitClaimAndMarkHistory() {
        WaitFixture fixture = new WaitFixture();
        fixture.task.setTimeoutAction(TimeoutAction.RESUME_WAIT.name())
            .setTimeoutStatus("PENDING").setTimeoutAt(new Date(1L));
        Flovira flovira = new Flovira();
        flovira.getTimeout().setEnabled(true);
        FlowEngine.setFlowConfig(flovira);

        TimeoutExecutionResult result = new TimeoutServiceImpl().executeDue(new Date(), 10);

        assertEquals(1, result.getClaimed());
        assertEquals(1, result.getSucceeded());
        assertEquals(0, fixture.timeoutClaims);
        assertTrue(fixture.params.getHisTaskExt().contains("WAIT_TIMEOUT"));
    }

    @Test
    public void shouldCentralizeSystemTaskSkipFlags() {
        final FlowParams[] captured = new FlowParams[1];
        TaskServiceImpl service = new TaskServiceImpl() {
            @Override
            public Instance skip(FlowParams flowParams, Task task) {
                captured[0] = flowParams;
                return null;
            }
        };

        service.skipSystemTask(FlowParams.build(), TestEntityFactory.create(Task.class));

        assertTrue(captured[0].isIgnore());
        assertTrue(captured[0].isIgnoreDepute());
        assertTrue(captured[0].isIgnoreCooperate());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectNonWaitTask() {
        WaitFixture fixture = new WaitFixture();
        fixture.task.setNodeType(NodeType.BETWEEN.getKey());
        fixture.service.resumeTask(10L, Collections.<String, Object>emptyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectAmbiguousWaitKey() {
        WaitFixture fixture = new WaitFixture();
        fixture.duplicate = true;
        fixture.service.resume(100L, "order.paid", Collections.<String, Object>emptyMap());
    }

    @Test
    public void shouldClaimAndExecuteDueTaskOnlyOnce() {
        TimeoutFixture fixture = new TimeoutFixture(false);
        TimeoutExecutionResult first = fixture.service.executeDue(new Date(), 10);
        TimeoutExecutionResult second = fixture.service.executeDue(new Date(), 10);

        assertEquals(1, first.getClaimed());
        assertEquals(1, first.getSucceeded());
        assertEquals(0, second.getClaimed());
        assertEquals(1, fixture.passes);
        assertEquals(TimeoutServiceImpl.SYSTEM_HANDLER, fixture.params.getHandler());
    }

    @Test
    public void shouldReleaseClaimWhenTimeoutActionFails() {
        TimeoutFixture fixture = new TimeoutFixture(true);
        TimeoutExecutionResult result = fixture.service.executeDue(new Date(), 10);

        assertEquals(1, result.getFailed());
        assertEquals(1, fixture.releases);
    }

    @Test
    public void shouldSkipDatabaseScanWhenSchedulerLockIsHeld() {
        TimeoutFixture fixture = new TimeoutFixture(false);
        FlowEngine.setTimeoutSchedulerLock(new TimeoutSchedulerLock() {
            @Override
            public boolean tryLock(String key, String owner, long leaseMillis) {
                return false;
            }

            @Override
            public void unlock(String key, String owner) {
            }
        });

        TimeoutExecutionResult result = fixture.service.executeDue(new Date(), 10);

        assertEquals(0, result.getScanned());
        assertEquals(0, fixture.scans);
    }

    @Test
    public void shouldFallbackToDatabaseClaimsWhenSchedulerLockFails() {
        TimeoutFixture fixture = new TimeoutFixture(false);
        FlowEngine.setTimeoutSchedulerLock(new TimeoutSchedulerLock() {
            @Override
            public boolean tryLock(String key, String owner, long leaseMillis) {
                throw new IllegalStateException("redis unavailable");
            }

            @Override
            public void unlock(String key, String owner) {
            }
        });

        TimeoutExecutionResult result = fixture.service.executeDue(new Date(), 10);

        assertEquals(1, result.getSucceeded());
        assertEquals(1, fixture.scans);
    }

    @Test
    public void shouldReleaseSchedulerLockWithTheAcquiredOwner() {
        TimeoutFixture fixture = new TimeoutFixture(false);
        final String[] acquiredOwner = new String[1];
        final String[] releasedOwner = new String[1];
        FlowEngine.setTimeoutSchedulerLock(new TimeoutSchedulerLock() {
            @Override
            public boolean tryLock(String key, String owner, long leaseMillis) {
                acquiredOwner[0] = owner;
                return true;
            }

            @Override
            public void unlock(String key, String owner) {
                releasedOwner[0] = owner;
            }
        });

        TimeoutExecutionResult result = fixture.service.executeDue(new Date(), 10);

        assertEquals(1, result.getSucceeded());
        assertNotNull(acquiredOwner[0]);
        assertEquals(acquiredOwner[0], releasedOwner[0]);
    }

    private static final class WaitFixture {
        private final WaitServiceImpl service = new WaitServiceImpl();
        private final Instance instance = TestEntityFactory.create(Instance.class).setId(100L)
            .setDefJson("WAIT_PAYMENT=order.paid");
        private volatile Task task = TestEntityFactory.create(Task.class).setId(10L).setInstanceId(100L)
            .setDefinitionId(1L).setNodeType(NodeType.WAIT.getKey()).setNodeCode("WAIT_PAYMENT");
        private int passes;
        private final AtomicBoolean claimed = new AtomicBoolean();
        private boolean duplicate;
        private FlowParams params;
        private int snapshotReads;
        private int timeoutClaims;

        private WaitFixture() {
            final TaskService taskService = proxy(TaskService.class, (method, args) -> {
                if ("getById".equals(method.getName())) return task;
                if ("getByInsIdAndNodeType".equals(method.getName())) {
                    if (task == null) return Collections.emptyList();
                    return duplicate ? java.util.Arrays.asList(task, task) : Collections.singletonList(task);
                }
                if ("claimWait".equals(method.getName())) {
                    return claimed.compareAndSet(false, true);
                }
                if ("claimTimeout".equals(method.getName())) {
                    timeoutClaims++;
                    return true;
                }
                if ("listDueTimeoutTasks".equals(method.getName())) {
                    return task == null ? Collections.emptyList() : Collections.singletonList(task);
                }
                if ("skipSystemTask".equals(method.getName())) {
                    params = (FlowParams) args[0];
                    passes++;
                    task = null;
                }
                return defaultValue(method.getReturnType());
            });
            final InsService insService = proxy(InsService.class, (method, args) -> {
                if ("getById".equals(method.getName())) {
                    snapshotReads++;
                    return instance;
                }
                return defaultValue(method.getReturnType());
            });
            FrameInvoker.setBeanFunction(type -> {
                if (TaskService.class.equals(type)) return taskService;
                if (InsService.class.equals(type)) return insService;
                if (WaitService.class.equals(type)) return service;
                return null;
            });
        }
    }

    private static final class TimeoutFixture {
        private final TimeoutServiceImpl service = new TimeoutServiceImpl();
        private final Task task = TestEntityFactory.create(Task.class).setId(20L).setInstanceId(100L)
            .setDefinitionId(1L).setNodeType(NodeType.BETWEEN.getKey()).setNodeCode("APPROVE")
            .setTimeoutAction(TimeoutAction.AUTO_PASS.name()).setTimeoutStatus("PENDING")
            .setTimeoutAt(new Date(1L));
        private final boolean fail;
        private boolean claimed;
        private int passes;
        private int releases;
        private int scans;
        private FlowParams params;

        private TimeoutFixture(boolean fail) {
            this.fail = fail;
            Flovira flovira = new Flovira();
            flovira.getTimeout().setEnabled(true);
            FlowEngine.setFlowConfig(flovira);
            final TaskService taskService = proxy(TaskService.class, (method, args) -> {
                if ("listDueTimeoutTasks".equals(method.getName())) {
                    scans++;
                    return Collections.singletonList(task);
                }
                if ("claimTimeout".equals(method.getName())) {
                    if (claimed) return false;
                    claimed = true;
                    return true;
                }
                if ("releaseTimeout".equals(method.getName())) {
                    releases++;
                    return true;
                }
                if ("skipSystemTask".equals(method.getName())) {
                    if (fail) throw new IllegalStateException("transition failed");
                    params = (FlowParams) args[0];
                    passes++;
                }
                return defaultValue(method.getReturnType());
            });
            final WaitService waitService = proxy(WaitService.class,
                (method, args) -> defaultValue(method.getReturnType()));
            FrameInvoker.setBeanFunction(type -> {
                if (TaskService.class.equals(type)) return taskService;
                if (WaitService.class.equals(type)) return waitService;
                return null;
            });
        }
    }

    private interface Invocation {
        Object invoke(Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (Object proxy, Method method, Object[] args) -> invocation.invoke(method, args));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static final class DirectTransactionExecutor implements TransactionExecutor {
        @Override
        public <T> T execute(TransactionCallback<T> callback) {
            return callback.execute();
        }

        @Override
        public void afterCommit(Runnable callback) {
            callback.run();
        }
    }

    private static final class TestJsonConvert implements JsonConvert {
        @Override
        public Map<String, Object> strToMap(String jsonStr) {
            return Collections.emptyMap();
        }

        @Override
        public <T> T strToBean(String jsonStr, Class<T> clazz) {
            if (DefJson.class.equals(clazz)) {
                String[] parts = jsonStr.split("=", 2);
                NodeJson node = new NodeJson().setNodeType(NodeType.WAIT.getKey())
                    .setNodeCode(parts[0]).setExt(parts.length > 1 ? parts[1] : null);
                DefJson defJson = new DefJson().setNodeList(Collections.singletonList(node));
                return clazz.cast(defJson);
            }
            WaitConfig config = new WaitConfig();
            config.setWaitKey(jsonStr);
            return clazz.cast(config);
        }

        @Override
        public <T> List<T> strToList(String jsonStr) {
            Map<String, Object> ext = new HashMap<String, Object>();
            ext.put("code", "waitConfig");
            ext.put("value", jsonStr);
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            list.add(ext);
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) list;
            return result;
        }

        @Override
        public String objToStr(Object variable) {
            return String.valueOf(variable);
        }
    }
}
