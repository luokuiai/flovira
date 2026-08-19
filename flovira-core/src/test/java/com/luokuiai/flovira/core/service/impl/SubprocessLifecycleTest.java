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
import com.luokuiai.flovira.core.dto.SubprocessConfig;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.SubprocessChild;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.SubprocessRun;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.ActivityStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SubprocessChildStatus;
import com.luokuiai.flovira.core.enums.SubprocessOutcome;
import com.luokuiai.flovira.core.enums.SubprocessRunStatus;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.core.service.DefService;
import com.luokuiai.flovira.core.service.InsService;
import com.luokuiai.flovira.core.service.NodeService;
import com.luokuiai.flovira.core.service.TaskService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import com.luokuiai.flovira.core.utils.SubprocessConfigUtil;
import com.luokuiai.flovira.core.utils.SubprocessPlanResolver;
import com.luokuiai.flovira.core.utils.page.Page;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

/**
 * 子流程生命周期测试
 *
 * @author warm
 */
public class SubprocessLifecycleTest {

    @Test
    public void shouldInitializeAtomicallyAndReuseRunOnRetry() {
        Fixture fixture = new Fixture();
        fixture.failStartAt = 2;
        try {
            fixture.service.initialize(fixture.task.getId());
        } catch (IllegalStateException expected) {
            assertEquals("child start failed", expected.getMessage());
        }
        assertEquals(0, fixture.runDao.runs.size());
        assertEquals(0, fixture.childDao.children.size());
        assertEquals(0, fixture.startedInstances.size());

        fixture.failStartAt = -1;
        SubprocessRun first = fixture.service.initialize(fixture.task.getId());
        SubprocessRun second = fixture.service.initialize(fixture.task.getId());
        assertSame(first, second);
        assertEquals(1, fixture.runDao.runs.size());
        assertEquals(2, fixture.childDao.children.size());
        assertEquals(2, fixture.startedInstances.size());
        assertEquals(SubprocessRunStatus.RUNNING.name(), first.getRunStatus());
    }

    @Test
    public void shouldReconcileOnlyMissingSafeWork() {
        Fixture fixture = new Fixture();
        SubprocessRun run = fixture.service.initialize(fixture.task.getId());
        SubprocessChild interrupted = fixture.childDao.children.get(0);
        interrupted.setChildInstanceId(null);
        interrupted.setChildStatus(SubprocessChildStatus.STARTING.name());
        run.setRunStatus(SubprocessRunStatus.INITIALIZING.name());

        fixture.service.reconcile();

        assertEquals(1, fixture.runDao.runs.size());
        assertEquals(2, fixture.childDao.children.size());
        assertEquals(3, fixture.startedInstances.size());
        assertEquals(SubprocessRunStatus.RUNNING.name(), run.getRunStatus());
    }

    @Test
    public void shouldAggregateAndResumeParentExactlyOnce() {
        Fixture fixture = new Fixture();
        SubprocessRun run = fixture.service.initialize(fixture.task.getId());
        SubprocessChild first = fixture.childDao.children.get(0);
        SubprocessChild second = fixture.childDao.children.get(1);

        fixture.service.notifyChildTerminal(first.getChildInstanceId(), SubprocessOutcome.SUCCEEDED);
        assertEquals(SubprocessRunStatus.RUNNING.name(), run.getRunStatus());
        assertEquals(0, fixture.parentPasses);

        fixture.service.notifyChildTerminal(second.getChildInstanceId(), SubprocessOutcome.SUCCEEDED);
        assertEquals(SubprocessRunStatus.COMPLETED.name(), run.getRunStatus());
        assertEquals(1, fixture.parentPasses);

        fixture.service.notifyChildTerminal(second.getChildInstanceId(), SubprocessOutcome.SUCCEEDED);
        assertEquals(1, fixture.parentPasses);
    }

