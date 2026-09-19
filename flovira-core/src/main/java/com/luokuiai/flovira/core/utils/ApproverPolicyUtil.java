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
import com.luokuiai.flovira.core.dto.*;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.handler.ApproverResolver;

import java.util.*;

/** 审批人解析后的空人员与提交人策略；不影响抄送节点。 */
public final class ApproverPolicyUtil {
    public static final String SYSTEM_HANDLER = "SYSTEM_AUTO_APPROVE";
    private static final ThreadLocal<Set<String>> ACTIVE_SKIPS = new ThreadLocal<Set<String>>();
    private ApproverPolicyUtil() {}

    /** 业务解析器可复用的设计器选项，与运行时采用相同默认行为。 */
    public static List<ApproverOption> options() {
        ApproverOption empty = new ApproverOption().setCode("emptyPolicy").setName("审批人为空时")
            .setDefaultValue("ERROR").setNodeTypes(Collections.singletonList("1")).setCondition("EMPTY")
            .setChoices(Arrays.asList(new ApproverOptionChoice().setValue("ERROR").setLabel("报错并阻止流转"),
                new ApproverOptionChoice().setValue("SKIP").setLabel("自动跳过"),
                transferChoice("emptyPolicySubjects")));
        ApproverOption same = new ApproverOption().setCode("sameAsStarterAction").setName("审批人与提交人为同一人时")
            .setDefaultValue("SELF_APPROVE").setNodeTypes(Collections.singletonList("1"))
            .setChoices(Arrays.asList(new ApproverOptionChoice().setValue("SELF_APPROVE").setLabel("本人审批"),
                new ApproverOptionChoice().setValue("AUTO_SKIP_OR_TRANSFER").setLabel("跳过或由其他人审批"),
                transferChoice("sameAsStarterSubjects")));
        return new ArrayList<ApproverOption>(Arrays.asList(empty, same));
    }

    private static ApproverOptionChoice transferChoice(String key) {
        return new ApproverOptionChoice().setValue("TRANSFER_TO_USER").setLabel("转交给指定人员")
            .setSelectionStrategy("USER").setSelectionConfigKey(key);
    }

    public static void validate(ApproverRule rule) {
        choice(rule, "emptyPolicy", "ERROR", "ERROR", "SKIP", "TRANSFER_TO_USER");
        choice(rule, "sameAsStarterAction", "SELF_APPROVE", "SELF_APPROVE", "AUTO_SKIP_OR_TRANSFER", "TRANSFER_TO_USER");
        for (String key : Arrays.asList("emptyPolicy", "sameAsStarterAction")) {
            if ("TRANSFER_TO_USER".equals(value(rule, key))) {
                ApproverRule target = transferRule(rule, key);
                FlowEngine.approverResolver("USER").validate(target);
            }
        }
    }

    static List<String> apply(Node node, ApproverRule rule, Instance instance, FlowParams params,
                              boolean preview, List<String> users) {
        if (!NodeType.isBetween(node.getNodeType())) return users;
        if (users == null || users.isEmpty()) {
            String policy = choice(rule, "emptyPolicy", "ERROR", "ERROR", "SKIP", "TRANSFER_TO_USER");
            if ("SKIP".equals(policy)) return new SkipPermissions("EMPTY_APPROVER");
            if ("TRANSFER_TO_USER".equals(policy)) {
                users = transfer(node, rule, "emptyPolicy", instance, params, preview);
            } else {
                return users;
            }
        }
        String policy = choice(rule, "sameAsStarterAction", "SELF_APPROVE", "SELF_APPROVE", "AUTO_SKIP_OR_TRANSFER", "TRANSFER_TO_USER");
        if ("SELF_APPROVE".equals(policy)) return users;
        // 使用实例的原始提交人，不能使用当前办理人；无实例预览须显式提供提交人。
        String starter = instance == null ? params.getHandler() : instance.getCreatedBy();
        if (starter == null || starter.trim().isEmpty()) {
            throw new IllegalStateException("Starter is required for sameAsStarterAction");
        }
        if (!users.contains(starter)) return users;
        List<String> result = new ArrayList<String>(users);
        result.removeAll(Collections.singleton(starter));
        if ("TRANSFER_TO_USER".equals(policy)) {
            List<String> targets = transfer(node, rule, "sameAsStarterAction", instance, params, preview);
            if (targets.contains(starter)) throw new IllegalStateException("Transfer target must differ from starter");
            result.addAll(targets);
        }
        return result.isEmpty() ? new SkipPermissions("SAME_AS_STARTER") : result;
    }

