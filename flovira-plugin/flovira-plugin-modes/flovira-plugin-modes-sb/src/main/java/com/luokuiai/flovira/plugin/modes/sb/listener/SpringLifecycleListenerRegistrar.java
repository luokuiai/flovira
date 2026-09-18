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
package com.luokuiai.flovira.plugin.modes.sb.listener;

import com.luokuiai.flovira.core.listener.lifecycle.LifecycleListenerRegistry;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleSubscription;
import com.luokuiai.flovira.core.listener.lifecycle.WorkflowLifecycleListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

/** 按 Bean 名发现原生监听器，保留容器代理和注入，不要求宿主手动注册。 */
public final class SpringLifecycleListenerRegistrar implements SmartInitializingSingleton, DisposableBean {
    private final ListableBeanFactory beans;
    private final LifecycleListenerRegistry registry;
    private AutoCloseable registration;

    public SpringLifecycleListenerRegistrar(ListableBeanFactory beans, LifecycleListenerRegistry registry) {
        this.beans = Objects.requireNonNull(beans, "beans");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public synchronized void afterSingletonsInstantiated() {
        if (registration != null) throw new IllegalStateException("Lifecycle listeners already initialized");
        Map<String, WorkflowLifecycleListener> listeners = beans.getBeansOfType(WorkflowLifecycleListener.class);
        Map<String, LifecycleSubscription> subscriptions = beans.getBeansOfType(LifecycleSubscription.class);
        registration = registry.install(listeners, new ArrayList<LifecycleSubscription>(subscriptions.values()));
    }

    @Override
    public synchronized void destroy() throws Exception {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }
}
