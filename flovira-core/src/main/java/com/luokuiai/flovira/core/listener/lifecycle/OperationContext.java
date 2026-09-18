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

import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/**
 * 操作上下文：可信运行事实、本次输入、持久化变量和可调整的工作变量相互分离。
 * 定义和实例信息在首次启动时同样完整，不依赖实例已入库。
 */
public final class OperationContext {
    private final String operationId;
    private final String action;
    private final String source;
    private final Long instanceId;
    private final Long taskId;
    private final String actor;
    private final Map<String, Object> variables;
    private final DefinitionSnapshot definition;
    private final InstanceSnapshot instance;
    private final String initiatorId;
    private final boolean newInstance;
    private final Map<String, Object> inputVariables;
    private final Map<String, Object> persistedVariables;

    /** 身份来自引擎实体；inputVariables 必须是未与持久化变量合并的本次输入。 */
    public OperationContext(String operationId, String action, String source,
                            Definition definition, Instance instance, Long taskId, String actor,
                            boolean newInstance, Map<String, Object> inputVariables) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(instance, "instance");
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.action = Objects.requireNonNull(action, "action");
        this.source = Objects.requireNonNull(source, "source");
        this.instanceId = instance.getId();
        this.taskId = taskId;
        this.actor = actor;
        this.definition = new DefinitionSnapshot(definition);
        this.instance = new InstanceSnapshot(instance);
        this.initiatorId = instance.getCreatedBy();
        this.newInstance = newInstance;
        this.inputVariables = freezeMap(inputVariables);
        this.persistedVariables = newInstance ? Collections.emptyMap() : freezeMap(instance.getVariableMap());
        this.variables = new LinkedHashMap<String, Object>(persistedVariables);
        this.variables.putAll(this.inputVariables);
    }

    public String getOperationId() { return operationId; }
    public String getAction() { return action; }
    public String getSource() { return source; }
    public Long getInstanceId() { return instanceId; }
    public Long getTaskId() { return taskId; }
    public String getActor() { return actor; }
    /** 流程定义元数据快照，包括租户；不向回调暴露可变引擎实体。 */
    public DefinitionSnapshot getDefinition() { return definition; }
    /** 当前实例的只读运行信息；首次启动时也存在。 */
    public InstanceSnapshot getInstance() { return instance; }
    /** 最初发起人，与当前操作人 actor 分离；不读取请求变量。 */
    public String getInitiatorId() { return initiatorId; }
    public boolean isNewInstance() { return newInstance; }
    /** 未合并历史变量的本次输入，不作为发起人或租户的可信来源。 */
    public Map<String, Object> getInputVariables() { return inputVariables; }
    /** 本次操作前的持久化变量；新实例为空，不能被本次输入或钩子覆盖。 */
    public Map<String, Object> getPersistedVariables() { return persistedVariables; }
    public Map<String, Object> getVariables() { return Collections.unmodifiableMap(variables); }
    public void setVariable(String name, Object value) {
        if (name == null || name.trim().isEmpty() || name.startsWith("flovira.")) {
            throw new IllegalArgumentException("Invalid or reserved workflow variable: " + name);
        }
        variables.put(name, freeze(value));
    }
    public void removeVariable(String name) {
        if (name == null || name.startsWith("flovira.")) {
            throw new IllegalArgumentException("Invalid or reserved workflow variable: " + name);
        }
        variables.remove(name);
    }

    public static final class DefinitionSnapshot {
        private final Long id;
        private final String tenantId;
        private final String flowCode;
        private final String flowName;
        private final String version;
        private final String businessType;
        private DefinitionSnapshot(Definition value) {
            id = value.getId(); tenantId = value.getTenantId(); flowCode = value.getFlowCode();
            flowName = value.getFlowName(); version = value.getVersion(); businessType = value.getBusinessType();
        }
        public Long getId() { return id; }
        public String getTenantId() { return tenantId; }
        public String getFlowCode() { return flowCode; }
        public String getFlowName() { return flowName; }
        public String getVersion() { return version; }
        public String getBusinessType() { return businessType; }
    }

    public static final class InstanceSnapshot {
        private final Long id;
        private final Long definitionId;
        private final String tenantId;
        private final String createdBy;
        private final String businessType;
        private final String businessId;
        private InstanceSnapshot(Instance value) {
            id = value.getId(); definitionId = value.getDefinitionId(); tenantId = value.getTenantId();
            createdBy = value.getCreatedBy(); businessType = value.getBusinessType(); businessId = value.getBusinessId();
        }
        public Long getId() { return id; }
        public Long getDefinitionId() { return definitionId; }
        public String getTenantId() { return tenantId; }
        public String getCreatedBy() { return createdBy; }
        public String getBusinessType() { return businessType; }
        public String getBusinessId() { return businessId; }
    }

    private static Map<String, Object> freezeMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        if (source != null) source.forEach((key, value) -> copy.put(key, freeze(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object freeze(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<Object, Object>();
            ((Map<?, ?>) value).forEach((key, item) -> copy.put(key, freeze(item)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<Object>();
            for (Object item : (List<?>) value) copy.add(freeze(item));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
