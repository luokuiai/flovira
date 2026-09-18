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

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.*;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao;
import com.luokuiai.flovira.core.utils.LifecycleConfigUtil;
import com.luokuiai.flovira.core.utils.MapUtil;
import java.util.*;

/** 单次已授权转换的快照与派发；不参与选路，不从历史状态码猜测事件。 */
public final class LifecycleTransition {
    private static final ThreadLocal<String> SYSTEM_ACTION = new ThreadLocal<String>();

    /** 内核自动操作的来源作用域；嵌套调用及异常都会恢复原上下文。 */
    public static <T> T system(String action, java.util.function.Supplier<T> work) {
        String previous = SYSTEM_ACTION.get();
        SYSTEM_ACTION.set(action);
        try { return work.get(); }
        finally { if (previous == null) SYSTEM_ACTION.remove(); else SYSTEM_ACTION.set(previous); }
    }
    private final Instance instance;
    private final Definition definition;
    private final FlowParams params;
    private final Task current;
    private final OperationContext operation;
    private final String beforeState;
    private final String returnContext;
    private final Map<Long, Task> previous = new LinkedHashMap<Long, Task>();
    private final Map<Long, Map<String, Object>> snapshots = new LinkedHashMap<Long, Map<String, Object>>();
    private final Map<String, Node> nodes = new LinkedHashMap<String, Node>();
    private final List<Task> entered = new ArrayList<Task>();
    private final LifecycleConfigResolver resolver = new LifecycleConfigResolver(FlowEngine.jsonConvert, FlowEngine.lifecycleListeners());

    public LifecycleTransition(Instance instance, Definition definition, Node node, Task current,
                               FlowParams params, String action, String source) {
        if (!FlowEngine.transactionExecutor().isTransactionActive()) {
            throw new IllegalStateException("Workflow transition requires an active transaction");
        }
        LifecycleCallbackGuard.check(instance.getId());
        this.instance = instance;
        this.definition = definition;
        this.current = current;
        this.params = params;
        this.beforeState = instance.getLifecycleState();
        this.returnContext = instance.getResubmissionContext();
        List<Node> all = FlowEngine.nodeService().getByDefId(definition.getId());
        LifecycleConfigUtil.validate(definition, all);
        for (Node item : all) nodes.put(item.getNodeCode(), item);
        if (node != null) nodes.put(node.getNodeCode(), node);
        if (!"START".equals(action)) {
            if (beforeState == null) throw new IllegalStateException("Instance requires explicit lifecycle migration");
            for (Task task : FlowEngine.taskService().getByInsId(instance.getId())) {
                requireExecution(task);
                previous.put(task.getId(), task);
                snapshots.put(task.getId(), taskSnapshot(task));
            }
        }
        operation = new OperationContext(UUID.randomUUID().toString(), SYSTEM_ACTION.get() == null ? action : SYSTEM_ACTION.get(),
            SYSTEM_ACTION.get() == null ? source : "SYSTEM", instance.getId(),
            current == null ? null : current.getId(), params.getHandler(),
            MapUtil.mergeAll(instance.getVariableMap(), params.getVariables()));
        FlowEngine.lifecycleDispatcher().beforeOperation(operation, subscriptions(node));
        instance.setVariables(FlowEngine.jsonConvert.objToStr(operation.getVariables()));
        params.variables(new LinkedHashMap<String, Object>(operation.getVariables()));
    }

    @SuppressWarnings("unchecked")
    private static FlowNodeExecutionDao<NodeExecution> dao() {
        FlowNodeExecutionDao<NodeExecution> dao = FrameInvoker.getBean(FlowNodeExecutionDao.class);
        if (dao == null) throw new IllegalStateException("Node execution DAO is not configured");
        return dao;
    }

    private String executionTenant() { return instance.getTenantId() == null ? "0" : instance.getTenantId(); }

    private void requireExecution(Task task) {
        NodeExecution execution = task.getNodeExecutionId() == null ? null : dao().get(executionTenant(), task.getNodeExecutionId());
        if (execution == null || !instance.getId().equals(execution.getInstanceId())
                || !definition.getId().equals(execution.getDefinitionId())
                || !Objects.equals(task.getNodeType(), execution.getNodeType())
                || !task.getNodeCode().equals(execution.getNodeCode()) || !"ACTIVE".equals(execution.getState())) {
            throw new IllegalStateException("Task requires explicit lifecycle execution migration: " + task.getId());
        }
    }

