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

import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Node;
import java.util.List;

/** 只检查旧监听字段；生命周期回调不读取业务扩展数据。 */
public final class LifecycleConfigUtil {
    private LifecycleConfigUtil() { }

    public static void validate(Definition definition, List<Node> nodes) {
        rejectLegacy(definition.getListenerType(), definition.getListenerPath());
        if (nodes == null) return;
        for (Node node : nodes) {
            rejectLegacy(node.getListenerType(), node.getListenerPath());
        }
    }
    private static void rejectLegacy(String types, String paths) {
        if (StringUtils.isEmpty(types) && StringUtils.isEmpty(paths)) return;
        throw new IllegalArgumentException("Persisted listener configuration is no longer supported; clear listenerType and listenerPath");
    }
}
