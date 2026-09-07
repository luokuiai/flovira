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
package com.luokuiai.flovira.ui.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

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
    public static final String EDITOR_NONE = "NONE";
    public static final String EDITOR_INLINE = "INLINE";
    public static final String EDITOR_DIALOG = "DIALOG";
    public static final String EXACTLY_ONE = "EXACTLY_ONE";
    public static final String ONE_OR_MORE = "ONE_OR_MORE";
    public static final String ZERO_OR_ONE = "ZERO_OR_ONE";
    public static final String ZERO_OR_MORE = "ZERO_OR_MORE";

    private String code;
    private String name;
    private String selectionType;
    private String resourceType;
    private String relationType;
    private boolean multiple = true;
    private String editorType = EDITOR_NONE;
    private String editorKey;
    private String resultCardinality;
    private List<DesignerApproverOption> options = new ArrayList<DesignerApproverOption>();

    public static DesignerApproverStrategy resource(String code, String name, String resourceType,
                                                      String relationType) {
        return new DesignerApproverStrategy().setCode(code).setName(name).setSelectionType(RESOURCE)
            .setResourceType(resourceType).setRelationType(relationType).setEditorType(EDITOR_DIALOG)
            .setResultCardinality(relationType == null ? ONE_OR_MORE : ZERO_OR_MORE);
    }

    public static DesignerApproverStrategy relation(String code, String name, String relationType) {
        return new DesignerApproverStrategy().setCode(code).setName(name).setSelectionType(RELATION)
            .setRelationType(relationType).setMultiple(false).setResultCardinality(ZERO_OR_MORE);
    }

    public static DesignerApproverStrategy expression(String code, String name) {
        return new DesignerApproverStrategy().setCode(code).setName(name).setSelectionType(EXPRESSION)
            .setMultiple(false).setEditorType(EDITOR_INLINE).setResultCardinality(EXACTLY_ONE);
    }
}
