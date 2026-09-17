/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.provider;

import com.luokuiai.flovira.example.common.model.DemoIdentity;
import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.service.DesignerDataProvider;
import com.luokuiai.flovira.ui.vo.DesignerResourceItem;
import com.luokuiai.flovira.ui.vo.DesignerResourcePage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ExampleDesignerDataProvider implements DesignerDataProvider {

    private final ExampleRepository repository;

    public ExampleDesignerDataProvider(ExampleRepository repository) {
        this.repository = repository;
    }

    @Override
    public DesignerResourcePage queryResources(DesignerResourceQuery query) {
        List<DesignerResourceItem> items = resources(query.getResourceType());
        String keyword = query.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowered = keyword.toLowerCase(Locale.ROOT);
            items = items.stream().filter(item -> (item.getName() + " " + item.getCode())
                .toLowerCase(Locale.ROOT).contains(lowered)).collect(Collectors.toList());
        }
        int pageSize = Math.max(1, query.getPageSize());
        int from = Math.min(Math.max(0, query.getPageNum() - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new DesignerResourcePage().setItems(new ArrayList<>(items.subList(from, to))).setTotal(items.size());
    }


    private List<DesignerResourceItem> resources(String resourceType) {
        if ("USER".equals(resourceType) || "SUBJECT".equals(resourceType)) {
            return repository.listIdentities().stream().map(user -> item(user.id(), user.id(), user.name(), "USER"))
                .collect(Collectors.toList());
        }
        if ("ROLE".equals(resourceType)) {
            return Arrays.asList(item("employee", "employee", "Employee", "ROLE"),
                item("manager", "manager", "Manager", "ROLE"),
                item("finance", "finance", "Finance", "ROLE"));
        }
        if ("ORGANIZATION".equals(resourceType)) {
            return Arrays.asList(item("engineering", "engineering", "Engineering", "ORGANIZATION"),
                item("finance", "finance", "Finance", "ORGANIZATION"));
        }
        if ("FORM".equals(resourceType)) {
            return Collections.singletonList(item("purchase-request-v1", "purchase-request-v1",
                "Purchase request v1", "FORM"));
        }
        if ("FORM_FIELD".equals(resourceType)) {
            return Arrays.asList(field("title", "Request title", "STRING"),
                field("amount", "Purchase amount", "NUMBER"), field("department", "Department", "STRING"));
        }
        if ("CATEGORY".equals(resourceType)) {
            return Collections.singletonList(item("examples", "examples", "Examples", "CATEGORY"));
        }
        return Collections.emptyList();
    }

    private DesignerResourceItem field(String id, String name, String type) {
        DesignerResourceItem item = item(id, id, name, "FORM_FIELD");
        item.getMetadata().put("type", type);
        return item;
    }

    private DesignerResourceItem item(String id, String code, String name, String type) {
        return new DesignerResourceItem().setId(id).setCode(code).setName(name).setResourceType(type);
    }
}
