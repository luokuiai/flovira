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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.NodeControlConfig;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.NodeType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class NodeControlConfigUtil {
    public static final String RESTART = "RESTART_FROM_BEGINNING";
    public static final String CONTINUE = "CONTINUE_FROM_REJECTED_NODE";
    public static final String INITIATOR_CODE = "flovira:initiator";

    private NodeControlConfigUtil() { }

    public static NodeControlConfig read(Node node) {
        if (StringUtils.isEmpty(node.getExt())) return null;
        List<Map<String, Object>> entries = FlowEngine.jsonConvert.strToList(node.getExt());
        NodeControlConfig result = null;
        if (entries == null) throw new IllegalArgumentException("Invalid node ext");
        for (Map<String, Object> entry : entries) {
            if (entry == null) throw new IllegalArgumentException("Invalid node ext entry");
            if (!"nodeControlConfig".equals(entry.get("code"))) continue;
            if (result != null) throw new IllegalArgumentException("Duplicate nodeControlConfig");
            Object value = entry.get("value");
            if (!(value instanceof String)) throw new IllegalArgumentException("nodeControlConfig must be JSON text");
            result = FlowEngine.jsonConvert.strToBean((String) value, NodeControlConfig.class);
            if (result == null || result.getSchemaVersion() != 1 || !NodeType.isBetween(node.getNodeType())) {
                throw new IllegalArgumentException("Unsupported nodeControlConfig");
            }
            if (!Arrays.asList("TO_INITIATOR", "TO_DRAFT", "TO_PREVIOUS", "TO_SPECIFIED_NODE",
                    "TO_REJECTOR_SPECIFIED_NODE", "REJECT").contains(result.getRejectStrategy())) {
                throw new IllegalArgumentException("Unknown rejectStrategy");
            }
            validateStrategy(result.getResubmitStrategy());
        }
        return result;
    }

    public static boolean toInitiator(NodeControlConfig config) {
        return config != null && ("TO_INITIATOR".equals(config.getRejectStrategy())
            || "TO_DRAFT".equals(config.getRejectStrategy()));
    }

    public static void validateStrategy(String strategy) {
        if (!RESTART.equals(strategy) && !CONTINUE.equals(strategy)) {
            throw new IllegalArgumentException("Unknown resubmitStrategy: " + strategy);
        }
    }
}
