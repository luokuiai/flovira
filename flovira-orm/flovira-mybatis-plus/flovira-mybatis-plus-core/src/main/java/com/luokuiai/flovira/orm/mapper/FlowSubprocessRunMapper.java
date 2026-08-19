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

import com.luokuiai.flovira.orm.entity.FlowSubprocessRun;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 子流程运行 Mapper。 @author warm */
public interface FlowSubprocessRunMapper extends FloviraMapper<FlowSubprocessRun> {
    @Select("select * from flow_subprocess_run where id=#{id} and tenant_id=#{tenantId} and del_flag='0' for update")
    FlowSubprocessRun lockById(@Param("tenantId") String tenantId, @Param("id") Long id);
    @Select("select * from flow_subprocess_run with (updlock,rowlock) where id=#{id} and tenant_id=#{tenantId} and del_flag='0'")
    FlowSubprocessRun lockByIdSqlServer(@Param("tenantId") String tenantId, @Param("id") Long id);
    @Select("select * from flow_subprocess_run where parent_instance_id=#{parentId} and tenant_id=#{tenantId} and del_flag='0' and run_status in ('INITIALIZING','RUNNING','READY_TO_RESUME','RESUMING','FAILED','CANCELLING') order by id for update")
    List<FlowSubprocessRun> lockActive(@Param("tenantId") String tenantId, @Param("parentId") Long parentId);
    @Select("select * from flow_subprocess_run with (updlock,rowlock) where parent_instance_id=#{parentId} and tenant_id=#{tenantId} and del_flag='0' and run_status in ('INITIALIZING','RUNNING','READY_TO_RESUME','RESUMING','FAILED','CANCELLING') order by id")
    List<FlowSubprocessRun> lockActiveSqlServer(@Param("tenantId") String tenantId, @Param("parentId") Long parentId);
}