    /** 在历史、任务和参与人写入之前建立执行关联。 */
    public void prepare(List<Task> tasks, List<Node> destinations) {
        for (Node node : destinations) nodes.put(node.getNodeCode(), node);
        for (Task task : tasks) {
            if (NodeType.isGateWay(task.getNodeType())) throw new IllegalStateException("Gateway cannot own a task execution");
            Node node = nodes.get(task.getNodeCode());
            if (NodeType.isBetween(task.getNodeType()) || NodeType.isCarbonCopy(task.getNodeType())) {
                AssignmentContext assignment = new AssignmentContext(instance.getId(), task.getId(), task.getNodeCode(), task.getPermissionList());
                FlowEngine.lifecycleDispatcher().beforeAssignment(assignment, subscriptions(node));
                if (assignment.getAssignees().isEmpty()) throw new IllegalStateException("Assignment requires at least one recipient");
                task.setPermissionList(new ArrayList<String>(assignment.getAssignees()));
            }
            task.setNodeExecutionId(open(task.getNodeCode(), task.getNodeType()).getId());
            entered.add(task);
        }
    }

    private NodeExecution open(String code, Integer type) {
        NodeExecution execution = FlowEngine.newNodeExecution();
        FlowEngine.dataFillHandler().idFill(execution);
        FlowEngine.dataFillHandler().insertFill(execution);
        execution.setTenantId(executionTenant());
        execution.setDeleted("0");
        execution.setInstanceId(instance.getId()).setDefinitionId(definition.getId()).setNodeCode(code)
            .setNodeType(type).setState("ACTIVE").setVersion(0).setEnteredAt(new Date());
        if (dao().save(execution) != 1) throw new IllegalStateException("Node execution insert failed");
        return execution;
    }

    public Long startNode(Node node) {
        NodeExecution execution = open(node.getNodeCode(), node.getNodeType());
        process(LifecycleEventType.PROCESS_STARTED, null);
        Map<String, Object> details = executionSnapshot(execution);
        details.put("tasks", Collections.emptyList());
        emit(LifecycleEventType.NODE_ENTERED, node, details);
        close(execution, "COMPLETED");
        details.put("reason", "COMPLETED");
        emit(LifecycleEventType.NODE_LEFT, node, details);
        return execution.getId();
    }

    public void finish(boolean approval, boolean assigneesChanged, String departure, String terminalReason,
                       LifecycleEventType processEvent) {
        Map<Long, Task> active = new LinkedHashMap<Long, Task>();
        for (Task task : FlowEngine.taskService().getByInsId(instance.getId())) active.put(task.getId(), task);
        if (processEvent == LifecycleEventType.PROCESS_RESUBMITTED) process(processEvent, null);
        if (approval && current != null && NodeType.isBetween(current.getNodeType())) {
            Map<String, Object> details = new LinkedHashMap<String, Object>(snapshots.get(current.getId()));
            details.put("participants", participants(details, operation.getActor(), true));
            details.put("nodeClosed", !active.containsKey(current.getId()));
            details.put("action", SkipType.isReject(params.getSkipType()) ? "REJECT" : "APPROVE");
            details.put("result", "ACCEPTED");
            emit(LifecycleEventType.APPROVAL_ACTION_COMPLETED, nodes.get(current.getNodeCode()), details);
        }
        if (assigneesChanged && current != null && active.containsKey(current.getId())) {
            Map<String, Object> after = taskSnapshot(active.get(current.getId()));
            Object beforeUsers = snapshots.get(current.getId()).get("assignees");
            if (!Objects.equals(beforeUsers, after.get("assignees"))) {
                Map<String, Object> details = new LinkedHashMap<String, Object>(after);
                details.put("before", beforeUsers);
                details.put("after", after.get("assignees"));
                emit(LifecycleEventType.ASSIGNEES_CHANGED, nodes.get(current.getNodeCode()), details);
            }
        }
        // 主办理节点先离开，随后清理被取消的其他活动执行。
        List<Task> leaving = new ArrayList<Task>(previous.values());
        if (current != null) leaving.sort((a, b) -> Boolean.compare(!a.getId().equals(current.getId()), !b.getId().equals(current.getId())));
        for (Task task : leaving) {
            if (active.containsKey(task.getId())) continue;
            String reason = "WITHDRAWN".equals(departure) ? departure
                : current != null && current.getId().equals(task.getId()) ? departure : "CANCELLED";
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>(snapshots.get(task.getId()));
            boolean completedByActor = approval && current != null && current.getId().equals(task.getId());
            snapshot.put("remainingAssignees", completedByActor
                ? participants(snapshot, operation.getActor(), false) : snapshot.get("assignees"));
            leave(task, snapshot, reason);
        }
        if (processEvent == LifecycleEventType.PROCESS_WITHDRAWN) process(processEvent, null);
        for (Task task : entered) {
            Map<String, Object> details = taskSnapshot(task);
            boolean end = NodeType.isEnd(task.getNodeType());
            details.put("tasks", end ? Collections.emptyList() : Collections.singletonList(taskSnapshot(task)));
            if (end) { details.put("taskId", null); details.put("assignees", Collections.emptyList()); }
            emit(LifecycleEventType.NODE_ENTERED, nodes.get(task.getNodeCode()), details);
            if (end || !active.containsKey(task.getId())) leave(task, details, end ? "COMPLETED" : "CANCELLED");
        }
        if (ProcessLifecycleState.ENDED.name().equals(instance.getLifecycleState())) {
            if (!dao().listActive(executionTenant(), instance.getId()).isEmpty()) {
                throw new IllegalStateException("Terminal instance still has active node executions");
            }
            process(LifecycleEventType.PROCESS_ENDED, "SUBPROCESS_CANCEL".equals(operation.getAction())
                ? "CANCELLED" : terminalReason == null ? "COMPLETED" : terminalReason);
        }
    }

