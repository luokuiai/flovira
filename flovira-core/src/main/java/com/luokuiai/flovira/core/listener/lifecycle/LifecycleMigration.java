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
import com.luokuiai.flovira.core.entity.*;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.orm.dao.FlowInstanceDao;
import com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao;
import com.luokuiai.flovira.core.utils.LifecycleConfigUtil;
import java.util.*;

/** 停写后由维护工具显式调用；只迁移审核确认的活动待办，不追发事件或猜测历史执行。 */
public final class LifecycleMigration {
    private LifecycleMigration() { }

    public static void migrateActive(Long instanceId, Set<Long> expectedTaskIds) {
        Objects.requireNonNull(expectedTaskIds, "expectedTaskIds");
        Set<Long> expected = new HashSet<Long>(expectedTaskIds);
        if (expected.isEmpty()) throw new IllegalArgumentException("Active migration requires reviewed task identities");
        FlowEngine.transactionExecutor().execute(() -> {
            if (!FlowEngine.transactionExecutor().isTransactionActive()) throw new IllegalStateException("Migration requires a transaction");
            LifecycleCallbackGuard.check(instanceId);
            Instance visible = FlowEngine.instanceService().getById(instanceId);
            if (visible == null) throw new IllegalArgumentException("Instance not found");
            FlowInstanceDao<Instance> instances = FlowEngine.instanceService().getDao();
            Instance instance = instances.lockForUpdate(visible.getTenantId(), instanceId);
            if (instance == null || instance.getLifecycleState() != null || NodeType.isEnd(instance.getNodeType())) {
                throw new IllegalStateException("Instance is terminal or already migrated");
            }
            List<Task> tasks = FlowEngine.taskService().getByInsId(instanceId);
            Set<Long> actual = new HashSet<Long>();
            Map<String, Node> nodes = new HashMap<String, Node>();
            Definition definition = FlowEngine.defService().getById(instance.getDefinitionId());
            List<Node> defined = FlowEngine.nodeService().getByDefId(instance.getDefinitionId());
            LifecycleConfigUtil.validate(definition, defined);
            for (Node node : defined) nodes.put(node.getNodeCode(), node);
            @SuppressWarnings("unchecked") FlowNodeExecutionDao<NodeExecution> executions = FrameInvoker.getBean(FlowNodeExecutionDao.class);
            if (executions == null) throw new IllegalStateException("Node execution DAO is not configured");
            if (!executions.listActive(instance.getTenantId() == null ? "0" : instance.getTenantId(), instanceId).isEmpty()) {
                throw new IllegalStateException("Partial lifecycle data requires manual reconciliation");
            }
            for (Task task : tasks) {
                actual.add(task.getId());
                Node node = nodes.get(task.getNodeCode());
                if (node == null || !Objects.equals(node.getNodeType(), task.getNodeType())
                        || NodeType.isGateWay(task.getNodeType()) || NodeType.isStart(task.getNodeType())
                        || NodeType.isEnd(task.getNodeType()) || NodeType.INITIATOR.getKey().equals(task.getNodeType())
                        || task.getNodeExecutionId() != null || task.getCreatedAt() == null) {
                    throw new IllegalStateException("Ambiguous task cannot be migrated: " + task.getId());
                }
            }
            if (!actual.equals(expected)) throw new IllegalStateException("Active tasks changed since migration review");
            for (Task task : tasks) {
                NodeExecution execution = FlowEngine.newNodeExecution();
                FlowEngine.dataFillHandler().idFill(execution);
                FlowEngine.dataFillHandler().insertFill(execution);
                execution.setTenantId(instance.getTenantId() == null ? "0" : instance.getTenantId());
                execution.setDeleted("0");
                execution.setInstanceId(instanceId).setDefinitionId(instance.getDefinitionId()).setNodeCode(task.getNodeCode())
                    .setNodeType(task.getNodeType()).setState("ACTIVE").setVersion(0).setEnteredAt(task.getCreatedAt());
                if (executions.save(execution) != 1) throw new IllegalStateException("Execution migration insert failed");
                task.setNodeExecutionId(execution.getId());
                FlowEngine.taskService().updateById(task);
                for (HisTask history : FlowEngine.hisTaskService().listByTaskId(task.getId())) {
                    if (!instanceId.equals(history.getInstanceId()) || !task.getNodeCode().equals(history.getNodeCode())) {
                        throw new IllegalStateException("Task history identity is inconsistent");
                    }
                    history.setNodeExecutionId(execution.getId());
                    FlowEngine.hisTaskService().updateById(history);
                }
            }
            instance.setLifecycleState(ProcessLifecycleState.ACTIVE.name());
            FlowEngine.instanceService().updateById(instance);
            return null;
        });
    }
}
