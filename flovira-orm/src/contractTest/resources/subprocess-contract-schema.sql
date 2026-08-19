DROP TABLE IF EXISTS flow_subprocess_event;
DROP TABLE IF EXISTS flow_subprocess_child;
DROP TABLE IF EXISTS flow_subprocess_run;
DROP TABLE IF EXISTS flow_task;

CREATE TABLE flow_task (
    id bigint PRIMARY KEY, definition_id bigint NOT NULL, instance_id bigint NOT NULL,
    node_code varchar(100) NOT NULL, node_name varchar(100), node_type smallint NOT NULL,
    flow_status varchar(20) NOT NULL, form_custom char(1) DEFAULT 'N', form_path varchar(100),
    create_time timestamp, create_by varchar(64) DEFAULT '', update_time timestamp,
    update_by varchar(64) DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0', tenant_id varchar(40),
    timeout_at timestamp, timeout_action varchar(32), timeout_config text,
    timeout_status varchar(16), timeout_claimed_at timestamp
);
CREATE INDEX idx_flow_task_timeout_due ON flow_task (timeout_status, timeout_at, timeout_claimed_at);

CREATE TABLE flow_subprocess_run (
    id bigint PRIMARY KEY, parent_instance_id bigint NOT NULL, parent_task_id bigint NOT NULL,
    parent_definition_id bigint NOT NULL, parent_node_code varchar(100) NOT NULL,
    child_flow_code varchar(100) NOT NULL, child_definition_id bigint NOT NULL,
    child_definition_version varchar(20) NOT NULL, completion_policy varchar(20) NOT NULL DEFAULT 'ALL',
    collection_fingerprint char(64) NOT NULL, expected_count integer NOT NULL DEFAULT 0,
    pending_count integer NOT NULL DEFAULT 0, running_count integer NOT NULL DEFAULT 0,
    completed_count integer NOT NULL DEFAULT 0, failed_count integer NOT NULL DEFAULT 0,
    cancelled_count integer NOT NULL DEFAULT 0, run_status varchar(30) NOT NULL,
    failure_code varchar(100), lock_version integer NOT NULL DEFAULT 0,
    initialized_at timestamp, completed_at timestamp, create_time timestamp, create_by varchar(64) DEFAULT '',
    update_time timestamp, update_by varchar(64) DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0',
    tenant_id varchar(40) NOT NULL DEFAULT '0', CONSTRAINT uk_subprocess_run_parent_task UNIQUE (tenant_id,parent_task_id)
);

CREATE TABLE flow_subprocess_child (
    id bigint PRIMARY KEY, run_id bigint NOT NULL, item_key varchar(200) NOT NULL, item_label varchar(200),
    child_business_key varchar(100) NOT NULL, child_flow_code varchar(100) NOT NULL,
    child_definition_id bigint NOT NULL, child_definition_version varchar(20) NOT NULL,
    child_instance_id bigint, child_status varchar(20) NOT NULL, outcome varchar(20),
    started_at timestamp, completed_at timestamp, create_time timestamp, create_by varchar(64) DEFAULT '',
    update_time timestamp, update_by varchar(64) DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0',
    tenant_id varchar(40) NOT NULL DEFAULT '0', CONSTRAINT uk_subprocess_child_item UNIQUE (tenant_id,run_id,item_key),
    CONSTRAINT uk_subprocess_child_instance UNIQUE (tenant_id,child_instance_id)
);

CREATE TABLE flow_subprocess_event (
    id bigint PRIMARY KEY, run_id bigint NOT NULL, child_id bigint, parent_instance_id bigint NOT NULL,
    child_instance_id bigint, parent_node_code varchar(100) NOT NULL, event_type varchar(50) NOT NULL,
    event_result varchar(30) NOT NULL, reason varchar(500), occurred_at timestamp NOT NULL,
    create_time timestamp, create_by varchar(64) DEFAULT '', update_time timestamp, update_by varchar(64) DEFAULT '',
    del_flag char(1) NOT NULL DEFAULT '0', tenant_id varchar(40) NOT NULL DEFAULT '0'
);
