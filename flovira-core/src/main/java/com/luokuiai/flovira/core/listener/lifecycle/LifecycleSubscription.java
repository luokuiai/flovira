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

import java.util.Objects;

/** 一个稳定监听代码在一个处理点和阶段的订阅。参数使用 JSON 文本以避免共享可变对象。 */
public final class LifecycleSubscription {
    private final String code;
    private final ListenerPoint point;
    private final DeliveryPhase phase;
    private final int order;
    private final String parameters;

    public LifecycleSubscription(String code, ListenerPoint point, DeliveryPhase phase,
                                 int order, String parameters) {
        if (code == null || code.trim().isEmpty() || !code.equals(code.trim())) {
            throw new IllegalArgumentException("Invalid lifecycle listener code");
        }
        this.code = code;
        this.point = Objects.requireNonNull(point, "point");
        this.phase = Objects.requireNonNull(phase, "phase");
        if (point.isBefore() && phase != DeliveryPhase.IN_TRANSACTION) {
            throw new IllegalArgumentException("Pre-operation listeners require IN_TRANSACTION");
        }
        this.order = order;
        this.parameters = parameters;
    }

    public String getCode() { return code; }
    public ListenerPoint getPoint() { return point; }
    public DeliveryPhase getPhase() { return phase; }
    public int getOrder() { return order; }
    public String getParameters() { return parameters; }
}
