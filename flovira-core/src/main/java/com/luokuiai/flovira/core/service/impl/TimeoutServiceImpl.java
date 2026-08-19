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
import com.luokuiai.flovira.core.dto.TimeoutExecutionResult;
import com.luokuiai.flovira.core.dto.WaitResumeResult;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.enums.TimeoutAction;
import com.luokuiai.flovira.core.lock.TimeoutSchedulerLock;
import com.luokuiai.flovira.core.service.TimeoutService;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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
        if (config == null || config.getTimeout() == null || !config.getTimeout().isEnabled()) {
            return new TimeoutExecutionResult(0, 0, 0, 0);
        }
        TimeoutSchedulerLock schedulerLock = FlowEngine.timeoutSchedulerLock();
        if (schedulerLock == null) {
            return executeDue(config, now, batchSize);
        }
        String lockKey = config.getTimeout().getSchedulerLockKey();
        String owner = UUID.randomUUID().toString();
        boolean locked;
        try {
            locked = schedulerLock.tryLock(lockKey, owner, config.getTimeout().getClaimTimeoutMillis());
        } catch (RuntimeException ex) {
            log.warn("Failed to acquire timeout scheduler lock, falling back to database claims", ex);
            return executeDue(config, now, batchSize);
        }
        if (!locked) {
            return new TimeoutExecutionResult(0, 0, 0, 0);
        }
        try {
            return executeDue(config, now, batchSize);
        } finally {
            try {
                schedulerLock.unlock(lockKey, owner);
            } catch (RuntimeException ex) {
                log.warn("Failed to release timeout scheduler lock {}", lockKey, ex);
            }
        }
    }

    private TimeoutExecutionResult executeDue(Flovira config, Date now, int batchSize) {
        Date scanTime = now == null ? new Date() : now;
        int limit = batchSize > 0 ? batchSize : config.getTimeout().getBatchSize();
        Date staleBefore = new Date(scanTime.getTime() - config.getTimeout().getClaimTimeoutMillis());
        List<Task> tasks = FlowEngine.taskService().listDueTimeoutTasks(scanTime, staleBefore, limit);
        int claimed = 0;
        int succeeded = 0;
        int failed = 0;
        for (Task candidate : tasks) {
            if (NodeType.isWait(candidate.getNodeType())) {
                try {
                    if (!TimeoutAction.RESUME_WAIT.name().equals(candidate.getTimeoutAction())) {
                        throw new IllegalStateException("WAIT timeout action must be RESUME_WAIT");
                    }
                    WaitResumeResult result = FlowEngine.waitService().resumeTimeoutTask(candidate.getId());
                    if ("RESUMED".equals(result.getStatus())) {
                        claimed++;
                        succeeded++;
                    }
                } catch (RuntimeException ex) {
                    failed++;
                    log.error("Failed to execute WAIT timeout action for task {}", candidate.getId(), ex);
                }
                continue;
            }
            if (!claim(candidate.getId(), scanTime, staleBefore)) {
                continue;
            }
            claimed++;
            try {
                executeInTransaction(candidate);
                succeeded++;
            } catch (RuntimeException ex) {
                failed++;
                release(candidate.getId());
                log.error("Failed to execute timeout action for task {}", candidate.getId(), ex);
            }
        }
        return new TimeoutExecutionResult(tasks.size(), claimed, succeeded, failed);
    }

    private boolean claim(final Long taskId, final Date claimedAt, final Date staleBefore) {
        return FlowEngine.transactionExecutor().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean execute() {
                return FlowEngine.taskService().claimTimeout(taskId, claimedAt, staleBefore);
            }
        });
    }

    private void release(final Long taskId) {
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                FlowEngine.taskService().releaseTimeout(taskId);
                return null;
            }
        });
    }

    private void executeInTransaction(final Task task) {
        FlowEngine.transactionExecutor().execute(new TransactionCallback<Object>() {
            @Override
            public Object execute() {
                TimeoutServiceImpl.this.execute(task);
                return null;
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
        FlowEngine.taskService().skipSystemTask(params, task);
    }

    private String timeoutHistory(Task task) {
        java.util.Map<String, Object> history = new java.util.HashMap<String, Object>();
        history.put("action", "TIMEOUT");
        history.put("timeoutAction", task.getTimeoutAction());
        history.put("timeoutAt", task.getTimeoutAt());
        return FlowEngine.jsonConvert.objToStr(history);
    }
}
