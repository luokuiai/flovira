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

import com.luokuiai.flovira.core.entity.SubprocessRun;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 子流程运行聚合。 @author warm */
@Data
@Accessors(chain = true)
public class FlowSubprocessRun implements SubprocessRun {
    private Long id;
    private Date createTime;
    private Date updateTime;
    private String createBy;
    private String updateBy;
    private String tenantId;
    private String delFlag;
    private Long parentInstanceId;
    private Long parentTaskId;
    private Long parentDefinitionId;
    private String parentNodeCode;
    private String childFlowCode;
    private Long childDefinitionId;
    private String childDefinitionVersion;
    private String completionPolicy;
    private String collectionFingerprint;
    private Integer expectedCount;
    private Integer pendingCount;
    private Integer runningCount;
    private Integer completedCount;
    private Integer failedCount;
    private Integer cancelledCount;
    private String runStatus;
    private String failureCode;
    private Integer lockVersion;
    private Date initializedAt;
    private Date completedAt;
}
