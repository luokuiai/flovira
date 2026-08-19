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
package com.luokuiai.flovira.orm.mapper;

import com.luokuiai.flovira.orm.entity.FlowSubprocessChild;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 子流程实例关系 Mapper。 @author warm */
public interface FlowSubprocessChildMapper extends FloviraMapper<FlowSubprocessChild> {
    @Select("select * from flow_subprocess_child where run_id=#{runId} and tenant_id=#{tenantId} and del_flag='0' order by id for update")
    List<FlowSubprocessChild> lockByRun(@Param("tenantId") String tenantId, @Param("runId") Long runId);
    @Select("select * from flow_subprocess_child with (updlock,rowlock) where run_id=#{runId} and tenant_id=#{tenantId} and del_flag='0' order by id")
    List<FlowSubprocessChild> lockByRunSqlServer(@Param("tenantId") String tenantId, @Param("runId") Long runId);
}
