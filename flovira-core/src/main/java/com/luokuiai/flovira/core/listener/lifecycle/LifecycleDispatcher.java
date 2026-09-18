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

import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 引擎产生生命周期事实，按代码注册的顺序调用宿主。 */
public final class LifecycleDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleDispatcher.class);
    private final LifecycleListenerRegistry registry;
    private final FailureHandler failureHandler;

    public interface FailureHandler {
        void failed(String listenerName, LifecycleEvent event, RuntimeException failure);
    }

    public LifecycleDispatcher(LifecycleListenerRegistry registry, FailureHandler failureHandler) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    }

    public void beforeOperation(OperationContext context) {
        Objects.requireNonNull(context, "context");
        for (WorkflowLifecycleListener listener : registry.getListeners().values()) {
            LifecycleCallbackGuard.invoke(context.getInstanceId(), () -> listener.beforeOperation(context));
        }
    }

    public void beforeAssignment(AssignmentContext context) {
        Objects.requireNonNull(context, "context");
        for (WorkflowLifecycleListener listener : registry.getListeners().values()) {
            LifecycleCallbackGuard.invoke(context.getInstanceId(), () -> listener.beforeAssignment(context));
        }
    }

    public void emit(LifecycleEvent event, TransactionExecutor transaction) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(transaction, "transaction");
        if (!transaction.isTransactionActive()) {
            throw new IllegalStateException("Lifecycle delivery requires an active transaction");
        }
        Map<String, WorkflowLifecycleListener> immediate = new LinkedHashMap<String, WorkflowLifecycleListener>();
        final Map<String, WorkflowLifecycleListener> committed = new LinkedHashMap<String, WorkflowLifecycleListener>();
        for (Map.Entry<String, WorkflowLifecycleListener> entry : registry.getListeners().entrySet()) {
            DeliveryPhase phase = Objects.requireNonNull(entry.getValue().getDeliveryPhase(), "deliveryPhase");
            (phase == DeliveryPhase.IN_TRANSACTION ? immediate : committed).put(entry.getKey(), entry.getValue());
        }
        for (WorkflowLifecycleListener listener : immediate.values()) {
            LifecycleCallbackGuard.invoke(event.getInstanceId(), () -> listener.onEvent(event));
        }
        if (!committed.isEmpty()) {
            transaction.afterCommit(() -> {
                for (Map.Entry<String, WorkflowLifecycleListener> entry : committed.entrySet()) {
                    try {
                        LifecycleCallbackGuard.invoke(event.getInstanceId(), () -> entry.getValue().onEvent(event));
                    } catch (RuntimeException failure) {
                        try {
                            failureHandler.failed(entry.getKey(), event, failure);
                        } catch (RuntimeException reportingFailure) {
                            LOG.error("Lifecycle error reporting failed: listener={}, event={}",
                                entry.getKey(), event.getEventId(), reportingFailure);
                            LOG.error("Lifecycle notification failed: listener={}, event={}",
                                entry.getKey(), event.getEventId(), failure);
                        }
                    }
                }
            });
        }
    }
}
