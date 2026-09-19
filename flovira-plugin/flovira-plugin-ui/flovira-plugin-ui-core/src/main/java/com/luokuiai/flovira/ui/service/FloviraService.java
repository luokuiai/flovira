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
package com.luokuiai.flovira.ui.service;

import lombok.extern.slf4j.Slf4j;
import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.ApproverStrategyDefinition;
import com.luokuiai.flovira.core.handler.ApproverResolver;
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.core.dto.*;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.RootEntity;
import com.luokuiai.flovira.core.enums.ActivityStatus;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.core.utils.NodeConfigValidator;
import com.luokuiai.flovira.core.utils.FlowConfigUtil;
import com.luokuiai.flovira.core.utils.SubprocessDefinitionValidator;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.exception.FlowException;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.utils.ExceptionUtil;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.core.utils.page.Page;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.dto.FormContentRequest;
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

    /** 复用设计 JSON、包内表单和流程包格式，不增加导出包装。 */
    public static ApiResult<Object> exportData(String type, Long id) {
        requireTransferType(type);
        if (id == null) throw new IllegalArgumentException("导出 ID 不能为空");
        switch (type) {
            case "design":
                Definition definition = FlowEngine.defService().getById(id);
                requireTransferTenant(definition);
                return ApiResult.ok(FlowEngine.jsonConvert.strToBean(
                    FlowEngine.defService().exportJson(id), DefJson.class));
            case "form":
                Form form = FlowEngine.formService().getById(id);
                requireTransferTenant(form);
                return ApiResult.ok(new WorkflowPackage.PackagedForm().setReference(id.toString())
                    .setFormCode(form.getFormCode()).setFormName(form.getFormName())
                    .setVersion(form.getVersion()).setFormContent(form.getFormContent()).setExt(form.getExt()));
            default:
                return ApiResult.ok(FlowEngine.defService().exportPackage(id));
        }
    }

    /** 三种导入均创建新版本；流程包内的表单引用由引擎重建。 */
    public static ApiResult<WorkflowImportResult> importData(String type, Map<String, Object> data) {
        requireTransferType(type);
        if (data == null || data.isEmpty()) throw new IllegalArgumentException("导入内容不能为空");
        String json = FlowEngine.jsonConvert.objToStr(data);
        return ApiResult.ok(FlowEngine.transactionExecutor().execute(() -> {
            if ("package".equals(type)) {
                return FlowEngine.defService().importPackage(
                    FlowEngine.jsonConvert.strToBean(json, WorkflowPackage.class), Collections.emptyMap());
            }
            if ("form".equals(type)) {
                WorkflowPackage.PackagedForm source = FlowEngine.jsonConvert.strToBean(json, WorkflowPackage.PackagedForm.class);
                if (StringUtils.isEmpty(source.getReference()) || StringUtils.isEmpty(source.getVersion())
                    || StringUtils.isEmpty(source.getFormCode()) || source.getFormCode().length() > 40
                    || StringUtils.isEmpty(source.getFormName()) || source.getFormName().length() > 100
                    || StringUtils.isEmpty(source.getFormContent())) {
                    throw new IllegalArgumentException("表单元数据或内容不完整");
                }
                Form form = FlowEngine.newForm().setFormCode(source.getFormCode()).setFormName(source.getFormName())
                    .setFormContent(source.getFormContent()).setExt(source.getExt())
                    .setPublishStatus(PublishStatus.UNPUBLISHED.getKey());
                FlowEngine.formService().parseDefinition(form);
                prepareImportedEntity(form);
                if (!FlowEngine.formService().save(form) || form.getId() == null) {
                    throw new FlowException("保存导入表单失败");
                }
                return new WorkflowImportResult().setFormReferences(
                    Collections.singletonMap(source.getReference(), form.getId().toString()));
            }
            DefJson source = FlowEngine.jsonConvert.strToBean(json, DefJson.class);
            if (source.getNodeList() == null || source.getNodeList().isEmpty()) {
                throw new IllegalArgumentException("流程节点列表为空");
            }
            // 先确认目标表单存在；单独设计不能隐式复制表单或留下悬空引用。
            if (!StringUtils.isEmpty(source.getFormId())) {
                Long formId;
                try {
                    formId = Long.valueOf(source.getFormId());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("目标表单不可验证，请先导入表单并填写目标 formId");
                }
                if (!formId.toString().equals(source.getFormId())) {
                    throw new IllegalArgumentException("目标表单引用无效");
                }
                requireTransferTenant(FlowEngine.formService().getById(formId));
            }
            DefJson design = DefJson.copyDef(DefJson.copyDef(source)).setCreatedBy(null).setUpdatedBy(null);
            for (NodeJson node : design.getNodeList()) {
                node.setCreatedBy(null).setUpdatedBy(null);
                node.getSkipList().forEach(skip -> skip.setCreatedBy(null).setUpdatedBy(null));
            }
            Definition candidate = DefJson.copyDef(design);
            NodeConfigValidator.validate(candidate.getNodeList());
            SubprocessDefinitionValidator.validateNodeConfigs(candidate.getNodeList());
            FlowCombine flow = FlowConfigUtil.structureFlow(candidate);
            candidate.setActivityStatus(ActivityStatus.ACTIVITY.getKey());
            prepareImportedEntity(candidate);
            flow.getAllNodes().forEach(FloviraService::prepareImportedEntity);
            flow.getAllSkips().forEach(FloviraService::prepareImportedEntity);
            Definition imported = FlowEngine.defService().insertFlow(candidate, flow.getAllNodes(), flow.getAllSkips());
            return new WorkflowImportResult().setRootDefinitionId(imported.getId())
                .setDefinitionIds(Collections.singletonMap(imported.getFlowCode(), imported.getId()));
        }));
    }

    private static void requireTransferType(String type) {
        if (!"design".equals(type) && !"form".equals(type) && !"package".equals(type)) {
            throw new IllegalArgumentException("type 必须为 design、form 或 package");
        }
    }

    private static void prepareImportedEntity(RootEntity entity) {
        entity.setTenantId(FlowEngine.tenantHandler() == null ? null : FlowEngine.tenantHandler().getTenantId());
        entity.setCreatedBy(null).setUpdatedBy(null);
    }

    private static void requireTransferTenant(RootEntity entity) {
        if (entity == null || (FlowEngine.tenantHandler() != null
            && !Objects.equals(StringUtils.emptyDefault(entity.getTenantId(), "0"),
                StringUtils.emptyDefault(FlowEngine.tenantHandler().getTenantId(), "0")))) {
            throw new FlowException("导入导出引用的定义或表单不存在");
        }
    }

    /**
     * 返回流程定义的配置
     *
     * @return {@code ApiResult<FloviraVo>}
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
     * 返回设计器能力；人员策略仅来自业务注册的 Resolver。
     *
     * @return 设计器能力清单
     */
    public static ApiResult<DesignerCapabilities> capabilities() {
        DesignerCapabilityProvider provider = FrameInvoker.getBean(DesignerCapabilityProvider.class);
        DesignerCapabilities capabilities = provider == null ? null : provider.getCapabilities();
        if (capabilities == null) capabilities = DesignerCapabilities.defaults();
        List<ApproverStrategyDefinition> strategies = new ArrayList<>();
        for (ApproverResolver resolver : FlowEngine.approverResolvers().values()) {
            strategies.add(resolver.getDefinition());
        }
        return ApiResult.ok(capabilities.setApproverStrategies(strategies)
            .setSubmitterStrategies(com.luokuiai.flovira.core.utils.SubmitterRuleUtil.strategies()));
    }

    /**
     * 查询业务系统提供的设计器基础数据。
     *
     * @param query 资源查询
     * @return 分页资源
     */
    public static ApiResult<DesignerResourcePage> queryResources(DesignerResourceQuery query) {
        DesignerDataProvider provider = FrameInvoker.getBean(DesignerDataProvider.class);
        DesignerResourcePage page = provider == null ? null : provider.queryResources(query);
        if (page == null && query != null && "FORM".equals(query.getResourceType())) {
            page = managedFormResources(query);
        }
        return ApiResult.ok(page == null ? new DesignerResourcePage() : page);
    }

    private static DesignerResourcePage managedFormResources(DesignerResourceQuery query) {
        Page<Form> forms = FlowEngine.formService().publishedPage(query.getKeyword(),
            query.getPageNum(), query.getPageSize());
        List<DesignerResourceItem> items = new ArrayList<DesignerResourceItem>();
        if (forms.getList() != null) {
            for (Form form : forms.getList()) {
                DesignerResourceItem item = new DesignerResourceItem()
                    .setId(String.valueOf(form.getId()))
                    .setCode(form.getFormCode())
                    .setName(form.getFormName())
                    .setResourceType("FORM");
                item.getMetadata().put("version", form.getVersion());
                items.add(item);
            }
        }
        return new DesignerResourcePage().setItems(items).setTotal(forms.getTotal());
    }


    /**
     * 保存流程json字符串
     *
     * @param defJson      流程数据集合
     * @return {@code ApiResult<Void>}
     * @throws Exception 异常
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    public static ApiResult<Void> saveJson(DefJson defJson) throws Exception {
        FlowEngine.defService().saveDef(defJson);
        return ApiResult.ok();
    }

    /**
     * 获取流程定义数据(包含节点和跳转)
     *
     * @param id 流程定义id
     * @return {@code ApiResult<DefVo>}
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    public static ApiResult<DefJson> queryDef(Long id) {
        try {
            DefJson defJson;
            if (id == null) {
                defJson = new DefJson();
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
     * @return {@code ApiResult<DefJson>}
     */
    public static ApiResult<DefJson> queryFlowChart(Long id) {
        try {
            Instance instance = FlowEngine.instanceService().getById(id);
            String defJsonStr = instance.getDefJson();
            DefJson defJson = FlowEngine.jsonConvert.strToBean(defJsonStr, DefJson.class);
            defJson.setInstance(instance);

            // 获取流程图三原色
            defJson.setChartStatusColor(FlowEngine.chartService().getChartRgb());
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

    /** 读取 Flovira 管理的表单内容。 */
    public static ApiResult<String> getFormContent(Long id) {
        try {
            Form form = FlowEngine.formService().getById(id);
            if (form == null) {
                throw new FlowException(com.luokuiai.flovira.core.constant.ExceptionCons.NOT_FOUND_FORM);
            }
            return ApiResult.ok(form.getFormContent());
        } catch (Exception e) {
            log.error("获取表单内容失败 - id: {}", id, e);
            throw new FlowException(ExceptionUtil.handleMsg("获取表单内容失败", e));
        }
    }

    /** 保存 Flovira 管理的表单内容。 */
    public static ApiResult<Void> saveFormContent(FormContentRequest request) {
        FlowEngine.formService().saveContent(request.getId(), request.getFormContent());
        return ApiResult.ok();
    }




    /**
     * 根据任务id获取待办任务业务表单标识及数据
     *
     * @param taskId 当前任务id
     * @return {@link ApiResult<FlowDto>}
     * @author liangli
     * Date: 2024/8/21 17:08
     **/
    public static ApiResult<FlowDto> load(Long taskId) {
        FlowParams flowParams = FlowParams.build();

        return ApiResult.ok(FlowEngine.taskService().load(taskId, flowParams));
    }

    /**
     * 根据任务id获取已办任务业务表单标识及数据
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
