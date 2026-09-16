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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.constant.ExceptionCons;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.core.orm.dao.FlowFormDao;
import com.luokuiai.flovira.core.orm.service.impl.FloviraServiceImpl;
import com.luokuiai.flovira.core.service.FormService;
import com.luokuiai.flovira.core.utils.AssertUtil;
import com.luokuiai.flovira.core.utils.ClassUtil;
import com.luokuiai.flovira.core.utils.CollUtil;
import com.luokuiai.flovira.core.utils.ObjectUtil;
import com.luokuiai.flovira.core.utils.page.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/** 流程表单 Service 实现。 @author vanlin @since 2024/8/19 10:07 */
public class FormServiceImpl extends FloviraServiceImpl<FlowFormDao<Form>, Form> implements FormService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormServiceImpl.class);

    @Override
    public FormService setDao(FlowFormDao<Form> floviraDao) {
        this.floviraDao = floviraDao;
        return this;
    }

    @Override
    public boolean publish(Long id) {
        Form form = requireForm(id);
        AssertUtil.isTrue(PublishStatus.PUBLISHED.getKey().equals(form.getPublishStatus()),
            ExceptionCons.FORM_ALREADY_PUBLISH);
        form.setPublishStatus(PublishStatus.PUBLISHED.getKey());
        return updateById(form);
    }

    @Override
    public boolean unPublish(Long id) {
        Form form = requireForm(id);
        String formId = String.valueOf(form.getId());
        Node nodeQuery = FlowEngine.newNode().setFormId(formId);
        Definition definitionQuery = FlowEngine.newDef().setFormId(formId);
        String tenantId = currentTenantId();
        if (tenantId != null) {
            nodeQuery.setTenantId(tenantId);
            definitionQuery.setTenantId(tenantId);
        }
        List<Node> nodes = FlowEngine.nodeService().list(nodeQuery);
        AssertUtil.isNotEmpty(nodes, ExceptionCons.EXIST_USE_FORM);
        List<Definition> definitions = FlowEngine.defService().list(definitionQuery);
        AssertUtil.isNotEmpty(definitions, ExceptionCons.EXIST_USE_FORM);
        AssertUtil.isTrue(PublishStatus.UNPUBLISHED.getKey().equals(form.getPublishStatus()),
            ExceptionCons.FORM_ALREADY_UN_PUBLISH);
        form.setPublishStatus(PublishStatus.UNPUBLISHED.getKey());
        return updateById(form);
    }

    @Override
    public boolean save(Form form) {
        applyTenant(form);
        form.setVersion(getNewVersion(form));
        return super.save(form);
    }

    @Override
    public boolean updateById(Form form) {
        requireForm(form.getId());
        applyTenant(form);
        return super.updateById(form);
    }

    @Override
    public boolean removeById(Serializable id) {
        Form form = requireForm((Long) id);
        return remove(form);
    }

    @Override
    public boolean removeByIds(Collection<? extends Serializable> ids) {
        boolean removed = true;
        for (Serializable id : ids) {
            removed = removeById(id) && removed;
        }
        return removed;
    }

    @Override
    public boolean copyForm(Long id) {
        Form source = requireForm(id);
        Form form = ClassUtil.clone(source);
        AssertUtil.isTrue(ObjectUtil.isNull(form), ExceptionCons.NOT_FOUND_FORM);
        form.setId(null)
            .setVersion(getNewVersion(form))
            .setPublishStatus(PublishStatus.UNPUBLISHED.getKey())
            .setCreatedAt(null)
            .setUpdatedAt(null);
        return super.save(form);
    }

    @Override
    public Form getByCode(String formCode, String formVersion) {
        Form query = applyTenant(FlowEngine.newForm().setFormCode(formCode).setVersion(formVersion));
        List<Form> list = list(query);
        AssertUtil.isTrue(CollUtil.isEmpty(list), ExceptionCons.NOT_FOUND_FORM);
        AssertUtil.isTrue(list.size() > 1, ExceptionCons.FORM_NOT_ONE);
        return list.get(0);
    }

    @Override
    public Form getById(Long id) {
        AssertUtil.isNull(id, ExceptionCons.ID_EMPTY);
        return getOne(applyTenant(FlowEngine.newForm().setId(id)));
    }

    @Override
    public Page<Form> publishedPage(String formName, Integer pageNum, Integer pageSize) {
        Form query = applyTenant(FlowEngine.newForm().setFormName(formName)
            .setPublishStatus(PublishStatus.PUBLISHED.getKey()));
        List<Form> forms = list(query, orderById());
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = Math.min((current - 1) * size, forms.size());
        int to = Math.min(from + size, forms.size());
        Page<Form> result = new Page<Form>(new ArrayList<Form>(forms.subList(from, to)), forms.size());
        result.setPageNum(current);
        result.setPageSize(size);
        return result;
    }

    @Override
    public boolean saveContent(Long id, String formContent) {
        Form form = requireForm(id);
        AssertUtil.isTrue(PublishStatus.PUBLISHED.getKey().equals(form.getPublishStatus()),
            ExceptionCons.FORM_ALREADY_PUBLISH);
        form.setFormContent(formContent);
        return updateById(form);
    }

    private Form requireForm(Long id) {
        Form form = getById(id);
        AssertUtil.isNull(form, ExceptionCons.NOT_FOUND_FORM);
        return form;
    }

    private Form applyTenant(Form form) {
        String tenantId = currentTenantId();
        if (tenantId != null) {
            form.setTenantId(tenantId);
        }
        return form;
    }

    private String currentTenantId() {
        return FlowEngine.tenantHandler() == null ? null : FlowEngine.tenantHandler().getTenantId();
    }

    private String getNewVersion(Form form) {
        List<Form> forms = getDao().queryByCodeList(Collections.singletonList(form.getFormCode()));
        int highestVersion = 0;
        for (Form otherForm : forms) {
            if (!form.getFormCode().equals(otherForm.getFormCode())) {
                continue;
            }
            try {
                highestVersion = Math.max(highestVersion, Integer.parseInt(otherForm.getVersion()));
            } catch (NumberFormatException e) {
                LOGGER.warn("忽略非数字表单版本 - formCode: {}, version: {}", form.getFormCode(),
                    otherForm.getVersion());
            }
        }
        return String.valueOf(highestVersion + 1);
    }
}
