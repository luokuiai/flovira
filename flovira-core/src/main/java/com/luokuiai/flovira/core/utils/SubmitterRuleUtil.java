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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.ApproverContext;
import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.ApproverStrategyDefinition;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.handler.ApproverResolver;
import java.util.ArrayList;
import java.util.List;

/**
 * 开始节点的提交范围。复用业务人员解析器，但不应用审批空人、转交或自动跳过策略。
 *
 * @author LuokuiAI
 */
public final class SubmitterRuleUtil {
    public static final String EXT_CONFIG = "submitterRule";
    public static final String ALL = "ALL";

    private SubmitterRuleUtil() {}

    public static List<ApproverStrategyDefinition> strategies() {
        List<ApproverStrategyDefinition> result = new ArrayList<ApproverStrategyDefinition>();
        result.add(new ApproverStrategyDefinition().setCode(ALL).setName("全员")
            .setSelectionType("RELATION").setEditorType("NONE").setMultiple(false));
        for (ApproverResolver resolver : FlowEngine.approverResolvers().values()) {
            if (!resolver.supportsSubmission()) continue;
            if (ALL.equals(resolver.getStrategy())) {
                throw new IllegalStateException("ALL is reserved for unrestricted submission");
            }
            ApproverStrategyDefinition source = resolver.getDefinition();
            ApproverStrategyDefinition target = new ApproverStrategyDefinition()
                .setCode(source.getCode()).setVersion(source.getVersion())
                .setName("USER".equals(source.getCode()) ? "指定人员" : "ROLE".equals(source.getCode()) ? "指定角色" : source.getName())
                .setSelectionType(source.getSelectionType()).setResourceType(source.getResourceType())
                .setRelationType(source.getRelationType()).setMultiple(source.isMultiple())
                .setMaxSubjects(source.getMaxSubjects()).setEditorType(source.getEditorType())
                .setEditorKey(source.getEditorKey()).setResultCardinality(source.getResultCardinality());
            // 仅暴露显式适用于开始节点的业务配置，避免混入会签、空人等审批策略。
            if (source.getOptions() != null) source.getOptions().stream()
                .filter(option -> option.getNodeTypes() != null && option.getNodeTypes().contains("0"))
                .forEach(option -> target.getOptions().add(option));
            result.add(target);
        }
        return result;
    }

    /** 缺少配置的已有流程保持全员可提交；存在但无效的配置不得退化成全员。 */
    public static ApproverRule read(Node node) {
        String value = FlowEngine.nodeService().getExt(node).get(EXT_CONFIG);
        if (value == null) return null;
        if (!NodeType.isStart(node.getNodeType()) || value.trim().isEmpty()) {
            throw new IllegalStateException("Submitter rule is only valid on a start node");
        }
        ApproverRule rule = FlowEngine.jsonConvert.strToBean(value, ApproverRule.class);
        validate(rule);
        return rule;
    }

    public static void validate(ApproverRule rule) {
        if (rule == null || rule.getSchemaVersion() != ApproverRule.CURRENT_SCHEMA_VERSION
            || StringUtils.isEmpty(rule.getStrategy())) {
            throw new IllegalStateException("Invalid submitter rule");
        }
        if (ALL.equals(rule.getStrategy())) {
            if (rule.getStrategyVersion() != 1 || !"RELATION".equals(rule.getSelectionType())
                || (rule.getSubjects() != null && !rule.getSubjects().isEmpty())
                || StringUtils.isNotEmpty(rule.getExpression()) || StringUtils.isNotEmpty(rule.getRelationType())
                || (rule.getConfig() != null && !rule.getConfig().isEmpty())) {
                throw new IllegalStateException("Invalid ALL submitter rule");
            }
            return;
        }
        ApproverResolver resolver = FlowEngine.approverResolver(rule.getStrategy());
        if (!resolver.supportsSubmission()) {
            throw new IllegalStateException("Strategy does not support submission: " + rule.getStrategy());
        }
        if (rule.getStrategyVersion() != resolver.getDefinition().getVersion()) {
            throw new IllegalStateException("Unsupported submitter configuration version: " + rule.getStrategy());
        }
        ApproverStrategyDefinition definition = resolver.getDefinition();
        if (!java.util.Objects.equals(rule.getSelectionType(), definition.getSelectionType())
            || rule.getSubjects() == null
            || (!definition.isMultiple() && rule.getSubjects().size() > 1)
            || (definition.getMaxSubjects() != null && rule.getSubjects().size() > definition.getMaxSubjects())) {
            throw new IllegalStateException("Invalid submitter selection");
        }
        if ("RESOURCE".equals(rule.getSelectionType()) && (rule.getSubjects().isEmpty()
            || rule.getSubjects().stream().anyMatch(subject -> subject == null || subject.getId() == null
                || subject.getId().trim().isEmpty() || !java.util.Objects.equals(subject.getType(), definition.getResourceType())))) {
            throw new IllegalStateException("Submitter resources are required");
        }
        if ("EXPRESSION".equals(rule.getSelectionType())
            && (rule.getExpression() == null || rule.getExpression().trim().isEmpty())) {
            throw new IllegalStateException("Submitter expression is required");
        }
        resolver.validate(rule);
    }

    /** 宿主认证后传入 handler；初始化钩子完成后、实例和任务持久化之前校验。 */
    public static void check(Node node, FlowParams params) {
        ApproverRule rule = read(node);
        if (rule == null || ALL.equals(rule.getStrategy())) return;
        if (params == null || params.getHandler() == null || params.getHandler().trim().isEmpty()) {
            throw new IllegalStateException("Submitter identity is required");
        }
        List<String> users = FlowEngine.approverResolver(rule.getStrategy())
            .resolve(new ApproverContext(node, rule, null, params, false));
        if (users == null || users.stream().anyMatch(user -> user == null || user.trim().isEmpty())) {
            throw new IllegalStateException("Submitter resolver returned invalid user IDs");
        }
        if (!users.contains(params.getHandler())) {
            throw new IllegalStateException("User is not allowed to submit this workflow");
        }
    }
}
