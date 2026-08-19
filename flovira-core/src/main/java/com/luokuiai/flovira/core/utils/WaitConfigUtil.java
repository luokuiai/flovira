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
import com.luokuiai.flovira.core.dto.NodeJson;
import com.luokuiai.flovira.core.dto.WaitConfig;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.NodeType;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 等待节点配置工具
 *
 * @author warm
 */
public final class WaitConfigUtil {

    public static final String EXT_CONFIG = "waitConfig";
    private static final Pattern WAIT_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.:-]{0,127}$");

    private WaitConfigUtil() {
    }

    public static WaitConfig read(Node node) {
        if (node == null || !NodeType.isWait(node.getNodeType())) {
            throw new IllegalStateException("Wait node not found");
        }
        String value = FlowEngine.nodeService().getExt(node).get(EXT_CONFIG);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalStateException("Wait node config is missing");
        }
        WaitConfig config = FlowEngine.jsonConvert.strToBean(value, WaitConfig.class);
        validate(config);
        return config;
    }

    public static WaitConfig read(NodeJson node) {
        if (node == null || !NodeType.isWait(node.getNodeType())) {
            throw new IllegalStateException("Wait node not found in instance definition snapshot");
        }
        String value = getExtValue(node.getExt());
        if (StringUtils.isEmpty(value)) {
            throw new IllegalStateException("Wait node config is missing from instance definition snapshot");
        }
        WaitConfig config = FlowEngine.jsonConvert.strToBean(value, WaitConfig.class);
        validate(config);
        return config;
    }

    private static String getExtValue(String ext) {
        if (StringUtils.isEmpty(ext)) {
            return null;
        }
        List<Map<String, Object>> extList = FlowEngine.jsonConvert.strToList(ext);
        if (CollUtil.isEmpty(extList)) {
            return null;
        }
        for (Map<String, Object> item : extList) {
            Object code = item.get("code");
            Object value = item.get("value");
            if (EXT_CONFIG.equals(code) && value != null) {
                return value.toString();
            }
        }
        return null;
    }

    public static void validate(WaitConfig config) {
        if (config == null || config.getSchemaVersion() != WaitConfig.CURRENT_SCHEMA_VERSION
            || StringUtils.isEmpty(config.getWaitKey()) || !WAIT_KEY_PATTERN.matcher(config.getWaitKey()).matches()) {
            throw new IllegalStateException("Unsupported wait node config");
        }
    }
}
