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
package com.luokuiai.flovira.core.orm.dao;

import com.luokuiai.flovira.core.entity.Task;

import java.util.List;
import java.util.Date;

/**
 * 待办任务Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowTaskDao<T extends Task> extends FloviraDao<T> {

    /**
     * 根据instanceIds删除
     *
     * @param instanceIds 主键
     * @return 结果
     */
    int deleteByInsIds(List<Long> instanceIds);

    List<T> getByInsIdAndNodeCodes(Long instanceId, List<String> nodeCodes);

    List<T> getByInsIdAndNodeType(Long instanceId, Integer nodeType);

    List<T> listByInsIds(List<Long> instanceIds);

    List<T> listDueTimeoutTasks(Date dueBefore, Date staleBefore, int limit);

    int claimTimeout(Long taskId, Date claimedAt, Date staleBefore);

    int claimWait(Long taskId, Date claimedAt);

    int releaseTimeout(Long taskId);
}
