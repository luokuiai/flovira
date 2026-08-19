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
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.dto.NodeJson;
import com.luokuiai.flovira.core.dto.WaitConfig;
import com.luokuiai.flovira.core.dto.WaitResumeResult;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.service.WaitService;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.utils.CollUtil;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.core.utils.WaitConfigUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 等待节点服务实现
 *
 * @author warm
 */
public class WaitServiceImpl implements WaitService {

    public static final String SYSTEM_HANDLER = "flovira:wait";

    @Override
    public WaitResumeResult resumeTask(final Long taskId, final Map<String, Object> variables) {
        return FlowEngine.transactionExecutor().execute(new TransactionCallback<WaitResumeResult>() {
            @Override
            public WaitResumeResult execute() {
                return doResumeTask(taskId, variables, "WAIT_RESUME");
            }
        });
    }

    private WaitResumeResult doResumeTask(Long taskId, Map<String, Object> variables, String action) {
        Task task = taskId == null ? null : FlowEngine.taskService().getById(taskId);
        if (task == null) {
            return new WaitResumeResult(null, taskId, null, "NOT_FOUND_OR_ALREADY_RESUMED");
        }
        if (!NodeType.isWait(task.getNodeType())) {
            throw new IllegalStateException("Task is not a WAIT task");
        }
        String waitKey = getWaitKey(task, loadWaitConfigs(task.getInstanceId()));
        if (!FlowEngine.taskService().claimWait(taskId, new java.util.Date())) {
            return new WaitResumeResult(task.getInstanceId(), taskId, waitKey, "NOT_FOUND_OR_ALREADY_RESUMED");
        }
        return advance(task, waitKey, variables, action);
    }

    @Override
    public WaitResumeResult resumeTimeoutTask(final Long taskId) {
        return FlowEngine.transactionExecutor().execute(new TransactionCallback<WaitResumeResult>() {
            @Override
            public WaitResumeResult execute() {
                return doResumeTask(taskId, Collections.<String, Object>emptyMap(), "WAIT_TIMEOUT");
            }
        });
    }

    private WaitResumeResult advance(Task task, String waitKey, Map<String, Object> variables, String action) {
        FlowParams flowParams = FlowParams.build()
            .skipType(SkipType.PASS.getKey())
            .flowStatus(FlowStatus.AUTO_PASS.getKey())
            .handler(SYSTEM_HANDLER)
            .message("WAIT_TIMEOUT".equals(action) ? "WAIT timeout resumed: " + waitKey : "WAIT resumed: " + waitKey)
            .hisTaskExt(waitHistory(waitKey, action))
            .variable(variables == null ? Collections.<String, Object>emptyMap() : variables);
        FlowEngine.taskService().skipSystemTask(flowParams, task);
        return new WaitResumeResult(task.getInstanceId(), task.getId(), waitKey, "RESUMED");
    }

    @Override
    public WaitResumeResult resume(final Long instanceId, final String waitKey, final Map<String, Object> variables) {
        return FlowEngine.transactionExecutor().execute(new TransactionCallback<WaitResumeResult>() {
            @Override
            public WaitResumeResult execute() {
                return doResume(instanceId, waitKey, variables);
            }
        });
    }

    private WaitResumeResult doResume(Long instanceId, String waitKey, Map<String, Object> variables) {
        List<Task> matches = new ArrayList<Task>();
        Map<String, WaitConfig> waitConfigs = loadWaitConfigs(instanceId);
        for (Task task : FlowEngine.taskService().getByInsIdAndNodeType(instanceId, NodeType.WAIT.getKey())) {
            if (waitKey != null && waitKey.equals(getWaitKey(task, waitConfigs))) {
                matches.add(task);
            }
        }
        if (matches.isEmpty()) {
            return new WaitResumeResult(instanceId, null, waitKey, "NOT_FOUND_OR_ALREADY_RESUMED");
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("More than one active WAIT task matches waitKey: " + waitKey);
        }
        Task task = matches.get(0);
        if (!FlowEngine.taskService().claimWait(task.getId(), new java.util.Date())) {
            return new WaitResumeResult(instanceId, task.getId(), waitKey, "NOT_FOUND_OR_ALREADY_RESUMED");
        }
        return advance(task, waitKey, variables, "WAIT_RESUME");
    }

    private Map<String, WaitConfig> loadWaitConfigs(Long instanceId) {
        Instance instance = instanceId == null ? null : FlowEngine.insService().getById(instanceId);
        if (instance == null || StringUtils.isEmpty(instance.getDefJson())) {
            throw new IllegalStateException("Instance definition snapshot is missing");
        }
        DefJson defJson = FlowEngine.jsonConvert.strToBean(instance.getDefJson(), DefJson.class);
        if (defJson == null || CollUtil.isEmpty(defJson.getNodeList())) {
            throw new IllegalStateException("Instance definition snapshot is invalid");
        }
        Map<String, WaitConfig> waitConfigs = new HashMap<String, WaitConfig>();
        for (NodeJson node : defJson.getNodeList()) {
            if (NodeType.isWait(node.getNodeType())) {
                waitConfigs.put(node.getNodeCode(), WaitConfigUtil.read(node));
            }
        }
        return waitConfigs;
    }

    private String getWaitKey(Task task, Map<String, WaitConfig> waitConfigs) {
        WaitConfig config = waitConfigs.get(task.getNodeCode());
        if (config == null) {
            throw new IllegalStateException("WAIT task node is missing from instance definition snapshot: "
                + task.getNodeCode());
        }
        return config.getWaitKey();
    }

    private String waitHistory(String waitKey, String action) {
        java.util.Map<String, Object> history = new java.util.HashMap<String, Object>();
        history.put("action", action);
        history.put("waitKey", waitKey);
        return FlowEngine.jsonConvert.objToStr(history);
    }
}
