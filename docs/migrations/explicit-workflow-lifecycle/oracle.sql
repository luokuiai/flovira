-- Offline development migration. Back up first; inspect existing columns before running.
-- No lifecycle state or historical execution is inferred by this schema migration.
ALTER TABLE flow_instance ADD lifecycle_state VARCHAR2(32);
ALTER TABLE flow_instance ADD resubmission_context CLOB;
ALTER TABLE flow_task ADD node_execution_id NUMBER(19);
ALTER TABLE flow_his_task ADD node_execution_id NUMBER(19);

CREATE TABLE flow_node_execution (
    id NUMBER(19) PRIMARY KEY,
    instance_id NUMBER(19) NOT NULL,
    definition_id NUMBER(19) NOT NULL,
    node_code VARCHAR2(96) NOT NULL,
    node_type NUMBER(10) NOT NULL,
    state VARCHAR2(20) NOT NULL,
    entered_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    close_reason VARCHAR2(20),
    version NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR2(64),
    updated_by VARCHAR2(64),
    tenant_id VARCHAR2(40) DEFAULT '0' NOT NULL,
    deleted CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX idx_node_execution_active ON flow_node_execution (tenant_id,instance_id,deleted,state,entered_at,id);

-- Existing subprocess business keys include two full IDs and a digest.
ALTER TABLE flow_instance MODIFY BUSINESS_ID VARCHAR2(128);
