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

import com.easy.query.core.api.pagination.EasyPageResult;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.utils.page.Page;
import com.luokuiai.flovira.orm.entity.FlowSubprocessChild;
import com.luokuiai.flovira.orm.entity.proxy.FlowSubprocessChildProxy;

import java.util.List;

/** 子流程实例关系 DAO。 @author warm */
public class FlowSubprocessChildDaoImpl extends FloviraDaoImpl<FlowSubprocessChild, FlowSubprocessChildProxy>
    implements FlowSubprocessChildDao<FlowSubprocessChild> {
    public FlowSubprocessChild newEntity() { return new FlowSubprocessChild(); }
    public FlowSubprocessChild findByChildInstanceId(String tenantId, Long childInstanceId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.childInstanceId().eq(childInstanceId); })
            .singleOrNull();
    }
    public FlowSubprocessChild findById(String tenantId, Long childId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.id().eq(childId); p.delFlag().eq("0"); })
            .singleOrNull();
    }
    public FlowSubprocessChild findByRunAndItem(String tenantId, Long runId, String itemKey) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.runId().eq(runId); p.itemKey().eq(itemKey); })
            .singleOrNull();
    }
    public List<FlowSubprocessChild> lockByRunId(String tenantId, Long runId) {
        return queryable().where(p -> { p.tenantId().eq(tenantId); p.runId().eq(runId); })
            .orderBy(p -> p.id().asc()).toList();
    }
    public Page<FlowSubprocessChild> pageByRunId(String tenantId, Long runId, Page<FlowSubprocessChild> page) {
        EasyPageResult<FlowSubprocessChild> result = queryable()
            .where(p -> { p.tenantId().eq(tenantId); p.runId().eq(runId); })
            .orderBy(p -> p.id().asc()).toPageResult(page.getPageNum(), page.getPageSize());
        Page<FlowSubprocessChild> value = new Page<>(result.getData(), result.getTotal());
        value.setPageNum(page.getPageNum()); value.setPageSize(page.getPageSize()); return value;
    }
    @Override public com.easy.query.core.expression.lambda.SQLActionExpression1<FlowSubprocessChildProxy>
        buildWhereCondition(FlowSubprocessChild entity) { return p -> p.id().eq(entity.getId()); }
    @Override public int delete(FlowSubprocessChild entity) {
        return (int) deletable().where(p -> p.id().eq(entity.getId())).executeRows();
    }
}
