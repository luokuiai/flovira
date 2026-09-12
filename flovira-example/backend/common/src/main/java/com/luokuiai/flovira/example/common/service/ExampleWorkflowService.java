/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.service;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.HisTask;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.core.enums.UserType;
import com.luokuiai.flovira.example.common.model.PurchaseRequest;
import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import com.luokuiai.flovira.example.common.security.DemoUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExampleWorkflowService {

    private final ExampleRepository repository;

    public ExampleWorkflowService(ExampleRepository repository) {
        this.repository = repository;
    }

    public List<DefinitionSummary> listDefinitions() {
        return FlowEngine.defService().list(FlowEngine.newDef()).stream()
            .sorted(Comparator.comparing(Definition::getId).reversed())
            .map(this::summary).collect(Collectors.toList());
    }

    @Transactional
    public DefinitionSummary saveDefinition(DefJson definition) throws Exception {
        if (definition == null || definition.getFlowCode() == null || definition.getFlowCode().trim().isEmpty()) {
            throw new IllegalArgumentException("flowCode is required");
        }
        definition.setCreatedBy(DemoUserContext.get()).setUpdatedBy(DemoUserContext.get());
        FlowEngine.defService().saveDef(definition, false);
        Definition saved = FlowEngine.defService().getByFlowCode(definition.getFlowCode()).stream()
            .max(Comparator.comparing(Definition::getId))
            .orElseThrow(() -> new IllegalStateException("Definition was not persisted"));
        return summary(saved);
    }

    @Transactional
    public DefinitionSummary publish(Long id) {
        Definition definition = requireDefinition(id);
        FlowEngine.defService().publish(id);
        return summary(FlowEngine.defService().getById(definition.getId()));
    }

    @Transactional
    public Instance start(String purchaseId, Long definitionId) {
        PurchaseRequest purchase = repository.findPurchase(purchaseId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown purchase request: " + purchaseId));
        Definition definition = requireDefinition(definitionId);
        if (!PublishStatus.PUBLISHED.getKey().equals(definition.getPublishStatus())) {
            throw new IllegalArgumentException("Definition must be published before starting a process");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("title", purchase.title());
        variables.put("amount", purchase.amount());
        variables.put("department", purchase.department());
        FlowParams params = FlowParams.build().handler(DemoUserContext.get()).variables(variables)
            .formData(new LinkedHashMap<>(variables));
        Instance instance = FlowEngine.instanceService().startByDefinitionId("purchase-request", purchaseId,
            definitionId, params);
        repository.markPurchaseStarted(purchaseId);
        return instance;
    }

    public List<TaskSummary> listTasksForCurrentUser() {
        String user = DemoUserContext.get();
        List<TaskSummary> result = new ArrayList<>();
        for (Task task : FlowEngine.taskService().list(FlowEngine.newTask())) {
            List<String> permissions = FlowEngine.userService().getPermission(task.getId(), UserType.APPROVAL.getKey());
            if (permissions.contains(user)) {
                result.add(taskSummary(task));
            }
        }
        result.sort(Comparator.comparing(TaskSummary::id).reversed());
        return result;
    }

    @Transactional
    public Instance handle(Long taskId, boolean approve, String message) {
        Task task = FlowEngine.taskService().getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Unknown task: " + taskId);
        }
        List<String> permissions = FlowEngine.userService().getPermission(taskId, UserType.APPROVAL.getKey());
        if (!permissions.contains(DemoUserContext.get())) {
            throw new IllegalArgumentException("Current demo user cannot handle task " + taskId);
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        return approve ? FlowEngine.taskService().pass(taskId, message, variables)
            : FlowEngine.taskService().reject(taskId, message, variables);
    }

    public Map<String, Object> progress(Long instanceId) {
        Instance instance = FlowEngine.instanceService().getById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("Unknown process instance: " + instanceId);
        }
        List<Task> tasks = FlowEngine.taskService().getByInsId(instanceId);
        List<HisTask> history = FlowEngine.hisTaskService().getByInsId(instanceId);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("instance", instance);
        view.put("currentTasks", tasks.stream().map(this::taskSummary).collect(Collectors.toList()));
        view.put("history", history);
        view.put("definition", FlowEngine.jsonConvert.strToMap(instance.getDefJson()));
        view.put("business", repository.findPurchase(instance.getBusinessId()).orElse(null));
        return view;
    }

    private Definition requireDefinition(Long id) {
        Definition definition = FlowEngine.defService().getById(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown definition: " + id);
        }
        return definition;
    }

    private DefinitionSummary summary(Definition definition) {
        return new DefinitionSummary(definition.getId(), definition.getFlowCode(), definition.getFlowName(),
            definition.getVersion(), definition.getPublishStatus());
    }

    private TaskSummary taskSummary(Task task) {
        return new TaskSummary(task.getId(), task.getInstanceId(), task.getDefinitionId(), task.getBusinessId(),
            task.getNodeCode(), task.getNodeName(), task.getFlowStatus());
    }

    public record DefinitionSummary(Long id, String flowCode, String flowName, String version,
                                    Integer publishStatus) {
    }

    public record TaskSummary(Long id, Long instanceId, Long definitionId, String businessId,
                              String nodeCode, String nodeName, String flowStatus) {
    }
}
