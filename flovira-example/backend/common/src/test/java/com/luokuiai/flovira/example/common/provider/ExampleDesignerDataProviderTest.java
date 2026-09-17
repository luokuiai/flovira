/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.provider;

import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.ApproverContext;
import com.luokuiai.flovira.core.dto.BusinessSubject;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.handler.AbstractRoleResolver;
import com.luokuiai.flovira.example.common.model.DemoIdentity;
import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.vo.DesignerResourcePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExampleDesignerDataProviderTest {

    private ExampleRepository repository;
    private ExampleDesignerDataProvider provider;

    @BeforeEach
    void setUp() {
        repository = mock(ExampleRepository.class);
        provider = new ExampleDesignerDataProvider(repository);
        when(repository.listIdentities()).thenReturn(Arrays.asList(
            new DemoIdentity("alice", "Alice Applicant", "engineering", "employee"),
            new DemoIdentity("manager", "Manny Manager", "engineering", "manager")));
    }

    @Test
    void exposesStableHostResources() {
        DesignerResourcePage users = provider.queryResources(query("USER"));
        DesignerResourcePage roles = provider.queryResources(query("ROLE"));
        DesignerResourcePage organizations = provider.queryResources(query("ORGANIZATION"));
        DesignerResourcePage fields = provider.queryResources(query("FORM_FIELD"));

        assertThat(users.getItems()).extracting("id").containsExactly("alice", "manager");
        assertThat(roles.getItems()).extracting("id").containsExactly("employee", "manager", "finance");
        assertThat(organizations.getItems()).extracting("id").containsExactly("engineering", "finance");
        assertThat(fields.getItems()).extracting("id").containsExactly("title", "amount", "department");
        assertThat(fields.getItems().get(1).getMetadata()).containsEntry("type", "NUMBER");
    }

    @Test
    void filtersAndPagesResourcesDeterministically() {
        DesignerResourceQuery query = query("ROLE").setKeyword("man").setPageSize(1);
        DesignerResourcePage page = provider.queryResources(query);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getItems()).extracting("id").containsExactly("manager");
    }

    @Test
    void resolvesCurrentRoleMembersOnlyWhenCalled() {
        AbstractRoleResolver resolver = new ExampleApproverConfiguration().roleApproverResolver(repository);
        ApproverRule rule = new ApproverRule().setStrategy("ROLE").setSubjects(Collections.singletonList(
            new BusinessSubject().setId("manager").setType("ROLE")));
        resolver.validate(rule);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findByRole("manager");
        when(repository.findByRole("manager")).thenReturn(Collections.singletonList(
            new DemoIdentity("manager", "Manager", "engineering", "manager")));
        assertThat(resolver.resolve(new ApproverContext(null, rule, null, new FlowParams(), false)))
            .containsExactly("manager");
        when(repository.findByRole("manager")).thenReturn(Collections.singletonList(
            new DemoIdentity("new-manager", "New manager", "engineering", "manager")));
        assertThat(resolver.resolve(new ApproverContext(null, rule, null, new FlowParams(), true)))
            .containsExactly("new-manager");
    }

    private DesignerResourceQuery query(String resourceType) {
        return new DesignerResourceQuery().setResourceType(resourceType);
    }
}
