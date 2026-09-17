/*
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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.FlowCombine;
import com.luokuiai.flovira.core.dto.NodeJson;
import com.luokuiai.flovira.core.dto.SkipJson;
import com.luokuiai.flovira.core.dto.WorkflowImportResult;
import com.luokuiai.flovira.core.dto.WorkflowPackage;
import com.luokuiai.flovira.core.dto.WorkflowPackage.PackagedForm;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.RootEntity;
import com.luokuiai.flovira.core.enums.ActivityStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.core.exception.FlowException;
import com.luokuiai.flovira.core.service.DefService;
import com.luokuiai.flovira.core.utils.FlowConfigUtil;
import com.luokuiai.flovira.core.utils.NodeConfigValidator;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.core.utils.SubprocessConfigUtil;
import com.luokuiai.flovira.core.utils.SubprocessDefinitionValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 流程包的依赖收集与导入编排，由 DefService 的事务入口调用。
 *
 * @author LuokuiAI
 */
final class WorkflowPackageTransfer {
    private final DefService definitions;

    WorkflowPackageTransfer(DefService definitions) {
        this.definitions = definitions;
    }

    WorkflowPackage exportPackage(Long definitionId) {
        require(definitionId != null, "流程定义 ID 不能为空");
        Definition root = definitions.getById(definitionId);
        require(root != null, "流程定义不存在");
        require(FlowEngine.tenantHandler() == null || Objects.equals(tenant(root.getTenantId()),
            tenant(FlowEngine.tenantHandler().getTenantId())), "流程定义不存在");
        WorkflowPackage result = new WorkflowPackage()
            .setSchemaVersion(WorkflowPackage.CURRENT_SCHEMA_VERSION).setRootFlowCode(root.getFlowCode());
        Map<String, Long> visited = new LinkedHashMap<>();
        Map<String, PackagedForm> forms = new LinkedHashMap<>();
        Set<String> external = new LinkedHashSet<>();
        collect(root, tenant(root.getTenantId()), result, visited, new LinkedHashSet<>(), forms, external);
        result.setForms(new ArrayList<>(forms.values())).setExternalFormIds(new ArrayList<>(external));
        return result;
    }

    private void collect(Definition definition, String sourceTenant, WorkflowPackage result,
                         Map<String, Long> visited, Set<String> path,
                         Map<String, PackagedForm> forms, Set<String> external) {
        require(Objects.equals(sourceTenant, tenant(definition.getTenantId())), "子流程属于其他租户");
        String code = definition.getFlowCode();
        require(path.add(code), "流程包存在循环子流程依赖: " + code);
        if (visited.containsKey(code)) {
            require(Objects.equals(visited.get(code), definition.getId()), "流程包包含同编码的不同版本: " + code);
            path.remove(code);
            return;
        }
        Definition full = definitions.getAllDataDefinition(definition.getId());
        require(full != null && Objects.equals(sourceTenant, tenant(full.getTenantId())), "流程定义不可访问");
        DefJson design = cleanDesign(DefJson.copyDef(full));
        // 在导出时也验证设计，避免生成无法重新导入的包。
        prepare(design);
        collectForm(design.getFormId(), sourceTenant, forms, external);
        for (Node node : full.getNodeList()) {
            if (NodeType.isSubProcess(node.getNodeType())) {
                String childCode = SubprocessConfigUtil.read(node).getFixedChildFlowCode();
                Definition child = definitions.getPublishByFlowCode(childCode);
                require(child != null && ActivityStatus.isActivity(child.getActivityStatus()),
                    "可运行子流程不存在: " + childCode);
                collect(child, sourceTenant, result, visited, path, forms, external);
            }
        }
        visited.put(code, definition.getId());
        result.getDefinitions().add(design);
        path.remove(code);
    }

