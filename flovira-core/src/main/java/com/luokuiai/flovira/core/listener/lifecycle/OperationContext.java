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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/** 操作身份只读；仅业务变量允许通过显式方法调整。 */
public final class OperationContext {
    private final String operationId;
    private final String action;
    private final String source;
    private final Long instanceId;
    private final Long taskId;
    private final String actor;
    private final Map<String, Object> variables;

    public OperationContext(String operationId, String action, String source,
                            Long instanceId, Long taskId, String actor, Map<String, Object> variables) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.action = Objects.requireNonNull(action, "action");
        this.source = Objects.requireNonNull(source, "source");
        this.instanceId = instanceId;
        this.taskId = taskId;
        this.actor = actor;
        this.variables = new LinkedHashMap<String, Object>();
        if (variables != null) this.variables.putAll(variables);
    }

    public String getOperationId() { return operationId; }
    public String getAction() { return action; }
    public String getSource() { return source; }
    public Long getInstanceId() { return instanceId; }
    public Long getTaskId() { return taskId; }
    public String getActor() { return actor; }
    public Map<String, Object> getVariables() { return Collections.unmodifiableMap(variables); }
    public void setVariable(String name, Object value) {
        if (name == null || name.trim().isEmpty() || name.startsWith("flovira.")) {
            throw new IllegalArgumentException("Invalid or reserved workflow variable: " + name);
        }
        variables.put(name, value);
    }
    public void removeVariable(String name) {
        if (name == null || name.startsWith("flovira.")) {
            throw new IllegalArgumentException("Invalid or reserved workflow variable: " + name);
        }
        variables.remove(name);
    }
}
