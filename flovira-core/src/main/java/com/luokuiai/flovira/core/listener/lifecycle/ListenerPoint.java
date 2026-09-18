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
package com.luokuiai.flovira.core.listener.lifecycle;

/** 订阅处理点，前置处理与不可变事实分开。 */
public enum ListenerPoint {
    BEFORE_OPERATION, BEFORE_ASSIGNMENT,
    PROCESS_STARTED, PROCESS_WITHDRAWN, PROCESS_RESUBMITTED, PROCESS_ENDED,
    NODE_ENTERED, NODE_LEFT, APPROVAL_ACTION_COMPLETED, ASSIGNEES_CHANGED;

    public boolean isBefore() {
        return this == BEFORE_OPERATION || this == BEFORE_ASSIGNMENT;
    }

    public static ListenerPoint event(LifecycleEventType type) {
        return valueOf(java.util.Objects.requireNonNull(type, "event type").name());
    }
}
