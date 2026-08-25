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
package com.luokuiai.flovira.core.service;

import com.luokuiai.flovira.core.dto.ProgressResult;

import java.util.Map;

/**
 * 流程节点审批人预计算服务。
 *
 * @author warm
 * @since 2026/8/25
 */
public interface ProgressService {

    /**
     * 从流程定义的开始节点之后预计算流程节点和办理人。
     *
     * @param definitionId 流程定义id
     * @param variables    业务参数，直接参与条件分支和办理人表达式计算
     * @return 预计算结果
     */
    ProgressResult previewByDefinitionId(Long definitionId, Map<String, Object> variables);

    /**
     * 从已发布流程定义的开始节点之后预计算流程节点和办理人。
     *
     * @param flowCode   流程编码
     * @param variables 业务参数，直接参与条件分支和办理人表达式计算
     * @return 预计算结果
     */
    ProgressResult previewByFlowCode(String flowCode, Map<String, Object> variables);

    /**
     * 合并实例变量与本次流程参数，从实例当前节点之后预计算流程节点和办理人。
     *
     * @param instanceId 流程实例id
     * @param variables 业务参数，覆盖同名实例变量后参与条件分支和办理人表达式计算
     * @return 预计算结果
     */
    ProgressResult previewByInstanceId(Long instanceId, Map<String, Object> variables);
}
