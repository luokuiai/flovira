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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.SubprocessConfig;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.ActivityStatus;
import com.luokuiai.flovira.core.enums.NodeType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 固定子流程依赖图校验器
 *
 * @author warm
 */
public final class SubprocessDefinitionValidator {

    private SubprocessDefinitionValidator() {
    }

    public static void validateNodeConfigs(List<Node> nodes) {
        for (Node node : nodes) {
            if (NodeType.isSubProcess(node.getNodeType())) {
                SubprocessConfigUtil.read(node);
            }
        }
    }

    public static void validateForPublish(Definition definition) {
        Set<String> path = new HashSet<>();
        path.add(definition.getFlowCode());
        validateChildren(definition, path);
    }

    private static void validateChildren(Definition parent, Set<String> path) {
        List<Node> nodes = FlowEngine.nodeService().getByDefId(parent.getId());
        for (Node node : nodes) {
            if (!NodeType.isSubProcess(node.getNodeType())) {
                continue;
            }
            SubprocessConfig config = SubprocessConfigUtil.read(node);
            String childCode = config.getFixedChildFlowCode();
            if (!path.add(childCode)) {
                throw new IllegalStateException("Subprocess dependency cycle: " + childCode);
            }
            Definition child = FlowEngine.defService().getPublishByFlowCode(childCode);
            if (child == null || !ActivityStatus.isActivity(child.getActivityStatus())) {
                throw new IllegalStateException("Runnable subprocess definition not found: " + childCode);
            }
            if (!Objects.equals(tenant(parent.getTenantId()), tenant(child.getTenantId()))) {
                throw new IllegalStateException("Subprocess definition belongs to another tenant: " + childCode);
            }
            validateChildren(child, path);
            path.remove(childCode);
        }
    }

    private static String tenant(String value) {
        return StringUtils.isEmpty(value) ? "0" : value;
    }
}
