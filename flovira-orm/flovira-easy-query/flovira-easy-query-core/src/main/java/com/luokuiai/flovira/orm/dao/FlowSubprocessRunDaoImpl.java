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

import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.orm.entity.FlowSubprocessRun;
import com.luokuiai.flovira.orm.entity.proxy.FlowSubprocessRunProxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 子流程运行 DAO。 @author warm */
public class FlowSubprocessRunDaoImpl extends FloviraDaoImpl<FlowSubprocessRun, FlowSubprocessRunProxy>
    implements FlowSubprocessRunDao<FlowSubprocessRun> {
    public FlowSubprocessRun newEntity() { return new FlowSubprocessRun(); }
    public FlowSubprocessRun findByParentTask(String tenantId, Long parentTaskId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.parentTaskId().eq(parentTaskId); })
            .singleOrNull();
    }
    public FlowSubprocessRun findById(String tenantId, Long runId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.id().eq(runId); p.delFlag().eq("0"); })
            .singleOrNull();
    }
    public FlowSubprocessRun lockById(String tenantId, Long runId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.id().eq(runId); }).singleOrNull();
    }
    public int claimReadyToResume(String tenantId, Long runId) {
        return (int) updatable().where(p -> { p.tenantId().eq(tenantId); p.id().eq(runId);
            p.delFlag().eq("0"); p.runStatus().eq("READY_TO_RESUME"); })
            .setColumns(p -> p.runStatus().set("RESUMING")).executeRows();
    }
    public List<FlowSubprocessRun> lockActiveByParent(String tenantId, Long parentInstanceId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.parentInstanceId().eq(parentInstanceId);
            p.runStatus().in(Arrays.asList("INITIALIZING", "RUNNING", "READY_TO_RESUME", "RESUMING", "FAILED", "CANCELLING")); })
            .orderBy(p -> p.id().asc()).toList();
    }
    public List<FlowSubprocessRun> findReconcileCandidates(int limit) {
        List<FlowSubprocessRun> values = queryable().where(p ->
            p.runStatus().in(Arrays.asList("INITIALIZING", "RUNNING", "READY_TO_RESUME"))).orderBy(p -> p.id().asc()).toList();
        return values.size() <= limit ? values : new ArrayList<>(values.subList(0, limit));
    }
    @Override public com.easy.query.core.expression.lambda.SQLActionExpression1<FlowSubprocessRunProxy>
        buildWhereCondition(FlowSubprocessRun entity) { return p -> p.id().eq(entity.getId()); }
    @Override public int delete(FlowSubprocessRun entity) {
        return (int) deletable().where(p -> p.id().eq(entity.getId())).executeRows();
    }
}
