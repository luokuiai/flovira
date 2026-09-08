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
package com.luokuiai.flovira.core.entity;

import com.luokuiai.flovira.core.FlowEngine;

import java.util.Date;

/**
 * 节点跳转关联对象 flow_skip
 *
 * @author warm
 * @since 2023-03-29
 */
public interface Skip extends RootEntity {

    @Override
    Long getId();

    @Override
    Skip setId(Long id);

    @Override
    Date getCreatedAt();

    @Override
    Skip setCreatedAt(Date createdAt);

    @Override
    Date getUpdatedAt();

    @Override
    Skip setUpdatedAt(Date updatedAt);

    @Override
    String getCreatedBy();

    @Override
    Skip setCreatedBy(String createdBy);

    @Override
    String getUpdatedBy();

    @Override
    Skip setUpdatedBy(String updatedBy);

    @Override
    String getTenantId();

    @Override
    Skip setTenantId(String tenantId);

    @Override
    String getDeleted();

    @Override
    Skip setDeleted(String deleted);

    Long getDefinitionId();

    Skip setDefinitionId(Long definitionId);

    Long getNodeId();

    Skip setNodeId(Long nodeId);

    String getSourceNodeCode();

    Skip setSourceNodeCode(String sourceNodeCode);

    Integer getSourceNodeType();

    Skip setSourceNodeType(Integer sourceNodeType);

    String getTargetNodeCode();

    Skip setTargetNodeCode(String targetNodeCode);

    Integer getTargetNodeType();

    Skip setTargetNodeType(Integer targetNodeType);

    String getSkipName();

    Skip setSkipName(String skipName);

    String getSkipType();

    Skip setSkipType(String skipType);

    String getSkipCondition();

    Skip setSkipCondition(String skipCondition);

    String getCoordinate();

    Skip setCoordinate(String coordinate);

    default Skip copy() {
        return FlowEngine.newSkip()
            .setTenantId(getTenantId())
            .setDeleted(getDeleted())
            .setDefinitionId(getDefinitionId())
            .setSourceNodeCode(getSourceNodeCode())
            .setSourceNodeType(getSourceNodeType())
            .setTargetNodeCode(getTargetNodeCode())
            .setTargetNodeType(getTargetNodeType())
            .setSkipName(getSkipName())
            .setSkipType(getSkipType())
            .setSkipCondition(getSkipCondition())
            .setCoordinate(getCoordinate());
    }

}
