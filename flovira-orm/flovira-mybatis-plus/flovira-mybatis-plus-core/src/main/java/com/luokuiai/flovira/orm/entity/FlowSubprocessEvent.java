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
package com.luokuiai.flovira.orm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 子流程编排事件。 @author warm */
@Data @Accessors(chain = true) @TableName("flow_subprocess_event")
public class FlowSubprocessEvent implements SubprocessEvent {
    @TableId private Long id;
    @TableField(fill = FieldFill.INSERT) private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Date updateTime;
    private String createBy; private String updateBy; private String tenantId;
    @TableLogic(value = "0", delval = "1") private String delFlag;
    private Long runId; private Long childId; private Long parentInstanceId; private Long childInstanceId;
    private String parentNodeCode; private String eventType; private String eventResult; private String reason;
    private Date occurredAt;
}
