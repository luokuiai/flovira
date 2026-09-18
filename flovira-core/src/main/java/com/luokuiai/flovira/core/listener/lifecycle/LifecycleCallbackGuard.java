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
package com.luokuiai.flovira.core.listener.lifecycle;

import java.util.HashSet;
import java.util.Set;

/** 回调期间不允许同步推进同一实例；线程状态在异常路径也必须清理。 */
public final class LifecycleCallbackGuard {
    private static final ThreadLocal<Set<Long>> ACTIVE = new ThreadLocal<Set<Long>>();

    private LifecycleCallbackGuard() { }

    public static void check(Long instanceId) {
        Set<Long> active = ACTIVE.get();
        if (instanceId != null && active != null && active.contains(instanceId)) {
            throw new IllegalStateException("Recursive workflow advancement from a lifecycle callback");
        }
    }

    static void invoke(Long instanceId, Runnable callback) {
        if (instanceId == null) { callback.run(); return; }
        check(instanceId);
        Set<Long> active = ACTIVE.get();
        if (active == null) { active = new HashSet<Long>(); ACTIVE.set(active); }
        active.add(instanceId);
        try { callback.run(); }
        finally {
            active.remove(instanceId);
            if (active.isEmpty()) ACTIVE.remove();
        }
    }
}
