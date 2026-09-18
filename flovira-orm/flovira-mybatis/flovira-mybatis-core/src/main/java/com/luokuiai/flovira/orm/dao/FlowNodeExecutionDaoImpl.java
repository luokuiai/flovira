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

import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao;
import com.luokuiai.flovira.orm.entity.FlowNodeExecution;
import com.luokuiai.flovira.orm.mapper.FlowNodeExecutionMapper;
import java.util.Date;
import java.util.List;

/** 节点执行 DAO。 */
public class FlowNodeExecutionDaoImpl implements FlowNodeExecutionDao<FlowNodeExecution> {
    private FlowNodeExecutionMapper mapper() { return FrameInvoker.getBean(FlowNodeExecutionMapper.class); }
    public int save(FlowNodeExecution entity) { return mapper().insert(entity); }
    public FlowNodeExecution get(String tenantId, Long id) { return mapper().get(tenantId, id); }
    public List<FlowNodeExecution> listActive(String tenantId, Long instanceId) { return mapper().listActive(tenantId, instanceId); }
    public int close(String tenantId, Long id, int version, String reason, Date closedAt) {
        return mapper().close(tenantId, id, version, reason, closedAt);
    }
}
