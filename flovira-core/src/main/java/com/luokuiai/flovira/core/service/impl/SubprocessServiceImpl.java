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
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.dto.SubprocessChildPlan;
import com.luokuiai.flovira.core.dto.SubprocessConfig;
import com.luokuiai.flovira.core.dto.SubprocessPlan;
import com.luokuiai.flovira.core.dto.SubprocessSummary;
import com.luokuiai.flovira.core.dto.SubprocessChildSummary;
import com.luokuiai.flovira.core.dto.SubprocessHistoryEntry;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.HisTask;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.SubprocessChild;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.SubprocessRun;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.ActivityStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.enums.SubprocessChildStatus;
import com.luokuiai.flovira.core.enums.SubprocessOutcome;
import com.luokuiai.flovira.core.enums.SubprocessRunStatus;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.core.service.SubprocessService;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.utils.CollUtil;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.core.utils.SubprocessPlanResolver;
import com.luokuiai.flovira.core.utils.SubprocessConfigUtil;
import com.luokuiai.flovira.core.utils.page.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 子流程运行服务实现
 *
 * @author warm
 */
public class SubprocessServiceImpl implements SubprocessService {

    private static final ThreadLocal<Long> RESUMING_TASK = new ThreadLocal<>();

    private FlowSubprocessRunDao<SubprocessRun> runDao;
    private FlowSubprocessChildDao<SubprocessChild> childDao;
    private FlowSubprocessEventDao<SubprocessEvent> eventDao;

    @Override
    public SubprocessService setDao(FlowSubprocessRunDao<SubprocessRun> runDao,
        FlowSubprocessChildDao<SubprocessChild> childDao, FlowSubprocessEventDao<SubprocessEvent> eventDao) {
        this.runDao = runDao;
        this.childDao = childDao;
        this.eventDao = eventDao;
        return this;
    }

    @Override
    public SubprocessRun initialize(final Long parentTaskId) {
        return FlowEngine.transactionExecutor().execute(new TransactionCallback<SubprocessRun>() {
            @Override
            public SubprocessRun execute() {
                return doInitialize(parentTaskId);
            }
        });
    }

    private SubprocessRun doInitialize(Long parentTaskId) {
        Task task = FlowEngine.taskService().getById(parentTaskId);
        if (task == null || !NodeType.isSubProcess(task.getNodeType())) {
            return null;
        }
        String tenantId = tenant(task.getTenantId());
        SubprocessRun existing = runDao.findByParentTask(tenantId, parentTaskId);
        Node node = FlowEngine.nodeService().getByDefIdAndNodeCode(task.getDefinitionId(), task.getNodeCode());
        SubprocessConfig config = SubprocessConfigUtil.read(node);
        Definition childDefinition = FlowEngine.defService().getPublishByFlowCode(config.getFixedChildFlowCode());
        if (childDefinition == null || !ActivityStatus.isActivity(childDefinition.getActivityStatus())) {
            throw new IllegalStateException("Runnable child definition not found: " + config.getFixedChildFlowCode());
        }
        if (!Objects.equals(tenantId, tenant(childDefinition.getTenantId()))) {
            throw new IllegalStateException("Subprocess child definition belongs to another tenant");
        }
        Instance parent = FlowEngine.insService().getById(task.getInstanceId());
        if (parent == null) {
            throw new IllegalStateException("Parent instance not found: " + task.getInstanceId());
        }
        SubprocessPlan plan = SubprocessPlanResolver.resolve(parent.getId(), task.getId(), task.getNodeCode(),
            childDefinition.getId(), childDefinition.getVersion(), parent.getVariableMap(), config.isAllowEmpty());
        if (existing != null && !Objects.equals(existing.getCollectionFingerprint(), plan.getFingerprint())) {
            throw new IllegalStateException("subprocessItems changed after initialization");
        }
        final SubprocessRun run = existing == null ? createRun(task, childDefinition, config, plan) : existing;
        for (SubprocessChildPlan childPlan : plan.getChildren()) {
            startChild(run, parent, childDefinition, childPlan);
        }
        List<SubprocessChild> children = childDao.lockByRunId(run.getTenantId(), run.getId());
        recompute(run, children);
        run.setInitializedAt(new Date());
        runDao.updateById(run);
        appendEvent(run, null, "INITIALIZATION_COMPLETED", "SUCCEEDED", null);
        if (SubprocessRunStatus.READY_TO_RESUME.name().equals(run.getRunStatus())) {
            FlowEngine.transactionExecutor().afterCommit(new Runnable() {
                @Override
                public void run() {
                    resumeReadyRun(run.getId());
                }
            });
        }
        return run;
    }

