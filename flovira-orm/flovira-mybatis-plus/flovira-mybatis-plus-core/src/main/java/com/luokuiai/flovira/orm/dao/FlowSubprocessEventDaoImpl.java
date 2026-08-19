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
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.orm.entity.FlowSubprocessEvent;
import com.luokuiai.flovira.orm.mapper.FlowSubprocessEventMapper;

import java.util.List;

/** 子流程事件 DAO。 @author warm */
public class FlowSubprocessEventDaoImpl implements FlowSubprocessEventDao<FlowSubprocessEvent> {
    private FlowSubprocessEventMapper mapper() { return FrameInvoker.getBean(FlowSubprocessEventMapper.class); }
    public int save(FlowSubprocessEvent entity) { return mapper().insert(entity); }
    public List<FlowSubprocessEvent> listByRunId(String tenantId, Long runId) {
        QueryWrapper<FlowSubprocessEvent> query = new QueryWrapper<>();
        if (tenantId == null) query.isNull("tenant_id"); else query.eq("tenant_id", tenantId);
        query.eq("run_id", runId).orderByAsc("occurred_at", "id");
        return mapper().selectList(query);
    }
}
