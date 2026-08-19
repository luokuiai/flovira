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
package com.luokuiai.flovira.ui.service;

import lombok.extern.slf4j.Slf4j;
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.*;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.FormCustomEnum;
import com.luokuiai.flovira.core.enums.ModelEnum;
import com.luokuiai.flovira.core.exception.FlowException;
import com.luokuiai.flovira.core.handler.BusinessRelationProvider;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.utils.ExceptionUtil;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.core.utils.page.Page;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.vo.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设计器Controller 可选择是否放行，放行可与业务系统共享权限，主要是用来访问业务系统数据
 *
 * @author warm
 */
@Slf4j
public class FloviraService {

    /**
     * 返回流程定义的配置
     *
     * @return ApiResult<FloviraVo>
     */
    public static ApiResult<FloviraVo> config() {
        FloviraVo floviraVo = new FloviraVo();
        Flovira flovira = FlowEngine.getFlowConfig();
        floviraVo.setFramework(flovira.getFramework().name());
        // 获取tokenName
        String tokenName = flovira.getTokenName();
        if (StringUtils.isEmpty(tokenName)) {
            return ApiResult.fail("未配置tokenName");
        }
        String[] tokenNames = tokenName.split(",");
        List<String> tokenNameList = Arrays.stream(tokenNames).filter(StringUtils::isNotEmpty)
            .map(String::trim).collect(Collectors.toList());
        floviraVo.setTokenNameList(tokenNameList);

        return ApiResult.ok(floviraVo);
    }

    /**
     * 返回业务系统声明的设计器能力；未声明时使用 Flovira 完整默认能力。
     *
     * @return 设计器能力清单
     */
    public static ApiResult<DesignerCapabilities> capabilities() {
        DesignerCapabilityProvider provider = FrameInvoker.getBean(DesignerCapabilityProvider.class);
        DesignerCapabilities capabilities = provider == null ? null : provider.getCapabilities();
        return ApiResult.ok(capabilities == null ? DesignerCapabilities.defaults() : capabilities);
    }

    /**
     * 查询业务系统提供的设计器基础数据。
     *
     * @param query 资源查询
     * @return 分页资源
     */
    public static ApiResult<DesignerResourcePage> queryResources(DesignerResourceQuery query) {
        DesignerDataProvider provider = FrameInvoker.getBean(DesignerDataProvider.class);
        if (provider == null) {
            return ApiResult.ok(new DesignerResourcePage());
        }
        DesignerResourcePage page = provider.queryResources(query);
        return ApiResult.ok(page == null ? new DesignerResourcePage() : page);
    }

    /**
     * 委托业务系统解析组织和角色关系。流程执行语义仍由 core 负责。
     *
     * @param query 关系查询
     * @return 主体引用
     */
    public static ApiResult<List<BusinessSubject>> resolveRelationship(BusinessRelationQuery query) {
        BusinessRelationProvider provider = FlowEngine.businessRelationProvider();
        if (provider == null) {
            return ApiResult.ok(Collections.emptyList());
        }
        List<BusinessSubject> subjects = provider.resolveRelationship(query);
        return ApiResult.ok(subjects == null ? Collections.emptyList() : subjects);
    }

    /**
     * 保存流程json字符串
     *
     * @param defJson      流程数据集合
     * @param onlyNodeSkip 是否只保存节点和跳转
     * @return ApiResult<Void>
     * @throws Exception 异常
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    public static ApiResult<Void> saveJson(DefJson defJson, boolean onlyNodeSkip) throws Exception {
        FlowEngine.defService().saveDef(defJson, onlyNodeSkip);
        return ApiResult.ok();
    }

    /**
     * 获取流程定义数据(包含节点和跳转)
     *
     * @param id 流程定义id
     * @return ApiResult<DefVo>
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    public static ApiResult<DefJson> queryDef(Long id) {
        try {
            DefJson defJson;
            if (id == null) {
                defJson = new DefJson()
                    .setModelValue(ModelEnum.CLASSICS.name())
                    .setFormCustom(FormCustomEnum.N.name());
            } else {
                defJson = FlowEngine.defService().queryDesign(id);
            }
            return ApiResult.ok(defJson);
        } catch (Exception e) {
            log.error("获取流程json字符串", e);
            throw new FlowException(ExceptionUtil.handleMsg("获取流程json字符串失败", e));
        }
    }

    /**
     * 获取流程图
     *
     * @param id 流程实例id
     * @return ApiResult<DefJson>
     */
    public static ApiResult<DefJson> queryFlowChart(Long id) {
        try {
            Instance instance = FlowEngine.insService().getById(id);
            String defJsonStr = instance.getDefJson();
            DefJson defJson = FlowEngine.jsonConvert.strToBean(defJsonStr, DefJson.class);
            defJson.setInstance(instance);

            // 获取流程图三原色
            defJson.setChartStatusColor(FlowEngine.chartService().getChartRgb(defJson.getModelValue()));
            // 是否显示流程图顶部文字
            defJson.setTopTextShow(FlowEngine.getFlowConfig().isTopTextShow());
            List<Task> tasks = FlowEngine.taskService().getByInsId(instance.getId());
            for (NodeJson node : defJson.getNodeList()) {
                if (!NodeType.isSubProcess(node.getNodeType())) {
                    continue;
                }
                for (Task task : tasks) {
                    if (Objects.equals(node.getNodeCode(), task.getNodeCode())) {
                        node.setSubprocessSummary(FlowEngine.subprocessService()
                            .getSummary(task.getTenantId(), task.getId()));
                        break;
                    }
                }
            }
            // 需要业务系统实现该接口
            ChartExtService chartExtService = FrameInvoker.getBean(ChartExtService.class);
            if (chartExtService != null) {
                chartExtService.initPromptContent(defJson);
                chartExtService.execute(defJson);
            }

            return ApiResult.ok(defJson);
        } catch (Exception e) {
            log.error("获取流程图", e);
            throw new FlowException(ExceptionUtil.handleMsg("获取流程图失败", e));
        }
    }

