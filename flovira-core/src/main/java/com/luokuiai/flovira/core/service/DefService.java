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
package com.luokuiai.flovira.core.service;

import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.FlowCombine;
import com.luokuiai.flovira.core.dto.WorkflowPackage;
import com.luokuiai.flovira.core.dto.WorkflowImportResult;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Skip;
import com.luokuiai.flovira.core.orm.service.IFloviraService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 流程定义Service接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface DefService extends IFloviraService<Definition> {

    /**
     * 导出当前租户的完整流程包，包含固定子流程和所引用的托管表单版本。
     * @param definitionId 根流程定义 ID
     * @return 不含运行实例的可迁移流程包
     */
    WorkflowPackage exportPackage(Long definitionId);

    /**
     * 在事务中导入流程包，创建新 ID 和未发布版本，失败整体回滚。
     * @param workflowPackage 流程包
     * @param externalFormReferences 外部表单的源引用到目标引用映射，无外部表单时传空 Map
     * @return 新定义 ID 和表单引用映射
     */
    WorkflowImportResult importPackage(WorkflowPackage workflowPackage, Map<String, String> externalFormReferences);

    /**
     * 导入单份设计数据，不包含表单；跨环境迁移请使用 importPackage。
     *
     * @param is 流程定义的输入流
     */
    Definition importIs(InputStream is);

    /**
     * 导入流程定义、流程节点和流程跳转数据
     *
     * @param defStr 流程定义的json字符串 {@link DefJson 的json格式}
     */
    Definition importJson(String defStr);

    /**
     * 导入流程定义、流程节点和流程跳转数据
     *
     * @param defJson 流程定义json对象，流程定义、流程节点和流程跳转按照主子集传递
     */
    Definition importDef(DefJson defJson);

    /**
     * 新增工作流定义，并初始化流程节点和流程跳转数据
     *
     * @param definition 流程定义
     * @param nodeList   流程节点
     * @param skipList   流程跳转
     */
    Definition insertFlow(Definition definition, List<Node> nodeList, List<Skip> skipList);

    /**
     * 只新增流程定义表数据
     *
     * @param definition 流程定义对象
     * @return boolean
     */
    boolean checkAndSave(Definition definition);

    /**
     * 保存流程节点和跳转
     *
     * @param defJson      流程定义json对象
     * @author xiarg
     * @since 2024/10/29 16:30
     */
    void saveDef(DefJson defJson) throws Exception;

    /**
     * 导出单份设计 JSON，不包含表单；跨环境迁移请使用 exportPackage。
     *
     * @param id 流程定义id
     * @return json字符串
     */
    String exportJson(Long id);

    /**
     * 获取流程定义全部数据(包含节点和跳转)
     *
     * @param id 流程定义id
     * @return Definition
     */
    Definition getAllDataDefinition(Long id);

    /**
     * 流程数据集合
     *
     * @param id 流程定义id
     * @return FlowCombine
     */
    FlowCombine getFlowCombine(Long id);

    /**
     * 流程数据集合不包含流程定义
     *
     * @param id 流程定义id
     * @return FlowCombine
     */
    FlowCombine getFlowCombineNoDef(Long id);

    /**
     * 流程数据集合
     *
     * @param definition 流程定义
     * @return FlowCombine
     */
    FlowCombine getFlowCombine(Definition definition);

    /**
     * 查询流程设计所需的数据，比如流程图渲染
     *
     * @param id 流程定义id
     * @return 流程定义json对象
     */
    DefJson queryDesign(Long id);

    /**
     * 根据流程定义code列表查询流程定义
     *
     * @param flowCodeList 流程定义code列表
     * @return {@code List<Definition>}
     */
    List<Definition> queryByCodeList(List<String> flowCodeList);

    /**
     * 更新流程定义发布状态
     *
     * @param defIds        流程定义id列表
     * @param publishStatus 流程定义发布状态
     */
    void updatePublishStatus(List<Long> defIds, Integer publishStatus);

    /**
     * 删除流程定义相关数据
     *
     * @param ids 流程定义id列表
     * @return boolean
     */
    boolean removeDef(List<Long> ids);

    /**
     * 发布流程定义
     *
     * @param id 流程定义id
     * @return boolean
     */
    boolean publish(Long id);

    /**
     * 取消发布流程定义
     *
     * @param id 流程定义id
     * @return boolean
     */
    boolean unPublish(Long id);

    /**
     * 复制流程定义
     *
     * @param id 流程定义id
     * @return boolean
     */
    boolean copyDef(Long id);

    /**
     * 激活流程
     *
     * @param id 流程定义id
     */
    boolean active(Long id);

    /**
     * 挂起流程：流程定义挂起后，相关的流程实例都无法继续流转
     *
     * @param id 流程定义id
     */
    boolean unActive(Long id);

    /**
     * 根据流程定义code查询流程定义
     *
     * @param flowCode 流程定义code
     * @return {@code List<Definition>}
     */
    List<Definition> getByFlowCode(String flowCode);

    /**
     * 根据流程定义code查询已发布的流程定义
     *
     * @param flowCode 流程定义code
     * @return Definition
     */
    Definition getPublishByFlowCode(String flowCode);
}
