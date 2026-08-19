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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
import com.luokuiai.flovira.orm.entity.FlowTask;
import com.luokuiai.flovira.orm.mapper.FlowTaskMapper;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
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
        return getMapper().delete(new LambdaQueryWrapper<FlowTask>().in(FlowTask::getInstanceId, instanceIds));
    }

    @Override
    public List<FlowTask> getByInsIdAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        LambdaQueryWrapper<FlowTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.in(FlowTask::getNodeCode, nodeCodes);
        return getMapper().selectList(queryWrapper);
    }

    @Override
    public List<FlowTask> getByInsIdAndNodeType(Long instanceId, Integer nodeType) {
        LambdaQueryWrapper<FlowTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.eq(FlowTask::getNodeType, nodeType);
        return getMapper().selectList(queryWrapper);
    }

    @Override
    public List<FlowTask> listByInsIds(List<Long> instanceIds) {
        return getMapper().selectList(new LambdaQueryWrapper<FlowTask>()
            .in(FlowTask::getInstanceId, instanceIds));
    }

    @Override
    public List<FlowTask> listDueTimeoutTasks(Date dueBefore, Date staleBefore, int limit) {
        LambdaQueryWrapper<FlowTask> pending = new LambdaQueryWrapper<FlowTask>()
            .eq(FlowTask::getTimeoutStatus, "PENDING")
            .le(FlowTask::getTimeoutAt, dueBefore);
        LambdaQueryWrapper<FlowTask> stale = new LambdaQueryWrapper<FlowTask>()
            .eq(FlowTask::getTimeoutStatus, "RUNNING")
            .le(FlowTask::getTimeoutAt, dueBefore)
            .le(FlowTask::getTimeoutClaimedAt, staleBefore);
        List<FlowTask> tasks = new ArrayList<>(getMapper().selectList(pending));
        tasks.addAll(getMapper().selectList(stale));
        tasks.sort(Comparator.comparing(FlowTask::getTimeoutAt).thenComparing(FlowTask::getId));
        return tasks.size() <= limit ? tasks : tasks.subList(0, limit);
    }

    @Override
    public int claimTimeout(Long taskId, Date claimedAt, Date staleBefore) {
        LambdaUpdateWrapper<FlowTask> pending = new LambdaUpdateWrapper<FlowTask>()
            .set(FlowTask::getTimeoutStatus, "RUNNING")
            .set(FlowTask::getTimeoutClaimedAt, claimedAt)
            .eq(FlowTask::getId, taskId)
            .eq(FlowTask::getTimeoutStatus, "PENDING");
        int updated = getMapper().update(null, pending);
        if (updated == 1) {
            return updated;
        }
        LambdaUpdateWrapper<FlowTask> stale = new LambdaUpdateWrapper<FlowTask>()
            .set(FlowTask::getTimeoutStatus, "RUNNING")
            .set(FlowTask::getTimeoutClaimedAt, claimedAt)
            .eq(FlowTask::getId, taskId)
            .eq(FlowTask::getTimeoutStatus, "RUNNING")
            .le(FlowTask::getTimeoutClaimedAt, staleBefore);
        return getMapper().update(null, stale);
    }

    @Override
    public int claimWait(Long taskId, Date claimedAt) {
        LambdaUpdateWrapper<FlowTask> update = new LambdaUpdateWrapper<FlowTask>()
            .set(FlowTask::getTimeoutStatus, "RUNNING")
            .set(FlowTask::getTimeoutClaimedAt, claimedAt)
            .eq(FlowTask::getId, taskId)
            .eq(FlowTask::getNodeType, NodeType.WAIT.getKey())
            .and(wrapper -> wrapper.isNull(FlowTask::getTimeoutStatus)
                .or().eq(FlowTask::getTimeoutStatus, "PENDING"));
        return getMapper().update(null, update);
    }

    @Override
    public int releaseTimeout(Long taskId) {
        LambdaUpdateWrapper<FlowTask> update = new LambdaUpdateWrapper<FlowTask>()
            .set(FlowTask::getTimeoutStatus, "PENDING")
            .set(FlowTask::getTimeoutClaimedAt, null)
            .eq(FlowTask::getId, taskId)
            .eq(FlowTask::getTimeoutStatus, "RUNNING");
        return getMapper().update(null, update);
    }
}
