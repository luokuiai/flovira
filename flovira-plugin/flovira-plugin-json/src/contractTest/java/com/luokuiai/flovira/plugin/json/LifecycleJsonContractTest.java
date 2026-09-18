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
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.utils.ExtConfigUtil;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Test;
import static org.junit.Assert.*;

public class LifecycleJsonContractTest {
    private final JsonConvert json = ServiceLoader.load(JsonConvert.class).iterator().next();
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
    public void readsJsonObjectSettingsAndPreservesNestedHostData() {
        JsonConvert previous = FlowEngine.jsonConvert;
        FlowEngine.jsonConvert = json;
        try {
            String ext = "{\"business\":{\"tags\":[\"a,b\",\"c\"],\"enabled\":false},"
                + "\"nodeControlConfig\":{\"schemaVersion\":1,\"rejectStrategy\":\"TO_INITIATOR\"}}";
            Map<String, String> values = ExtConfigUtil.read(ext);
            assertEquals(Boolean.FALSE, json.strToMap(values.get("business")).get("enabled"));
            assertEquals(java.util.Arrays.asList("a,b", "c"), json.strToMap(values.get("business")).get("tags"));
            assertEquals("TO_INITIATOR", json.strToBean(values.get("nodeControlConfig"),
                com.luokuiai.flovira.core.dto.NodeControlConfig.class).getRejectStrategy());
            assertEquals(2, json.strToMap(ext).size());
            assertThrows(RuntimeException.class, () -> ExtConfigUtil.read("[]"));
            assertThrows(RuntimeException.class, () -> ExtConfigUtil.read("{broken"));
        } finally { FlowEngine.jsonConvert = previous; }
    }
}
