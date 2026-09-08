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

import java.util.Date;
import java.util.List;

/**
 * 待办任务记录对象 flow_task
 *
 * @author warm
 * @since 2023-03-29
 */
public interface Task extends RootEntity {

    @Override
    Long getId();

    @Override
    Task setId(Long id);

    @Override
    Date getCreatedAt();

    @Override
    Task setCreatedAt(Date createdAt);

    @Override
    Date getUpdatedAt();

    @Override
    Task setUpdatedAt(Date updatedAt);

    @Override
    String getCreatedBy();

    @Override
    Task setCreatedBy(String createdBy);

    @Override
    String getUpdatedBy();

    @Override
    Task setUpdatedBy(String updatedBy);

    @Override
    String getTenantId();

    @Override
    Task setTenantId(String tenantId);

    @Override
    String getDeleted();

    @Override
    Task setDeleted(String deleted);

    /**
     * 获取流程定义ID
     * @return 流程定义ID
     */
    Long getDefinitionId();

    Task setDefinitionId(Long definitionId);

    /**
     * 获取流程实例ID
     * @return 流程实例ID
     */
    Long getInstanceId();

    Task setInstanceId(Long instanceId);

    String getFlowName();

    Task setFlowName(String flowName);

    /**
     * 获取业务ID
     * @return 业务ID
     */
    String getBusinessId();

    Task setBusinessId(String businessId);

    String getNodeCode();

    Task setNodeCode(String nodeCode);

    String getNodeName();

    Task setNodeName(String nodeName);

    Integer getNodeType();

    Task setNodeType(Integer nodeType);

    /**
     * 获取流程状态
     * @see com.luokuiai.flovira.core.enums.FlowStatus
     * @return 流程状态
     */
    String getFlowStatus();

    Task setFlowStatus(String flowStatus);

    List<String> getPermissionList();

    Task setPermissionList(List<String> permissionList);

    List<User> getUserList();

    Task setUserList(List<User> userList);

    String getFormId();

    Task setFormId(String formId);

    Date getTimeoutAt();

    Task setTimeoutAt(Date timeoutAt);

    String getTimeoutAction();

    Task setTimeoutAction(String timeoutAction);

    String getTimeoutConfig();

    Task setTimeoutConfig(String timeoutConfig);

    String getTimeoutStatus();

    Task setTimeoutStatus(String timeoutStatus);

    Date getTimeoutClaimedAt();

    Task setTimeoutClaimedAt(Date timeoutClaimedAt);
}
