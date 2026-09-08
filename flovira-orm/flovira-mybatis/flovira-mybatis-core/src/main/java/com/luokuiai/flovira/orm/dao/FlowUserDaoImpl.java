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
import com.luokuiai.flovira.core.orm.dao.FlowUserDao;
import com.luokuiai.flovira.core.utils.CollUtil;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.orm.entity.FlowUser;
import com.luokuiai.flovira.orm.mapper.FlowUserMapper;
import com.luokuiai.flovira.orm.utils.TenantDeleteUtil;

import java.util.List;

/**
 * 流程用户Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowUserDaoImpl extends FloviraDaoImpl<FlowUser> implements FlowUserDao<FlowUser> {

    @Override
    public FlowUserMapper getMapper() {
        return FrameInvoker.getBean(FlowUserMapper.class);
    }

    @Override
    public FlowUser newEntity() {
        return new FlowUser();
    }

    @Override
    public int deleteByTaskIds(List<Long> taskIdList) {
        FlowUser entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDeleted())) {
            return getMapper().updateByTaskIdsLogic(taskIdList, entity, FlowEngine.getFlowConfig().getLogicDeleteValue(),
                entity.getDeleted());
        }
        return getMapper().deleteByTaskIds(taskIdList, entity);
    }

    @Override
    public List<FlowUser> listByAssociatedIdsAndTypes(List<Long> associatedIds, String[] types) {
        String dataSourceType = FlowEngine.dataSourceType();
        if (CollUtil.isNotEmpty(associatedIds) && associatedIds.size() == 1) {
            return getMapper().listByAssociatedIdsAndTypes(types, null
                , TenantDeleteUtil.getEntity(newEntity()).setAssociatedId(associatedIds.get(0)), dataSourceType);
        }
        return getMapper().listByAssociatedIdsAndTypes(types, associatedIds
            , TenantDeleteUtil.getEntity(newEntity()), dataSourceType);
    }

    @Override
    public List<FlowUser> listByProcessedBys(Long associatedId, List<String> processedBys, String[] types) {
        String dataSourceType = FlowEngine.dataSourceType();
        if (CollUtil.isNotEmpty(processedBys) && processedBys.size() == 1) {
            return getMapper().listByProcessedBys(types, null, TenantDeleteUtil
                .getEntity(newEntity()).setAssociatedId(associatedId).setProcessedBy(processedBys.get(0)), dataSourceType);
        }
        return getMapper().listByProcessedBys(types, processedBys
            , TenantDeleteUtil.getEntity(newEntity()).setAssociatedId(associatedId), dataSourceType);
    }
}
