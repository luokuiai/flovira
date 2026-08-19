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
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.NodeType;

/**
 * 子流程节点配置工具
 *
 * @author warm
 */
public final class SubprocessConfigUtil {

    public static final String EXT_CONFIG = "subprocessConfig";

    private SubprocessConfigUtil() {
    }

    public static SubprocessConfig read(Node node) {
        if (node == null || !NodeType.isSubProcess(node.getNodeType())) {
            throw new IllegalStateException("Subprocess node not found");
        }
        String value = FlowEngine.nodeService().getExt(node).get(EXT_CONFIG);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalStateException("Subprocess node config is missing");
        }
        SubprocessConfig config = FlowEngine.jsonConvert.strToBean(value, SubprocessConfig.class);
        validate(config);
        return config;
    }

    public static void validate(SubprocessConfig config) {
        if (config == null || config.getSchemaVersion() != SubprocessConfig.CURRENT_SCHEMA_VERSION
            || StringUtils.isEmpty(config.getFixedChildFlowCode()) || !"ALL".equals(config.getCompletionPolicy())) {
            throw new IllegalStateException("Unsupported subprocess node config");
        }
    }
}
