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
import com.luokuiai.flovira.core.constant.FlowCons;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.dto.FormChangeRecord;
import com.luokuiai.flovira.core.dto.FormDefinition;
import com.luokuiai.flovira.core.dto.FormFieldDefinition;
import com.luokuiai.flovira.core.dto.FormFieldChange;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.entity.HisTask;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.entity.User;
import com.luokuiai.flovira.core.enums.CooperateType;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.SkipType;
import com.luokuiai.flovira.core.orm.dao.FlowHisTaskDao;
import com.luokuiai.flovira.core.orm.service.impl.FloviraServiceImpl;
import com.luokuiai.flovira.core.service.FormService;
import com.luokuiai.flovira.core.service.HisTaskService;
import com.luokuiai.flovira.core.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * 历史任务记录Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
public class HisTaskServiceImpl extends FloviraServiceImpl<FlowHisTaskDao<HisTask>, HisTask> implements HisTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HisTaskServiceImpl.class);

    @Override
    public HisTaskService setDao(FlowHisTaskDao<HisTask> floviraDao) {
        this.floviraDao = floviraDao;
        return this;
    }

    @Override
    public List<HisTask> listByTaskId(Long taskId) {
        return list(FlowEngine.newHisTask().setTaskId(taskId));
    }

    @Override
    public List<HisTask> listByTaskIdAndCooperateTypes(Long taskId, Integer... cooperateTypes) {
        if (ArrayUtil.isEmpty(cooperateTypes)) {
            return listByTaskId(taskId);
        }
        if (cooperateTypes.length == 1) {
            return list(FlowEngine.newHisTask().setTaskId(taskId).setCooperateType(cooperateTypes[0]));
        }
        return getDao().listByTaskIdAndCooperateTypes(taskId, cooperateTypes);
    }

    @Override
    public List<HisTask> getByInsAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        return getDao().getByInsAndNodeCodes(instanceId, nodeCodes);
    }

    @Override
    public boolean deleteByInsIds(List<Long> instanceIds) {
        return SqlHelper.retBool(getDao().deleteByInsIds(instanceIds));
    }

    @Override
    public HisTask setSkipInsHis(Task task, List<Node> nextNodes, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        return setSkipHis(task, nextNodes, flowParams, flowStatus);
    }

    @Override
    public List<HisTask> setSkipHisList(List<Task> taskList, List<Node> nextNodes, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        List<HisTask> hisTasks = new ArrayList<>();
        for (Task task : taskList) {
            HisTask hisTask = setSkipHis(task, nextNodes, flowParams, flowStatus);
            hisTasks.add(hisTask);
        }
        return hisTasks;
    }

    @Override
    public HisTask setSkipHisTask(Task task, Node nextNode, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        return setSkipHis(task, CollUtil.toList(nextNode), flowParams, flowStatus);
    }


    @Override
    public HisTask setCooperateHis(Task task, FlowParams flowParams
        , List<String> collaborators) {
        String flowStatus = getFlowStatus(flowParams);
        HisTask hisTask = FlowEngine.newHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(ObjectUtil.defaultNull(flowParams.getCooperateType(), CooperateType.APPROVAL.getKey()))
            .setCollaborator(StreamUtils.join(collaborators, c -> c))
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(task.getNodeCode())
            .setTargetNodeName(task.getNodeName())
            .setApprover(flowParams.getHandler())
            .setSkipType(flowParams.getSkipType())
            .setFlowStatus(StringUtils.emptyDefault(flowStatus, FlowStatus.APPROVAL.getKey()))
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    @Override
    public HisTask notSkip(Task task, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        HisTask hisTask = FlowEngine.newHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(ObjectUtil.defaultNull(flowParams.getCooperateType(), CooperateType.APPROVAL.getKey()))
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(task.getNodeCode())
            .setTargetNodeName(task.getNodeName())
            .setApprover(flowParams.getHandler())
            .setSkipType(SkipType.NONE.getKey())
            .setFlowStatus(flowStatus)
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    @Override
    public HisTask setDeputeHisTask(Task task, FlowParams flowParams, User entrustedUser) {
        String flowStatus = getFlowStatus(flowParams);
        HisTask hisTask = FlowEngine.newHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(CooperateType.DEPUTE.getKey())
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(task.getNodeCode())
            .setTargetNodeName(task.getNodeName())
            .setApprover(flowParams.getHandler())
            .setCollaborator(entrustedUser.getCreateBy())
            .setSkipType(flowParams.getSkipType())
            .setFlowStatus(StringUtils.isNotEmpty(flowStatus)
                ? flowStatus : SkipType.isReject(flowParams.getSkipType())
                ? FlowStatus.REJECT.getKey() : FlowStatus.PASS.getKey())
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    @Override
    public HisTask setSignHisTask(Task task, FlowParams flowParams, String nodeRatio, boolean isPass) {
        String flowStatus = getFlowStatus(flowParams);
        HisTask hisTask = FlowEngine.newHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(CooperateType.isCountersign(nodeRatio)
                ? CooperateType.COUNTERSIGN.getKey() : CooperateType.VOTE.getKey())
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setApprover(flowParams.getHandler())
            .setMessage(flowParams.getMessage())
            .setSkipType(isPass ? SkipType.PASS.getKey() : SkipType.REJECT.getKey())
            .setFlowStatus(StringUtils.isNotEmpty(flowStatus)
                ? flowStatus : isPass
                ? FlowStatus.PASS.getKey() : FlowStatus.REJECT.getKey())
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    @Override
    public List<HisTask> getByInsId(Long instanceId) {
        return FlowEngine.hisTaskService().list(FlowEngine.newHisTask().setInstanceId(instanceId));
    }

    @Override
    public List<FormChangeRecord> getFormChanges(Long instanceId) {
        List<HisTask> source = getByInsId(instanceId);
        if (CollUtil.isEmpty(source)) {
            return new ArrayList<FormChangeRecord>();
        }
        List<HisTask> hisTasks = new ArrayList<HisTask>(source);
        hisTasks.sort((left, right) -> compareHistoryOrder(left, right));

        List<FormChangeRecord> records = new ArrayList<FormChangeRecord>();
        Map<String, Map<String, String>> fieldLabelCache = new HashMap<String, Map<String, String>>();
        Map<String, Object> previousFormData = null;
        for (HisTask hisTask : hisTasks) {
            if (hisTask == null || SkipType.NONE.getKey().equals(hisTask.getSkipType())) {
                continue;
            }
            Map<String, Object> currentFormData = getFormData(hisTask);
            if (currentFormData == null) {
                continue;
            }
            if (previousFormData == null) {
                previousFormData = new LinkedHashMap<String, Object>(currentFormData);
                continue;
            }

            List<FormFieldChange> changes = compareFormData(previousFormData, currentFormData);
            if (CollUtil.isNotEmpty(changes)) {
                fillFieldLabels(hisTask, changes, fieldLabelCache);
                records.add(new FormChangeRecord()
                    .setHisTaskId(hisTask.getId())
                    .setTaskId(hisTask.getTaskId())
                    .setInstanceId(hisTask.getInstanceId())
                    .setNodeCode(hisTask.getNodeCode())
                    .setNodeName(hisTask.getNodeName())
                    .setApprover(hisTask.getApprover())
                    .setChangeTime(eventTime(hisTask))
                    .setFormCustom(hisTask.getFormCustom())
                    .setFormPath(hisTask.getFormPath())
                    .setChanges(changes));
            }
            previousFormData = new LinkedHashMap<String, Object>(currentFormData);
        }
        return records;
    }

    @Override
    public List<HisTask> listByBusinessKey(String businessType, String businessId) {
        List<Instance> instances = FlowEngine.insService().listByBusinessKey(businessType, businessId);
        if (CollUtil.isEmpty(instances)) {
            return new ArrayList<HisTask>();
        }
        List<Long> instanceIds = StreamUtils.toList(instances, Instance::getId);
        return new ArrayList<HisTask>(getDao().listByInsIds(instanceIds));
    }

    private HisTask setSkipHis(Task task, List<Node> nextNodes, FlowParams flowParams, String flowStatus) {
        HisTask hisTask = FlowEngine.newHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(ObjectUtil.defaultNull(flowParams.getCooperateType(), CooperateType.APPROVAL.getKey()))
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(StreamUtils.join(nextNodes, Node::getNodeCode))
            .setTargetNodeName(StreamUtils.join(nextNodes, Node::getNodeName))
            .setApprover(flowParams.getHandler())
            .setSkipType(flowParams.getSkipType())
            .setFlowStatus(StringUtils.isNotEmpty(flowStatus)
                ? flowStatus : SkipType.isReject(flowParams.getSkipType())
                ? FlowStatus.REJECT.getKey() : FlowStatus.PASS.getKey())
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    private String getFlowStatus(FlowParams flowParams) {
        return StringUtils.emptyDefault(flowParams.getHisStatus(), flowParams.getFlowStatus());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFormData(HisTask hisTask) {
        Map<String, Object> variables = hisTask.getVariableMap();
        if (MapUtil.isEmpty(variables)) {
            return null;
        }
        Object formData = variables.get(FlowCons.FORM_DATA);
        return formData instanceof Map ? (Map<String, Object>) formData : null;
    }

    private List<FormFieldChange> compareFormData(Map<String, Object> previous, Map<String, Object> current) {
        Set<String> fieldKeys = new LinkedHashSet<String>();
        fieldKeys.addAll(previous.keySet());
        fieldKeys.addAll(current.keySet());

        List<FormFieldChange> changes = new ArrayList<FormFieldChange>();
        for (String fieldKey : fieldKeys) {
            boolean existed = previous.containsKey(fieldKey);
            boolean exists = current.containsKey(fieldKey);
            Object beforeValue = previous.get(fieldKey);
            Object afterValue = current.get(fieldKey);
            if (existed && exists && valuesEqual(beforeValue, afterValue)) {
                continue;
            }
            String changeType = !existed ? FormFieldChange.ADDED
                : !exists ? FormFieldChange.REMOVED : FormFieldChange.UPDATED;
            changes.add(new FormFieldChange()
                .setFieldKey(fieldKey)
                .setFieldLabel(fieldKey)
                .setChangeType(changeType)
                .setBeforeValue(beforeValue)
                .setAfterValue(afterValue));
        }
        return changes;
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            try {
                return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0;
            } catch (NumberFormatException ignored) {
                // NaN and infinity fall back to the regular object comparison.
            }
        }
        return Objects.deepEquals(left, right);
    }

    private void fillFieldLabels(HisTask hisTask, List<FormFieldChange> changes,
        Map<String, Map<String, String>> fieldLabelCache) {
        Map<String, String> labels = getFieldLabels(hisTask, fieldLabelCache);
        if (MapUtil.isEmpty(labels)) {
            return;
        }
        for (FormFieldChange change : changes) {
            String label = labels.get(change.getFieldKey());
            if (StringUtils.isNotEmpty(label)) {
                change.setFieldLabel(label);
            }
        }
    }

    private Map<String, String> getFieldLabels(HisTask hisTask,
        Map<String, Map<String, String>> fieldLabelCache) {
        if (!FlowCons.FORM_CUSTOM_Y.equals(hisTask.getFormCustom())
            || StringUtils.isEmpty(hisTask.getFormPath())) {
            return Collections.emptyMap();
        }
        String formPath = hisTask.getFormPath();
        if (fieldLabelCache.containsKey(formPath)) {
            return fieldLabelCache.get(formPath);
        }

        Map<String, String> labels = Collections.emptyMap();
        Long formId = parseFormId(formPath);
        if (formId != null) {
            FormService formService = FlowEngine.formService();
            Form form = formService == null ? null : formService.getById(formId);
            labels = parseFieldLabels(formService, form);
        }
        fieldLabelCache.put(formPath, labels);
        return labels;
    }

    private Long parseFormId(String formPath) {
        try {
            return Long.valueOf(formPath);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, String> parseFieldLabels(FormService formService, Form form) {
        if (formService == null || form == null || StringUtils.isEmpty(form.getFormContent())
            || FlowEngine.jsonConvert == null) {
            return Collections.emptyMap();
        }
        FormDefinition definition;
        try {
            definition = formService.parseDefinition(form);
        } catch (RuntimeException e) {
            LOGGER.debug("Unable to parse form definition for form id {}", form.getId(), e);
            return Collections.emptyMap();
        }
        if (definition == null || !FormDefinition.VERSION_1.equals(definition.getSchemaVersion())
            || CollUtil.isEmpty(definition.getFields())) {
            return Collections.emptyMap();
        }

        Map<String, String> labels = new HashMap<String, String>();
        for (FormFieldDefinition field : definition.getFields()) {
            if (field != null && StringUtils.isNotEmpty(field.getKey())
                && StringUtils.isNotEmpty(field.getLabel())) {
                labels.put(field.getKey(), field.getLabel());
            }
        }
        return labels;
    }

    private int compareHistoryOrder(HisTask left, HisTask right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int timeCompare = compareNullable(eventTime(left), eventTime(right));
        return timeCompare != 0 ? timeCompare : compareNullable(left.getId(), right.getId());
    }

    private Date eventTime(HisTask hisTask) {
        return hisTask.getUpdateTime() != null ? hisTask.getUpdateTime() : hisTask.getCreateTime();
    }

    private <T extends Comparable<T>> int compareNullable(T left, T right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }
}
