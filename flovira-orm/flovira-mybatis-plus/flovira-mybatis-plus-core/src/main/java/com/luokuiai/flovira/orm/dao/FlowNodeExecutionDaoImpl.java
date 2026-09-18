/*
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
package com.luokuiai.flovira.orm.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao;
import com.luokuiai.flovira.orm.entity.FlowNodeExecution;
import com.luokuiai.flovira.orm.mapper.FlowNodeExecutionMapper;
import java.util.Date;
import java.util.List;

/** 节点执行 DAO，查询和条件转换显式限定租户及逻辑删除。 */
public class FlowNodeExecutionDaoImpl implements FlowNodeExecutionDao<FlowNodeExecution> {
    private FlowNodeExecutionMapper mapper() { return FrameInvoker.getBean(FlowNodeExecutionMapper.class); }
    private QueryWrapper<FlowNodeExecution> query(String tenantId) {
        QueryWrapper<FlowNodeExecution> query = new QueryWrapper<FlowNodeExecution>();
        if (tenantId == null) query.isNull("tenant_id"); else query.eq("tenant_id", tenantId);
        return query.eq("deleted", "0");
    }
    public int save(FlowNodeExecution entity) { return mapper().insert(entity); }
    public FlowNodeExecution get(String tenantId, Long id) { return mapper().selectOne(query(tenantId).eq("id", id)); }
    public List<FlowNodeExecution> listActive(String tenantId, Long instanceId) {
        return mapper().selectList(query(tenantId).eq("instance_id", instanceId).eq("state", "ACTIVE").orderByAsc("entered_at", "id"));
    }
    public int close(String tenantId, Long id, int version, String reason, Date closedAt) {
        UpdateWrapper<FlowNodeExecution> update = new UpdateWrapper<FlowNodeExecution>();
        if (tenantId == null) update.isNull("tenant_id"); else update.eq("tenant_id", tenantId);
        update.eq("id", id).eq("deleted", "0").eq("state", "ACTIVE").eq("version", version)
            .set("state", "CLOSED").set("close_reason", reason).set("closed_at", closedAt)
            .set("updated_at", closedAt).set("version", version + 1);
        return mapper().update(null, update);
    }
}
