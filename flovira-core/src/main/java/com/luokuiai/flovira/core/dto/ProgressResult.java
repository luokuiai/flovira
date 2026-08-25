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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程节点进度预计算结果。
 *
 * @author warm
 * @since 2026/8/25
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProgressResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long definitionId;

    private Long instanceId;

    /**
     * 本次预计算的起始节点，不包含在 nodes 中。
     */
    private String sourceNodeCode;

    private List<ProgressNode> nodes = new ArrayList<>();
}
