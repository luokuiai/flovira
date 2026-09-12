/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.security;

public final class DemoUserContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private DemoUserContext() {
    }

    public static String get() {
        String user = CURRENT.get();
        return user == null ? "alice" : user;
    }

    public static void set(String user) {
        CURRENT.set(user);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
