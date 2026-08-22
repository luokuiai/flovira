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
package com.luokuiai.flovira.core.service;

import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.orm.service.IFloviraService;

import java.util.List;

/**
 * 流程实例Service接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface InsService extends IFloviraService<Instance> {

    /**
     * 传入业务id开启流程
     *
     * @param businessId: 业务id[必传]
     * @param flowParams: 包含流程相关参数的对象
     *                    - flowCode: 流程编码 [必传]
     *                    - handler: 当前办理人唯一标识[必传]
     *                    - variable: 流程变量[按需传输]
     *                    - nextHandler: 执行的下个任务的办理人[按需传输]
     *                    - nextHandlerAppend: 个任务处理人配置类型（true-追加，false-覆盖，默认false）[按需传输]
     *                    - flowStatus: 流程状态，自定义流程状态[按需传输]
     *                    - ext: 扩展字段，预留给业务系统使用[按需传输]
     * @return 流程实例
     */
    Instance start(String businessId, FlowParams flowParams);

    /**
     * 使用独立业务类型和业务id开启流程
     *
     * @param businessType 业务类型
     * @param businessId   业务id
     * @param flowParams   流程参数
     * @return 流程实例
     */
    Instance start(String businessType, String businessId, FlowParams flowParams);

    /**
     * 使用明确的流程定义版本开启流程
     *
     * @param businessId 业务id
     * @param definitionId 流程定义id
     * @param flowParams 流程参数
     * @return 流程实例
     */
    Instance startByDefinitionId(String businessId, Long definitionId, FlowParams flowParams);

    Instance startByDefinitionId(String businessType, String businessId, Long definitionId, FlowParams flowParams);

    /**
     * 根据业务类型和业务id查询流程实例
     *
     * @param businessType 业务类型
     * @param businessId   业务id
     * @return 流程实例集合
     */
    List<Instance> listByBusinessKey(String businessType, String businessId);

    /**
     * 根据实例ids，删除流程
     *
     * @param instanceIds: 流程实例集合
     */
    boolean remove(List<Long> instanceIds);

    /**
     * 根据流程定义id，查询流程实例集合
     *
     * @param definitionId 流程定义id
     * @return {@code List<Instance>}
     */
    List<Instance> getByDefId(Long definitionId);

    /**
     * 激活实例
     *
     * @param id 流程实例id
     */
    boolean active(Long id);

    /**
     * 挂起实例，流程实例挂起后，该流程实例无法继续流转
     *
     * @param id 流程实例id
     */
    boolean unActive(Long id);

    /**
     * 根据流程定义id集合，查询流程实例集合
     *
     * @param defIds 流程定义id集合
     * @return 流程实例集合
     */
    List<Instance> listByDefIds(List<Long> defIds);


    /**
     * 按照流程变量key删除流程变量
     *
     * @param instanceId 流程实例id
     * @param keys       流程变量key
     */
    void removeVariables(Long instanceId, String... keys);
}
