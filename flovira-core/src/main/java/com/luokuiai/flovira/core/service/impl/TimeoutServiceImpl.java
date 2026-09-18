/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleTransition;
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.dto.TimeoutExecutionResult;
import com.luokuiai.flovira.core.dto.WaitResumeResult;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.enums.TimeoutAction;
import com.luokuiai.flovira.core.service.TimeoutService;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 节点超时服务实现
 *
 * @author warm
 */
public class TimeoutServiceImpl implements TimeoutService {

    public static final String SYSTEM_HANDLER = "flovira:timeout";
    private static final Logger log = LoggerFactory.getLogger(TimeoutServiceImpl.class);

    @Override
    public TimeoutExecutionResult executeDue(Date now, int batchSize) {
        Flovira config = FlowEngine.getFlowConfig();
        TimeoutExecutionResult result = new TimeoutExecutionResult(0, 0, 0, 0);
        if (config == null || config.getTimeout() == null || !config.getTimeout().isEnabled()) {
            return result;
        }
        Date scanTime = now == null ? new Date() : new Date(now.getTime());
        int limit = batchSize > 0 ? batchSize : config.getTimeout().getBatchSize();
        Date staleBefore = new Date(scanTime.getTime() - config.getTimeout().getClaimTimeoutMillis());
        List<Task> tasks = FlowEngine.taskService().listDueTimeoutTasks(scanTime, staleBefore, limit);
        result.setScanned(tasks.size());
        for (Task candidate : tasks) {
            try {
                if (executeTimeout(candidate.getId(), scanTime, config.getTimeout(), result)) {
                    result.setSucceeded(result.getSucceeded() + 1);
                }
            } catch (RuntimeException ex) {
                result.setFailed(result.getFailed() + 1);
                log.error("Failed to execute timeout action for task {}", candidate.getId(), ex);
            }
        }
        return result;
    }

    @Override
    public boolean executeTimeout(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        Flovira config = FlowEngine.getFlowConfig();
        if (config == null || config.getTimeout() == null || !config.getTimeout().isEnabled()) {
            return false;
        }
        return executeTimeout(taskId, new Date(), config.getTimeout(), new TimeoutExecutionResult(0, 0, 0, 0));
    }

    private boolean executeTimeout(final Long taskId, final Date now, final Flovira.Timeout config,
                                   final TimeoutExecutionResult result) {
        return FlowEngine.transactionExecutor().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean execute() {
                // 消息可能提前、重复或延迟到达，不能直接使用扫描时的任务快照推进。
                Task task = FlowEngine.taskService().getById(taskId);
                if (task == null || task.getTimeoutAt() == null || task.getTimeoutAt().after(now)) {
                    return false;
                }
                if (NodeType.isWait(task.getNodeType())) {
                    if (!TimeoutAction.RESUME_WAIT.name().equals(task.getTimeoutAction())) {
                        throw new IllegalStateException("WAIT timeout action must be RESUME_WAIT");
                    }
                    WaitResumeResult resumed = FlowEngine.waitService().resumeTimeoutTask(taskId);
                    if (!"RESUMED".equals(resumed.getStatus())) {
                        return false;
                    }
                    result.setClaimed(result.getClaimed() + 1);
                    return true;
                }
                if (!NodeType.BETWEEN.getKey().equals(task.getNodeType())
                    || (!TimeoutAction.AUTO_PASS.name().equals(task.getTimeoutAction())
                        && !TimeoutAction.AUTO_REJECT.name().equals(task.getTimeoutAction()))) {
                    throw new IllegalStateException("Unsupported task timeout action");
                }
                Date staleBefore = new Date(now.getTime() - config.getClaimTimeoutMillis());
                // 抢占与推进共用事务：持有数据库写锁直到提交，失败由事务回滚抢占状态。
                // 不再单独提交租约或无条件释放，避免旧执行者干扰后续执行者。
                if (!FlowEngine.taskService().claimTimeout(taskId, now, staleBefore)) {
                    return false;
                }
                result.setClaimed(result.getClaimed() + 1);
                TimeoutServiceImpl.this.execute(task);
                return true;
            }
        });
    }

    private void execute(Task task) {
        if (TimeoutAction.RESUME_WAIT.name().equals(task.getTimeoutAction())) {
            throw new IllegalStateException("RESUME_WAIT is only valid for WAIT tasks");
        }
        String skipType = TimeoutAction.AUTO_REJECT.name().equals(task.getTimeoutAction())
            ? SkipType.REJECT.getKey() : SkipType.PASS.getKey();
        String flowStatus = TimeoutAction.AUTO_PASS.name().equals(task.getTimeoutAction())
            ? FlowStatus.AUTO_PASS.getKey() : null;
        FlowParams params = FlowParams.build()
            .skipType(skipType)
            .flowStatus(flowStatus)
            .handler(SYSTEM_HANDLER)
            .message("Task timeout: " + task.getTimeoutAction())
            .hisTaskExt(timeoutHistory(task));
        LifecycleTransition.system("APPROVAL_TIMEOUT", () -> FlowEngine.taskService().skipSystemTask(params, task));
    }

    private String timeoutHistory(Task task) {
        java.util.Map<String, Object> history = new java.util.HashMap<String, Object>();
        history.put("action", "TIMEOUT");
        history.put("timeoutAction", task.getTimeoutAction());
        history.put("timeoutAt", task.getTimeoutAt());
        return FlowEngine.jsonConvert.objToStr(history);
    }
}
