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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.dto.NodeTimeoutConfig;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.NodeType;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点扩展配置校验器
 *
 * @author warm
 */
public final class NodeConfigValidator {

    private NodeConfigValidator() {
    }

    public static void validate(List<Node> nodes) {
        if (nodes == null) {
            return;
        }
        Map<String, String> waitKeys = new HashMap<String, String>();
        for (Node node : nodes) {
            if (NodeType.isWait(node.getNodeType())) {
                String waitKey = WaitConfigUtil.read(node).getWaitKey();
                String previousNodeCode = waitKeys.put(waitKey, node.getNodeCode());
                if (previousNodeCode != null) {
                    throw new IllegalStateException("Duplicate waitKey " + waitKey + " on nodes "
                        + previousNodeCode + " and " + node.getNodeCode());
                }
            }
            if (NodeType.isCarbonCopy(node.getNodeType())) {
                if (ApproverRuleUtil.read(node, ApproverRuleUtil.CARBON_COPY_EXT_CONFIG) == null
                    && StringUtils.isEmpty(node.getPermissionFlag())) {
                    throw new IllegalStateException("Carbon copy recipients are required on node "
                        + node.getNodeCode());
                }
            }
            NodeTimeoutConfig timeoutConfig = TimeoutConfigUtil.read(node);
            if (timeoutConfig != null) {
                TimeoutConfigUtil.validate(node, timeoutConfig);
            }
        }
    }
}