    private SubprocessRun createRun(Task task, Definition childDefinition, SubprocessConfig config,
        SubprocessPlan plan) {
        SubprocessRun run = FlowEngine.newSubprocessRun();
        FlowEngine.dataFillHandler().idFill(run);
        FlowEngine.dataFillHandler().insertFill(run);
        run.setTenantId(tenant(task.getTenantId()));
        run.setDelFlag("0");
        run.setParentInstanceId(task.getInstanceId());
        run.setParentTaskId(task.getId());
        run.setParentDefinitionId(task.getDefinitionId());
        run.setParentNodeCode(task.getNodeCode());
        run.setChildFlowCode(childDefinition.getFlowCode());
        run.setChildDefinitionId(childDefinition.getId());
        run.setChildDefinitionVersion(childDefinition.getVersion());
        run.setCompletionPolicy(config.getCompletionPolicy());
        run.setCollectionFingerprint(plan.getFingerprint());
        run.setExpectedCount(plan.getChildren().size());
        run.setPendingCount(plan.getChildren().size());
        run.setRunningCount(0);
        run.setCompletedCount(0);
        run.setFailedCount(0);
        run.setCancelledCount(0);
        run.setRunStatus(SubprocessRunStatus.INITIALIZING.name());
        run.setLockVersion(0);
        runDao.save(run);
        appendEvent(run, null, "INITIALIZATION_STARTED", "STARTED", null);
        return run;
    }

    private void startChild(SubprocessRun run, Instance parent, Definition childDefinition,
        SubprocessChildPlan plan) {
        SubprocessChild child = childDao.findByRunAndItem(run.getTenantId(), run.getId(), plan.getItemKey());
        if (child == null) {
            child = FlowEngine.newSubprocessChild();
            FlowEngine.dataFillHandler().idFill(child);
            FlowEngine.dataFillHandler().insertFill(child);
            child.setTenantId(run.getTenantId());
            child.setDelFlag("0");
            child.setRunId(run.getId());
            child.setItemKey(plan.getItemKey());
            child.setItemLabel(plan.getItemLabel());
            child.setChildBusinessKey(plan.getBusinessKey());
            child.setChildFlowCode(childDefinition.getFlowCode());
            child.setChildDefinitionId(childDefinition.getId());
            child.setChildDefinitionVersion(childDefinition.getVersion());
            child.setChildStatus(SubprocessChildStatus.STARTING.name());
            childDao.save(child);
        }
        if (child.getChildInstanceId() != null) {
            return;
        }
        FlowParams params = FlowParams.build().flowCode(childDefinition.getFlowCode())
            .handler(parent.getCreateBy()).variable(plan.getVariables());
        Instance childInstance = FlowEngine.insService().startByDefinitionId(
            plan.getBusinessKey(), childDefinition.getId(), params);
        child.setChildInstanceId(childInstance.getId());
        child.setChildStatus(SubprocessChildStatus.RUNNING.name());
        child.setStartedAt(new Date());
        childDao.updateById(child);
        appendEvent(run, child, "CHILD_STARTED", "SUCCEEDED", null);
    }

