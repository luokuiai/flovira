/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.web;

import com.luokuiai.flovira.core.dto.ApiResult;
import com.luokuiai.flovira.core.exception.FlowException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExampleExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResult.fail(400, exception.getMessage()));
    }

    @ExceptionHandler(FlowException.class)
    public ResponseEntity<ApiResult<Void>> workflow(FlowException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.fail(409, exception.getMessage()));
    }
}