    private void leave(Task task, Map<String, Object> original, String reason) {
        NodeExecution execution = dao().get(executionTenant(), task.getNodeExecutionId());
        close(execution, reason);
        Map<String, Object> details = new LinkedHashMap<String, Object>(original);
        details.put("reason", reason);
        if (!details.containsKey("tasks")) details.put("tasks", Collections.singletonList(original));
        emit(LifecycleEventType.NODE_LEFT, nodes.get(task.getNodeCode()), details);
    }

    private void close(NodeExecution execution, String reason) {
        if (execution == null || dao().close(executionTenant(), execution.getId(), execution.getVersion(), reason, new Date()) != 1) {
            throw new IllegalStateException("Node execution was already closed");
        }
    }

    private Map<String, Object> taskSnapshot(Task task) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("taskId", task.getId());
        value.put("nodeExecutionId", task.getNodeExecutionId());
        value.put("nodeCode", task.getNodeCode());
        value.put("nodeType", task.getNodeType());
        value.put("formId", task.getFormId());
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        List<User> assigned = FlowEngine.userService().listByTaskIdAndTypes(task.getId());
        if (assigned != null) {
            assigned.sort(Comparator.comparing(User::getId));
            for (User user : assigned) {
                Map<String, Object> relation = new LinkedHashMap<String, Object>();
                relation.put("participationId", user.getId());
                relation.put("userId", user.getProcessedBy());
                relation.put("type", user.getType());
                relation.put("createdBy", user.getCreatedBy());
                users.add(relation);
            }
        }
        value.put("assignees", users);
        return value;
    }

    private Map<String, Object> executionSnapshot(NodeExecution execution) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("nodeExecutionId", execution.getId());
        value.put("nodeCode", execution.getNodeCode());
        value.put("nodeType", execution.getNodeType());
        value.put("assignees", Collections.emptyList());
        return value;
    }

    private void process(LifecycleEventType type, String reason) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("reason", reason);
        emit(type, null, details);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> participants(Map<String, Object> snapshot, String actor, boolean matching) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> user : (List<Map<String, Object>>) snapshot.get("assignees")) {
            if (Objects.equals(actor, user.get("userId")) == matching) result.add(user);
        }
        return result;
    }

    private List<LifecycleSubscription> subscriptions(Node node) {
        List<LifecycleSubscription> definitionSubscriptions = resolver.read(definition.getExt(), null);
        return node == null ? definitionSubscriptions : resolver.combine(definitionSubscriptions, resolver.read(node.getExt(), node.getNodeType()));
    }

    private void emit(LifecycleEventType type, Node node, Map<String, Object> details) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("tenantId", instance.getTenantId());
        value.put("instanceId", instance.getId());
        value.put("definitionId", definition.getId());
        value.put("definitionVersion", definition.getVersion());
        value.put("businessType", instance.getBusinessType());
        value.put("businessId", instance.getBusinessId());
        value.put("actor", operation.getActor());
        value.put("action", operation.getAction());
        value.put("source", operation.getSource());
        value.put("beforeState", beforeState);
        value.put("state", instance.getLifecycleState());
        value.put("returnContext", returnContext == null ? null : FlowEngine.jsonConvert.strToMap(returnContext));
        value.put("targetNodeCode", params.getNodeCode());
        List<String> targets = new ArrayList<String>();
        for (Task task : entered) targets.add(task.getNodeCode());
        value.put("targetNodeCodes", targets);
        value.put("opinion", params.getMessage());
        value.put("variables", instance.getVariableMap());
        value.put("formData", params.getVariables().get("formData"));
        for (String key : Arrays.asList("parentInstanceId", "parentTaskId", "parentNodeExecutionId")) {
            value.put(key, instance.getVariableMap().get("flovira.subprocess." + key));
        }
        value.putAll(details);
        FlowEngine.lifecycleDispatcher().emit(new LifecycleEvent(UUID.randomUUID().toString(), operation.getOperationId(),
            type, instance.getId(), System.currentTimeMillis(), FlowEngine.jsonConvert.objToStr(value)),
            subscriptions(node), FlowEngine.transactionExecutor());
    }
}
