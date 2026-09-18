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
package com.luokuiai.flovira.core.listener.lifecycle;

import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.json.JsonConvert;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 配置只引用已注册的代码；不加载类名或执行表达式。 */
public final class LifecycleConfigResolver {
    public static final String EXT_CONFIG = "lifecycle";
    private final JsonConvert json;
    private final LifecycleListenerRegistry registry;

    public LifecycleConfigResolver(JsonConvert json, LifecycleListenerRegistry registry) {
        this.json = Objects.requireNonNull(json, "json");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /** nodeType 为 null 表示流程作用域。 */
    public List<LifecycleSubscription> read(String ext, Integer nodeType) {
        if (ext == null || ext.trim().isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> entries = json.strToList(ext);
        if (entries == null) throw new IllegalArgumentException("Invalid lifecycle ext");
        String value = null;
        for (Map<String, Object> entry : entries) {
            if (entry == null) throw new IllegalArgumentException("Invalid ext entry");
            if (!EXT_CONFIG.equals(entry.get("code"))) continue;
            if (value != null) throw new IllegalArgumentException("Duplicate lifecycle config");
            Object candidate = entry.get("value");
            if (!(candidate instanceof String) || ((String) candidate).trim().isEmpty()) {
                throw new IllegalArgumentException("Lifecycle config must be JSON text");
            }
            value = (String) candidate;
        }
        return value == null ? Collections.emptyList() : parse(value, nodeType);
    }

    public List<LifecycleSubscription> parse(String value, Integer nodeType) {
        LifecycleConfig config = json.strToBean(value, LifecycleConfig.class);
        if (config == null || !Integer.valueOf(LifecycleConfig.CURRENT_SCHEMA_VERSION).equals(config.getSchemaVersion())
                || config.getSubscriptions() == null) {
            throw new IllegalArgumentException("Unsupported lifecycle config version or missing subscriptions");
        }
        List<LifecycleSubscription> result = new ArrayList<LifecycleSubscription>();
        for (LifecycleConfig.Entry entry : config.getSubscriptions()) {
            if (entry == null || entry.getPoint() == null || entry.getPhase() == null) {
                throw new IllegalArgumentException("Missing lifecycle subscription point or phase");
            }
            ListenerPoint point = ListenerPoint.valueOf(entry.getPoint());
            validateScope(point, nodeType);
            result.add(new LifecycleSubscription(entry.getCode(), point, DeliveryPhase.valueOf(entry.getPhase()),
                entry.getOrder() == null ? 0 : entry.getOrder(), entry.getParameters()));
        }
        validateReferences(result);
        return Collections.unmodifiableList(result);
    }

    public List<LifecycleSubscription> combine(List<LifecycleSubscription> definition,
                                               List<LifecycleSubscription> node) {
        List<LifecycleSubscription> result = new ArrayList<LifecycleSubscription>(definition);
        result.addAll(node);
        validateReferences(result);
        return Collections.unmodifiableList(result);
    }

    private void validateReferences(List<LifecycleSubscription> subscriptions) {
        for (ListenerPoint point : ListenerPoint.values()) {
            for (DeliveryPhase phase : DeliveryPhase.values()) registry.select(point, phase, subscriptions);
        }
    }

    private static void validateScope(ListenerPoint point, Integer nodeType) {
        if (nodeType == null) return;
        if (NodeType.getByKey(nodeType) == null || NodeType.isGateWay(nodeType)) {
            throw new IllegalArgumentException("Lifecycle listeners are not supported on node type " + nodeType);
        }
        if (point.name().startsWith("PROCESS_")) {
            throw new IllegalArgumentException("Process listeners require definition or global scope");
        }
        if ((point == ListenerPoint.APPROVAL_ACTION_COMPLETED || point == ListenerPoint.ASSIGNEES_CHANGED)
                && !NodeType.isBetween(nodeType)) {
            throw new IllegalArgumentException("Participant listeners require an approval node");
        }
        if (point == ListenerPoint.BEFORE_ASSIGNMENT
                && !NodeType.isBetween(nodeType) && !NodeType.isCarbonCopy(nodeType)) {
            throw new IllegalArgumentException("Assignment listeners require a node with recipients");
        }
    }
}
