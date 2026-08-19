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
import com.luokuiai.flovira.core.dto.SubprocessChildPlan;
import com.luokuiai.flovira.core.dto.SubprocessPlan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 子流程运行计划解析器
 *
 * @author warm
 */
public final class SubprocessPlanResolver {

    public static final String ITEMS_VARIABLE = "subprocessItems";
    private static final String RESERVED_PREFIX = "flovira.subprocess.";
    private static final Set<String> RESERVED_KEYS;

    static {
        Set<String> keys = new LinkedHashSet<>();
        keys.add("tenantId");
        keys.add("starterId");
        keys.add("workflowInstanceId");
        keys.add("definitionId");
        RESERVED_KEYS = Collections.unmodifiableSet(keys);
    }

    private SubprocessPlanResolver() {
    }

    public static SubprocessPlan resolve(Long parentInstanceId, Long parentTaskId, String parentNodeCode,
        Long childDefinitionId, String childDefinitionVersion, Map<String, Object> parentVariables,
        boolean allowEmpty) {
        Object value = parentVariables == null ? null : parentVariables.get(ITEMS_VARIABLE);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("subprocessItems must be a list");
        }
        List<?> items = (List<?>) value;
        Flovira config = FlowEngine.getFlowConfig();
        int limit = config == null ? Flovira.DEFAULT_SUBPROCESS_MAX_CHILDREN : config.getSubprocessMaxChildren();
        if (limit < 1) {
            throw new IllegalStateException("flovira.subprocess-max-children must be greater than 0");
        }
        if (items.size() > limit) {
            throw new IllegalArgumentException("subprocessItems exceeds configured limit " + limit);
        }
        if (items.isEmpty() && !allowEmpty) {
            throw new IllegalArgumentException("subprocessItems must not be empty");
        }

        Set<String> keys = new LinkedHashSet<>();
        List<SubprocessChildPlan> plans = new ArrayList<>();
        List<Map<String, Object>> identity = new ArrayList<>();
        for (Object valueItem : items) {
            if (!(valueItem instanceof Map)) {
                throw new IllegalArgumentException("subprocess item must be an object");
            }
            Map<?, ?> item = (Map<?, ?>) valueItem;
            String itemKey = requiredScalar(item.get("itemKey"), "subprocess itemKey must not be blank");
            if (!keys.add(itemKey)) {
                throw new IllegalArgumentException("duplicate subprocess itemKey: " + itemKey);
            }
            String itemLabel = optionalScalar(item.get("itemLabel"));
            Map<String, Object> variables = variables(item.get("variables"));
            variables.put(RESERVED_PREFIX + "parentInstanceId", parentInstanceId);
            variables.put(RESERVED_PREFIX + "parentTaskId", parentTaskId);
            variables.put(RESERVED_PREFIX + "parentNodeCode", parentNodeCode);
            variables.put(RESERVED_PREFIX + "itemKey", itemKey);
            String businessKey = "SUB:" + parentInstanceId + ":" + parentTaskId + ":"
                + sha256(parentNodeCode + ":" + itemKey).substring(0, 24);
            plans.add(new SubprocessChildPlan(itemKey, itemLabel, businessKey, variables));

            Map<String, Object> fingerprintItem = new LinkedHashMap<>();
            fingerprintItem.put("itemKey", itemKey);
            fingerprintItem.put("itemLabel", itemLabel);
            fingerprintItem.put("variables", canonicalize(variables));
            identity.add(fingerprintItem);
        }
        Collections.sort(identity, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return String.valueOf(left.get("itemKey")).compareTo(String.valueOf(right.get("itemKey")));
            }
        });
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("definitionId", childDefinitionId);
        fingerprint.put("definitionVersion", childDefinitionVersion);
        fingerprint.put("items", identity);
        return new SubprocessPlan(sha256(FlowEngine.jsonConvert.objToStr(fingerprint)), plans);
    }

    private static Map<String, Object> variables(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value == null) {
            return result;
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("subprocess variables must be an object");
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalArgumentException("subprocess variable key must be a string");
            }
            String key = ((String) entry.getKey()).trim();
            if (key.length() == 0 || key.startsWith(RESERVED_PREFIX) || RESERVED_KEYS.contains(key)) {
                throw new IllegalArgumentException("subprocess variable is reserved: " + key);
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static Object canonicalize(Object value) {
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List) {
            List<Object> values = new ArrayList<>();
            for (Object item : (List<?>) value) {
                values.add(canonicalize(item));
            }
            return values;
        }
        return value;
    }

    private static String requiredScalar(Object value, String message) {
        String result = optionalScalar(value);
        if (result == null || result.length() == 0) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private static String optionalScalar(Object value) {
        if (value == null || value instanceof Map || value instanceof List) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
