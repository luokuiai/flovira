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
import com.luokuiai.flovira.core.constant.ApproverStrategy;
import com.luokuiai.flovira.core.constant.FlowCons;
import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.BusinessRelationQuery;
import com.luokuiai.flovira.core.dto.BusinessSubject;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.handler.BusinessRelationProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批人规则读取与运行时解析。
 *
 * @author warm
 */
public final class ApproverRuleUtil {

    public static final String EXT_CONFIG = "approverRule";
    public static final String CARBON_COPY_EXT_CONFIG = "carbonCopyRule";
    public static final String SUBJECT_USER = "USER";
    public static final String SELECTION_RESOURCE = "RESOURCE";
    public static final String SELECTION_RELATION = "RELATION";
    public static final String SELECTION_EXPRESSION = "EXPRESSION";

    private ApproverRuleUtil() {
    }

    public static ApproverRule read(Node node) {
        return read(node, EXT_CONFIG);
    }

    public static ApproverRule read(Node node, String configCode) {
        String value = FlowEngine.nodeService().getExt(node).get(configCode);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        ApproverRule rule = FlowEngine.jsonConvert.strToBean(value, ApproverRule.class);
        validate(rule);
        return rule;
    }

    public static List<String> resolve(Node node, FlowParams flowParams) {
        return resolve(node, flowParams, EXT_CONFIG);
    }

    public static List<String> resolveCarbonCopy(Node node, FlowParams flowParams) {
        return resolve(node, flowParams, CARBON_COPY_EXT_CONFIG);
    }

    private static List<String> resolve(Node node, FlowParams flowParams, String configCode) {
        ApproverRule rule = read(node, configCode);
        if (rule == null) {
            return StringUtils.str2List(node.getPermissionFlag(), FlowCons.SPLIT_AT);
        }

        List<String> resolved;
        String relationType = relationType(rule);
        String selectionType = selectionType(rule);
        if (SELECTION_EXPRESSION.equals(selectionType)) {
            resolved = Collections.singletonList(rule.getExpression());
        } else if (StringUtils.isNotEmpty(relationType)) {
            resolved = resolveRelations(rule.getSubjects(), relationType, flowParams);
        } else if (SELECTION_RESOURCE.equals(selectionType)) {
            resolved = subjectIds(rule.getSubjects(), ApproverStrategy.USER.equals(rule.getStrategy())
                ? SUBJECT_USER : null);
        } else {
            throw new IllegalStateException("Unsupported approver strategy: " + rule.getStrategy());
        }

        List<String> unique = unique(resolved);
        if (CollUtil.isEmpty(unique)) {
            throw new IllegalStateException("Approver rule resolved no handlers");
        }
        return new ResolvedPermissionList(unique);
    }

    public static boolean isResolved(List<String> permissions) {
        return permissions instanceof ResolvedPermissionList;
    }

    public static void validate(ApproverRule rule) {
        if (rule == null || rule.getSchemaVersion() != ApproverRule.CURRENT_SCHEMA_VERSION
            || StringUtils.isEmpty(rule.getStrategy())) {
            throw new IllegalStateException("Unsupported approver rule");
        }
        String selectionType = selectionType(rule);
        if (!SELECTION_RESOURCE.equals(selectionType) && !SELECTION_RELATION.equals(selectionType)
            && !SELECTION_EXPRESSION.equals(selectionType)) {
            throw new IllegalStateException("Unsupported approver selection type: " + selectionType);
        }
        if (SELECTION_EXPRESSION.equals(selectionType)) {
            if (StringUtils.isEmpty(rule.getExpression())) {
                throw new IllegalStateException("Approver expression is required");
            }
            return;
        }
        if (SELECTION_RESOURCE.equals(selectionType) && CollUtil.isEmpty(rule.getSubjects())) {
            throw new IllegalStateException("Approver subjects are required");
        }
        if (SELECTION_RELATION.equals(selectionType) && StringUtils.isEmpty(relationType(rule))) {
            throw new IllegalStateException("Approver relation type is required");
        }
    }

