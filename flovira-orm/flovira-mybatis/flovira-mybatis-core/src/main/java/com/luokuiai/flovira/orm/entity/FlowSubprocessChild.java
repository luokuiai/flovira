/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 */
package com.luokuiai.flovira.orm.entity;

import com.luokuiai.flovira.core.entity.SubprocessChild;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 子流程实例关系。 @author warm */
@Data
@Accessors(chain = true)
public class FlowSubprocessChild implements SubprocessChild {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;
    private String tenantId;
    private String deleted;
    private Long runId;
    private String itemKey;
    private String itemLabel;
    private String childBusinessKey;
    private String childFlowCode;
    private Long childDefinitionId;
    private String childDefinitionVersion;
    private Long childInstanceId;
    private String childStatus;
    private String outcome;
    private Date startedAt;
    private Date completedAt;
}
