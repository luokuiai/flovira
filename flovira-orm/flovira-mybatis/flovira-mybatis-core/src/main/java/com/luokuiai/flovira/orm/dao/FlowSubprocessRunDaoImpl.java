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
package com.luokuiai.flovira.orm.dao;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.orm.entity.FlowSubprocessRun;
import com.luokuiai.flovira.orm.mapper.FlowSubprocessRunMapper;

import java.util.ArrayList;
import java.util.List;

/** 子流程运行 DAO。 @author warm */
public class FlowSubprocessRunDaoImpl implements FlowSubprocessRunDao<FlowSubprocessRun> {
    private FlowSubprocessRunMapper mapper() { return FrameInvoker.getBean(FlowSubprocessRunMapper.class); }
    public int save(FlowSubprocessRun entity) { return mapper().insert(entity); }
    public int updateById(FlowSubprocessRun entity) { return mapper().updateById(entity); }
    public FlowSubprocessRun findByParentTask(String tenantId, Long parentTaskId) {
        return mapper().findByParentTask(tenantId, parentTaskId);
    }
    public FlowSubprocessRun findById(String tenantId, Long runId) { return mapper().findById(tenantId, runId); }
    public FlowSubprocessRun lockById(String tenantId, Long runId) {
        return mapper().lockById(tenantId, runId, FlowEngine.dataSourceType());
    }
    public int claimReadyToResume(String tenantId, Long runId) {
        return mapper().claimReadyToResume(tenantId, runId);
    }
    public List<FlowSubprocessRun> lockActiveByParent(String tenantId, Long parentInstanceId) {
        return mapper().lockActiveByParent(tenantId, parentInstanceId, FlowEngine.dataSourceType());
    }
    public List<FlowSubprocessRun> findReconcileCandidates(int limit) {
        List<FlowSubprocessRun> values = mapper().findReconcileCandidates();
        return values.size() <= limit ? values : new ArrayList<>(values.subList(0, limit));
    }
}
