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
package com.luokuiai.flovira.core.service;

import com.luokuiai.flovira.core.dto.SubprocessSummary;
import com.luokuiai.flovira.core.dto.SubprocessChildSummary;
import com.luokuiai.flovira.core.dto.SubprocessHistoryEntry;
import com.luokuiai.flovira.core.entity.SubprocessChild;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.SubprocessRun;
import com.luokuiai.flovira.core.enums.SubprocessOutcome;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.core.utils.page.Page;

import java.util.List;

/**
 * 子流程运行服务
 *
 * @author warm
 */
public interface SubprocessService {

    SubprocessService setDao(FlowSubprocessRunDao<SubprocessRun> runDao,
        FlowSubprocessChildDao<SubprocessChild> childDao, FlowSubprocessEventDao<SubprocessEvent> eventDao);

    SubprocessRun initialize(Long parentTaskId);

    void notifyChildTerminal(Long childInstanceId, SubprocessOutcome outcome);

    void resumeReadyRun(Long runId);

    void cancelByParent(Long parentInstanceId, String reason);

    void cancelByTask(Long parentTaskId, String reason);

    void reconcile();

    SubprocessSummary getSummary(String tenantId, Long parentTaskId);

    Page<SubprocessChild> pageChildren(String tenantId, Long runId, Page<SubprocessChild> page);

    Page<SubprocessChildSummary> pageChildSummaries(String tenantId, Long runId,
        Page<SubprocessChildSummary> page);

    List<SubprocessEvent> listEvents(String tenantId, Long runId);

    List<SubprocessHistoryEntry> listCombinedHistory(String tenantId, Long runId, Long childId);

    void onTasksCreated(List<com.luokuiai.flovira.core.entity.Task> tasks);

    void beforeTaskLeave(com.luokuiai.flovira.core.entity.Task task, String skipType);

    void onInstanceTerminal(com.luokuiai.flovira.core.entity.Instance instance, SubprocessOutcome outcome);
}
