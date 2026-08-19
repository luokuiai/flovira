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
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.orm.entity.FlowSubprocessChild;
import com.luokuiai.flovira.orm.mapper.FlowSubprocessChildMapper;

import java.util.ArrayList;
import java.util.List;

/** 子流程实例关系 DAO。 @author warm */
public class FlowSubprocessChildDaoImpl implements FlowSubprocessChildDao<FlowSubprocessChild> {
    private FlowSubprocessChildMapper mapper() { return FrameInvoker.getBean(FlowSubprocessChildMapper.class); }
    public int save(FlowSubprocessChild entity) { return mapper().insert(entity); }
    public int updateById(FlowSubprocessChild entity) { return mapper().updateById(entity); }
    public FlowSubprocessChild findByChildInstanceId(String tenantId, Long childInstanceId) {
        QueryWrapper<FlowSubprocessChild> query = tenant(tenantId);
        query.eq("child_instance_id", childInstanceId);
        return mapper().selectOne(query);
    }
    public FlowSubprocessChild findById(String tenantId, Long childId) {
        QueryWrapper<FlowSubprocessChild> query = tenant(tenantId);
        query.eq("id", childId).eq("del_flag", "0");
        return mapper().selectOne(query);
    }
    public FlowSubprocessChild findByRunAndItem(String tenantId, Long runId, String itemKey) {
        QueryWrapper<FlowSubprocessChild> query = tenant(tenantId);
        query.eq("run_id", runId).eq("item_key", itemKey);
        return mapper().selectOne(query);
    }
    public List<FlowSubprocessChild> lockByRunId(String tenantId, Long runId) {
        return "sqlserver".equals(FlowEngine.dataSourceType())
            ? mapper().lockByRunSqlServer(tenantId, runId) : mapper().lockByRun(tenantId, runId);
    }
    public com.luokuiai.flovira.core.utils.page.Page<FlowSubprocessChild> pageByRunId(String tenantId, Long runId,
        com.luokuiai.flovira.core.utils.page.Page<FlowSubprocessChild> page) {
        QueryWrapper<FlowSubprocessChild> query = tenant(tenantId);
        query.eq("run_id", runId).orderByAsc("id");
        List<FlowSubprocessChild> all = mapper().selectList(query);
        int from = Math.min((page.getPageNum() - 1) * page.getPageSize(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        com.luokuiai.flovira.core.utils.page.Page<FlowSubprocessChild> value =
            new com.luokuiai.flovira.core.utils.page.Page<>(new ArrayList<>(all.subList(from, to)), all.size());
        value.setPageNum(page.getPageNum()); value.setPageSize(page.getPageSize());
        return value;
    }
    private QueryWrapper<FlowSubprocessChild> tenant(String tenantId) {
        QueryWrapper<FlowSubprocessChild> query = new QueryWrapper<>();
        return tenantId == null ? query.isNull("tenant_id") : query.eq("tenant_id", tenantId);
    }
}
