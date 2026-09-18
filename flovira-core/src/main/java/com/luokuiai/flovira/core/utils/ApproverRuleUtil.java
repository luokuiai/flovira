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
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.handler.ApproverResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 审批规则的配置校验与节点到达时解析。没有人员、关系或表达式的隐式兜底。
 *
 * @author warm
 */
public final class ApproverRuleUtil {
    public static final String EXT_CONFIG = "approverRule";
    public static final String CARBON_COPY_EXT_CONFIG = "carbonCopyRule";

    private ApproverRuleUtil() {}

    public static ApproverRule read(Node node) {
        return read(node, EXT_CONFIG);
    }

    public static ApproverRule read(Node node, String code) {
        String value = FlowEngine.nodeService().getExt(node).get(code);
        if (StringUtils.isEmpty(value)) return null;
        ApproverRule rule = FlowEngine.jsonConvert.strToBean(value, ApproverRule.class);
        validate(rule);
        return rule;
    }

    public static void validate(ApproverRule rule) {
        if (rule == null || rule.getSchemaVersion() != ApproverRule.CURRENT_SCHEMA_VERSION
            || StringUtils.isEmpty(rule.getStrategy())) {
            throw new IllegalStateException("Invalid approver rule");
        }
        ApproverResolver resolver = FlowEngine.approverResolver(rule.getStrategy());
        if (rule.getStrategyVersion() != resolver.getDefinition().getVersion()) {
            throw new IllegalStateException("Unsupported approver configuration version: "
                + rule.getStrategy() + "/" + rule.getStrategyVersion());
        }
        resolver.validate(rule);
        ApproverPolicyUtil.validate(rule);
    }

    public static List<String> resolve(Node node, Instance instance, FlowParams params, boolean preview) {
        if (!NodeType.isBetween(node.getNodeType()) && !NodeType.isCarbonCopy(node.getNodeType())) {
            return Collections.emptyList();
        }
        String code = NodeType.isCarbonCopy(node.getNodeType()) ? CARBON_COPY_EXT_CONFIG : EXT_CONFIG;
        ApproverRule rule = read(node, code);
        if (rule == null) {
            throw new IllegalStateException("Approver rule is required on node " + node.getNodeCode());
        }
        List<String> users = FlowEngine.approverResolver(rule.getStrategy())
            .resolve(new ApproverContext(node, rule, instance, params, preview));
        users = ApproverPolicyUtil.apply(node, rule, instance, params, preview, users);
        if (users == null || (users.isEmpty() && !ApproverPolicyUtil.isSkip(users))) {
            throw new IllegalStateException("Approver rule resolved no handlers: " + rule.getStrategy());
        }
        for (String user : users) {
            if (user == null || user.trim().isEmpty()) {
                throw new IllegalStateException("Approver resolver returned an empty user ID");
            }
        }
        return ApproverPolicyUtil.isSkip(users) ? users
            : new ResolvedPermissionList(new ArrayList<String>(new LinkedHashSet<String>(users)));
    }

    public static boolean isResolved(List<String> users) {
        return users instanceof ResolvedPermissionList || ApproverPolicyUtil.isSkip(users);
    }

    private static final class ResolvedPermissionList extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        private ResolvedPermissionList(List<String> users) { super(users); }
    }
}
