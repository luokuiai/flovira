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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 设计器审批人策略描述。
 *
 * @author warm
 */
@Getter
@Setter
@Accessors(chain = true)
public class DesignerApproverStrategy {

    public static final String RESOURCE = "RESOURCE";
    public static final String RELATION = "RELATION";
    public static final String EXPRESSION = "EXPRESSION";

    private String code;
    private String name;
    private String selectionType;
    private String resourceType;
    private String relationType;
    private boolean multiple = true;

    public static DesignerApproverStrategy resource(String code, String name, String resourceType,
                                                      String relationType) {
        return new DesignerApproverStrategy().setCode(code).setName(name).setSelectionType(RESOURCE)
            .setResourceType(resourceType).setRelationType(relationType);
    }

    public static DesignerApproverStrategy relation(String code, String name, String relationType) {
        return new DesignerApproverStrategy().setCode(code).setName(name).setSelectionType(RELATION)
            .setRelationType(relationType).setMultiple(false);
    }

    public static DesignerApproverStrategy expression(String code, String name) {
        return new DesignerApproverStrategy().setCode(code).setName(name).setSelectionType(EXPRESSION)
            .setMultiple(false);
    }
}
