/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.service;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.FormDefinition;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.orm.service.IFloviraService;
import com.luokuiai.flovira.core.utils.StringUtils;
import com.luokuiai.flovira.core.utils.page.Page;

/** 流程表单 Service。 @author vanlin @since 2024/8/19 10:06 */
public interface FormService extends IFloviraService<Form> {
    @Override
    boolean save(Form form);

    boolean publish(Long id);

    boolean unPublish(Long id);

    boolean copyForm(Long id);

    Form getByCode(String formCode, String formVersion);

    Form getById(Long id);

    Page<Form> publishedPage(String formName, Integer pageNum, Integer pageSize);

    boolean saveContent(Long id, String formContent);

    default boolean saveDefinition(Long id, FormDefinition definition) {
        String content = definition == null ? null : FlowEngine.jsonConvert.objToStr(definition);
        return saveContent(id, content);
    }

    default FormDefinition getDefinition(Long id) {
        return parseDefinition(getById(id));
    }

    default FormDefinition parseDefinition(Form form) {
        if (form == null || StringUtils.isEmpty(form.getFormContent())) {
            return null;
        }
        return FlowEngine.jsonConvert.strToBean(form.getFormContent(), FormDefinition.class);
    }
}
