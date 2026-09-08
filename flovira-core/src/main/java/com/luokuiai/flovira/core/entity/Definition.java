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
import java.util.List;

/**
 * 流程定义对象 flow_definition
 *
 * @author warm
 * @since 2023-03-29
 */
public interface Definition extends RootEntity {

    @Override
    Long getId();

    @Override
    Definition setId(Long id);

    @Override
    Date getCreatedAt();

    @Override
    Definition setCreatedAt(Date createdAt);

    @Override
    Date getUpdatedAt();

    @Override
    Definition setUpdatedAt(Date updatedAt);

    @Override
    String getCreatedBy();

    @Override
    Definition setCreatedBy(String createdBy);

    @Override
    String getUpdatedBy();

    @Override
    Definition setUpdatedBy(String updatedBy);

    @Override
    String getTenantId();

    @Override
    Definition setTenantId(String tenantId);

    @Override
    String getDeleted();

    @Override
    Definition setDeleted(String deleted);

    /**
     * 获取流程编码
     * @return 流程编码
     */
    String getFlowCode();

    /**
     * 设置流程编码
     * @param flowCode flowCode
     * @return Definition
     */
    Definition setFlowCode(String flowCode);

    /**
     * 获取流程名称
     * @return 流程名称
     */
    String getFlowName();

    /**
     * 设置流程名称
     * @param flowName flowName
     * @return Definition
     */
    Definition setFlowName(String flowName);

    String getCategory();

    Definition setCategory(String category);

    /**
     * 获取流程定义的版本号
     * @return 版本号
     */
    String getVersion();

    Definition setVersion(String version);

    /**
     * 获取是否发布状态 (0未发布 1已发布 9已失效)
     * @return 发布状态
     */
    Integer getPublishStatus();

    Definition setPublishStatus(Integer publishStatus);

    /**
     * 审批表单是否自定义（Y=是 N=否）
     * @return 是否自定义
     */
    String getFormCustom();

    Definition setFormCustom(String formCustom);

    String getFormPath();

    Definition setFormPath(String formPath);

    String getExt();

    Definition setExt(String ext);

    List<Node> getNodeList();

    Definition setNodeList(List<Node> nodeList);

    List<User> getUserList();

    Definition setUserList(List<User> userList);

    /**
     * 流程激活状态（0=挂起 1=激活）
     * @see com.luokuiai.flovira.core.enums.ActivityStatus
     * @return 流程激活状态
     */
    Integer getActivityStatus();

    Definition setActivityStatus(Integer activityStatus);

    /**
     * 获取监听器类型
     * @return 监听器类型
     */
    String getListenerType();

    Definition setListenerType(String listenerType);

    /**
     * 获取监听器路径
     * @return 监听器路径
     */
    String getListenerPath();

    Definition setListenerPath(String listenerPath);

    default Definition copy() {
        return FlowEngine.newDef()
            .setTenantId(this.getTenantId())
            .setDeleted(this.getDeleted())
            .setFlowCode(this.getFlowCode())
            .setFlowName(this.getFlowName())
            .setCategory(this.getCategory())
            .setVersion(this.getVersion())
            .setFormCustom(this.getFormCustom())
            .setFormPath(this.getFormPath())
            .setListenerType(this.getListenerType())
            .setListenerPath(this.getListenerPath())
            .setExt(this.getExt())
            .setCreatedBy(this.getCreatedBy())
            .setUpdatedBy(this.getUpdatedBy());

    }
}
