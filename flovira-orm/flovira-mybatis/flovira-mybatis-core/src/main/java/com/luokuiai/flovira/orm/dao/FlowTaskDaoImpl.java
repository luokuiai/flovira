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
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.orm.entity.FlowTask;
import com.luokuiai.flovira.orm.mapper.FlowTaskMapper;
import com.luokuiai.flovira.orm.utils.TenantDeleteUtil;

import java.util.List;
import java.util.Date;

/**
 * 待办任务Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowTaskDaoImpl extends FloviraDaoImpl<FlowTask> implements FlowTaskDao<FlowTask> {

    @Override
    public FlowTaskMapper getMapper() {
        return FrameInvoker.getBean(FlowTaskMapper.class);
    }

    @Override
    public FlowTask newEntity() {
        return new FlowTask();
    }

    /**
     * 根据instanceIds删除
     *
     * @param instanceIds 主键
     * @return 结果
     */
    @Override
    public int deleteByInsIds(List<Long> instanceIds) {
        FlowTask entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            return getMapper().updateByInsIdsLogic(instanceIds, entity, FlowEngine.getFlowConfig().getLogicDeleteValue(), entity.getDelFlag());
        }
        return getMapper().deleteByInsIds(instanceIds, entity);
    }

    @Override
    public List<FlowTask> getByInsIdAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        return getMapper().getByInsIdAndNodeCodes(instanceId, nodeCodes, TenantDeleteUtil.getEntity(newEntity()));
    }

    @Override
    public List<FlowTask> getByInsIdAndNodeType(Long instanceId, Integer nodeType) {
        return getMapper().getByInsIdAndNodeType(instanceId, nodeType, TenantDeleteUtil.getEntity(newEntity()));
    }

    @Override
    public List<FlowTask> listByInsIds(List<Long> instanceIds) {
        return getMapper().listByInsIds(instanceIds, TenantDeleteUtil.getEntity(newEntity()));
    }

    @Override
    public List<FlowTask> listDueTimeoutTasks(Date dueBefore, Date staleBefore, int limit) {
        List<FlowTask> tasks = getMapper().listDueTimeoutTasks(
            dueBefore, staleBefore, TenantDeleteUtil.getEntity(newEntity()));
        return tasks.size() <= limit ? tasks : tasks.subList(0, limit);
    }

    @Override
    public int claimTimeout(Long taskId, Date claimedAt, Date staleBefore) {
        FlowTask entity = TenantDeleteUtil.getEntity(newEntity());
        int updated = getMapper().claimPendingTimeout(taskId, claimedAt, entity);
        return updated == 1 ? updated : getMapper().claimStaleTimeout(taskId, claimedAt, staleBefore, entity);
    }

    @Override
    public int claimWait(Long taskId, Date claimedAt) {
        return getMapper().claimWait(taskId, claimedAt, TenantDeleteUtil.getEntity(newEntity()));
    }

    @Override
    public int releaseTimeout(Long taskId) {
        return getMapper().releaseTimeout(taskId, TenantDeleteUtil.getEntity(newEntity()));
    }
}
