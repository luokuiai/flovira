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

import java.util.List;

/** 子流程运行 Mapper。 @author warm */
public interface FlowSubprocessRunMapper {
    int insert(FlowSubprocessRun entity);
    int updateById(FlowSubprocessRun entity);
    FlowSubprocessRun findByParentTask(@Param("tenantId") String tenantId, @Param("parentTaskId") Long parentTaskId);
    FlowSubprocessRun findById(@Param("tenantId") String tenantId, @Param("runId") Long runId);
    FlowSubprocessRun lockById(@Param("tenantId") String tenantId, @Param("runId") Long runId,
        @Param("dataSourceType") String dataSourceType);
    int claimReadyToResume(@Param("tenantId") String tenantId, @Param("runId") Long runId);
    List<FlowSubprocessRun> lockActiveByParent(@Param("tenantId") String tenantId,
        @Param("parentInstanceId") Long parentInstanceId, @Param("dataSourceType") String dataSourceType);
    List<FlowSubprocessRun> findReconcileCandidates();
}
