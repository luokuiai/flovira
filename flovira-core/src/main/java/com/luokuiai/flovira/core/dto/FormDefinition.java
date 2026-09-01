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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flovira 标准表单定义。
 *
 * @author warm
 * @since 2026/9/1
 */
@Getter
@Setter
@Accessors(chain = true)
public class FormDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String VERSION_1 = "1";

    private String schemaVersion = VERSION_1;

    private List<FormFieldDefinition> fields = new ArrayList<FormFieldDefinition>();

    /**
     * 可选渲染器配置，core 不解析其内容。渲染控件应使用字段 key 绑定数据。
     */
    private Map<String, Object> renderer;
}
