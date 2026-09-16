/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.security;

import com.luokuiai.flovira.core.handler.PermissionHandler;

import java.util.Collections;
import java.util.List;

/** Development-only identity adapter used by the examples. */
public class DemoPermissionHandler implements PermissionHandler {

    @Override
    public List<String> permissions() {
        return Collections.singletonList(DemoUserContext.get());
    }

    @Override
    public String getHandler() {
        return DemoUserContext.get();
    }
}
