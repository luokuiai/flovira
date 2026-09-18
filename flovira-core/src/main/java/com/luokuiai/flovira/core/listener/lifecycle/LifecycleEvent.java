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

/**
 * 不可变事件信封。上下文为创建时序列化的独立 JSON 快照，不暴露运行中实体。
 * contextJson 的字段契约由事件类型决定，监听器使用宿主选择的 JSON 提供者读取。
 */
public final class LifecycleEvent {
    private final String eventId;
    private final String operationId;
    private final LifecycleEventType type;
    private final Long instanceId;
    private final long occurredAt;
    private final String contextJson;

    public LifecycleEvent(String eventId, String operationId, LifecycleEventType type,
                          Long instanceId, long occurredAt, String contextJson) {
        this.eventId = requireText(eventId, "eventId");
        this.operationId = requireText(operationId, "operationId");
        this.type = Objects.requireNonNull(type, "type");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.occurredAt = occurredAt;
        this.contextJson = requireText(contextJson, "contextJson");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
    public String getEventId() { return eventId; }
    public String getOperationId() { return operationId; }
    public LifecycleEventType getType() { return type; }
    public Long getInstanceId() { return instanceId; }
    public long getOccurredAt() { return occurredAt; }
    public String getContextJson() { return contextJson; }
}
