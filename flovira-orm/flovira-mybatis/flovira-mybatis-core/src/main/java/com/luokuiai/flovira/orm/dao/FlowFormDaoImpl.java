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
import com.luokuiai.flovira.core.orm.dao.FlowFormDao;
import com.luokuiai.flovira.orm.entity.FlowForm;
import com.luokuiai.flovira.orm.mapper.FlowFormMapper;
import com.luokuiai.flovira.orm.utils.TenantDeleteUtil;

import java.util.List;

/**
 * @author vanlin
 * @className FlowFormDaoImpl
 * @description
 * @since 2024/8/19 14:29
 */
public class FlowFormDaoImpl extends FloviraDaoImpl<FlowForm> implements FlowFormDao<FlowForm> {
    @Override
    public FlowFormMapper getMapper() {
        return FrameInvoker.getBean(FlowFormMapper.class);
    }

    @Override
    public FlowForm newEntity() {
        return new FlowForm();
    }

    @Override
    public List<FlowForm> queryByCodeList(List<String> formCodeList) {
        return getMapper().queryByCodeList(formCodeList, TenantDeleteUtil.getEntity(newEntity()));
    }
}
