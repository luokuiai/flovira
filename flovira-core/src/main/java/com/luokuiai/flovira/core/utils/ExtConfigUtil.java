/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import java.util.LinkedHashMap;
import java.util.Map;

/** 读取 JSON 扩展对象，不修改原始数据。 */
public final class ExtConfigUtil {
    private ExtConfigUtil() { }

    public static Map<String, String> read(String ext) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (ext == null || ext.trim().isEmpty()) return values;
        if (!ext.trim().startsWith("{")) throw new IllegalArgumentException("Ext must be a JSON object");
        Map<String, Object> object = FlowEngine.jsonConvert.strToMap(ext);
        if (object == null) throw new IllegalArgumentException("Ext must be a JSON object");
        for (Map.Entry<String, Object> entry : object.entrySet()) put(values, entry.getKey(), entry.getValue());
        return values;
    }

    private static void put(Map<String, String> values, String code, Object value) {
        if (code == null || code.isEmpty() || value == null) return;
        values.put(code, value instanceof String ? (String) value : FlowEngine.jsonConvert.objToStr(value));
    }
}
