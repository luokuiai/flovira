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

import com.luokuiai.flovira.core.entity.SubprocessEvent;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 子流程编排事件。 @author warm */
@Data
@Accessors(chain = true)
public class FlowSubprocessEvent implements SubprocessEvent {
    private Long id;
    private Date createTime;
    private Date updateTime;
    private String createBy;
    private String updateBy;
    private String tenantId;
    private String delFlag;
    private Long runId;
    private Long childId;
    private Long parentInstanceId;
    private Long childInstanceId;
    private String parentNodeCode;
    private String eventType;
    private String eventResult;
    private String reason;
    private Date occurredAt;
}
