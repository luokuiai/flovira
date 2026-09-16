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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 标准表单元数据校验，不解析宿主私有的渲染器配置或业务数据。 */
public final class FormDefinitionValidator {
    private FormDefinitionValidator() { }

    public static void validate(FormDefinition definition) {
        require(definition != null && FormDefinition.VERSION_1.equals(definition.getSchemaVersion()), "不支持的表单定义版本");
        fields(definition.getFields(), "表单", 0);
    }

    private static void fields(List<FormFieldDefinition> fields, String path, int depth) {
        require(fields != null, path + "缺少字段定义");
        Set<String> keys = new HashSet<>();
        for (FormFieldDefinition field : fields) {
            require(field != null && field.getKey() != null
                && field.getKey().matches("[A-Za-z_][A-Za-z0-9_]*"), path + "字段编码不合法");
            require(keys.add(field.getKey()), path + "字段编码重复: " + field.getKey());
            require(depth != 0 || !("this".equals(field.getKey()) || "root".equals(field.getKey())), "顶层字段编码不能使用 this 或 root");
            require(field.getLabel() != null && !field.getLabel().trim().isEmpty(), path + "字段名称不能为空");
            field(field, path + " / " + field.getLabel(), depth);
        }
    }

    private static void field(FormFieldDefinition field, String path, int depth) {
        require(field != null && depth < 32, path + "字段嵌套过深或存在循环引用");
        String type = field.getDataType();
        if (FormFieldDefinition.OBJECT.equals(type)) {
            require(field.getFields() != null && !field.getFields().isEmpty(), path + "对象必须定义子字段");
            require(field.getItems() == null, path + "对象不能定义数组元素");
            fields(field.getFields(), path, depth + 1);
        } else if (FormFieldDefinition.ARRAY.equals(type)) {
            require(field.getFields() == null || field.getFields().isEmpty(), path + "数组子字段应定义在 items 中");
            require(field.getItems() != null, path + "数组必须定义元素类型");
            field(field.getItems(), path + " / 每一项", depth + 1);
        } else {
            require(Arrays.asList(FormFieldDefinition.STRING, FormFieldDefinition.NUMBER, FormFieldDefinition.BOOLEAN,
                FormFieldDefinition.DATE, FormFieldDefinition.DATETIME).contains(type), path + "字段类型不支持");
            require((field.getFields() == null || field.getFields().isEmpty()) && field.getItems() == null,
                path + "基础类型不能包含子字段");
        }
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new FlowException(message);
    }
}
