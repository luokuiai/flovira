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
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.constant.FlowCons;
import com.luokuiai.flovira.core.dto.FormChangeRecord;
import com.luokuiai.flovira.core.dto.FormDefinition;
import com.luokuiai.flovira.core.dto.FormFieldDefinition;
import com.luokuiai.flovira.core.dto.FormFieldChange;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.entity.HisTask;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.json.JsonConvert;
import com.luokuiai.flovira.core.service.FormService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 审批节点表单变更计算测试。
 *
 * @author warm
 */
public class HisTaskFormChangeTest {

    @Before
    public void setUp() {
        FormDefinition definition = new FormDefinition().setFields(Arrays.asList(
            new FormFieldDefinition()
                .setKey("amount")
                .setLabel("申请金额")
                .setDataType(FormFieldDefinition.NUMBER)
        ));
        Form form = TestEntityFactory.create(Form.class)
            .setId(20L)
            .setFormContent("standard-form-definition");
        FormServiceImpl formService = new FormServiceImpl() {
            @Override
            public Form getById(Long id) {
                return Long.valueOf(20L).equals(id) ? form : null;
            }
        };
        FrameInvoker.setBeanFunction(type -> FormService.class.equals(type)
            ? formService : null);
        FlowEngine.jsonConvert = new DefinitionJsonConvert(definition);
    }

    @After
    public void tearDown() {
        FrameInvoker.setBeanFunction(type -> null);
        FlowEngine.jsonConvert = null;
    }

    @Test
    public void shouldReportApprovalChangesInCompletionOrderAndIgnorePendingHistory() {
        HisTask submitted = history(1L, 1000L, "applicant", "start", SkipType.PASS.getKey(),
            formData("amount", 100, "reason", "采购设备"));
        HisTask pending = history(2L, 2000L, "manager", "manager", SkipType.NONE.getKey(),
            formData("amount", 120));
        HisTask approved = history(3L, 3000L, "manager", "manager", SkipType.PASS.getKey(),
            formData("amount", 120, "department", "研发部"));
        HistoryService service = new HistoryService(Arrays.asList(approved, pending, submitted));

        List<FormChangeRecord> records = service.getFormChanges(10L);

        assertEquals(1, records.size());
        FormChangeRecord record = records.get(0);
        assertEquals("manager", record.getApprover());
        assertEquals("manager", record.getNodeCode());
        assertEquals(Long.valueOf(3L), record.getHisTaskId());
        assertEquals(FlowCons.FORM_CUSTOM_Y, record.getFormCustom());
        assertEquals("20", record.getFormPath());
        assertEquals(3, record.getChanges().size());

        Map<String, FormFieldChange> changes = byField(record.getChanges());
        assertEquals(FormFieldChange.UPDATED, changes.get("amount").getChangeType());
        assertEquals(100, changes.get("amount").getBeforeValue());
        assertEquals(120, changes.get("amount").getAfterValue());
        assertEquals("申请金额", changes.get("amount").getFieldLabel());
        assertEquals(FormFieldChange.REMOVED, changes.get("reason").getChangeType());
        assertEquals(FormFieldChange.ADDED, changes.get("department").getChangeType());
        assertEquals("department", changes.get("department").getFieldLabel());
    }

    @Test
    public void shouldTreatEquivalentNumbersAsUnchanged() {
        HisTask submitted = history(1L, 1000L, "applicant", "start", SkipType.PASS.getKey(),
            formData("amount", 100));
        HisTask approved = history(2L, 2000L, "manager", "manager", SkipType.PASS.getKey(),
            formData("amount", 100.0D));

        List<FormChangeRecord> records = new HistoryService(Arrays.asList(submitted, approved))
            .getFormChanges(10L);

        assertTrue(records.isEmpty());
    }

    @Test
    public void shouldReadStandardDefinitionWithoutRenderer() {
        FormDefinition definition = FlowEngine.formService().getDefinition(20L);

        assertEquals(FormDefinition.VERSION_1, definition.getSchemaVersion());
        assertEquals(1, definition.getFields().size());
        assertEquals("amount", definition.getFields().get(0).getKey());
        assertNull(definition.getRenderer());
    }

    @Test
    public void shouldFallBackToFieldKeyWhenLegacyContentCannotBeParsed() {
        FlowEngine.jsonConvert = new DefinitionJsonConvert(null);
        HisTask submitted = history(1L, 1000L, "applicant", "start", SkipType.PASS.getKey(),
            formData("amount", 100));
        HisTask approved = history(2L, 2000L, "manager", "manager", SkipType.PASS.getKey(),
            formData("amount", 120));

        List<FormChangeRecord> records = new HistoryService(Arrays.asList(submitted, approved))
            .getFormChanges(10L);

        assertEquals("amount", records.get(0).getChanges().get(0).getFieldLabel());
    }

    private static HisTask history(Long id, long updateTime, String approver, String nodeCode,
        String skipType, Map<String, Object> formData) {
        HisTask hisTask = TestEntityFactory.create(HisTask.class)
            .setId(id)
            .setTaskId(id + 100L)
            .setInstanceId(10L)
            .setNodeCode(nodeCode)
            .setNodeName(nodeCode)
            .setApprover(approver)
            .setSkipType(skipType)
            .setFormCustom(FlowCons.FORM_CUSTOM_Y)
            .setFormPath("20")
            .setUpdateTime(new Date(updateTime));
        Map<String, Object> variables = new LinkedHashMap<String, Object>();
        variables.put(FlowCons.FORM_DATA, formData);
        TestEntityFactory.put(hisTask, "VariableMap", variables);
        return hisTask;
    }

    private static Map<String, Object> formData(Object... values) {
        Map<String, Object> formData = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            formData.put((String) values[i], values[i + 1]);
        }
        return formData;
    }

    private static Map<String, FormFieldChange> byField(List<FormFieldChange> changes) {
        Map<String, FormFieldChange> result = new LinkedHashMap<String, FormFieldChange>();
        for (FormFieldChange change : changes) {
            result.put(change.getFieldKey(), change);
        }
        return result;
    }

    private static class HistoryService extends HisTaskServiceImpl {

        private final List<HisTask> histories;

        private HistoryService(List<HisTask> histories) {
            this.histories = new ArrayList<HisTask>(histories);
        }

        @Override
        public List<HisTask> getByInsId(Long instanceId) {
            return new ArrayList<HisTask>(histories);
        }
    }

    private static class DefinitionJsonConvert implements JsonConvert {

        private final FormDefinition definition;

        private DefinitionJsonConvert(FormDefinition definition) {
            this.definition = definition;
        }

        @Override
        public Map<String, Object> strToMap(String jsonStr) {
            return new LinkedHashMap<String, Object>();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T strToBean(String jsonStr, Class<T> clazz) {
            if (definition == null) {
                throw new IllegalArgumentException("legacy content");
            }
            return (T) definition;
        }

        @Override
        public <T> List<T> strToList(String jsonStr) {
            return new ArrayList<T>();
        }

        @Override
        public String objToStr(Object variable) {
            return "standard-form-definition";
        }
    }
}