    private static List<String> transfer(Node node, ApproverRule source, String key, Instance instance,
                                         FlowParams params, boolean preview) {
        ApproverRule target = transferRule(source, key);
        List<String> users = FlowEngine.approverResolver("USER")
            .resolve(new ApproverContext(node, target, instance, params, preview));
        if (users == null || users.isEmpty()) throw new IllegalStateException("Transfer resolved no handlers");
        for (String user : users) {
            if (user == null || user.trim().isEmpty()) throw new IllegalStateException("Invalid transfer user ID");
        }
        return users;
    }

    private static ApproverRule transferRule(ApproverRule source, String key) {
        Object raw = value(source, "emptyPolicy".equals(key) ? "emptyPolicySubjects" : "sameAsStarterSubjects");
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) {
            throw new IllegalStateException("Transfer subjects are required: " + key);
        }
        List<BusinessSubject> subjects = new ArrayList<BusinessSubject>();
        for (Object item : (List<?>) raw) {
            BusinessSubject subject;
            if (item instanceof BusinessSubject) {
                subject = (BusinessSubject) item;
            } else if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                if (!(map.get("id") instanceof String) || !(map.get("type") instanceof String)) {
                    throw new IllegalStateException("Invalid transfer subject");
                }
                subject = FlowEngine.jsonConvert.strToBean(FlowEngine.jsonConvert.objToStr(item), BusinessSubject.class);
            } else {
                throw new IllegalStateException("Invalid transfer subject");
            }
            if (subject == null || !"USER".equals(subject.getType()) || subject.getId() == null
                || subject.getId().trim().isEmpty()) throw new IllegalStateException("Invalid transfer subject");
            subjects.add(subject);
        }
        ApproverResolver resolver = FlowEngine.approverResolver("USER");
        return new ApproverRule().setStrategy("USER").setStrategyVersion(resolver.getDefinition().getVersion())
            .setSelectionType("RESOURCE").setSubjects(subjects).setConfig(Collections.<String, Object>emptyMap());
    }

    private static Object value(ApproverRule rule, String key) {
        return rule.getConfig() == null ? null : rule.getConfig().get(key);
    }

    private static String choice(ApproverRule rule, String key, String fallback, String... allowed) {
        Object value = value(rule, key);
        if (value == null) return fallback;
        if (!Arrays.asList(allowed).contains(value)) throw new IllegalStateException("Unsupported " + key + ": " + value);
        return (String) value;
    }

    public static boolean isSkip(List<String> permissions) {
        return permissions instanceof SkipPermissions;
    }

    /** 保留显式跳过标记；监听器或 nextHandler 指定人员后不再自动跳过。 */
    static List<String> preserveSkip(List<String> original, List<String> permissions) {
        return isSkip(original) && permissions.isEmpty() ? original : permissions;
    }

    public static Instance advanceTasks(List<Task> tasks, Map<String, Object> variables) {
        Instance advanced = null;
        if (tasks == null) return null;
        for (Task task : new ArrayList<Task>(tasks)) {
            if (!NodeType.isBetween(task.getNodeType()) || !isSkip(task.getPermissionList())
                || !task.getPermissionList().isEmpty()) continue;
            String reason = ((SkipPermissions) task.getPermissionList()).reason;
            Set<String> active = ACTIVE_SKIPS.get();
            if (active == null) {
                active = new HashSet<String>();
                ACTIVE_SKIPS.set(active);
            }
            String key = task.getInstanceId() + ":" + task.getNodeCode();
            if (!active.add(key)) throw new IllegalStateException("Automatic approval cycle: " + task.getNodeCode());
            try {
                advanced = FlowEngine.taskService().skipSystemTask(FlowParams.build().skipType(SkipType.PASS.getKey())
                    .flowStatus(FlowStatus.AUTO_PASS.getKey()).handler(SYSTEM_HANDLER)
                    .message(reason).hisTaskExt(FlowEngine.jsonConvert.objToStr(Collections.singletonMap("action", reason)))
                    .variables(variables), task);
            } finally {
                active.remove(key);
                if (active.isEmpty()) ACTIVE_SKIPS.remove();
            }
        }
        return advanced;
    }

    private static final class SkipPermissions extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        private final String reason;
        private SkipPermissions(String reason) { this.reason = reason; }
    }
}
