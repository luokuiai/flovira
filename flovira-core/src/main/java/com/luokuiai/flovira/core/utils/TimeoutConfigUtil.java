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
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.NodeTimeoutConfig;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.TimeoutAction;

import java.util.Date;
import java.util.Map;

/**
 * 节点超时配置工具
 *
 * @author warm
 */
public final class TimeoutConfigUtil {

    public static final String EXT_CONFIG = "timeoutConfig";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";

    private TimeoutConfigUtil() {
    }

    public static NodeTimeoutConfig read(Node node) {
        Map<String, String> ext = FlowEngine.nodeService().getExt(node);
        String value = ext.get(EXT_CONFIG);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        NodeTimeoutConfig config = FlowEngine.jsonConvert.strToBean(value, NodeTimeoutConfig.class);
        validate(node, config);
        return config;
    }

    public static void validate(Node node, NodeTimeoutConfig config) {
        if (config == null || !config.isEnabled()) {
            return;
        }
        if (config.getSchemaVersion() != NodeTimeoutConfig.CURRENT_SCHEMA_VERSION || config.getDuration() < 1
            || durationMillis(config) < 1) {
            throw new IllegalStateException("Unsupported timeout config");
        }
        if (NodeType.isBetween(node.getNodeType()) && !TimeoutAction.isApprovalAction(config.getAction())) {
            throw new IllegalStateException("Unsupported approval timeout action");
        }
        if (NodeType.isWait(node.getNodeType()) && !TimeoutAction.isWaitAction(config.getAction())) {
            throw new IllegalStateException("Unsupported wait timeout action");
        }
        if (!NodeType.isBetween(node.getNodeType()) && !NodeType.isWait(node.getNodeType())) {
            throw new IllegalStateException("Node type does not support timeout");
        }
    }

    public static void applySnapshot(Node node, Task task, Date createdAt) {
        Flovira flowConfig = FlowEngine.getFlowConfig();
        if (flowConfig == null || flowConfig.getTimeout() == null || !flowConfig.getTimeout().isEnabled()) {
            return;
        }
        NodeTimeoutConfig config = read(node);
        if (config == null || !config.isEnabled()) {
            return;
        }
        long durationMillis = durationMillis(config);
        if (Long.MAX_VALUE - createdAt.getTime() < durationMillis) {
            throw new IllegalStateException("Timeout duration is too large");
        }
        task.setTimeoutAt(new Date(createdAt.getTime() + durationMillis))
            .setTimeoutAction(config.getAction())
            .setTimeoutConfig(FlowEngine.jsonConvert.objToStr(config))
            .setTimeoutStatus(STATUS_PENDING)
            .setTimeoutClaimedAt(null);
    }

    private static long durationMillis(NodeTimeoutConfig config) {
        long unitMillis;
        if ("MINUTES".equals(config.getDurationUnit())) {
            unitMillis = 60L * 1000L;
        } else if ("HOURS".equals(config.getDurationUnit())) {
            unitMillis = 60L * 60L * 1000L;
        } else if ("DAYS".equals(config.getDurationUnit())) {
            unitMillis = 24L * 60L * 60L * 1000L;
        } else {
            throw new IllegalStateException("Unsupported timeout duration unit");
        }
        if (config.getDuration() > Long.MAX_VALUE / unitMillis) {
            throw new IllegalStateException("Timeout duration is too large");
        }
        return config.getDuration() * unitMillis;
    }
}
