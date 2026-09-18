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
package com.luokuiai.flovira.orm.mapper;

import com.luokuiai.flovira.orm.entity.FlowInstance;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 流程实例Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowInstanceMapper extends FloviraMapper<FlowInstance> {

    @Select({"<script>SELECT * FROM flow_instance WHERE id=#{instanceId} AND deleted='0'",
        "<choose><when test='tenantId != null'>AND tenant_id=#{tenantId}</when>",
        "<otherwise>AND tenant_id IS NULL</otherwise></choose> FOR UPDATE</script>"})
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    FlowInstance lockForUpdate(@Param("tenantId") String tenantId, @Param("instanceId") Long instanceId);
}
