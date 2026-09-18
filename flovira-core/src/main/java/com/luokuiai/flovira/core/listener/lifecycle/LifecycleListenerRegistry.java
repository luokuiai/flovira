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

/**
 * 程序化注册与全局订阅，不依赖 Spring、FrameInvoker 或数据库。
 * 注册和选择采用同步快照；回调执行不持有注册表锁。
 */
public final class LifecycleListenerRegistry {
    private final Map<String, WorkflowLifecycleListener> listeners = new LinkedHashMap<String, WorkflowLifecycleListener>();
    private final List<LifecycleSubscription> globals = new ArrayList<LifecycleSubscription>();

    public synchronized List<String> registeredCodes() {
        return Collections.unmodifiableList(new ArrayList<String>(listeners.keySet()));
    }

    /** 原子安装容器发现的对象；返回的句柄仅清理本批拥有的注册。 */
    public synchronized AutoCloseable install(Map<String, WorkflowLifecycleListener> beans,
                                              List<LifecycleSubscription> subscriptions) {
        Objects.requireNonNull(beans, "beans");
        Objects.requireNonNull(subscriptions, "subscriptions");
        LifecycleListenerRegistry staged = new LifecycleListenerRegistry();
        staged.listeners.putAll(listeners);
        staged.globals.addAll(globals);
        Map<String, WorkflowLifecycleListener> owned = new LinkedHashMap<String, WorkflowLifecycleListener>(beans);
        for (Map.Entry<String, WorkflowLifecycleListener> entry : owned.entrySet()) {
            staged.register(entry.getKey(), entry.getValue());
        }
        for (LifecycleSubscription subscription : subscriptions) staged.subscribeGlobally(subscription);
        List<LifecycleSubscription> added = new ArrayList<LifecycleSubscription>(staged.globals);
        added.removeAll(globals);
        listeners.putAll(owned);
        globals.addAll(added);
        return () -> {
            synchronized (LifecycleListenerRegistry.this) {
                globals.removeAll(added);
                for (Map.Entry<String, WorkflowLifecycleListener> entry : owned.entrySet()) {
                    if (listeners.get(entry.getKey()) == entry.getValue()) listeners.remove(entry.getKey());
                }
            }
        };
    }

    public synchronized void register(String code, WorkflowLifecycleListener listener) {
        if (code == null || code.trim().isEmpty() || !code.equals(code.trim())) {
            throw new IllegalArgumentException("Invalid lifecycle listener code");
        }
        Objects.requireNonNull(listener, "listener");
        if (listeners.containsKey(code)) throw new IllegalArgumentException("Duplicate lifecycle listener: " + code);
        listeners.put(code, listener);
    }

    public synchronized void subscribeGlobally(LifecycleSubscription subscription) {
        requireRegistered(subscription);
        for (LifecycleSubscription existing : globals) {
            if (sameKey(existing, subscription)) {
                requireEquivalent(existing, subscription);
                return;
            }
        }
        globals.add(subscription);
    }

    public synchronized Map<String, WorkflowLifecycleListener> getListeners() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, WorkflowLifecycleListener>(listeners));
    }

    public synchronized List<Invocation> select(ListenerPoint point, DeliveryPhase phase,
                                               List<LifecycleSubscription> local) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(phase, "phase");
        List<LifecycleSubscription> all = new ArrayList<LifecycleSubscription>(globals);
        if (local != null) all.addAll(local);
        Map<String, LifecycleSubscription> selected = new LinkedHashMap<String, LifecycleSubscription>();
        for (LifecycleSubscription subscription : all) {
            requireRegistered(subscription);
            if (subscription.getPoint() != point || subscription.getPhase() != phase) continue;
            LifecycleSubscription previous = selected.get(subscription.getCode());
            if (previous != null) requireEquivalent(previous, subscription);
            else selected.put(subscription.getCode(), subscription);
        }
        List<LifecycleSubscription> ordered = new ArrayList<LifecycleSubscription>(selected.values());
        ordered.sort(Comparator.comparingInt(LifecycleSubscription::getOrder).thenComparing(LifecycleSubscription::getCode));
        List<Invocation> result = new ArrayList<Invocation>();
        for (LifecycleSubscription subscription : ordered) {
            result.add(new Invocation(listeners.get(subscription.getCode()), subscription));
        }
        return Collections.unmodifiableList(result);
    }

    private void requireRegistered(LifecycleSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        if (!listeners.containsKey(subscription.getCode())) {
            throw new IllegalArgumentException("Unknown lifecycle listener: " + subscription.getCode());
        }
    }
    private static boolean sameKey(LifecycleSubscription a, LifecycleSubscription b) {
        return a.getCode().equals(b.getCode()) && a.getPoint() == b.getPoint() && a.getPhase() == b.getPhase();
    }
    private static void requireEquivalent(LifecycleSubscription a, LifecycleSubscription b) {
        if (a.getOrder() != b.getOrder() || !Objects.equals(a.getParameters(), b.getParameters())) {
            throw new IllegalArgumentException("Conflicting lifecycle subscription: " + a.getCode());
        }
    }

    /** 一次选择的稳定调用快照。 */
    public static final class Invocation {
        private final WorkflowLifecycleListener listener;
        private final LifecycleSubscription subscription;
        private Invocation(WorkflowLifecycleListener listener, LifecycleSubscription subscription) {
            this.listener = listener;
            this.subscription = subscription;
        }
        public WorkflowLifecycleListener getListener() { return listener; }
        public LifecycleSubscription getSubscription() { return subscription; }
    }
}
