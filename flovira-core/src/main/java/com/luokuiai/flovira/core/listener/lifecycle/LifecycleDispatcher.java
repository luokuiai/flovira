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
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 只负责调用和阶段交付；引擎在实际转换位置产生事件。 */
public final class LifecycleDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleDispatcher.class);
    private final LifecycleListenerRegistry registry;
    private final FailureHandler failureHandler;

    public interface FailureHandler {
        void failed(String listenerCode, LifecycleEvent event, RuntimeException failure);
    }

    public LifecycleDispatcher(LifecycleListenerRegistry registry, FailureHandler failureHandler) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    }

    public void beforeOperation(OperationContext context, List<LifecycleSubscription> local) {
        Objects.requireNonNull(context, "context");
        for (LifecycleListenerRegistry.Invocation invocation : registry.select(
                ListenerPoint.BEFORE_OPERATION, DeliveryPhase.IN_TRANSACTION, local)) {
            LifecycleCallbackGuard.invoke(context.getInstanceId(), () ->
                invocation.getListener().beforeOperation(context, invocation.getSubscription().getParameters()));
        }
    }

    public void beforeAssignment(AssignmentContext context, List<LifecycleSubscription> local) {
        Objects.requireNonNull(context, "context");
        for (LifecycleListenerRegistry.Invocation invocation : registry.select(
                ListenerPoint.BEFORE_ASSIGNMENT, DeliveryPhase.IN_TRANSACTION, local)) {
            LifecycleCallbackGuard.invoke(context.getInstanceId(), () ->
                invocation.getListener().beforeAssignment(context, invocation.getSubscription().getParameters()));
        }
    }

    public void emit(LifecycleEvent event, List<LifecycleSubscription> local, TransactionExecutor transaction) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(transaction, "transaction");
        if (!transaction.isTransactionActive()) {
            throw new IllegalStateException("Lifecycle delivery requires an active transaction");
        }
        ListenerPoint point = ListenerPoint.event(event.getType());
        // 先验证两个阶段，防止无效配置在部分事务内处理后才被发现。
        List<LifecycleListenerRegistry.Invocation> immediate = registry.select(point, DeliveryPhase.IN_TRANSACTION, local);
        final List<LifecycleListenerRegistry.Invocation> committed = registry.select(point, DeliveryPhase.AFTER_COMMIT, local);
        for (LifecycleListenerRegistry.Invocation invocation : immediate) {
            LifecycleCallbackGuard.invoke(event.getInstanceId(), () ->
                invocation.getListener().onEvent(event, invocation.getSubscription().getParameters()));
        }
        if (!committed.isEmpty()) {
            transaction.afterCommit(() -> {
                for (LifecycleListenerRegistry.Invocation invocation : committed) {
                    try {
                        LifecycleCallbackGuard.invoke(event.getInstanceId(), () ->
                            invocation.getListener().onEvent(event, invocation.getSubscription().getParameters()));
                    } catch (RuntimeException failure) {
                        try {
                            failureHandler.failed(invocation.getSubscription().getCode(), event, failure);
                        } catch (RuntimeException reportingFailure) {
                            LOG.error("Lifecycle error reporting failed: listener={}, event={}",
                                invocation.getSubscription().getCode(), event.getEventId(), reportingFailure);
                            LOG.error("Lifecycle notification failed: listener={}, event={}",
                                invocation.getSubscription().getCode(), event.getEventId(), failure);
                        }
                    }
                }
            });
        }
    }
}
