/*
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
package com.luokuiai.flovira.core.listener.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 代码注册表；回调执行使用快照，不持有注册表锁。 */
public final class LifecycleListenerRegistry {
    private final Map<String, WorkflowLifecycleListener> listeners = new LinkedHashMap<String, WorkflowLifecycleListener>();

    /** 原子安装容器发现的对象；关闭时仅清理本批拥有的注册。 */
    public synchronized AutoCloseable install(Map<String, WorkflowLifecycleListener> beans) {
        Objects.requireNonNull(beans, "beans");
        Map<String, WorkflowLifecycleListener> owned = new LinkedHashMap<String, WorkflowLifecycleListener>(beans);
        LifecycleListenerRegistry staged = new LifecycleListenerRegistry();
        staged.listeners.putAll(listeners);
        for (Map.Entry<String, WorkflowLifecycleListener> entry : owned.entrySet()) {
            staged.register(entry.getKey(), entry.getValue());
        }
        listeners.putAll(owned);
        return () -> {
            synchronized (LifecycleListenerRegistry.this) {
                for (Map.Entry<String, WorkflowLifecycleListener> entry : owned.entrySet()) {
                    if (listeners.get(entry.getKey()) == entry.getValue()) listeners.remove(entry.getKey());
                }
            }
        };
    }

    public synchronized void register(String name, WorkflowLifecycleListener listener) {
        if (name == null || name.trim().isEmpty() || !name.equals(name.trim())) {
            throw new IllegalArgumentException("Invalid lifecycle listener name");
        }
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(listener.getDeliveryPhase(), "deliveryPhase");
        if (listeners.containsKey(name)) throw new IllegalArgumentException("Duplicate lifecycle listener: " + name);
        listeners.put(name, listener);
    }

    public synchronized Map<String, WorkflowLifecycleListener> getListeners() {
        List<Map.Entry<String, WorkflowLifecycleListener>> ordered =
            new ArrayList<Map.Entry<String, WorkflowLifecycleListener>>(listeners.entrySet());
        ordered.sort(Comparator.<Map.Entry<String, WorkflowLifecycleListener>>comparingInt(entry -> entry.getValue().getOrder())
            .thenComparing(Map.Entry::getKey));
        Map<String, WorkflowLifecycleListener> snapshot = new LinkedHashMap<String, WorkflowLifecycleListener>();
        for (Map.Entry<String, WorkflowLifecycleListener> entry : ordered) snapshot.put(entry.getKey(), entry.getValue());
        return Collections.unmodifiableMap(snapshot);
    }
}
