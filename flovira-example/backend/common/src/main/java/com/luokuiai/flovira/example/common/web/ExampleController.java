/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.web;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.ApiResult;
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.example.common.model.DemoIdentity;
import com.luokuiai.flovira.example.common.model.PurchaseRequest;
import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import com.luokuiai.flovira.example.common.service.ExampleWorkflowService;
import com.luokuiai.flovira.example.common.service.ExampleWorkflowService.DefinitionSummary;
import com.luokuiai.flovira.example.common.service.ExampleWorkflowService.TaskSummary;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/example/v1")
public class ExampleController {

    private final ExampleRepository repository;
    private final ExampleWorkflowService workflow;
    private final Environment environment;

    public ExampleController(ExampleRepository repository, ExampleWorkflowService workflow, Environment environment) {
        this.repository = repository;
        this.workflow = workflow;
        this.environment = environment;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("application", environment.getProperty("spring.application.name", "flovira-example"));
        data.put("database", FlowEngine.dataSourceType());
        return ApiResult.ok(data);
    }

    @GetMapping("/identities")
    public ApiResult<List<DemoIdentity>> identities() {
        return ApiResult.ok(repository.listIdentities());
    }

    @GetMapping("/definitions")
    public ApiResult<List<DefinitionSummary>> definitions() {
        return ApiResult.ok(workflow.listDefinitions());
    }

    @PostMapping("/definitions")
    public ApiResult<DefinitionSummary> saveDefinition(@RequestBody DefJson definition) throws Exception {
        return ApiResult.ok(workflow.saveDefinition(definition), "Definition saved");
    }

    @PostMapping("/definitions/{id}/publish")
    public ApiResult<DefinitionSummary> publish(@PathVariable("id") Long id) {
        return ApiResult.ok(workflow.publish(id), "Definition published");
    }

    @GetMapping("/purchases")
    public ApiResult<List<PurchaseRequest>> purchases() {
        return ApiResult.ok(repository.listPurchases());
    }

    @PostMapping("/purchases/{id}/start")
    public ApiResult<Instance> start(@PathVariable("id") String id, @RequestBody StartRequest request) {
        return ApiResult.ok(workflow.start(id, request.definitionId()), "Process started");
    }

    @GetMapping("/tasks")
    public ApiResult<List<TaskSummary>> tasks() {
        return ApiResult.ok(workflow.listTasksForCurrentUser());
    }

    @PostMapping("/tasks/{id}/approve")
    public ApiResult<Instance> approve(@PathVariable("id") Long id, @RequestBody HandleRequest request) {
        return ApiResult.ok(workflow.handle(id, true, request.message()), "Task approved");
    }

    @PostMapping("/tasks/{id}/reject")
    public ApiResult<Instance> reject(@PathVariable("id") Long id, @RequestBody HandleRequest request) {
        return ApiResult.ok(workflow.handle(id, false, request.message()), "Task rejected");
    }

    @GetMapping("/instances/{id}/progress")
    public ApiResult<Map<String, Object>> progress(@PathVariable("id") Long id) {
        return ApiResult.ok(workflow.progress(id));
    }

    public record StartRequest(Long definitionId) {
    }

    public record HandleRequest(String message) {
    }
}
