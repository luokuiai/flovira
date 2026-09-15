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
package com.luokuiai.flovira.core.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程包导入结果，返回新定义及表单引用，供宿主审核和发布。
 *
 * @author LuokuiAI
 */
@Data
@Accessors(chain = true)
public class WorkflowImportResult {
    private Long rootDefinitionId;
    /** 流程编码到新定义 ID，按子流程在前的依赖顺序排列。 */
    private Map<String, Long> definitionIds = new LinkedHashMap<>();
    /** 包内表单引用到目标表单引用，包含宿主显式提供的映射。 */
    private Map<String, String> formReferences = new LinkedHashMap<>();
}
