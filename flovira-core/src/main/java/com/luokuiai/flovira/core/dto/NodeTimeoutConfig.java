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
package com.luokuiai.flovira.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 节点超时配置
 *
 * @author warm
 */
@Data
public class NodeTimeoutConfig implements Serializable {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private boolean enabled;
    /** DURATION 或 FORM_FIELD；未指定时按固定时长处理。 */
    private String source = "DURATION";
    /** formData 内的字段路径，不含数组，例如 schedule.deadline。 */
    private String fieldCode;
    private String fieldLabel;
    private long duration;
    private String durationUnit;
    private String action;
}