    @Test
    public void shouldRetryFailedParentResumeWithoutRepeatingChildCompletion() {
        Fixture fixture = new Fixture();
        SubprocessRun run = fixture.service.initialize(fixture.task.getId());
        List<SubprocessChild> children = fixture.childDao.children;
        fixture.service.notifyChildTerminal(children.get(0).getChildInstanceId(), SubprocessOutcome.SUCCEEDED);
        fixture.failNextParentPass = true;
        try {
            fixture.service.notifyChildTerminal(children.get(1).getChildInstanceId(), SubprocessOutcome.SUCCEEDED);
        } catch (IllegalStateException expected) {
            assertEquals("parent pass failed", expected.getMessage());
        }
        assertEquals(SubprocessRunStatus.READY_TO_RESUME.name(), run.getRunStatus());
        assertEquals(Integer.valueOf(2), run.getCompletedCount());

        fixture.service.resumeReadyRun(run.getId());
        assertEquals(SubprocessRunStatus.COMPLETED.name(), run.getRunStatus());
        assertEquals(1, fixture.parentPasses);
    }

    @Test
    public void shouldCancelActiveChildrenInStableOrderAndRemainIdempotent() {
        Fixture fixture = new Fixture();
        SubprocessRun run = fixture.service.initialize(fixture.task.getId());
        SubprocessChild first = fixture.childDao.children.get(0);
        SubprocessChild second = fixture.childDao.children.get(1);
        first.setId(30L);
        second.setId(10L);

        fixture.service.cancelByTask(fixture.task.getId(), "withdrawn");
        assertEquals(Arrays.asList(second.getChildInstanceId(), first.getChildInstanceId()), fixture.terminatedInstances);
        assertEquals(SubprocessRunStatus.CANCELLED.name(), run.getRunStatus());

        fixture.service.cancelByTask(fixture.task.getId(), "withdrawn again");
        assertEquals(2, fixture.terminatedInstances.size());

        Task reentered = fixture.newParentTask(11L);
        fixture.task = reentered;
        SubprocessRun nextRun = fixture.service.initialize(reentered.getId());
        assertNotEquals(run.getId(), nextRun.getId());
    }

    @Test
    public void shouldCancelAllActiveChildrenForParentTerminationOrWithdrawal() {
        Fixture termination = new Fixture();
        SubprocessRun terminatedRun = termination.service.initialize(termination.task.getId());
        termination.service.cancelByParent(termination.parent.getId(), "terminated");
        assertEquals(SubprocessRunStatus.CANCELLED.name(), terminatedRun.getRunStatus());
        assertEquals(2, termination.terminatedInstances.size());

        Fixture withdrawal = new Fixture();
        SubprocessRun withdrawnRun = withdrawal.service.initialize(withdrawal.task.getId());
        withdrawal.service.cancelByParent(withdrawal.parent.getId(), "withdrawn");
        assertEquals(SubprocessRunStatus.CANCELLED.name(), withdrawnRun.getRunStatus());
        assertEquals(2, withdrawal.terminatedInstances.size());
    }

    @Test
    public void shouldRollbackPartialCancellationAndAllowRetry() {
        Fixture fixture = new Fixture();
        SubprocessRun run = fixture.service.initialize(fixture.task.getId());
        fixture.failTerminationAt = 2;
        try {
            fixture.service.cancelByTask(fixture.task.getId(), "withdrawn");
        } catch (IllegalStateException expected) {
            assertEquals("child termination failed", expected.getMessage());
        }
        assertEquals(SubprocessRunStatus.RUNNING.name(), run.getRunStatus());
        assertEquals(SubprocessChildStatus.RUNNING.name(), fixture.childDao.children.get(0).getChildStatus());
        assertEquals(SubprocessChildStatus.RUNNING.name(), fixture.childDao.children.get(1).getChildStatus());
        assertEquals(0, fixture.terminatedInstances.size());

        fixture.failTerminationAt = -1;
        fixture.service.cancelByTask(fixture.task.getId(), "withdrawn");
        assertEquals(SubprocessRunStatus.CANCELLED.name(), run.getRunStatus());
        assertEquals(2, fixture.terminatedInstances.size());
    }