    private void collectForm(String reference, String sourceTenant,
                             Map<String, PackagedForm> forms, Set<String> external) {
        if (StringUtils.isEmpty(reference) || forms.containsKey(reference) || external.contains(reference)) {
            return;
        }
        Form form = null;
        Long id = managedId(reference);
        if (id != null) {
            form = FlowEngine.formService().getById(id);
        }
        if (form == null) {
            external.add(reference);
            return;
        }
        require(Objects.equals(sourceTenant, tenant(form.getTenantId())), "表单属于其他租户");
        PackagedForm packaged = new PackagedForm().setReference(reference).setFormCode(form.getFormCode())
            .setFormName(form.getFormName()).setVersion(form.getVersion())
            .setFormContent(form.getFormContent()).setExt(form.getExt());
        validateForm(packaged);
        forms.put(reference, packaged);
    }

    WorkflowImportResult importPackage(WorkflowPackage input, Map<String, String> externalReferences) {
        require(input != null && Integer.valueOf(WorkflowPackage.CURRENT_SCHEMA_VERSION).equals(input.getSchemaVersion()),
            "不支持的流程包版本");
        require(input.getDefinitions() != null && !input.getDefinitions().isEmpty(), "流程包缺少设计");
        require(input.getForms() != null && input.getExternalFormIds() != null, "流程包缺少表单清单");
        Map<String, FlowCombine> flows = new LinkedHashMap<>();
        Set<String> referencedForms = new LinkedHashSet<>();
        for (DefJson source : input.getDefinitions()) {
            DefJson design = cleanDesign(source);
            FlowCombine flow = prepare(design);
            String code = design.getFlowCode();
            require(flows.put(code, flow) == null, "重复的流程编码: " + code);
            addReference(referencedForms, design.getFormId());
        }
        List<String> ordered = new ArrayList<>();
        order(input.getRootFlowCode(), flows, new LinkedHashSet<>(), new LinkedHashSet<>(), ordered);
        require(ordered.size() == flows.size(), "流程包包含根流程未引用的设计");

        Map<String, PackagedForm> forms = new LinkedHashMap<>();
        Set<String> declaredForms = new LinkedHashSet<>();
        for (PackagedForm form : input.getForms()) {
            validateForm(form);
            require(declaredForms.add(form.getReference()), "重复的表单引用: " + form.getReference());
            forms.put(form.getReference(), form);
        }
        Map<String, String> references = new LinkedHashMap<>();
        Map<String, String> supplied = externalReferences == null ? Collections.emptyMap() : externalReferences;
        for (String reference : input.getExternalFormIds()) {
            require(!StringUtils.isEmpty(reference) && declaredForms.add(reference), "重复或无效的外部表单引用");
            String target = supplied.get(reference);
            require(!StringUtils.isEmpty(target) && target.length() <= 100, "缺少有效的外部表单映射: " + reference);
            references.put(reference, target);
        }
        require(supplied.keySet().equals(references.keySet()), "外部表单映射包含未声明的引用");
        require(declaredForms.equals(referencedForms), "流程包表单清单与设计引用不一致");

        // 清单与依赖全部通过后再落库。目标租户和审计字段不接受包内数据。
        for (PackagedForm source : forms.values()) {
            Form form = FlowEngine.newForm().setFormCode(source.getFormCode()).setFormName(source.getFormName())
                .setFormContent(source.getFormContent()).setExt(source.getExt())
                .setPublishStatus(PublishStatus.UNPUBLISHED.getKey());
            targetEntity(form);
            require(FlowEngine.formService().save(form), "保存导入表单失败");
            require(form.getId() != null, "导入表单未生成 ID");
            references.put(source.getReference(), form.getId().toString());
        }
        WorkflowImportResult result = new WorkflowImportResult().setFormReferences(references);
        for (String code : ordered) {
            FlowCombine flow = flows.get(code);
            Definition definition = flow.getDefinition();
            definition.setFormId(remap(definition.getFormId(), references))
                .setPublishStatus(PublishStatus.UNPUBLISHED.getKey())
                .setActivityStatus(ActivityStatus.ACTIVITY.getKey());
            targetEntity(definition);
            require(definitions.checkAndSave(definition), "保存导入流程失败: " + code);
            for (Node node : flow.getAllNodes()) {
                node.setDefinitionId(definition.getId()).setVersion(definition.getVersion());
                targetEntity(node);
            }
            flow.getAllSkips().forEach(skip -> {
                skip.setDefinitionId(definition.getId());
                targetEntity(skip);
            });
            FlowEngine.nodeService().saveBatch(flow.getAllNodes());
            FlowEngine.skipService().saveBatch(flow.getAllSkips());
            result.getDefinitionIds().put(code, definition.getId());
        }
        return result.setRootDefinitionId(result.getDefinitionIds().get(input.getRootFlowCode()));
    }

