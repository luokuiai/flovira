/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0.
 */
package com.luokuiai.flovira.example.common.repository;

import com.luokuiai.flovira.example.common.model.DemoIdentity;
import com.luokuiai.flovira.example.common.model.PurchaseRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ExampleRepository {

    private final JdbcClient jdbc;

    public ExampleRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<DemoIdentity> listIdentities() {
        return jdbc.sql("select id, name, organization_id, role_id from example_user order by id")
            .query((rs, rowNum) -> new DemoIdentity(rs.getString("id"), rs.getString("name"),
                rs.getString("organization_id"), rs.getString("role_id")))
            .list();
    }

    public Optional<DemoIdentity> findIdentity(String id) {
        return jdbc.sql("select id, name, organization_id, role_id from example_user where id = :id")
            .param("id", id)
            .query((rs, rowNum) -> new DemoIdentity(rs.getString("id"), rs.getString("name"),
                rs.getString("organization_id"), rs.getString("role_id")))
            .optional();
    }

    public List<DemoIdentity> findByRole(String roleId) {
        return jdbc.sql("select id, name, organization_id, role_id from example_user where role_id = :roleId order by id")
            .param("roleId", roleId)
            .query((rs, rowNum) -> new DemoIdentity(rs.getString("id"), rs.getString("name"),
                rs.getString("organization_id"), rs.getString("role_id")))
            .list();
    }

    public List<DemoIdentity> findByOrganization(String organizationId) {
        return jdbc.sql("select id, name, organization_id, role_id from example_user where organization_id = :organizationId order by id")
            .param("organizationId", organizationId)
            .query((rs, rowNum) -> new DemoIdentity(rs.getString("id"), rs.getString("name"),
                rs.getString("organization_id"), rs.getString("role_id")))
            .list();
    }

    public List<PurchaseRequest> listPurchases() {
        return jdbc.sql("select id, title, applicant_id, amount, department, status from example_purchase_request order by id")
            .query((rs, rowNum) -> new PurchaseRequest(rs.getString("id"), rs.getString("title"),
                rs.getString("applicant_id"), rs.getBigDecimal("amount"), rs.getString("department"),
                rs.getString("status")))
            .list();
    }

    public Optional<PurchaseRequest> findPurchase(String id) {
        return jdbc.sql("select id, title, applicant_id, amount, department, status from example_purchase_request where id = :id")
            .param("id", id)
            .query((rs, rowNum) -> new PurchaseRequest(rs.getString("id"), rs.getString("title"),
                rs.getString("applicant_id"), rs.getBigDecimal("amount"), rs.getString("department"),
                rs.getString("status")))
            .optional();
    }

    public void markPurchaseStarted(String id) {
        jdbc.sql("update example_purchase_request set status = 'IN_REVIEW' where id = :id")
            .param("id", id)
            .update();
    }
}
