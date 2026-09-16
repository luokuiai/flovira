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

import com.luokuiai.flovira.core.dto.FormDefinition;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.utils.FormDefinitionValidator;
import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.ServiceLoader;
import static org.junit.Assert.*;

public class NestedFormJsonContractTest {
    @Test public void preservesTypedObjectAndArrayMetadata() throws Exception {
        JsonConvert json = ServiceLoader.load(JsonConvert.class).iterator().next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = getClass().getResourceAsStream("/nested-form-conditions.json")) {
            assertNotNull(input);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) bytes.write(buffer, 0, length);
        }
        Map<String, Object> fixture = json.strToMap(bytes.toString("UTF-8"));
        FormDefinition definition = json.strToBean(json.objToStr(fixture.get("form")), FormDefinition.class);
        FormDefinitionValidator.validate(definition);
        FormDefinition copy = json.strToBean(json.objToStr(definition), FormDefinition.class);
        FormDefinitionValidator.validate(copy);
        assertEquals("城市", copy.getFields().get(0).getFields().get(0).getLabel());
        assertEquals("number", copy.getFields().get(1).getItems().getFields().get(0).getDataType());
        assertEquals("string", copy.getFields().get(2).getItems().getDataType());
        assertEquals("host-owned", copy.getRenderer().get("layout"));
    }
}