    public static ApiResult<SubprocessSummary> subprocessSummary(Long parentTaskId) {
        return ApiResult.ok(FlowEngine.subprocessService().getSummary(currentTenantId(), parentTaskId));
    }

    public static ApiResult<Page<SubprocessChildSummary>> subprocessChildren(Long runId, int pageNum, int pageSize) {
        Page<SubprocessChildSummary> page = new Page<SubprocessChildSummary>(pageNum, pageSize);
        return ApiResult.ok(FlowEngine.subprocessService().pageChildSummaries(currentTenantId(), runId, page));
    }

    public static ApiResult<List<SubprocessEvent>> subprocessEvents(Long runId) {
        return ApiResult.ok(FlowEngine.subprocessService().listEvents(currentTenantId(), runId));
    }

    public static ApiResult<List<SubprocessHistoryEntry>> subprocessHistory(Long runId, Long childId) {
        return ApiResult.ok(FlowEngine.subprocessService().listCombinedHistory(currentTenantId(), runId, childId));
    }

    private static String currentTenantId() {
        return FlowEngine.tenantHandler() == null ? "0" : FlowEngine.tenantHandler().getTenantId();
    }

    /**
     * 读取表单内容
     *
     * @param id
     * @return
     */
    public static ApiResult<String> getFormContent(Long id) {
        try {
            return ApiResult.ok(FlowEngine.formService().getById(id).getFormContent());
        } catch (Exception e) {
            log.error("获取表单内容字符串", e);
            throw new FlowException(ExceptionUtil.handleMsg("获取表单内容字符串失败", e));
        }
    }

    /**
     * 保存表单内容,该接口不需要系统实现
     *
     * @param flowDto
     * @return
     */
    public static ApiResult<Void> saveFormContent(FlowDto flowDto) {
        FlowEngine.formService().saveContent(flowDto.getId(), flowDto.getFormContent());
        return ApiResult.ok();
    }


    /**
     * 根据任务id获取待办任务表单及数据
     *
     * @param taskId 当前任务id
     * @return {@link ApiResult<FlowDto>}
     * @author liangli
     * @date 2024/8/21 17:08
     **/
    public static ApiResult<FlowDto> load(Long taskId) {
        FlowParams flowParams = FlowParams.build();

        return ApiResult.ok(FlowEngine.taskService().load(taskId, flowParams));
    }

    /**
     * 根据任务id获取已办任务表单及数据
     *
     * @param hisTaskId
     * @return
     */
    public static ApiResult<FlowDto> hisLoad(Long hisTaskId) {
        FlowParams flowParams = FlowParams.build();

        return ApiResult.ok(FlowEngine.taskService().hisLoad(hisTaskId, flowParams));
    }

    /**
     * 通用表单流程审批接口
     *
     * @param formData
     * @param taskId
     * @param skipType
     * @param message
     * @param nodeCode
     * @return
     */
    public static ApiResult<Instance> handle(Map<String, Object> formData, Long taskId, String skipType
        , String message, String nodeCode) {
        FlowParams flowParams = FlowParams.build()
            .skipType(skipType)
            .nodeCode(nodeCode)
            .message(message);

        flowParams.formData(formData);

        return ApiResult.ok(FlowEngine.taskService().skip(taskId, flowParams));
    }

}
