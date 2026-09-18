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
package com.luokuiai.flovira.core.handler;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.*;
import com.luokuiai.flovira.core.entity.*;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.*;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import com.luokuiai.flovira.core.utils.SubmitterRuleUtil;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class SubmitterRuleTest {
    private JsonConvert previousJson;
    private ApproverRule rule;
    private String raw;
    private List<String> users;
    private int resolutions;
    private final Node node = TestEntityFactory.create(Node.class).setNodeType(0).setNodeCode("start").setDefinitionId(1L);
    private final Map<Class<?>, Object> beans = new HashMap<Class<?>, Object>();

    @Before public void setup() {
        previousJson = FlowEngine.jsonConvert;
        users = Collections.singletonList("starter");
        beans.put(NodeService.class, Proxy.newProxyInstance(NodeService.class.getClassLoader(), new Class<?>[]{NodeService.class},
            (proxy, method, args) -> {
                if ("getExt".equals(method.getName())) return raw == null ? Collections.emptyMap() : Collections.singletonMap("submitterRule", raw);
                throw new AssertionError("Unexpected node mutation or resolution: " + method.getName());
            }));
        FrameInvoker.setBeanFunction(beans::get);
        FlowEngine.jsonConvert = new JsonConvert() {
            public <T> T strToBean(String value, Class<T> type) { return type.cast(rule); }
            public Map<String, Object> strToMap(String value) { return Collections.emptyMap(); }
            public <T> List<T> strToList(String value) { return Collections.emptyList(); }
            public String objToStr(Object value) { return String.valueOf(value); }
        };
        register(resolver("USER", true), resolver("ROLE", true));
    }
    @After public void reset() {
        FlowEngine.jsonConvert = previousJson;
        FrameInvoker.setBeanFunction(type -> null);
        FrameInvoker.setBeansFunction(type -> Collections.emptyList());
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void register(ApproverResolver... values) {
        FrameInvoker.setBeansFunction(type -> ApproverResolver.class.equals(type) ? (Collection) Arrays.asList(values) : Collections.emptyList());
    }
    private ApproverResolver resolver(final String code, final boolean submission) {
        return new ApproverResolver() {
            public String getStrategy() { return code; }
            public boolean supportsSubmission() { return submission; }
            public ApproverStrategyDefinition getDefinition() { return ApproverStrategyDefinition.resource(code, code, code, null); }
            public void validate(ApproverRule value) { }
            public List<String> resolve(ApproverContext context) {
                assertSame(node, context.getNode());
                assertNull(context.getInstance());
                assertFalse(context.isPreview());
                resolutions++;
                return users;
            }
        };
    }
    private void configure(String code) {
        raw = "configured";
        rule = new ApproverRule().setStrategy(code).setSelectionType("RESOURCE")
            .setSubjects(Collections.singletonList(new BusinessSubject().setId("selection").setType(code)));
    }
    @Test public void absentAndExplicitAllRequireNoResolver() {
        register();
        SubmitterRuleUtil.check(node, FlowParams.build().handler("anyone"));
        raw = "all";
        rule = new ApproverRule().setStrategy("ALL").setSelectionType("RELATION");
        SubmitterRuleUtil.check(node, FlowParams.build().handler("anyone"));
        assertEquals(0, resolutions);
    }
    @Test public void userAndRoleResolveAtSubmissionWithoutApprovalFallbacks() {
        for (String strategy : Arrays.asList("USER", "ROLE")) {
            configure(strategy);
            SubmitterRuleUtil.read(node);
            int before = resolutions;
            SubmitterRuleUtil.check(node, FlowParams.build().handler("starter"));
            assertEquals(before + 1, resolutions);
            assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.check(node, FlowParams.build().handler("outsider")));
        }
        users = Collections.emptyList();
        rule.setConfig(Collections.<String, Object>singletonMap("emptyPolicy", "SKIP"));
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.check(node, FlowParams.build().handler("starter")));
    }
    @Test public void rejectsMalformedUnknownVersionAndUnsupportedRulesWithoutResolution() {
        raw = "null";
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.read(node));
        configure("UNKNOWN");
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.read(node));
        configure("USER"); rule.setStrategyVersion(2);
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.read(node));
        configure("USER"); rule.setSubjects(Collections.emptyList());
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.read(node));
        configure("CUSTOM"); register(resolver("CUSTOM", false));
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.read(node));
        assertEquals(0, resolutions);
        register(resolver("CUSTOM", true));
        SubmitterRuleUtil.check(node, FlowParams.build().handler("starter"));
        assertEquals(1, resolutions);
        node.setNodeType(1);
        assertThrows(IllegalStateException.class, () -> SubmitterRuleUtil.read(node));
    }
}
