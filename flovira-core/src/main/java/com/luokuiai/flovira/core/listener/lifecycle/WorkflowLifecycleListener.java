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

/**
 * 框架无关的工作流监听器。执行前上下文允许受控调整，事实事件不可修改。
 * 宿主通过注册表注册对象，既可全局订阅，也可供定义／节点按稳定代码引用。
 */
public interface WorkflowLifecycleListener {
    default void beforeOperation(OperationContext context, String parameters) { }
    default void beforeAssignment(AssignmentContext context, String parameters) { }
    default void onEvent(LifecycleEvent event, String parameters) { }
}
