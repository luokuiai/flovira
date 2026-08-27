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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.constant.ExceptionCons;
import com.luokuiai.flovira.core.dto.FlowCombine;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.dto.ProgressNode;
import com.luokuiai.flovira.core.dto.ProgressResult;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.service.ProgressService;
import com.luokuiai.flovira.core.utils.ApproverRuleUtil;
import com.luokuiai.flovira.core.utils.AssertUtil;
import com.luokuiai.flovira.core.utils.CollUtil;
import com.luokuiai.flovira.core.utils.ExpressionUtil;
import com.luokuiai.flovira.core.utils.MapUtil;
import com.luokuiai.flovira.core.utils.StreamUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程节点审批人预计算服务实现。
 *
 * @author warm
 * @since 2026/8/25
 */
public class ProgressServiceImpl implements ProgressService {

    @Override
    public ProgressResult previewByDefinitionId(Long definitionId, Map<String, Object> variables) {
        AssertUtil.isNull(definitionId, ExceptionCons.NOT_DEFINITION_ID);
        Definition definition = FlowEngine.defService().getById(definitionId);
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);
        return preview(definitionId, null, startNode(definitionId),
            Collections.<String, Object>emptyMap(), variables);
    }

    @Override
    public ProgressResult previewByFlowCode(String flowCode, Map<String, Object> variables) {
        Definition definition = FlowEngine.defService().getPublishByFlowCode(flowCode);
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);
        return preview(definition.getId(), null, startNode(definition.getId()),
            Collections.<String, Object>emptyMap(), variables);
    }

    @Override
    public ProgressResult previewByInstanceId(Long instanceId, Map<String, Object> variables) {
        AssertUtil.isNull(instanceId, ExceptionCons.NULL_INSTANCE_ID);
        Instance instance = FlowEngine.insService().getById(instanceId);
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        Node sourceNode = FlowEngine.nodeService()
            .getByDefIdAndNodeCode(instance.getDefinitionId(), instance.getNodeCode());
        AssertUtil.isNull(sourceNode, ExceptionCons.LOST_CUR_NODE);
        return preview(instance.getDefinitionId(), instanceId, sourceNode, instance.getVariableMap(), variables);
    }

    private ProgressResult preview(Long definitionId, Long instanceId, Node sourceNode,
                                   Map<String, Object> baseVariables, Map<String, Object> variables) {
        FlowParams previewParams = new FlowParams().variable(MapUtil.mergeAll(baseVariables, variables));
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(definitionId);
        List<ProgressNode> progressNodes = calculate(sourceNode, previewParams, flowCombine);
        return new ProgressResult()
            .setDefinitionId(definitionId)
            .setInstanceId(instanceId)
            .setSourceNodeCode(sourceNode.getNodeCode())
            .setNodes(progressNodes);
    }

    private List<ProgressNode> calculate(Node sourceNode, FlowParams flowParams, FlowCombine flowCombine) {
        List<ProgressNode> result = new ArrayList<>();
        if (NodeType.isEnd(sourceNode.getNodeType())) {
            return result;
        }

        Deque<Node> pending = new ArrayDeque<>();
        addAll(pending, nextNodes(sourceNode, flowParams, flowCombine));
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            Node node = pending.removeFirst();
            if (node == null || !visited.add(node.getNodeCode())) {
                continue;
            }
            if (NodeType.isWorkNode(node.getNodeType())) {
                result.add(toProgressNode(node, flowParams));
            }
            if (!NodeType.isEnd(node.getNodeType())) {
                addAll(pending, nextNodes(node, flowParams, flowCombine));
            }
        }
        return result;
    }

    private List<Node> nextNodes(Node node, FlowParams flowParams, FlowCombine flowCombine) {
        return FlowEngine.nodeService().getNextNodeList(node, null, SkipType.PASS.getKey(),
            flowParams.getVariable(), null, flowCombine);
    }

    private ProgressNode toProgressNode(Node node, FlowParams flowParams) {
        List<String> handlers = Collections.emptyList();
        if (!NodeType.isWait(node.getNodeType())) {
            Task task = FlowEngine.newTask().setPermissionList(NodeType.isCarbonCopy(node.getNodeType())
                ? ApproverRuleUtil.resolveCarbonCopy(node, flowParams)
                : ApproverRuleUtil.resolve(node, flowParams));
            ExpressionUtil.evalVariable(Collections.singletonList(task), flowParams);
            handlers = new ArrayList<>(task.getPermissionList());
        }
        return new ProgressNode()
            .setNodeCode(node.getNodeCode())
            .setNodeName(node.getNodeName())
            .setNodeType(node.getNodeType())
            .setHandlers(handlers);
    }

    private Node startNode(Long definitionId) {
        Node startNode = FlowEngine.nodeService().getStartNode(definitionId);
        AssertUtil.isNull(startNode, ExceptionCons.LOST_START_NODE);
        return startNode;
    }

    private void addAll(Deque<Node> pending, List<Node> nodes) {
        if (CollUtil.isNotEmpty(nodes)) {
            pending.addAll(StreamUtils.filter(nodes, node -> node != null));
        }
    }
}
