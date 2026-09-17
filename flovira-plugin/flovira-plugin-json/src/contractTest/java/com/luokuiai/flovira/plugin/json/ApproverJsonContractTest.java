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

package com.luokuiai.flovira.plugin.json;

import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.ApproverStrategyDefinition;
import com.luokuiai.flovira.core.json.JsonConvert;
import org.junit.Test;
import java.util.ServiceLoader;
import static org.junit.Assert.*;

public class ApproverJsonContractTest {
    @Test public void preservesBusinessCodeVersionSubjectsAndOpaqueConfiguration() {
        JsonConvert json = ServiceLoader.load(JsonConvert.class).iterator().next();
        ApproverRule rule = json.strToBean("{\"schemaVersion\":1,\"strategyVersion\":3,"
            + "\"strategy\":\"PROJECT_OWNER\",\"selectionType\":\"RELATION\","
            + "\"subjects\":[{\"id\":\"project-1\",\"type\":\"PROJECT\",\"name\":\"Project one\"}],"
            + "\"config\":{\"scope\":{\"tenant\":\"current\"}}}", ApproverRule.class);
        ApproverRule copy = json.strToBean(json.objToStr(rule), ApproverRule.class);
        assertEquals("PROJECT_OWNER", copy.getStrategy());
        assertEquals(3, copy.getStrategyVersion());
        assertEquals("project-1", copy.getSubjects().get(0).getId());
        assertEquals(rule.getConfig(), copy.getConfig());
        ApproverStrategyDefinition descriptor = new ApproverStrategyDefinition()
            .setCode("PROJECT_OWNER").setName("Project owner").setVersion(3);
        assertEquals(3, json.strToBean(json.objToStr(descriptor), ApproverStrategyDefinition.class).getVersion());
    }
}
