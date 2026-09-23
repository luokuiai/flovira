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

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.NodeTimeoutConfig;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.TimeoutAction;

import java.util.Date;
import java.util.Map;
import java.util.Collections;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

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
        if (config.getSchemaVersion() != NodeTimeoutConfig.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported timeout config");
        }
        if (isFormField(config)) {
            String code = config.getFieldCode();
            if (code == null || !code.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")
                || code.split("\\.").length > 32) {
                throw new IllegalStateException("Invalid timeout form field");
            }
        } else if (config.getSource() != null && !"DURATION".equals(config.getSource())) {
            throw new IllegalStateException("Unsupported timeout source");
        } else if (config.getDuration() < 1 || durationMillis(config) < 1) {
            throw new IllegalStateException("Unsupported timeout duration");
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
        applySnapshot(node, task, createdAt, Collections.<String, Object>emptyMap());
    }

    public static void applySnapshot(Node node, Task task, Date createdAt, Map<String, Object> variables) {
        if (!FlowEngine.isTimeoutEnabled()) {
            return;
        }
        NodeTimeoutConfig config = read(node);
        if (config == null || !config.isEnabled()) {
            return;
        }
        Date deadline;
        if (isFormField(config)) {
            deadline = formDeadline(config, variables);
        } else {
            long durationMillis = durationMillis(config);
            try {
                deadline = new Date(Math.addExact(createdAt.getTime(), durationMillis));
            } catch (ArithmeticException exception) {
                throw new IllegalStateException("Timeout duration is too large", exception);
            }
        }
        task.setTimeoutAt(deadline)
            .setTimeoutAction(config.getAction())
            .setTimeoutConfig(FlowEngine.jsonConvert.objToStr(config))
            .setTimeoutStatus(STATUS_PENDING)
            .setTimeoutClaimedAt(null);
    }

    private static boolean isFormField(NodeTimeoutConfig config) {
        return "FORM_FIELD".equals(config.getSource());
    }

    /** 到达节点时冻结截止时间；无时区的表单日期时间使用服务器时区。 */
    static Date formDeadline(NodeTimeoutConfig config, Map<String, Object> variables) {
        Object value = variables == null ? null : variables.get("formData");
        for (String key : config.getFieldCode().split("\\.")) {
            value = value instanceof Map ? ((Map<?, ?>) value).get(key) : null;
        }
        try {
            if (value instanceof Date) return new Date(((Date) value).getTime());
            if (value instanceof Number) return new Date(new BigDecimal(value.toString()).longValueExact());
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                String text = ((String) value).trim().replace(' ', 'T');
                Instant instant;
                try {
                    instant = OffsetDateTime.parse(text).toInstant();
                } catch (DateTimeParseException noOffset) {
                    instant = text.length() == 10
                        ? LocalDate.parse(text).atStartOfDay(ZoneId.systemDefault()).toInstant()
                        : LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant();
                }
                return Date.from(instant);
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid timeout form date: " + config.getFieldCode(), exception);
        }
        throw new IllegalStateException("Missing or invalid timeout form date: " + config.getFieldCode());
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
