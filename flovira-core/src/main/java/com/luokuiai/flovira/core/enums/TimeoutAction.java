/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.enums;

/**
 * 节点超时动作
 *
 * @author warm
 */
public enum TimeoutAction {
    AUTO_PASS,
    AUTO_REJECT,
    RESUME_WAIT;

    public static boolean isApprovalAction(String action) {
        return AUTO_PASS.name().equals(action) || AUTO_REJECT.name().equals(action);
    }

    public static boolean isWaitAction(String action) {
        return RESUME_WAIT.name().equals(action);
    }
}
