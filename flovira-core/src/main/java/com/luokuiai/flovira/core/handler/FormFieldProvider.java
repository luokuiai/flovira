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
package com.luokuiai.flovira.core.handler;

import java.util.Map;

/**
 * 由业务系统提供表单字段名称，供审批数据变更记录展示使用。
 * 表单结构、版本及页面均由业务系统维护。
 *
 * @author LuokuiAI
 */
public interface FormFieldProvider {

    /**
     * @param formId 办理时保存的外部表单标识，可包含业务版本
     * @return 字段编码到名称的映射；未找到时返回空映射，展示时回退到字段编码
     */
    Map<String, String> getFieldLabels(String formId);
}
