/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.NodeJson;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SubprocessOutcome;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNull;

/**
 * 未使用子流程时的兼容性测试
 *
 * @author warm
 */
public class SubprocessUnusedRegressionTest {

    @Test
    public void shouldIgnoreOrdinaryTasksAndInstancesWithoutStartingTransactions() {
        FlowEngine.setTransactionExecutor(new TransactionExecutor() {
            public <T> T execute(TransactionCallback<T> callback) {
                throw new AssertionError("ordinary flow must not start a subprocess transaction");
            }
            public void afterCommit(Runnable callback) {
                throw new AssertionError("ordinary flow must not register a subprocess callback");
            }
        });
        SubprocessServiceImpl service = new SubprocessServiceImpl();
        Task task = TestEntityFactory.create(Task.class).setId(1L).setNodeType(NodeType.BETWEEN.getKey());
        Instance instance = TestEntityFactory.create(Instance.class).setId(2L);
        TestEntityFactory.put(instance, "VariableMap", Collections.emptyMap());

        service.onTasksCreated(Collections.singletonList(task));
        service.beforeTaskLeave(task, "PASS");
        service.onInstanceTerminal(instance, SubprocessOutcome.SUCCEEDED);

        assertNull(new NodeJson().getSubprocessSummary());
    }
}
