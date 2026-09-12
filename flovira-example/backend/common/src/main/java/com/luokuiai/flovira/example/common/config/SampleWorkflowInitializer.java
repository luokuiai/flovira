/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.config;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.example.common.security.DemoUserContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

@Component
public class SampleWorkflowInitializer implements ApplicationRunner {

    static final String FLOW_CODE = "example_purchase_approval";

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        DemoUserContext.set("alice");
        try {
            List<Definition> definitions = FlowEngine.defService().getByFlowCode(FLOW_CODE);
            Definition definition;
            if (definitions.isEmpty()) {
                try (InputStream input = new ClassPathResource("sample-flow.json").getInputStream()) {
                    definition = FlowEngine.defService().importIs(input);
                }
            } else {
                definition = definitions.stream().max(Comparator.comparing(Definition::getId)).orElseThrow();
            }
            if (!PublishStatus.PUBLISHED.getKey().equals(definition.getPublishStatus())) {
                FlowEngine.defService().publish(definition.getId());
            }
        } finally {
            DemoUserContext.clear();
        }
    }
}
