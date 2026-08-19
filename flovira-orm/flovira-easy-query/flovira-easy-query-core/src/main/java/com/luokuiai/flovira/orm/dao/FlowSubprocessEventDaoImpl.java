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

import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.orm.entity.FlowSubprocessEvent;
import com.luokuiai.flovira.orm.entity.proxy.FlowSubprocessEventProxy;

import java.util.List;

/** 子流程事件 DAO。 @author warm */
public class FlowSubprocessEventDaoImpl extends FloviraDaoImpl<FlowSubprocessEvent, FlowSubprocessEventProxy>
    implements FlowSubprocessEventDao<FlowSubprocessEvent> {
    public FlowSubprocessEvent newEntity() { return new FlowSubprocessEvent(); }
    public List<FlowSubprocessEvent> listByRunId(String tenantId, Long runId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.runId().eq(runId); })
            .orderBy(p -> { p.occurredAt().asc(); p.id().asc(); }).toList();
    }
    @Override public com.easy.query.core.expression.lambda.SQLActionExpression1<FlowSubprocessEventProxy>
        buildWhereCondition(FlowSubprocessEvent entity) { return p -> p.id().eq(entity.getId()); }
    @Override public int delete(FlowSubprocessEvent entity) {
        return (int) deletable().where(p -> p.id().eq(entity.getId())).executeRows();
    }
}
