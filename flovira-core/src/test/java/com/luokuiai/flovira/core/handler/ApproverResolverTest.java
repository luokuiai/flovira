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
import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.utils.ApproverRuleUtil;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * 办理人解析器注册测试。
 *
 * @author warm
 */
public class ApproverResolverTest {

    @After
    public void resetBeans() {
        FrameInvoker.setBeansFunction(type -> Collections.emptyList());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldSelectResolverByConfiguredStrategyCode() {
        ApproverResolver expected = resolver("CUSTOM_APPROVER");
        ApproverResolver other = resolver("OTHER_APPROVER");
        FrameInvoker.setBeansFunction(type -> (Collection) Arrays.asList(other, expected));

        assertSame(expected, FlowEngine.approverResolver("CUSTOM_APPROVER"));
        assertNull(FlowEngine.approverResolver("NOT_CONFIGURED"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldDelegateCustomConfigurationValidationToResolver() {
        ApproverResolver resolver = resolver("CUSTOM_APPROVER");
        FrameInvoker.setBeansFunction(type -> (Collection) Collections.singletonList(resolver));
        ApproverRule rule = new ApproverRule();
        rule.setSchemaVersion(ApproverRule.CURRENT_SCHEMA_VERSION);
        rule.setStrategy("CUSTOM_APPROVER");
        rule.setSelectionType("CUSTOM_CONFIGURATION");

        ApproverRuleUtil.validate(rule);
    }

    private ApproverResolver resolver(final String strategy) {
        return new ApproverResolver() {
            @Override
            public String getStrategy() {
                return strategy;
            }

            @Override
            public List<String> resolve(Node node, ApproverRule rule, FlowParams flowParams) {
                return Collections.emptyList();
            }
        };
    }
}
