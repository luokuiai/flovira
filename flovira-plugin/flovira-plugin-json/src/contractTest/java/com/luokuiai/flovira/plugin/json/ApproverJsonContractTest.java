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
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.ApproverContext;
import com.luokuiai.flovira.core.handler.AbstractUserResolver;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.utils.ApproverPolicyUtil;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import java.util.ServiceLoader;
import static org.junit.Assert.*;

public class ApproverJsonContractTest {
    @Test public void validatesTransferSubjectsFromUntypedConfigWithEveryJsonProvider() {
        JsonConvert previous = FlowEngine.jsonConvert;
        JsonConvert json = ServiceLoader.load(JsonConvert.class).iterator().next();
        AbstractUserResolver resolver = new AbstractUserResolver() {
            public void validate(ApproverRule rule) {
                assertEquals("USER", rule.getStrategy());
                assertEquals("backup", rule.getSubjects().get(0).getId());
                assertEquals("USER", rule.getSubjects().get(0).getType());
                assertEquals("Backup", rule.getSubjects().get(0).getName());
                assertEquals("tenant-a", rule.getSubjects().get(0).getMetadata().get("tenant"));
            }
            public List<String> resolve(ApproverContext context) { throw new AssertionError("Validation must not resolve users"); }
        };
        try {
            FlowEngine.jsonConvert = json;
            FrameInvoker.setBeansFunction(type -> Collections.singletonList(resolver));
            ApproverRule rule = json.strToBean("{\"strategy\":\"ROLE\",\"config\":{"
                + "\"emptyPolicy\":\"TRANSFER_TO_USER\",\"emptyPolicySubjects\":[{"
                + "\"id\":\"backup\",\"type\":\"USER\",\"name\":\"Backup\",\"metadata\":{\"tenant\":\"tenant-a\"}}]}}", ApproverRule.class);
            ApproverPolicyUtil.validate(rule);
            ApproverPolicyUtil.validate(json.strToBean(json.objToStr(rule), ApproverRule.class));
        } finally {
            FlowEngine.jsonConvert = previous;
            FrameInvoker.setBeansFunction(type -> Collections.emptyList());
        }
    }

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
