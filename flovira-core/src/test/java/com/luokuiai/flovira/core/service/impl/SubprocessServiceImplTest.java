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

import com.luokuiai.flovira.core.dto.SubprocessSummary;
import com.luokuiai.flovira.core.dto.SubprocessHistoryEntry;
import com.luokuiai.flovira.core.entity.HisTask;
import com.luokuiai.flovira.core.entity.SubprocessChild;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.SubprocessRun;
import com.luokuiai.flovira.core.enums.SubprocessChildStatus;
import com.luokuiai.flovira.core.enums.SubprocessRunStatus;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.core.service.HisTaskService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 子流程运行聚合测试
 *
 * @author warm
 */
public class SubprocessServiceImplTest {

    @Test
    public void shouldReturnSummaryWithoutLoadingChildrenOrHistory() {
        final SubprocessRun run = run(5);
        run.setId(9L);
        run.setRunStatus(SubprocessRunStatus.RUNNING.name());
        run.setPendingCount(1).setRunningCount(2).setCompletedCount(2).setFailedCount(0).setCancelledCount(0);
        FlowSubprocessRunDao<SubprocessRun> runDao = proxy(FlowSubprocessRunDao.class,
            (method, args) -> "findByParentTask".equals(method.getName()) ? run : defaultValue(method));
        FlowSubprocessChildDao<SubprocessChild> childDao = proxy(FlowSubprocessChildDao.class,
            (method, args) -> { throw new AssertionError("summary must not load child rows"); });
        FlowSubprocessEventDao<SubprocessEvent> eventDao = proxy(FlowSubprocessEventDao.class,
            (method, args) -> { throw new AssertionError("summary must not load event rows"); });

        SubprocessSummary summary = new SubprocessServiceImpl().setDao(runDao, childDao, eventDao)
            .getSummary("tenant", 7L);

        assertEquals(Long.valueOf(9L), summary.getRunId());
        assertEquals(5, summary.getTotal());
        assertEquals(1, summary.getPending());
        assertEquals(2, summary.getRunning());
        assertEquals(2, summary.getCompleted());
    }

    @Test
    public void shouldKeepParentWaitingWhileAChildIsActive() throws Exception {
        SubprocessRun run = run(2);
        recompute(run, Arrays.asList(child(SubprocessChildStatus.COMPLETED), child(SubprocessChildStatus.RUNNING)));
        assertEquals(SubprocessRunStatus.RUNNING.name(), run.getRunStatus());
        assertEquals(Integer.valueOf(1), run.getCompletedCount());
        assertEquals(Integer.valueOf(1), run.getRunningCount());
    }

    @Test
    public void shouldBecomeReadyWhenEveryChildCompleted() throws Exception {
        SubprocessRun run = run(2);
        recompute(run, Arrays.asList(child(SubprocessChildStatus.COMPLETED), child(SubprocessChildStatus.COMPLETED)));
        assertEquals(SubprocessRunStatus.READY_TO_RESUME.name(), run.getRunStatus());
        assertEquals(Integer.valueOf(2), run.getCompletedCount());
    }

    @Test
    public void shouldFailAggregateForFailedOrCancelledChild() throws Exception {
        SubprocessRun failed = run(2);
        recompute(failed, Arrays.asList(child(SubprocessChildStatus.COMPLETED), child(SubprocessChildStatus.FAILED)));
        assertEquals(SubprocessRunStatus.FAILED.name(), failed.getRunStatus());
        assertEquals("CHILD_FAILED", failed.getFailureCode());

        SubprocessRun cancelled = run(1);
        recompute(cancelled, Arrays.asList(child(SubprocessChildStatus.CANCELLED)));
        assertEquals(SubprocessRunStatus.FAILED.name(), cancelled.getRunStatus());
        assertEquals("CHILD_CANCELLED", cancelled.getFailureCode());
    }

    @Test
    public void shouldExposeCompletedChildHistoryWhileSiblingIsActive() {
        final SubprocessRun run = run(2);
        run.setId(9L);
        run.setTenantId("tenant");
        run.setParentInstanceId(100L);
        run.setRunStatus(SubprocessRunStatus.RUNNING.name());
        final SubprocessChild completed = child(SubprocessChildStatus.COMPLETED);
        completed.setId(21L);
        completed.setRunId(9L);
        completed.setChildInstanceId(201L);
        completed.setItemLabel("completed child");
        final HisTask childHistory = TestEntityFactory.create(HisTask.class);
        childHistory.setInstanceId(201L);
        childHistory.setNodeCode("APPROVE");
        childHistory.setNodeName("Approve");
        childHistory.setCreateTime(new Date(2L));
        final HisTaskService historyService = proxy(HisTaskService.class,
            (method, args) -> "getByInsId".equals(method.getName()) && Long.valueOf(201L).equals(args[0])
                ? Collections.singletonList(childHistory) : Collections.emptyList());
        FrameInvoker.setBeanFunction(type -> HisTaskService.class.equals(type) ? historyService : null);

        FlowSubprocessRunDao<SubprocessRun> runDao = proxy(FlowSubprocessRunDao.class,
            (method, args) -> "findById".equals(method.getName()) ? run : defaultValue(method));
        FlowSubprocessChildDao<SubprocessChild> childDao = proxy(FlowSubprocessChildDao.class,
            (method, args) -> "findById".equals(method.getName()) ? completed : defaultValue(method));
        FlowSubprocessEventDao<SubprocessEvent> eventDao = proxy(FlowSubprocessEventDao.class,
            (method, args) -> "listByRunId".equals(method.getName())
                ? Collections.emptyList() : defaultValue(method));

        List<SubprocessHistoryEntry> history = new SubprocessServiceImpl().setDao(runDao, childDao, eventDao)
            .listCombinedHistory("tenant", 9L, 21L);

        assertEquals(1, history.size());
        assertEquals("CHILD", history.get(0).getSource());
        assertEquals("completed child", history.get(0).getItemLabel());
        assertEquals(Long.valueOf(201L), history.get(0).getInstanceId());
        assertEquals(SubprocessRunStatus.RUNNING.name(), run.getRunStatus());
    }

    private SubprocessRun run(int expected) {
        return TestEntityFactory.create(SubprocessRun.class).setExpectedCount(expected);
    }

    private SubprocessChild child(SubprocessChildStatus status) {
        return TestEntityFactory.create(SubprocessChild.class).setChildStatus(status.name());
    }

    private void recompute(SubprocessRun run, List<SubprocessChild> children) throws Exception {
        Method method = SubprocessServiceImpl.class.getDeclaredMethod("recompute", SubprocessRun.class, List.class);
        method.setAccessible(true);
        method.invoke(new SubprocessServiceImpl(), run, children);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (proxy, method, args) -> invocation.invoke(method, args));
    }

    private Object defaultValue(Method method) {
        if (method.getReturnType() == int.class) return 0;
        return null;
    }

    private interface Invocation {
        Object invoke(Method method, Object[] args);
    }
}
