/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.security;

import com.luokuiai.flovira.example.common.model.DemoIdentity;
import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoUserInterceptorTest {

    private final ExampleRepository repository = mock(ExampleRepository.class);
    private final DemoUserInterceptor interceptor = new DemoUserInterceptor(repository);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void clearContext() {
        DemoUserContext.clear();
    }

    @Test
    void propagatesKnownIdentityToPermissionHandlerAndClearsIt() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(DemoUserInterceptor.HEADER)).thenReturn("manager");
        when(repository.findIdentity("manager")).thenReturn(Optional.of(
            new DemoIdentity("manager", "Manny Manager", "engineering", "manager")));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(new DemoPermissionHandler().getHandler()).isEqualTo("manager");
        assertThat(new DemoPermissionHandler().permissions()).containsExactly("manager");

        interceptor.afterCompletion(request, response, new Object(), null);
        assertThat(DemoUserContext.get()).isEqualTo("alice");
    }

    @Test
    void rejectsUnknownIdentity() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(DemoUserInterceptor.HEADER)).thenReturn("intruder");
        when(repository.findIdentity("intruder")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("intruder");
    }
}
