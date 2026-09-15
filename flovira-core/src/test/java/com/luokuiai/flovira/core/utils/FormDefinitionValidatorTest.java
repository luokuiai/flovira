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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.dto.FormDefinition;
import com.luokuiai.flovira.core.dto.FormFieldDefinition;
import com.luokuiai.flovira.core.exception.FlowException;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertThrows;

public class FormDefinitionValidatorTest {
    private FormFieldDefinition field(String key, String type) {
        return new FormFieldDefinition().setKey(key).setLabel(key).setDataType(type);
    }
    private FormDefinition form(FormFieldDefinition... fields) {
        return new FormDefinition().setFields(Arrays.asList(fields));
    }
    @Test public void acceptsNestedObjectsAndTypedArrayItems() {
        FormFieldDefinition item = new FormFieldDefinition().setDataType("object")
            .setFields(Collections.singletonList(field("amount", "number")));
        FormDefinitionValidator.validate(form(field("address", "object")
            .setFields(Collections.singletonList(field("city", "string"))),
            field("details", "array").setItems(item),
            field("tags", "array").setItems(new FormFieldDefinition().setDataType("string"))));
    }
    @Test public void rejectsMissingShapeNamesAndDuplicateKeys() {
        assertThrows(FlowException.class, () -> FormDefinitionValidator.validate(form(field("object", "object"))));
        assertThrows(FlowException.class, () -> FormDefinitionValidator.validate(form(field("array", "array"))));
        assertThrows(FlowException.class, () -> FormDefinitionValidator.validate(form(field("name", "string").setLabel(""))));
        assertThrows(FlowException.class, () -> FormDefinitionValidator.validate(form(field("a", "string"), field("a", "number"))));
        assertThrows(FlowException.class, () -> FormDefinitionValidator.validate(form(field("a", "number").setItems(field("b", "string")))));
    }
    @Test public void rejectsCyclesWithoutStackOverflow() {
        FormFieldDefinition recursive = field("recursive", "object");
        recursive.setFields(Collections.singletonList(recursive));
        assertThrows(FlowException.class, () -> FormDefinitionValidator.validate(form(recursive)));
    }
}