    @Override
    public void notifyChildTerminal(final Long childInstanceId, final SubprocessOutcome outcome) {
        final Long[] readyRun = new Long[1];
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                SubprocessChild located = childDao.findByChildInstanceId(currentTenantId(), childInstanceId);
                if (located == null) {
                    return null;
                }
                SubprocessRun run = runDao.lockById(located.getTenantId(), located.getRunId());
                if (run == null || isClosed(run)) {
                    return null;
                }
                List<SubprocessChild> children = childDao.lockByRunId(run.getTenantId(), run.getId());
                SubprocessChild child = findChild(children, childInstanceId);
                if (child == null || isTerminal(child)) {
                    return null;
                }
                applyOutcome(child, outcome);
                childDao.updateById(child);
                recompute(run, children);
                runDao.updateById(run);
                appendEvent(run, child, "CHILD_TERMINAL", outcome.name(), null);
                if (SubprocessRunStatus.READY_TO_RESUME.name().equals(run.getRunStatus())) {
                    readyRun[0] = run.getId();
                }
                return null;
            }
        });
        if (readyRun[0] != null) {
            FlowEngine.transactionExecutor().afterCommit(new Runnable() {
                @Override
                public void run() {
                    resumeReadyRun(readyRun[0]);
                }
            });
        }
    }

    @Override
    public void resumeReadyRun(final Long runId) {
        resumeReadyRun(currentTenantId(), runId);
    }

    private void resumeReadyRun(final String tenantId, final Long runId) {
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                if (runDao.claimReadyToResume(tenantId, runId) != 1) {
                    return null;
                }
                SubprocessRun run = runDao.lockById(tenantId, runId);
                Task task = FlowEngine.taskService().getById(run.getParentTaskId());
                if (task == null || !Objects.equals(task.getInstanceId(), run.getParentInstanceId())
                    || !Objects.equals(task.getNodeCode(), run.getParentNodeCode())) {
                    run.setRunStatus(SubprocessRunStatus.FAILED.name());
                    run.setFailureCode("PARENT_TASK_CHANGED");
                    runDao.updateById(run);
                    return null;
                }
                RESUMING_TASK.set(task.getId());
                try {
                    FlowEngine.taskService().pass(task.getId(), "子流程全部完成",
                        Collections.<String, Object>emptyMap(), null, null);
                } finally {
                    RESUMING_TASK.remove();
                }
                run.setRunStatus(SubprocessRunStatus.COMPLETED.name());
                run.setCompletedAt(new Date());
                runDao.updateById(run);
                appendEvent(run, null, "PARENT_RESUMED", "SUCCEEDED", null);
                return null;
            }
        });
    }

    @Override
    public void cancelByParent(final Long parentInstanceId, final String reason) {
        final Instance parent = FlowEngine.insService().getById(parentInstanceId);
        if (parent == null) {
            return;
        }
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                List<SubprocessRun> runs = runDao.lockActiveByParent(tenant(parent.getTenantId()), parentInstanceId);
                for (SubprocessRun run : runs) {
                    cancelRun(run, reason);
                }
                return null;
            }
        });
    }

    @Override
    public void cancelByTask(final Long parentTaskId, final String reason) {
        final Task task = FlowEngine.taskService().getById(parentTaskId);
        if (task == null) {
            return;
        }
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                String tenantId = tenant(task.getTenantId());
                SubprocessRun run = runDao.findByParentTask(tenantId, parentTaskId);
                if (run != null && !isClosed(run)) {
                    cancelRun(runDao.lockById(tenantId, run.getId()), reason);
                }
                return null;
            }
        });
    }

    private void cancelRun(SubprocessRun run, String reason) {
        run.setRunStatus(SubprocessRunStatus.CANCELLING.name());
        runDao.updateById(run);
        List<SubprocessChild> children = childDao.lockByRunId(run.getTenantId(), run.getId());
        Collections.sort(children, new Comparator<SubprocessChild>() {
            @Override
            public int compare(SubprocessChild left, SubprocessChild right) {
                return left.getId().compareTo(right.getId());
            }
        });
        for (SubprocessChild child : children) {
            if (!isTerminal(child) && child.getChildInstanceId() != null) {
                FlowEngine.taskService().terminationByInsId(child.getChildInstanceId(), FlowParams.build()
                    .handler("system-subprocess").ignore(true).message(reason));
                applyOutcome(child, SubprocessOutcome.CANCELLED);
                childDao.updateById(child);
            }
        }
        recompute(run, children);
        run.setRunStatus(SubprocessRunStatus.CANCELLED.name());
        run.setCompletedAt(new Date());
        runDao.updateById(run);
        appendEvent(run, null, "RUN_CANCELLED", "SUCCEEDED", reason);
    }

    @Override
    public void reconcile() {
        List<SubprocessRun> runs = runDao.findReconcileCandidates(64);
        for (SubprocessRun run : runs) {
            if (SubprocessRunStatus.READY_TO_RESUME.name().equals(run.getRunStatus())) {
                resumeReadyRun(run.getTenantId(), run.getId());
            } else if (SubprocessRunStatus.INITIALIZING.name().equals(run.getRunStatus())) {
                initialize(run.getParentTaskId());
            } else if (SubprocessRunStatus.RUNNING.name().equals(run.getRunStatus())) {
                reconcileRunning(run);
            }
        }
    }

    private void reconcileRunning(final SubprocessRun candidate) {
        final boolean[] ready = new boolean[1];
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                SubprocessRun run = runDao.lockById(candidate.getTenantId(), candidate.getId());
                if (run == null || !SubprocessRunStatus.RUNNING.name().equals(run.getRunStatus())) {
                    return null;
                }
                recompute(run, childDao.lockByRunId(run.getTenantId(), run.getId()));
                runDao.updateById(run);
                ready[0] = SubprocessRunStatus.READY_TO_RESUME.name().equals(run.getRunStatus());
                return null;
            }
        });
        if (ready[0]) {
            resumeReadyRun(candidate.getTenantId(), candidate.getId());
        }
    }

    @Override
    public SubprocessSummary getSummary(String tenantId, Long parentTaskId) {
        SubprocessRun run = runDao.findByParentTask(tenant(tenantId), parentTaskId);
        if (run == null) {
            return null;
        }
        SubprocessSummary summary = new SubprocessSummary();
        summary.setRunId(run.getId());
        summary.setStatus(run.getRunStatus());
        summary.setTotal(value(run.getExpectedCount()));
        summary.setPending(value(run.getPendingCount()));
        summary.setRunning(value(run.getRunningCount()));
        summary.setCompleted(value(run.getCompletedCount()));
        summary.setFailed(value(run.getFailedCount()));
        summary.setCancelled(value(run.getCancelledCount()));
        return summary;
    }

    @Override
    public Page<SubprocessChild> pageChildren(String tenantId, Long runId, Page<SubprocessChild> page) {
        if (page.getPageSize() > 64) {
            page.setPageSize(64);
        }
        return childDao.pageByRunId(tenant(tenantId), runId, page);
    }

    @Override
    public Page<SubprocessChildSummary> pageChildSummaries(String tenantId, Long runId,
        Page<SubprocessChildSummary> page) {
        Page<SubprocessChild> childPage = pageChildren(tenantId, runId,
            new Page<SubprocessChild>(page.getPageNum(), page.getPageSize()));
        List<SubprocessChildSummary> summaries = new ArrayList<SubprocessChildSummary>();
        for (SubprocessChild child : childPage.getList()) {
            SubprocessChildSummary summary = new SubprocessChildSummary();
            summary.setId(child.getId());
            summary.setRunId(child.getRunId());
            summary.setItemKey(child.getItemKey());
            summary.setItemLabel(child.getItemLabel());
            summary.setChildFlowCode(child.getChildFlowCode());
            summary.setChildDefinitionId(child.getChildDefinitionId());
            summary.setChildDefinitionVersion(child.getChildDefinitionVersion());
            summary.setChildInstanceId(child.getChildInstanceId());
            summary.setChildStatus(child.getChildStatus());
            summary.setOutcome(child.getOutcome());
            summary.setStartedAt(child.getStartedAt());
            summary.setCompletedAt(child.getCompletedAt());
            if (child.getChildInstanceId() != null) {
                List<Task> tasks = FlowEngine.taskService().getByInsId(child.getChildInstanceId());
                if (CollUtil.isNotEmpty(tasks)) {
                    summary.setCurrentNodeCode(tasks.get(0).getNodeCode());
                    summary.setCurrentNodeName(tasks.get(0).getNodeName());
                }
            }
            summaries.add(summary);
        }
        Page<SubprocessChildSummary> result = new Page<SubprocessChildSummary>(summaries, childPage.getTotal());
        result.setPageNum(childPage.getPageNum());
        result.setPageSize(childPage.getPageSize());
        return result;
    }

    @Override
    public List<SubprocessEvent> listEvents(String tenantId, Long runId) {
        return eventDao.listByRunId(tenant(tenantId), runId);
    }

    @Override
    public List<SubprocessHistoryEntry> listCombinedHistory(String tenantId, Long runId, Long childId) {
        String normalizedTenant = tenant(tenantId);
        SubprocessRun run = runDao.findById(normalizedTenant, runId);
        if (run == null) {
            return Collections.emptyList();
        }
        List<SubprocessHistoryEntry> entries = new ArrayList<SubprocessHistoryEntry>();
        appendHistory(entries, "PARENT", run, null,
            FlowEngine.hisTaskService().getByInsId(run.getParentInstanceId()));
        for (SubprocessEvent event : eventDao.listByRunId(normalizedTenant, runId)) {
            SubprocessHistoryEntry entry = new SubprocessHistoryEntry();
            entry.setSource("ORCHESTRATION");
            entry.setOccurredAt(event.getOccurredAt());
            entry.setRunId(runId);
            entry.setChildId(event.getChildId());
            entry.setInstanceId(event.getChildInstanceId());
            entry.setNodeCode(event.getParentNodeCode());
            entry.setAction(event.getEventType());
            entry.setOutcome(event.getEventResult());
            entry.setMessage(event.getReason());
            entries.add(entry);
        }
        if (childId != null) {
            SubprocessChild child = childDao.findById(normalizedTenant, childId);
            if (child != null && Objects.equals(runId, child.getRunId()) && child.getChildInstanceId() != null) {
                appendHistory(entries, "CHILD", run, child,
                    FlowEngine.hisTaskService().getByInsId(child.getChildInstanceId()));
            }
        }
        Collections.sort(entries, new Comparator<SubprocessHistoryEntry>() {
            @Override
            public int compare(SubprocessHistoryEntry left, SubprocessHistoryEntry right) {
                Date leftTime = left.getOccurredAt();
                Date rightTime = right.getOccurredAt();
                if (leftTime == null) return rightTime == null ? 0 : 1;
                return rightTime == null ? -1 : leftTime.compareTo(rightTime);
            }
        });
        return entries;
    }

    private void appendHistory(List<SubprocessHistoryEntry> entries, String source, SubprocessRun run,
        SubprocessChild child, List<HisTask> history) {
        for (HisTask hisTask : history) {
            SubprocessHistoryEntry entry = new SubprocessHistoryEntry();
            entry.setSource(source);
            entry.setOccurredAt(hisTask.getCreateTime());
            entry.setRunId(run.getId());
            entry.setChildId(child == null ? null : child.getId());
            entry.setItemLabel(child == null ? null : child.getItemLabel());
            entry.setInstanceId(hisTask.getInstanceId());
            entry.setNodeCode(hisTask.getNodeCode());
            entry.setNodeName(hisTask.getNodeName());
            entry.setAction(hisTask.getSkipType());
            entry.setOutcome(hisTask.getFlowStatus());
            entry.setActor(hisTask.getApprover());
            entry.setMessage(hisTask.getMessage());
            entries.add(entry);
        }
    }

    @Override
    public void onTasksCreated(List<Task> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (final Task task : tasks) {
            if (NodeType.isSubProcess(task.getNodeType())) {
                FlowEngine.transactionExecutor().afterCommit(new Runnable() {
                    @Override
                    public void run() {
                        initialize(task.getId());
                    }
                });
            }
        }
    }

    @Override
    public void beforeTaskLeave(Task task, String skipType) {
        if (task == null || !NodeType.isSubProcess(task.getNodeType())) {
            return;
        }
        if (SkipType.isPass(skipType)) {
            if (!Objects.equals(RESUMING_TASK.get(), task.getId())) {
                throw new IllegalStateException("Subprocess task can only be passed by the subprocess orchestrator");
            }
            return;
        }
        cancelByTask(task.getId(), "PARENT_NODE_LEFT");
    }

    @Override
    public void onInstanceTerminal(final Instance instance, final SubprocessOutcome outcome) {
        Map<String, Object> variables = instance == null ? null : instance.getVariableMap();
        if (variables == null || !variables.containsKey("flovira.subprocess.parentInstanceId")) {
            return;
        }
        FlowEngine.transactionExecutor().afterCommit(new Runnable() {
            @Override
            public void run() {
                notifyChildTerminal(instance.getId(), outcome);
            }
        });
    }

    private void recompute(SubprocessRun run, List<SubprocessChild> children) {
        int completed = count(children, SubprocessChildStatus.COMPLETED);
        int failed = count(children, SubprocessChildStatus.FAILED);
        int cancelled = count(children, SubprocessChildStatus.CANCELLED);
        int running = count(children, SubprocessChildStatus.RUNNING)
            + count(children, SubprocessChildStatus.STARTING);
        int pending = count(children, SubprocessChildStatus.PENDING);
        run.setCompletedCount(completed);
        run.setFailedCount(failed);
        run.setCancelledCount(cancelled);
        run.setRunningCount(running);
        run.setPendingCount(pending);
        if (failed > 0 || cancelled > 0) {
            run.setRunStatus(SubprocessRunStatus.FAILED.name());
            run.setFailureCode(failed > 0 ? "CHILD_FAILED" : "CHILD_CANCELLED");
        } else if (completed == value(run.getExpectedCount())) {
            run.setRunStatus(SubprocessRunStatus.READY_TO_RESUME.name());
            run.setFailureCode(null);
        } else {
            run.setRunStatus(SubprocessRunStatus.RUNNING.name());
        }
    }

    private int count(List<SubprocessChild> children, SubprocessChildStatus status) {
        int result = 0;
        for (SubprocessChild child : children) {
            if (status.name().equals(child.getChildStatus())) {
                result++;
            }
        }
        return result;
    }

    private void applyOutcome(SubprocessChild child, SubprocessOutcome outcome) {
        child.setOutcome(outcome.name());
        child.setCompletedAt(new Date());
        if (SubprocessOutcome.SUCCEEDED == outcome) {
            child.setChildStatus(SubprocessChildStatus.COMPLETED.name());
        } else if (SubprocessOutcome.FAILED == outcome) {
            child.setChildStatus(SubprocessChildStatus.FAILED.name());
        } else {
            child.setChildStatus(SubprocessChildStatus.CANCELLED.name());
        }
    }

    private SubprocessChild findChild(List<SubprocessChild> children, Long childInstanceId) {
        for (SubprocessChild child : children) {
            if (Objects.equals(child.getChildInstanceId(), childInstanceId)) {
                return child;
            }
        }
        return null;
    }

    private boolean isTerminal(SubprocessChild child) {
        return SubprocessChildStatus.COMPLETED.name().equals(child.getChildStatus())
            || SubprocessChildStatus.FAILED.name().equals(child.getChildStatus())
            || SubprocessChildStatus.CANCELLED.name().equals(child.getChildStatus());
    }

    private boolean isClosed(SubprocessRun run) {
        return SubprocessRunStatus.COMPLETED.name().equals(run.getRunStatus())
            || SubprocessRunStatus.CANCELLED.name().equals(run.getRunStatus())
            || SubprocessRunStatus.CANCELLING.name().equals(run.getRunStatus())
            || SubprocessRunStatus.RESUMING.name().equals(run.getRunStatus());
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String currentTenantId() {
        return tenant(FlowEngine.tenantHandler() == null ? null : FlowEngine.tenantHandler().getTenantId());
    }

    private String tenant(String tenantId) {
        return StringUtils.isEmpty(tenantId) ? "0" : tenantId;
    }

    private void appendEvent(SubprocessRun run, SubprocessChild child, String type, String result,
        String reason) {
        SubprocessEvent event = FlowEngine.newSubprocessEvent();
        FlowEngine.dataFillHandler().idFill(event);
        FlowEngine.dataFillHandler().insertFill(event);
        event.setTenantId(run.getTenantId());
        event.setDelFlag("0");
        event.setRunId(run.getId());
        event.setChildId(child == null ? null : child.getId());
        event.setParentInstanceId(run.getParentInstanceId());
        event.setChildInstanceId(child == null ? null : child.getChildInstanceId());
        event.setParentNodeCode(run.getParentNodeCode());
        event.setEventType(type);
        event.setEventResult(result);
        event.setReason(reason);
        event.setOccurredAt(new Date());
        eventDao.save(event);
    }
}
