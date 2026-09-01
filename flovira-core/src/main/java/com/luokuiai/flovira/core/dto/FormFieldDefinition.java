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
package com.luokuiai.flovira.core.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Flovira 标准表单字段定义。
 *
 * @author warm
 * @since 2026/9/1
 */
@Getter
@Setter
@Accessors(chain = true)
public class FormFieldDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STRING = "string";

    public static final String NUMBER = "number";

    public static final String BOOLEAN = "boolean";

    public static final String DATE = "date";

    public static final String DATETIME = "datetime";

    public static final String OBJECT = "object";

    public static final String ARRAY = "array";

    /**
     * 与 formData 顶层字段键一致的稳定标识。
     */
    private String key;

    /**
     * 字段显示名称。
     */
    private String label;

    /**
     * 字段数据类型，不表示 input、textarea 等 UI 组件。
     */
    private String dataType;
}