    @Test
    public void shouldFailRunForFailedOrCancelledChild() {
        Fixture failed = new Fixture();
        SubprocessRun failedRun = failed.service.initialize(failed.task.getId());
        failed.service.notifyChildTerminal(failed.childDao.children.get(0).getChildInstanceId(),
            SubprocessOutcome.FAILED);
        assertEquals(SubprocessRunStatus.FAILED.name(), failedRun.getRunStatus());
        assertEquals("CHILD_FAILED", failedRun.getFailureCode());

        Fixture cancelled = new Fixture();
        SubprocessRun cancelledRun = cancelled.service.initialize(cancelled.task.getId());
        cancelled.service.notifyChildTerminal(cancelled.childDao.children.get(0).getChildInstanceId(),
            SubprocessOutcome.CANCELLED);
        assertEquals(SubprocessRunStatus.FAILED.name(), cancelledRun.getRunStatus());
        assertEquals("CHILD_CANCELLED", cancelledRun.getFailureCode());
    }

    private static final class Fixture {

        private final InMemoryRunDao runDao = new InMemoryRunDao();
        private final InMemoryChildDao childDao = new InMemoryChildDao();
        private final InMemoryEventDao eventDao = new InMemoryEventDao();
        private final List<Instance> startedInstances = new ArrayList<Instance>();
        private final List<Long> terminatedInstances = new ArrayList<Long>();
        private final Definition childDefinition;
        private final Node node;
        private final Instance parent;
        private final SubprocessServiceImpl service;
        private Task task;
        private int failStartAt = -1;
        private int failTerminationAt = -1;
        private int parentPasses;
        private boolean failNextParentPass;

        private Fixture() {
            task = newParentTask(10L);
            node = TestEntityFactory.create(Node.class).setNodeType(NodeType.SUB_PROCESS.getKey())
                .setDefinitionId(1L).setNodeCode("SUB").setExt("child");
            childDefinition = TestEntityFactory.create(Definition.class).setId(2L).setFlowCode("child")
                .setVersion("1").setTenantId("0").setActivityStatus(ActivityStatus.ACTIVITY.getKey());
            parent = TestEntityFactory.create(Instance.class).setId(100L).setTenantId("0").setCreateBy("starter");
            TestEntityFactory.put(parent, "VariableMap", variables());

            final TaskService taskService = service(TaskService.class);
            final NodeService nodeService = service(NodeService.class);
            final DefService defService = service(DefService.class);
            final InsService insService = service(InsService.class);
            FrameInvoker.setBeanFunction(type -> {
                if (TaskService.class.equals(type)) return taskService;
                if (NodeService.class.equals(type)) return nodeService;
                if (DefService.class.equals(type)) return defService;
                if (InsService.class.equals(type)) return insService;
                return null;
            });
            FlowEngine.setNewSubprocessRun(() -> TestEntityFactory.create(SubprocessRun.class));
            FlowEngine.setNewSubprocessChild(() -> TestEntityFactory.create(SubprocessChild.class));
            FlowEngine.setNewSubprocessEvent(() -> TestEntityFactory.create(SubprocessEvent.class));
            FlowEngine.setFlowConfig(new com.luokuiai.flovira.core.config.Flovira());
            FlowEngine.jsonConvert = new FixtureJsonConvert();
            FlowEngine.initDataFillHandler(null);
            FlowEngine.setTransactionExecutor(new SnapshotTransactionExecutor(this));
            service = (SubprocessServiceImpl) new SubprocessServiceImpl().setDao(runDao, childDao, eventDao);
        }

        private Task newParentTask(Long id) {
            return TestEntityFactory.create(Task.class).setId(id).setTenantId("0").setDefinitionId(1L)
                .setInstanceId(100L).setNodeCode("SUB").setNodeType(NodeType.SUB_PROCESS.getKey());
        }

        private Map<String, Object> variables() {
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            items.add(item("a"));
            items.add(item("b"));
            Map<String, Object> variables = new LinkedHashMap<String, Object>();
            variables.put(SubprocessPlanResolver.ITEMS_VARIABLE, items);
            return variables;
        }

