/*
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
package com.luokuiai.flovira.orm.entity;

import com.luokuiai.flovira.core.entity.NodeExecution;
import lombok.Data;
import lombok.experimental.Accessors;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

/** 节点执行持久化实体。 */
@Data
@Accessors(chain = true)
@TableName("flow_node_execution")
public class FlowNodeExecution implements NodeExecution {
    @TableId private Long id;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;
    private String createdBy;
    private String updatedBy;
    private String tenantId;
    @TableLogic(value = "0", delval = "1") private String deleted = "0";
    private Long instanceId;
    private Long definitionId;
    private String nodeCode;
    private Integer nodeType;
    private String state;
    private java.util.Date enteredAt;
    private java.util.Date closedAt;
    private String closeReason;
    private Integer version = 0;
}
