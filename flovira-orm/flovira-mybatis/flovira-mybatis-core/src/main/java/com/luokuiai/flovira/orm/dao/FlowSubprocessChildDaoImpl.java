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
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.utils.page.Page;
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
        return mapper().findByChildInstanceId(tenantId, childInstanceId);
    }
    public FlowSubprocessChild findById(String tenantId, Long childId) { return mapper().findById(tenantId, childId); }
    public FlowSubprocessChild findByRunAndItem(String tenantId, Long runId, String itemKey) {
        return mapper().findByRunAndItem(tenantId, runId, itemKey);
    }
    public List<FlowSubprocessChild> lockByRunId(String tenantId, Long runId) {
        return mapper().lockByRunId(tenantId, runId, FlowEngine.dataSourceType());
    }
    public Page<FlowSubprocessChild> pageByRunId(String tenantId, Long runId, Page<FlowSubprocessChild> page) {
        List<FlowSubprocessChild> all = mapper().listByRunId(tenantId, runId);
        int from = Math.min((page.getPageNum() - 1) * page.getPageSize(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        Page<FlowSubprocessChild> result = new Page<>(new ArrayList<>(all.subList(from, to)), all.size());
        result.setPageNum(page.getPageNum());
        result.setPageSize(page.getPageSize());
        return result;
    }
}
