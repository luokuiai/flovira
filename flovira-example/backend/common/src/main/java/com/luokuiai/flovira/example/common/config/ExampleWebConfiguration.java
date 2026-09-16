/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.config;

import com.luokuiai.flovira.core.handler.FormFieldProvider;
import com.luokuiai.flovira.example.common.security.DemoUserInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class ExampleWebConfiguration implements WebMvcConfigurer {

    private final DemoUserInterceptor demoUserInterceptor;

    public ExampleWebConfiguration(DemoUserInterceptor demoUserInterceptor) {
        this.demoUserInterceptor = demoUserInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoUserInterceptor).addPathPatterns("/api/example/**", "/flovira/**");
    }

    @Bean
    public FormFieldProvider exampleFormFieldProvider() {
        return formId -> {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("title", "Request title");
            fields.put("amount", "Purchase amount");
            fields.put("department", "Department");
            return fields;
        };
    }
}