        private Map<String, Object> item(String key) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("itemKey", key);
            item.put("variables", new LinkedHashMap<String, Object>());
            return item;
        }

        @SuppressWarnings("unchecked")
        private <T> T service(final Class<T> type) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (Object proxy, Method method, Object[] args) -> {
                    String name = method.getName();
                    if (type == TaskService.class && "getById".equals(name)) return task;
                    if (type == TaskService.class && "pass".equals(name)) {
                        if (failNextParentPass) {
                            failNextParentPass = false;
                            throw new IllegalStateException("parent pass failed");
                        }
                        parentPasses++;
                        return null;
                    }
                    if (type == TaskService.class && "terminationByInsId".equals(name)) {
                        if (terminatedInstances.size() + 1 == failTerminationAt) {
                            throw new IllegalStateException("child termination failed");
                        }
                        terminatedInstances.add((Long) args[0]);
                        return null;
                    }
                    if (type == NodeService.class && "getByDefIdAndNodeCode".equals(name)) return node;
                    if (type == NodeService.class && "getExt".equals(name)) {
                        return Collections.singletonMap(SubprocessConfigUtil.EXT_CONFIG, node.getExt());
                    }
                    if (type == DefService.class && "getPublishByFlowCode".equals(name)) return childDefinition;
                    if (type == InsService.class && "getById".equals(name)) return parent;
                    if (type == InsService.class && "startByDefinitionId".equals(name)) {
                        if (startedInstances.size() + 1 == failStartAt) {
                            throw new IllegalStateException("child start failed");
                        }
                        Instance child = TestEntityFactory.create(Instance.class)
                            .setId(200L + startedInstances.size()).setTenantId("0");
                        startedInstances.add(child);
                        return child;
                    }
                    return defaultValue(method.getReturnType());
                });
        }
    }

    private static final class SnapshotTransactionExecutor implements TransactionExecutor {

        private final Fixture fixture;

        private SnapshotTransactionExecutor(Fixture fixture) {
            this.fixture = fixture;
        }

        @Override
        public <T> T execute(TransactionCallback<T> callback) {
            List<SubprocessRun> runs = new ArrayList<SubprocessRun>(fixture.runDao.runs);
            List<SubprocessChild> children = new ArrayList<SubprocessChild>(fixture.childDao.children);
            int events = fixture.eventDao.events.size();
            int starts = fixture.startedInstances.size();
            int terminations = fixture.terminatedInstances.size();
            String runStatus = runs.isEmpty() ? null : runs.get(0).getRunStatus();
            List<String> childStatuses = new ArrayList<String>();
            List<String> childOutcomes = new ArrayList<String>();
            for (SubprocessChild child : children) {
                childStatuses.add(child.getChildStatus());
                childOutcomes.add(child.getOutcome());
            }
            try {
                return callback.execute();
            } catch (RuntimeException e) {
                fixture.runDao.runs.clear();
                fixture.runDao.runs.addAll(runs);
                fixture.childDao.children.clear();
                fixture.childDao.children.addAll(children);
                while (fixture.eventDao.events.size() > events) fixture.eventDao.events.remove(events);
                while (fixture.startedInstances.size() > starts) fixture.startedInstances.remove(starts);
                while (fixture.terminatedInstances.size() > terminations) {
                    fixture.terminatedInstances.remove(terminations);
                }
                if (!runs.isEmpty()) runs.get(0).setRunStatus(runStatus);
                for (int i = 0; i < children.size(); i++) {
                    children.get(i).setChildStatus(childStatuses.get(i));
                    children.get(i).setOutcome(childOutcomes.get(i));
                    if (childOutcomes.get(i) == null) children.get(i).setCompletedAt(null);
                }
                throw e;
            }
        }

        @Override
        public void afterCommit(Runnable callback) {
            callback.run();
        }
    }

    private static final class InMemoryRunDao implements FlowSubprocessRunDao<SubprocessRun> {

        private final List<SubprocessRun> runs = new ArrayList<SubprocessRun>();

        public int save(SubprocessRun entity) { runs.add(entity); return 1; }
        public int updateById(SubprocessRun entity) { return 1; }
        public SubprocessRun findByParentTask(String tenantId, Long parentTaskId) {
            for (SubprocessRun run : runs) if (parentTaskId.equals(run.getParentTaskId())) return run;
            return null;
        }
        public SubprocessRun findById(String tenantId, Long runId) { return byId(runId); }
        public SubprocessRun lockById(String tenantId, Long runId) { return byId(runId); }
        public int claimReadyToResume(String tenantId, Long runId) {
            SubprocessRun run = byId(runId);
            if (run == null || !SubprocessRunStatus.READY_TO_RESUME.name().equals(run.getRunStatus())) return 0;
            run.setRunStatus(SubprocessRunStatus.RESUMING.name());
            return 1;
        }
        public List<SubprocessRun> lockActiveByParent(String tenantId, Long parentInstanceId) {
            List<SubprocessRun> result = new ArrayList<SubprocessRun>();
            for (SubprocessRun run : runs) {
                if (parentInstanceId.equals(run.getParentInstanceId())
                    && !SubprocessRunStatus.COMPLETED.name().equals(run.getRunStatus())
                    && !SubprocessRunStatus.CANCELLED.name().equals(run.getRunStatus())) result.add(run);
            }
            return result;
        }
        public List<SubprocessRun> findReconcileCandidates(int limit) {
            return new ArrayList<SubprocessRun>(runs);
        }
        private SubprocessRun byId(Long id) {
            for (SubprocessRun run : runs) if (id.equals(run.getId())) return run;
            return null;
        }
    }

    private static final class InMemoryChildDao implements FlowSubprocessChildDao<SubprocessChild> {

        private final List<SubprocessChild> children = new ArrayList<SubprocessChild>();

        public int save(SubprocessChild entity) { children.add(entity); return 1; }
        public int updateById(SubprocessChild entity) { return 1; }
        public SubprocessChild findByChildInstanceId(String tenantId, Long childInstanceId) {
            for (SubprocessChild child : children) {
                if (childInstanceId.equals(child.getChildInstanceId())) return child;
            }
            return null;
        }
        public SubprocessChild findById(String tenantId, Long childId) {
            for (SubprocessChild child : children) if (childId.equals(child.getId())) return child;
            return null;
        }
        public SubprocessChild findByRunAndItem(String tenantId, Long runId, String itemKey) {
            for (SubprocessChild child : children) {
                if (runId.equals(child.getRunId()) && itemKey.equals(child.getItemKey())) return child;
            }
            return null;
        }
        public List<SubprocessChild> lockByRunId(String tenantId, Long runId) {
            List<SubprocessChild> result = new ArrayList<SubprocessChild>();
            for (SubprocessChild child : children) if (runId.equals(child.getRunId())) result.add(child);
            return result;
        }
        public Page<SubprocessChild> pageByRunId(String tenantId, Long runId, Page<SubprocessChild> page) {
            return new Page<SubprocessChild>(lockByRunId(tenantId, runId), children.size());
        }
    }

    private static final class InMemoryEventDao implements FlowSubprocessEventDao<SubprocessEvent> {

        private final List<SubprocessEvent> events = new ArrayList<SubprocessEvent>();

        public int save(SubprocessEvent entity) { events.add(entity); return 1; }
        public List<SubprocessEvent> listByRunId(String tenantId, Long runId) {
            List<SubprocessEvent> result = new ArrayList<SubprocessEvent>();
            for (SubprocessEvent event : events) if (runId.equals(event.getRunId())) result.add(event);
            return result;
        }
    }

    private static final class FixtureJsonConvert implements JsonConvert {
        public Map<String, Object> strToMap(String jsonStr) { return Collections.emptyMap(); }
        public <T> T strToBean(String jsonStr, Class<T> clazz) {
            SubprocessConfig config = new SubprocessConfig();
            config.setFixedChildFlowCode(jsonStr);
            return clazz.cast(config);
        }
        public <T> List<T> strToList(String jsonStr) { return Collections.emptyList(); }
        public String objToStr(Object variable) { return String.valueOf(variable); }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
