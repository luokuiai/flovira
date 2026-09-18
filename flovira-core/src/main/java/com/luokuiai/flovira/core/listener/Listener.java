/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.listener;

import java.io.Serializable;

/**
 * 独立表单加载扩展；流程转换请使用 WorkflowLifecycleListener
 *
 * @author warm
 */
public interface Listener extends Serializable {

    /**
     * 表单数据加载监听器，1.3.0 内置表单使用
     */
    String LISTENER_FORM_LOAD = "formLoad";

    /**
     * 通知
     *
     * @param variable variable
     */
    void notify(ListenerVariable variable);
}
