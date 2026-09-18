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
package com.luokuiai.flovira.core.orm.dao;

import com.luokuiai.flovira.core.entity.NodeExecution;
import java.util.Date;
import java.util.List;

/** 执行状态转换必须位于引擎操作事务内，查询显式限定租户及逻辑删除。 */
public interface FlowNodeExecutionDao<T extends NodeExecution> {
    int save(T execution);
    T get(String tenantId, Long id);
    List<T> listActive(String tenantId, Long instanceId);
    int close(String tenantId, Long id, int version, String reason, Date closedAt);
}
