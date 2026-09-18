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

import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LifecycleListenerRegistryTest {
    private LifecycleSubscription subscription(String code, ListenerPoint point, DeliveryPhase phase, int order) {
        return new LifecycleSubscription(code, point, phase, order, null);
    }

    @Test
    public void standaloneGlobalRegistrationDeduplicatesLocalAndOrdersMultipleListeners() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        List<String> calls = new ArrayList<String>();
        for (String code : Arrays.asList("second", "first")) {
            registry.register(code, new WorkflowLifecycleListener() {
                public void beforeOperation(OperationContext context, String parameters) {
                    calls.add(code);
                    context.setVariable(code, true);
                }
            });
        }
        LifecycleSubscription second = subscription("second", ListenerPoint.BEFORE_OPERATION, DeliveryPhase.IN_TRANSACTION, 2);
        registry.subscribeGlobally(second);
        registry.subscribeGlobally(subscription("first", ListenerPoint.BEFORE_OPERATION, DeliveryPhase.IN_TRANSACTION, 1));
        OperationContext context = operation();
        new LifecycleDispatcher(registry, (code,event,error) -> fail()).beforeOperation(context, Arrays.asList(second));
        assertEquals(Arrays.asList("first", "second"), calls);
        assertEquals(Boolean.TRUE, context.getVariables().get("first"));
        assertEquals(Boolean.TRUE, context.getVariables().get("second"));
    }

    @Test
    public void rejectsDuplicateCodesWithoutReplacingOriginal() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        WorkflowLifecycleListener original = new WorkflowLifecycleListener() {};
        registry.register("code", original);
        assertThrows(IllegalArgumentException.class, () -> registry.register("code", new WorkflowLifecycleListener() {}));
        assertSame(original, registry.getListeners().get("code"));
        assertThrows(UnsupportedOperationException.class, () -> registry.getListeners().clear());
    }

    @Test
    public void rejectsUnknownConflictingAndDeferredPreOperationSubscriptions() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        LifecycleSubscription global = subscription("code", ListenerPoint.NODE_ENTERED, DeliveryPhase.IN_TRANSACTION, 1);
        assertThrows(IllegalArgumentException.class, () -> registry.subscribeGlobally(global));
        registry.register("code", new WorkflowLifecycleListener() {});
        registry.subscribeGlobally(global);
        assertThrows(IllegalArgumentException.class, () -> registry.select(ListenerPoint.NODE_ENTERED,
            DeliveryPhase.IN_TRANSACTION, Arrays.asList(subscription("code", ListenerPoint.NODE_ENTERED, DeliveryPhase.IN_TRANSACTION, 2))));
        assertThrows(IllegalArgumentException.class, () -> subscription("code", ListenerPoint.BEFORE_ASSIGNMENT, DeliveryPhase.AFTER_COMMIT, 0));
    }

    @Test
    public void propagatesPreOperationFailureAndProtectsReservedVariables() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        registry.register("deny", new WorkflowLifecycleListener() {
            public void beforeOperation(OperationContext context, String parameters) { throw new IllegalStateException("denied"); }
        });
        registry.subscribeGlobally(subscription("deny", ListenerPoint.BEFORE_OPERATION, DeliveryPhase.IN_TRANSACTION, 0));
        OperationContext context = operation();
        assertThrows(IllegalStateException.class, () -> new LifecycleDispatcher(registry, (c,e,x) -> fail()).beforeOperation(context, null));
        assertThrows(IllegalArgumentException.class, () -> context.setVariable("flovira.subprocess.parentInstanceId", "other"));
        assertThrows(UnsupportedOperationException.class, () -> context.getVariables().put("x", "y"));
    }

    @Test
    public void assignmentDraftDoesNotExposeMutableRecipientList() {
        List<String> input = new ArrayList<String>(Arrays.asList("a", "a"));
        AssignmentContext context = new AssignmentContext(1L, 2L, "approval", input);
        input.clear();
        assertEquals(Arrays.asList("a"), context.getAssignees());
        assertThrows(UnsupportedOperationException.class, () -> context.getAssignees().add("b"));
        assertThrows(IllegalArgumentException.class, () -> context.setAssignees(Arrays.asList(" ")));
        context.setAssignees(Arrays.asList("b"));
        assertEquals(Arrays.asList("b"), context.getAssignees());
    }

    @Test
    public void afterCommitUsesCapturedSelectionAndContinuesAfterFailure() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        List<String> calls = new ArrayList<String>();
        registry.register("bad", new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event, String parameters) { throw new IllegalStateException("notification failed"); }
        });
        registry.register("good", new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event, String parameters) { calls.add("good"); }
        });
        registry.subscribeGlobally(subscription("bad", ListenerPoint.NODE_ENTERED, DeliveryPhase.AFTER_COMMIT, 1));
        registry.subscribeGlobally(subscription("good", ListenerPoint.NODE_ENTERED, DeliveryPhase.AFTER_COMMIT, 2));
        DeferredTransaction tx = new DeferredTransaction();
        new LifecycleDispatcher(registry, (code,event,error) -> calls.add(code)).emit(event(), null, tx);
        assertTrue(calls.isEmpty());
        registry.register("later", new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event, String parameters) { calls.add("later"); }
        });
        registry.subscribeGlobally(subscription("later", ListenerPoint.NODE_ENTERED, DeliveryPhase.AFTER_COMMIT, 3));
        tx.callbacks.forEach(Runnable::run);
        assertEquals(Arrays.asList("bad", "good"), calls);
    }

    @Test
    public void inTransactionFailurePropagatesWithoutSchedulingAfterCommit() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        registry.register("bad", new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event, String parameters) { throw new IllegalStateException("rollback"); }
        });
        registry.subscribeGlobally(subscription("bad", ListenerPoint.NODE_ENTERED, DeliveryPhase.IN_TRANSACTION, 1));
        registry.subscribeGlobally(subscription("bad", ListenerPoint.NODE_ENTERED, DeliveryPhase.AFTER_COMMIT, 1));
        DeferredTransaction tx = new DeferredTransaction();
        assertThrows(IllegalStateException.class, () -> new LifecycleDispatcher(registry, (c,e,x) -> fail()).emit(event(), null, tx));
        assertTrue(tx.callbacks.isEmpty());
    }

    @Test
    public void operationSeparatesRuntimeFactsInputPersistedAndWorkingVariables() {
        Definition definition = TestEntityFactory.create(Definition.class).setId(10L).setTenantId("tenant-a");
        Instance instance = TestEntityFactory.create(Instance.class).setId(1L).setDefinitionId(10L)
            .setCreatedBy("initiator").setTenantId("tenant-a");
        Map<String, Object> original = new LinkedHashMap<String, Object>();
        original.put("department", "department-a");
        Map<String, Object> stored = new LinkedHashMap<String, Object>();
        stored.put("context", original); stored.put("historyOnly", true);
        TestEntityFactory.put(instance, "VariableMap", stored);
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("context", "forged"); input.put("tenantId", "forged");
        OperationContext operation = new OperationContext("op", "APPROVE", "USER", definition, instance, 2L,
            "current-operator", false, input);
        definition.setTenantId("changed"); instance.setCreatedBy("changed"); original.put("department", "changed"); input.clear();
        assertEquals("tenant-a", operation.getDefinition().getTenantId());
        assertEquals("initiator", operation.getInitiatorId());
        assertEquals("current-operator", operation.getActor());
        assertEquals("forged", operation.getInputVariables().get("context"));
        assertFalse(operation.getInputVariables().containsKey("historyOnly"));
        assertEquals("forged", operation.getVariables().get("context"));
        Map<?, ?> snapshot = (Map<?, ?>) operation.getPersistedVariables().get("context");
        assertEquals("department-a", snapshot.get("department"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        operation.setVariable("context", snapshot);
        operation.removeVariable("historyOnly");
        assertEquals(snapshot, operation.getVariables().get("context"));
        assertTrue(operation.getPersistedVariables().containsKey("historyOnly"));
        assertFalse(operation.getVariables().containsKey("historyOnly"));
    }

    @Test
    public void newInstanceHasCompleteRuntimeFactsAndNoPersistedVariables() {
        Definition definition = TestEntityFactory.create(Definition.class).setId(10L).setTenantId("tenant-a");
        Instance instance = TestEntityFactory.create(Instance.class).setId(1L).setDefinitionId(10L).setCreatedBy("starter");
        TestEntityFactory.put(instance, "VariableMap", Collections.singletonMap("untrusted", true));
        OperationContext operation = new OperationContext("op", "START", "USER", definition, instance, null,
            "starter", true, Collections.singletonMap("input", "value"));
        assertTrue(operation.isNewInstance());
        assertNotNull(operation.getDefinition());
        assertNotNull(operation.getInstance());
        assertEquals("starter", operation.getInitiatorId());
        assertTrue(operation.getPersistedVariables().isEmpty());
        assertEquals(Collections.singletonMap("input", "value"), operation.getInputVariables());
        assertThrows(NullPointerException.class, () -> new OperationContext("op", "START", "USER", null, instance,
            null, "starter", true, null));
    }

    private OperationContext operation() {
        return new OperationContext("op", "APPROVE", "MANUAL", TestEntityFactory.create(Definition.class).setId(10L),
            TestEntityFactory.create(Instance.class).setId(1L).setDefinitionId(10L).setCreatedBy("starter"),
            2L, "user", false, null);
    }

    private LifecycleEvent event() { return new LifecycleEvent("event", "operation", LifecycleEventType.NODE_ENTERED, 1L, 1L, "{}"); }
    private static final class DeferredTransaction implements TransactionExecutor {
        final List<Runnable> callbacks = new ArrayList<Runnable>();
        public <T> T execute(TransactionCallback<T> callback) { return callback.execute(); }
        public void afterCommit(Runnable callback) { callbacks.add(callback); }
        public boolean isTransactionActive() { return true; }
    }
}
