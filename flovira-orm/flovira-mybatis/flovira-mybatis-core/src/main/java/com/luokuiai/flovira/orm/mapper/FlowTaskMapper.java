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

import org.apache.ibatis.annotations.Param;
import com.luokuiai.flovira.orm.entity.FlowTask;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Date;

/**
 * 待办任务Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowTaskMapper extends FloviraMapper<FlowTask> {

    /**
     * 根据instanceIds删除
     *
     * @param instanceIds 主键
     * @return 结果
     */
    int deleteByInsIds(@Param("instanceIds") Collection<? extends Serializable> instanceIds
        , @Param("entity") FlowTask entity);

    /**
     * 逻辑删除
     *
     * @param instanceIds 需要删除的数据主键集合
     * @return 结果
     */
    int updateByInsIdsLogic(@Param("instanceIds") Collection<? extends Serializable> instanceIds
        , @Param("entity") FlowTask entity
        , @Param("logicDeleteValue") String logicDeleteValue
        , @Param("logicNotDeleteValue") String logicNotDeleteValue);

    /**
     * 根据实例id和节点code查询
     *
     * @param instanceId 实例id
     * @param nodeCodes  节点code
     * @param entity     实体
     * @return 结果
     */
    List<FlowTask> getByInsIdAndNodeCodes(@Param("instanceId") Long instanceId,
                                          @Param("nodeCodes") List<String> nodeCodes,
                                          @Param("entity") FlowTask entity);

    List<FlowTask> getByInsIdAndNodeType(@Param("instanceId") Long instanceId,
                                         @Param("nodeType") Integer nodeType,
                                         @Param("entity") FlowTask entity);

    List<FlowTask> listByInsIds(@Param("instanceIds") List<Long> instanceIds,
                                @Param("entity") FlowTask entity);

    List<FlowTask> listDueTimeoutTasks(@Param("dueBefore") Date dueBefore,
                                       @Param("staleBefore") Date staleBefore,
                                       @Param("entity") FlowTask entity);

    int claimPendingTimeout(@Param("taskId") Long taskId, @Param("claimedAt") Date claimedAt,
                            @Param("entity") FlowTask entity);

    int claimStaleTimeout(@Param("taskId") Long taskId, @Param("claimedAt") Date claimedAt,
                          @Param("staleBefore") Date staleBefore, @Param("entity") FlowTask entity);

    int claimWait(@Param("taskId") Long taskId, @Param("claimedAt") Date claimedAt,
                  @Param("entity") FlowTask entity);

    int releaseTimeout(@Param("taskId") Long taskId, @Param("entity") FlowTask entity);
}
