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

import com.easy.query.core.expression.lambda.SQLActionExpression1;
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.orm.entity.FlowTask;
import com.luokuiai.flovira.orm.entity.proxy.FlowTaskProxy;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

/**
 * 待办任务Mapper接口
 * @author link2fun
 */
public class FlowTaskDaoImpl extends FloviraDaoImpl<FlowTask, FlowTaskProxy> implements FlowTaskDao<FlowTask> {

    @Override
    public FlowTask newEntity() {
        return new FlowTask();
    }

    @Override
    public int deleteByInsIds(List<Long> instanceIds) {
        return (int) deletable()
            .where(proxy -> {
                proxy.instanceId().in(instanceIds);
            })
            .executeRows();

    }

    @Override
    public int delete(FlowTask entity) {
        return (int) deletable()
            .where(buildWhereCondition(entity))
            .executeRows();

    }

    @Override
    public List<FlowTask> getByInsIdAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        return queryable()
                .where(proxy -> {
                    proxy.instanceId().eq(instanceId);
                    proxy.nodeCode().in(nodeCodes);
                }).toList();
    }

    @Override
    public List<FlowTask> getByInsIdAndNodeType(Long instanceId, Integer nodeType) {
        return queryable()
                .where(proxy -> {
                    proxy.instanceId().eq(instanceId);
                    proxy.nodeType().eq(nodeType);
                }).toList();
    }

    @Override
    public List<FlowTask> listByInsIds(List<Long> instanceIds) {
        return queryable().where(proxy -> proxy.instanceId().in(instanceIds)).toList();
    }

    @Override
    public List<FlowTask> listDueTimeoutTasks(Date dueBefore, Date staleBefore, int limit) {
        List<FlowTask> tasks = new ArrayList<>();
        tasks.addAll(queryable().where(proxy -> {
            proxy.timeoutStatus().eq("PENDING");
            proxy.timeoutAt().le(dueBefore);
        }).toList());
        tasks.addAll(queryable().where(proxy -> {
            proxy.timeoutStatus().eq("RUNNING");
            proxy.timeoutAt().le(dueBefore);
            proxy.timeoutClaimedAt().le(staleBefore);
        }).toList());
        tasks.sort(Comparator.comparing(FlowTask::getTimeoutAt).thenComparing(FlowTask::getId));
        return tasks.size() <= limit ? tasks : tasks.subList(0, limit);
    }

    @Override
    public int claimTimeout(Long taskId, Date claimedAt, Date staleBefore) {
        long updated = updatable().where(proxy -> {
            proxy.id().eq(taskId);
            proxy.timeoutStatus().eq("PENDING");
        }).setColumns(proxy -> {
            proxy.timeoutStatus().set("RUNNING");
            proxy.timeoutClaimedAt().set(claimedAt);
        }).executeRows();
        if (updated == 1) {
            return 1;
        }
        return (int) updatable().where(proxy -> {
            proxy.id().eq(taskId);
            proxy.timeoutStatus().eq("RUNNING");
            proxy.timeoutClaimedAt().le(staleBefore);
        }).setColumns(proxy -> proxy.timeoutClaimedAt().set(claimedAt)).executeRows();
    }

    @Override
    public int claimWait(Long taskId, Date claimedAt) {
        long updated = updatable().where(proxy -> {
            proxy.id().eq(taskId);
            proxy.nodeType().eq(NodeType.WAIT.getKey());
            proxy.timeoutStatus().eq("PENDING");
        }).setColumns(proxy -> {
            proxy.timeoutStatus().set("RUNNING");
            proxy.timeoutClaimedAt().set(claimedAt);
        }).executeRows();
        if (updated == 1) {
            return 1;
        }
        return (int) updatable().where(proxy -> {
            proxy.id().eq(taskId);
            proxy.nodeType().eq(NodeType.WAIT.getKey());
            proxy.timeoutStatus().isNull();
        }).setColumns(proxy -> {
            proxy.timeoutStatus().set("RUNNING");
            proxy.timeoutClaimedAt().set(claimedAt);
        }).executeRows();
    }

    @Override
    public int releaseTimeout(Long taskId) {
        return (int) updatable().where(proxy -> {
            proxy.id().eq(taskId);
            proxy.timeoutStatus().eq("RUNNING");
        }).setColumns(proxy -> {
            proxy.timeoutStatus().set("PENDING");
            proxy.timeoutClaimedAt().set((Date) null);
        }).executeRows();
    }

    /** 参照 mybatis 实现， 构建删除条件 */
    @Override
    public SQLActionExpression1<FlowTaskProxy> buildWhereCondition(FlowTask entity) {
        return o -> {
            o.id().eq(Objects.nonNull(entity.getId()), entity.getId());
            o.nodeCode().eq(StringUtils.isNotEmpty(entity.getNodeCode()), entity.getNodeCode());
            o.nodeName().eq(StringUtils.isNotEmpty(entity.getNodeName()), entity.getNodeName());
            o.nodeType().eq(Objects.nonNull(entity.getNodeType()), entity.getNodeType());
            o.definitionId().eq(Objects.nonNull(entity.getDefinitionId()), entity.getDefinitionId());
            o.instanceId().eq(Objects.nonNull(entity.getInstanceId()), entity.getInstanceId());
            o.formCustom().eq(StringUtils.isNotEmpty(entity.getFormCustom()), entity.getFormCustom());
            o.formPath().eq(StringUtils.isNotEmpty(entity.getFormPath()), entity.getFormPath());
            o.createTime().eq(Objects.nonNull(entity.getCreateTime()), entity.getCreateTime());
            o.updateTime().eq(Objects.nonNull(entity.getUpdateTime()), entity.getUpdateTime());
            o.tenantId().eq(StringUtils.isNotEmpty(entity.getTenantId()), entity.getTenantId());
            o.createBy().eq(StringUtils.isNotEmpty(entity.getCreateBy()), entity.getCreateBy());
            o.updateBy().eq(StringUtils.isNotEmpty(entity.getUpdateBy()), entity.getUpdateBy());
            o.flowStatus().eq(StringUtils.isNotEmpty(entity.getFlowStatus()), entity.getFlowStatus());
        };

    }
}
