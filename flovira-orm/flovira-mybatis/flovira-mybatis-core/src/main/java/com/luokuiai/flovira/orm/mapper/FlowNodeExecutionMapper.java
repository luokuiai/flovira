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
package com.luokuiai.flovira.orm.mapper;

import com.luokuiai.flovira.orm.entity.FlowNodeExecution;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

/** 使用显式租户谓词的节点执行映射，两套 ORM 共享相同转换条件。 */
public interface FlowNodeExecutionMapper {
    int insert(FlowNodeExecution entity);
    FlowNodeExecution get(@Param("tenantId") String tenantId, @Param("id") Long id);
    List<FlowNodeExecution> listActive(@Param("tenantId") String tenantId, @Param("instanceId") Long instanceId);
    int close(@Param("tenantId") String tenantId, @Param("id") Long id, @Param("version") int version,
              @Param("reason") String reason, @Param("closedAt") Date closedAt);
}