    private static List<String> resolveRelations(List<BusinessSubject> subjects, String relationType,
                                                  FlowParams flowParams) {
        BusinessRelationProvider provider = FlowEngine.businessRelationProvider();
        if (provider == null) {
            throw new IllegalStateException("BusinessRelationProvider is required for " + relationType);
        }
        List<String> handlers = new ArrayList<String>();
        if (CollUtil.isEmpty(subjects)) {
            handlers.addAll(subjectIds(provider.resolveRelationship(new BusinessRelationQuery()
                .setRelationType(relationType).setContext(copyContext(flowParams))), SUBJECT_USER));
            return handlers;
        }
        for (BusinessSubject subject : subjects) {
            if (subject == null || StringUtils.isEmpty(subject.getId())) {
                throw new IllegalStateException("Approver subject id is required");
            }
            BusinessRelationQuery query = new BusinessRelationQuery()
                .setRelationType(relationType)
                .setContext(copyContext(flowParams));
            if (BusinessRelationProvider.ORGANIZATION_MEMBERS.equals(relationType)
                || ApproverStrategy.ORGANIZATION.equals(subject.getType())) {
                query.setOrganizationId(subject.getId());
            } else {
                query.setSubjectId(subject.getId());
            }
            handlers.addAll(subjectIds(provider.resolveRelationship(query), SUBJECT_USER));
        }
        return handlers;
    }

    private static String relationType(ApproverRule rule) {
        if (StringUtils.isNotEmpty(rule.getRelationType())) {
            return rule.getRelationType();
        }
        if (ApproverStrategy.ROLE.equals(rule.getStrategy())) {
            return BusinessRelationProvider.ROLE_MEMBERS;
        }
        if (ApproverStrategy.ORGANIZATION.equals(rule.getStrategy())) {
            return BusinessRelationProvider.ORGANIZATION_MEMBERS;
        }
        return null;
    }

    private static String selectionType(ApproverRule rule) {
        if (StringUtils.isNotEmpty(rule.getSelectionType())) {
            return rule.getSelectionType();
        }
        if (ApproverStrategy.EXPRESSION.equals(rule.getStrategy())) {
            return SELECTION_EXPRESSION;
        }
        if (ApproverStrategy.USER.equals(rule.getStrategy()) || ApproverStrategy.ROLE.equals(rule.getStrategy())
            || ApproverStrategy.ORGANIZATION.equals(rule.getStrategy())) {
            return SELECTION_RESOURCE;
        }
        return SELECTION_RELATION;
    }

    private static Map<String, Object> copyContext(FlowParams flowParams) {
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        if (flowParams != null && MapUtil.isNotEmpty(flowParams.getVariable())) {
            context.putAll(flowParams.getVariable());
        }
        return context;
    }

    private static List<String> subjectIds(List<BusinessSubject> subjects, String requiredType) {
        List<String> ids = new ArrayList<String>();
        if (CollUtil.isEmpty(subjects)) {
            return ids;
        }
        for (BusinessSubject subject : subjects) {
            if (subject == null || StringUtils.isEmpty(subject.getId())
                || (StringUtils.isNotEmpty(requiredType) && !requiredType.equals(subject.getType()))) {
                throw new IllegalStateException("Invalid resolved approver subject");
            }
            ids.add(subject.getId());
        }
        return ids;
    }

    private static List<String> unique(List<String> values) {
        Map<String, String> unique = new LinkedHashMap<String, String>();
        if (values != null) {
            for (String value : values) {
                if (StringUtils.isNotEmpty(value)) {
                    unique.put(value, value);
                }
            }
        }
        return new ArrayList<String>(unique.values());
    }

    private static final class ResolvedPermissionList extends ArrayList<String> {

        private static final long serialVersionUID = 1L;

        private ResolvedPermissionList(List<String> permissions) {
            super(permissions);
        }
    }
}
