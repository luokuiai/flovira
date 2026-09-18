-- Offline development migration. Back up first; inspect existing columns before running.
-- No lifecycle state or historical execution is inferred by this schema migration.
ALTER TABLE flow_instance ADD lifecycle_state VARCHAR(32);
ALTER TABLE flow_instance ADD resubmission_context TEXT;
ALTER TABLE flow_task ADD node_execution_id BIGINT;
ALTER TABLE flow_his_task ADD node_execution_id BIGINT;

CREATE TABLE flow_node_execution (
    id BIGINT PRIMARY KEY,
    instance_id BIGINT NOT NULL,
    definition_id BIGINT NOT NULL,
    node_code VARCHAR(96) NOT NULL,
    node_type INTEGER NOT NULL,
    state VARCHAR(20) NOT NULL,
    entered_at DATETIME(3) NOT NULL,
    closed_at DATETIME(3),
    close_reason VARCHAR(20),
    version INTEGER DEFAULT 0 NOT NULL,
    created_at DATETIME(3),
    updated_at DATETIME(3),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    tenant_id VARCHAR(40) DEFAULT '0' NOT NULL,
    deleted CHAR(1) DEFAULT '0' NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_node_execution_active ON flow_node_execution (tenant_id,instance_id,deleted,state,entered_at,id);

-- Existing subprocess business keys include two full IDs and a digest.
ALTER TABLE flow_instance MODIFY business_id VARCHAR(128) NOT NULL;
