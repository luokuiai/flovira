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
package com.luokuiai.flovira.core.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 父流程、编排事件与指定子流程的统一历史条目
 *
 * @author warm
 */
@Data
public class SubprocessHistoryEntry implements Serializable {
    private String source;
    private Date occurredAt;
    private Long runId;
    private Long childId;
    private String itemLabel;
    private Long instanceId;
    private String nodeCode;
    private String nodeName;
    private String action;
    private String outcome;
    private String actor;
    private String message;
}
