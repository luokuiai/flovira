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

/** 宿主代码回调；业务范围由实现自行判断，不依赖流程／节点扩展配置。 */
public interface WorkflowLifecycleListener {
    /** 回调顺序；相同顺序按注册名称排序。 */
    default int getOrder() { return 0; }
    /** 事实事件的交付阶段；执行前回调始终在事务内。 */
    default DeliveryPhase getDeliveryPhase() { return DeliveryPhase.IN_TRANSACTION; }
    default void beforeOperation(OperationContext context) { }
    default void beforeAssignment(AssignmentContext context) { }
    default void onEvent(LifecycleEvent event) { }
}
