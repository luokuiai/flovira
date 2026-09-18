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
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleConfigResolver;
import com.luokuiai.flovira.core.listener.lifecycle.LifecycleSubscription;
import java.util.List;

/** 在保存、导入和发布时统一校验跨作用域订阅。 */
public final class LifecycleConfigUtil {
    private LifecycleConfigUtil() { }

    public static void validate(Definition definition, List<Node> nodes) {
        rejectLegacy(definition.getListenerType(), definition.getListenerPath());
        LifecycleConfigResolver resolver = new LifecycleConfigResolver(FlowEngine.jsonConvert,
            FlowEngine.lifecycleListeners());
        List<LifecycleSubscription> subscriptions = resolver.read(definition.getExt(), null);
        if (nodes == null) return;
        for (Node node : nodes) {
            rejectLegacy(node.getListenerType(), node.getListenerPath());
            resolver.combine(subscriptions, resolver.read(node.getExt(), node.getNodeType()));
        }
    }
    private static void rejectLegacy(String types, String paths) {
        if (StringUtils.isEmpty(types) && StringUtils.isEmpty(paths)) return;
        if (StringUtils.isEmpty(types)) throw new IllegalArgumentException("Legacy listener path requires explicit migration");
        for (String type : types.split(",", -1)) {
            if (!"formLoad".equals(type.trim())) {
                throw new IllegalArgumentException("Legacy callback " + type + " must migrate to lifecycle subscriptions; only formLoad remains separate");
            }
        }
    }
}
