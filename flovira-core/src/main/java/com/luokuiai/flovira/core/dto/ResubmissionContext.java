/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luokuiai.flovira.core.dto;

import lombok.Data;

/** 引擎持久化的退回上下文，不从调用方变量读取。 */
@Data
public class ResubmissionContext {
    private int schemaVersion = 1;
    private Long definitionId;
    private Long sourceTaskId;
    private String sourceNodeCode;
    private Long initiatorTaskId;
    private String strategy;
}
