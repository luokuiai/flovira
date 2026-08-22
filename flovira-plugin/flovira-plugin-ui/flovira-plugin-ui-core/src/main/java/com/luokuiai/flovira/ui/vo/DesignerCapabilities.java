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
package com.luokuiai.flovira.ui.vo;

import com.luokuiai.flovira.core.constant.ApproverStrategy;
import com.luokuiai.flovira.core.handler.BusinessRelationProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 设计器能力清单。
 *
 * @author warm
 */
@Getter
@Setter
@Accessors(chain = true)
public class DesignerCapabilities {

    public static final int SCHEMA_VERSION = 1;

    private int schemaVersion = SCHEMA_VERSION;
    private List<String> nodeTypes = new ArrayList<String>();
    private List<DesignerApproverStrategy> approverStrategies = new ArrayList<DesignerApproverStrategy>();
    private List<String> approvalModes = new ArrayList<String>();
    private List<String> returnPolicies = new ArrayList<String>();
    private List<String> timeoutNodeTypes = new ArrayList<String>();
    private List<String> operations = new ArrayList<String>();
    private List<String> resourceTypes = new ArrayList<String>();

    public static DesignerCapabilities defaults() {
        return new DesignerCapabilities()
            .setNodeTypes(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7"))
            .setApproverStrategies(Arrays.asList(
                DesignerApproverStrategy.resource(ApproverStrategy.USER, "用户", "USER", null),
                DesignerApproverStrategy.resource(ApproverStrategy.ROLE, "角色", "ROLE",
                    BusinessRelationProvider.ROLE_MEMBERS),
                DesignerApproverStrategy.resource(ApproverStrategy.ORGANIZATION, "组织", "ORGANIZATION",
                    BusinessRelationProvider.ORGANIZATION_MEMBERS),
                DesignerApproverStrategy.expression(ApproverStrategy.EXPRESSION, "表达式")))
            .setApprovalModes(Arrays.asList("OR", "VOTE", "COUNTERSIGN"))
            .setReturnPolicies(Arrays.asList("PREVIOUS", "ANY", "REJECT"))
            .setTimeoutNodeTypes(Arrays.asList("1", "7"))
            .setOperations(Arrays.asList("SAVE", "PUBLISH", "VALIDATE", "IMPORT", "EXPORT"))
            .setResourceTypes(Arrays.asList("USER", "ROLE", "ORGANIZATION", "SUBJECT", "CATEGORY",
                "FORM_PATH", "FORM_FIELD", "DICTIONARY", "SUBPROCESS", "NODE_EXTENSION", "LISTENER"));
    }
}
