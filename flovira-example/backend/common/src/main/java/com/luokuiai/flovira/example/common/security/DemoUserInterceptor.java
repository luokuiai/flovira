/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.security;

import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DemoUserInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Demo-User";

    private final ExampleRepository repository;

    public DemoUserInterceptor(ExampleRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String user = request.getHeader(HEADER);
        if (user == null || user.trim().isEmpty()) {
            user = "alice";
        }
        if (repository.findIdentity(user).isEmpty()) {
            throw new IllegalArgumentException("Unknown demo identity: " + user);
        }
        DemoUserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        DemoUserContext.clear();
    }
}
