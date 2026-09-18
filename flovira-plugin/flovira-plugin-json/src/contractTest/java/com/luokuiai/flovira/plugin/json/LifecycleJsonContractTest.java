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
package com.luokuiai.flovira.plugin.json;

import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.listener.lifecycle.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Test;
import static org.junit.Assert.*;

public class LifecycleJsonContractTest {
    private final JsonConvert json = ServiceLoader.load(JsonConvert.class).iterator().next();
    private final LifecycleListenerRegistry registry = new LifecycleListenerRegistry();

    private LifecycleConfigResolver resolver() {
        registry.register("businessListener", new WorkflowLifecycleListener() { });
        return new LifecycleConfigResolver(json, registry);
    }

    @Test
    public void preservesNodeControlAndResubmissionIdentitySnapshots() {
        com.luokuiai.flovira.core.dto.NodeControlConfig control = json.strToBean(
            "{\"schemaVersion\":1,\"allowRollback\":true,\"allowTransfer\":false,\"allowAddSign\":true,"
                + "\"allowMinusSign\":false,\"rejectStrategy\":\"TO_INITIATOR\","
                + "\"resubmitStrategy\":\"CONTINUE_FROM_REJECTED_NODE\"}",
            com.luokuiai.flovira.core.dto.NodeControlConfig.class);
        assertEquals("CONTINUE_FROM_REJECTED_NODE", json.strToBean(json.objToStr(control),
            com.luokuiai.flovira.core.dto.NodeControlConfig.class).getResubmitStrategy());
        assertTrue(control.isAllowAddSign());
        com.luokuiai.flovira.core.dto.ResubmissionContext snapshot = new com.luokuiai.flovira.core.dto.ResubmissionContext();
        snapshot.setDefinitionId(9007199254740993L);
        snapshot.setSourceTaskId(9007199254740995L);
        snapshot.setInitiatorTaskId(9007199254740997L);
        snapshot.setSourceNodeCode("approve");
        snapshot.setStrategy(control.getResubmitStrategy());
        assertEquals(snapshot, json.strToBean(json.objToStr(snapshot),
            com.luokuiai.flovira.core.dto.ResubmissionContext.class));
    }

    @Test
    public void preservesBeanNameParametersAndAllEightEventsThroughExtRoundTrip() {
        LifecycleConfigResolver resolver = resolver();
        for (LifecycleEventType type : LifecycleEventType.values()) {
            String text = config(type.name(), "AFTER_COMMIT", "businessListener");
            LifecycleConfig config = json.strToBean(text, LifecycleConfig.class);
            Map<String, String> ext = new LinkedHashMap<String, String>();
            ext.put("code", "lifecycle");
            ext.put("value", json.objToStr(config));
            List<LifecycleSubscription> subscriptions = resolver.read(json.objToStr(Collections.singletonList(ext)), null);
            assertEquals(1, subscriptions.size());
            assertEquals("businessListener", subscriptions.get(0).getCode());
            assertEquals(ListenerPoint.valueOf(type.name()), subscriptions.get(0).getPoint());
            assertEquals(DeliveryPhase.AFTER_COMMIT, subscriptions.get(0).getPhase());
            assertEquals("{\"business\":\"value\"}", subscriptions.get(0).getParameters());
        }
    }

    @Test
    public void rejectsUnknownReferencesVersionsEventsPhasesAndIncompatibleNodes() {
        LifecycleConfigResolver resolver = resolver();
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("NODE_ENTERED", "IN_TRANSACTION", "missing"), 1));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("UNKNOWN", "IN_TRANSACTION", "businessListener"), 1));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("NODE_ENTERED", "UNKNOWN", "businessListener"), 1));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("BEFORE_OPERATION", "AFTER_COMMIT", "businessListener"), 1));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("PROCESS_STARTED", "IN_TRANSACTION", "businessListener"), 1));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("NODE_ENTERED", "IN_TRANSACTION", "businessListener"), 4));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("ASSIGNEES_CHANGED", "IN_TRANSACTION", "businessListener"), 7));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(config("BEFORE_ASSIGNMENT", "IN_TRANSACTION", "businessListener"), 2));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse("{\"schemaVersion\":2,\"subscriptions\":[]}", null));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse("{\"subscriptions\":[]}", null));
    }

    @Test
    public void rejectsConflictsAcrossGlobalDefinitionAndNodeScopes() {
        LifecycleConfigResolver resolver = resolver();
        String text = config("NODE_ENTERED", "IN_TRANSACTION", "businessListener");
        List<LifecycleSubscription> definition = resolver.parse(text, null);
        List<LifecycleSubscription> node = resolver.parse(text, 1);
        assertEquals(1, registry.select(ListenerPoint.NODE_ENTERED, DeliveryPhase.IN_TRANSACTION,
            resolver.combine(definition, node)).size());
        List<LifecycleSubscription> conflicting = resolver.parse(text.replace("value", "different"), 1);
        assertThrows(IllegalArgumentException.class, () -> resolver.combine(definition, conflicting));
        registry.subscribeGlobally(definition.get(0));
        assertThrows(IllegalArgumentException.class, () -> resolver.parse(text.replace("value", "different"), 1));
    }

    private static String config(String point, String phase, String code) {
        return "{\"schemaVersion\":1,\"subscriptions\":[{\"code\":\"" + code + "\",\"point\":\"" + point
            + "\",\"phase\":\"" + phase + "\",\"order\":12,\"parameters\":\"{\\\"business\\\":\\\"value\\\"}\"}]}";
    }
}
