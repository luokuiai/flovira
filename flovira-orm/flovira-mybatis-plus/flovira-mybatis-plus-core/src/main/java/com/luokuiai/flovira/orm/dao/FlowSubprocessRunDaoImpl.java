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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.orm.entity.FlowSubprocessRun;
import com.luokuiai.flovira.orm.mapper.FlowSubprocessRunMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 子流程运行 DAO。 @author warm */
public class FlowSubprocessRunDaoImpl implements FlowSubprocessRunDao<FlowSubprocessRun> {
    private FlowSubprocessRunMapper mapper() { return FrameInvoker.getBean(FlowSubprocessRunMapper.class); }
    public int save(FlowSubprocessRun entity) { return mapper().insert(entity); }
    public int updateById(FlowSubprocessRun entity) { return mapper().updateById(entity); }
    public FlowSubprocessRun findByParentTask(String tenantId, Long parentTaskId) {
        QueryWrapper<FlowSubprocessRun> query = tenant(tenantId);
        query.eq("parent_task_id", parentTaskId);
        return mapper().selectOne(query);
    }
    public FlowSubprocessRun findById(String tenantId, Long runId) {
        QueryWrapper<FlowSubprocessRun> query = tenant(tenantId);
        query.eq("id", runId).eq("del_flag", "0");
        return mapper().selectOne(query);
    }
    public FlowSubprocessRun lockById(String tenantId, Long runId) {
        return "sqlserver".equals(FlowEngine.dataSourceType())
            ? mapper().lockByIdSqlServer(tenantId, runId) : mapper().lockById(tenantId, runId);
    }
    public int claimReadyToResume(String tenantId, Long runId) {
        UpdateWrapper<FlowSubprocessRun> update = new UpdateWrapper<>();
        update.eq("tenant_id", tenantId).eq("id", runId).eq("del_flag", "0")
            .eq("run_status", "READY_TO_RESUME").set("run_status", "RESUMING")
            .setSql("lock_version=lock_version+1");
        return mapper().update(null, update);
    }
    public List<FlowSubprocessRun> lockActiveByParent(String tenantId, Long parentInstanceId) {
        return "sqlserver".equals(FlowEngine.dataSourceType())
            ? mapper().lockActiveSqlServer(tenantId, parentInstanceId) : mapper().lockActive(tenantId, parentInstanceId);
    }
    public List<FlowSubprocessRun> findReconcileCandidates(int limit) {
        QueryWrapper<FlowSubprocessRun> query = new QueryWrapper<>();
        query.in("run_status", Arrays.asList("INITIALIZING", "RUNNING", "READY_TO_RESUME")).orderByAsc("id");
        List<FlowSubprocessRun> values = mapper().selectList(query);
        return values.size() <= limit ? values : new ArrayList<>(values.subList(0, limit));
    }
    private QueryWrapper<FlowSubprocessRun> tenant(String tenantId) {
        QueryWrapper<FlowSubprocessRun> query = new QueryWrapper<>();
        return tenantId == null ? query.isNull("tenant_id") : query.eq("tenant_id", tenantId);
    }
}
