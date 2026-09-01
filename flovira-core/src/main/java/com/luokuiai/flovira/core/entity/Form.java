/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *    Copyright 2026, LuokuiAI (luokuiai@gmail.com).
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

/**
 * 流程表单 flow_form
 *
 * @author vanlin
 * @since 2024/8/19 9:59
 */
public interface Form extends RootEntity {

    @Override
    Long getId();

    @Override
    Form setId(Long id);

    @Override
    Date getCreateTime();

    @Override
    Form setCreateTime(Date createTime);

    @Override
    Date getUpdateTime();

    @Override
    Form setUpdateTime(Date updateTime);

    @Override
    String getCreateBy();

    @Override
    Form setCreateBy(String createBy);

    @Override
    String getUpdateBy();

    @Override
    Form setUpdateBy(String updateBy);

    @Override
    String getTenantId();

    @Override
    Form setTenantId(String tenantId);

    @Override
    String getDelFlag();

    @Override
    Form setDelFlag(String delFlag);

    /**
     * 获取表单编码
     * @return 表单编码
     */
    String getFormCode();

    Form setFormCode(String formCode);

    String getFormName();

    Form setFormName(String formName);

    String getVersion();

    Form setVersion(String version);

    /**
     * 是否发布（0未发布 1已发布 9失效）
     */
    Integer getIsPublish();

    Form setIsPublish(Integer isPublish);

    /**
     * 表单类型（0内置表单，1外挂表单）。外挂表单可同时保存字段定义和业务页面路径。
     */
    Integer getFormType();

    Form setFormType(Integer formType);

    /**
     * 表单定义内容，标准结构见 {@link com.luokuiai.flovira.core.dto.FormDefinition}，
     * 旧的自定义内容仍可保存。
     */
    String getFormContent();

    Form setFormContent(String formContent);

    String getFormPath();

    Form setFormPath(String formPath);

    String getExt();

    Form setExt(String ext);
}
