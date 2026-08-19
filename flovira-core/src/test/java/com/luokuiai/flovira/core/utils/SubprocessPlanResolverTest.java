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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.SubprocessPlan;
import com.luokuiai.flovira.core.json.JsonConvert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 子流程计划解析测试
 *
 * @author warm
 */
public class SubprocessPlanResolverTest {

    @Before
    public void setUp() {
        FlowEngine.setFlowConfig(new Flovira());
        FlowEngine.jsonConvert = new TestJsonConvert();
    }

    @Test
    public void shouldDefaultToOneHundredTwentyEightChildren() {
        assertEquals(128, FlowEngine.getFlowConfig().getSubprocessMaxChildren());
    }

    @Test
    public void shouldBuildDeterministicPlan() {
        Map<String, Object> variables = variables(item("b"), item("a"));
        SubprocessPlan first = SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables, false);
        SubprocessPlan second = SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables(item("a"), item("b")), false);
        assertEquals(first.getFingerprint(), second.getFingerprint());
        assertEquals(2, first.getChildren().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectDuplicateKeys() {
        SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables(item("a"), item("a")), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectConfiguredLimit() {
        Flovira flovira = new Flovira();
        flovira.setSubprocessMaxChildren(1);
        FlowEngine.setFlowConfig(flovira);
        SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables(item("a"), item("b")), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectReservedVariable() {
        Map<String, Object> item = item("a");
        Map<String, Object> childVariables = new LinkedHashMap<>();
        childVariables.put("flovira.subprocess.parentInstanceId", 9L);
        item.put("variables", childVariables);
        SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1", variables(item), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankItemKey() {
        SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables(item(" ")), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyItemsByDefault() {
        SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables(), false);
    }

    @Test
    public void shouldAllowEmptyItemsWhenConfigured() {
        SubprocessPlan plan = SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1",
            variables(), true);
        assertTrue(plan.getChildren().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldEnforceDefaultLimit() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 129; i++) {
            items.add(item(String.valueOf(i)));
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(SubprocessPlanResolver.ITEMS_VARIABLE, items);
        SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1", variables, false);
    }

    @Test
    public void shouldAllowTrustedConfigurationAboveDefault() {
        Flovira flovira = new Flovira();
        flovira.setSubprocessMaxChildren(256);
        FlowEngine.setFlowConfig(flovira);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 129; i++) {
            items.add(item(String.valueOf(i)));
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(SubprocessPlanResolver.ITEMS_VARIABLE, items);
        SubprocessPlan plan = SubprocessPlanResolver.resolve(1L, 2L, "NODE", 3L, "1", variables, false);
        assertEquals(129, plan.getChildren().size());
    }

    private Map<String, Object> variables(Map<String, Object>... items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(SubprocessPlanResolver.ITEMS_VARIABLE, new ArrayList<>(Arrays.asList(items)));
        return result;
    }

    private Map<String, Object> item(String key) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemKey", key);
        result.put("itemLabel", key);
        result.put("variables", new LinkedHashMap<String, Object>());
        return result;
    }

    private static class TestJsonConvert implements JsonConvert {
        public Map<String, Object> strToMap(String jsonStr) { throw new UnsupportedOperationException(); }
        public <T> T strToBean(String jsonStr, Class<T> clazz) { throw new UnsupportedOperationException(); }
        public <T> List<T> strToList(String jsonStr) { throw new UnsupportedOperationException(); }
        public String objToStr(Object variable) { return String.valueOf(variable); }
    }
}
