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

import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowDefinitionDao;
import com.luokuiai.flovira.orm.entity.FlowDefinition;
import com.luokuiai.flovira.orm.mapper.FlowDefinitionMapper;
import com.luokuiai.flovira.orm.utils.TenantDeleteUtil;

import java.util.List;

/**
 * 流程定义Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowDefinitionDaoImpl extends FloviraDaoImpl<FlowDefinition> implements FlowDefinitionDao<FlowDefinition> {

    @Override
    public FlowDefinitionMapper getMapper() {
        return FrameInvoker.getBean(FlowDefinitionMapper.class);
    }

    @Override
    public FlowDefinition newEntity() {
        return new FlowDefinition();
    }

    @Override
    public List<FlowDefinition> queryByCodeList(List<String> flowCodeList) {
        return getMapper().queryByCodeList(flowCodeList, TenantDeleteUtil.getEntity(newEntity()));
    }

    @Override
    public void updatePublishStatus(List<Long> ids, Integer publishStatus) {
        getMapper().updatePublishStatus(ids, publishStatus, TenantDeleteUtil.getEntity(newEntity()));
    }

}
