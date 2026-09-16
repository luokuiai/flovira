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
package com.luokuiai.flovira.core.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 可迁移的流程包：设计、固定子流程及精确引用的表单版本，不含实例数据。
 *
 * @author LuokuiAI
 */
@Data
@Accessors(chain = true)
public class WorkflowPackage {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private Integer schemaVersion;
    private String rootFlowCode;
    private List<DefJson> definitions = new ArrayList<>();
    private List<PackagedForm> forms = new ArrayList<>();
    /** 无法在当前租户的 flow_form 中解析的引用，导入时必须显式映射。 */
    private List<String> externalFormIds = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class PackagedForm {
        /** 包内引用键，不能直接作为目标数据库主键使用。 */
        private String reference;
        private String formCode;
        private String formName;
        private String version;
        private String formContent;
        private String ext;
    }
}