    private static DefJson cleanDesign(DefJson source) {
        require(source != null && source.getNodeList() != null, "流程设计或节点列表为空");
        for (NodeJson node : source.getNodeList()) {
            require(node != null && node.getSkipList() != null, "无效的节点或连线列表");
            for (SkipJson skip : node.getSkipList()) {
                require(skip != null && Objects.equals(node.getNodeCode(), skip.getSourceNodeCode()),
                    "连线来源与所属节点不一致");
            }
        }
        // 使用现有白名单转换，去除源 ID、运行信息和未知字段，并避免修改调用方对象。
        DefJson copy = DefJson.copyDef(DefJson.copyDef(source));
        copy.setId(null).setCreatedBy(null).setUpdatedBy(null).setPublishStatus(null);
        for (NodeJson node : copy.getNodeList()) {
            node.setCreatedBy(null).setUpdatedBy(null);
            node.getSkipList().forEach(skip -> skip.setCreatedBy(null).setUpdatedBy(null));
        }
        return copy;
    }

    private static FlowCombine prepare(DefJson design) {
        require(!StringUtils.isEmpty(design.getFlowCode()) && design.getFlowCode().length() <= 40,
            "流程编码为空或超过 40 位");
        require(!StringUtils.isEmpty(design.getFlowName()) && design.getFlowName().length() <= 100,
            "流程名称为空或超过 100 位");
        require(!StringUtils.isEmpty(design.getBusinessType()) && design.getBusinessType().length() <= 128,
            "流程业务类型为空或超过 128 位");
        Definition definition = DefJson.copyDef(design);
        definition.setId(null);
        FlowCombine flow = FlowConfigUtil.structureFlow(definition);
        SubprocessDefinitionValidator.validateNodeConfigs(flow.getAllNodes());
        NodeConfigValidator.validate(flow.getAllNodes());
        return flow;
    }

    private static void order(String code, Map<String, FlowCombine> flows, Set<String> path,
                              Set<String> visited, List<String> ordered) {
        require(flows.containsKey(code), "流程包缺少根流程或子流程: " + code);
        require(path.add(code), "流程包存在循环子流程依赖: " + code);
        if (!visited.contains(code)) {
            for (Node node : flows.get(code).getAllNodes()) {
                if (NodeType.isSubProcess(node.getNodeType())) {
                    order(SubprocessConfigUtil.read(node).getFixedChildFlowCode(), flows, path, visited, ordered);
                }
            }
            visited.add(code);
            ordered.add(code);
        }
        path.remove(code);
    }

    private static void validateForm(PackagedForm form) {
        require(form != null && !StringUtils.isEmpty(form.getReference()), "无效的包内表单");
        require(!StringUtils.isEmpty(form.getFormCode()) && form.getFormCode().length() <= 40, "无效的表单编码");
        require(!StringUtils.isEmpty(form.getFormName()) && form.getFormName().length() <= 100, "无效的表单名称");
        require(!StringUtils.isEmpty(form.getVersion()), "缺少表单源版本");
        require(!StringUtils.isEmpty(form.getFormContent()), "表单内容为空: " + form.getReference());
    }

    private static void targetEntity(RootEntity entity) {
        entity.setTenantId(FlowEngine.tenantHandler() == null ? null : FlowEngine.tenantHandler().getTenantId());
        entity.setCreatedBy(null).setUpdatedBy(null);
    }

    private static String tenant(String value) {
        return StringUtils.isEmpty(value) ? "0" : value;
    }

    private static Long managedId(String reference) {
        try {
            Long id = Long.valueOf(reference);
            return id.toString().equals(reference) ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void addReference(Set<String> references, String reference) {
        if (!StringUtils.isEmpty(reference)) {
            references.add(reference);
        }
    }

    private static String remap(String reference, Map<String, String> references) {
        return StringUtils.isEmpty(reference) ? reference : references.get(reference);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new FlowException(message);
        }
    }
}
