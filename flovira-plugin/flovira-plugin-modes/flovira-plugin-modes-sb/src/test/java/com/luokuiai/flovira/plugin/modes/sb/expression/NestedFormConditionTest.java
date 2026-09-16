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
package com.luokuiai.flovira.plugin.modes.sb.expression;

import com.luokuiai.flovira.plugin.json.JsonConvertJackson;
import com.luokuiai.flovira.plugin.modes.sb.helper.SpelHelper;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import static org.junit.Assert.*;

public class NestedFormConditionTest {
    @Test @SuppressWarnings("unchecked")
    public void evaluatesFrontendFixtureWithoutCombiningDifferentRows() throws Exception {
        new SpelHelper().setApplicationContext(new StaticApplicationContext());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = getClass().getResourceAsStream("/nested-form-conditions.json")) {
            assertNotNull(input);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) bytes.write(buffer, 0, length);
        }
        Map<String, Object> fixture = new JsonConvertJackson().strToMap(bytes.toString("UTF-8"));
        ConditionStrategySpel strategy = new ConditionStrategySpel();
        for (Map<String, Object> test : (List<Map<String, Object>>) fixture.get("cases")) {
            Map<String, Object> variables = (Map<String, Object>) test.get("variables");
            assertEquals(test.get("name").toString(), test.get("any"),
                strategy.eval(fixture.get("anyExpression").toString().substring(6), variables));
            assertEquals(test.get("name").toString(), test.get("all"),
                strategy.eval(fixture.get("allExpression").toString().substring(6), variables));
        }
    }
    @Test public void handlesObjectPathsPrimitiveArraysAndCounts() {
        new SpelHelper().setApplicationContext(new StaticApplicationContext());
        Map<String, Object> variables = new HashMap<>();
        variables.put("address", Collections.singletonMap("city", "上海"));
        variables.put("tags", java.util.Arrays.asList("urgent", "travel"));
        assertEquals(true, SpelHelper.parseExpression("#{#address != null and #address['city'] == '上海'}", variables));
        assertEquals(true, SpelHelper.parseExpression("#{#tags != null and #tags.size() > 0 and #tags.?[(#this != null and #this == 'travel')].size() > 0}", variables));
        assertEquals(true, SpelHelper.parseExpression("#{#tags != null and #tags.size() == 2}", variables));
    }
}
