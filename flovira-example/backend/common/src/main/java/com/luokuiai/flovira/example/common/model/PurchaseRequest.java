/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.model;

import java.math.BigDecimal;

public record PurchaseRequest(String id, String title, String applicantId, BigDecimal amount,
                              String department, String status) {
}
