DROP TABLE IF EXISTS flow_subprocess_event;
DROP TABLE IF EXISTS flow_subprocess_child;
DROP TABLE IF EXISTS flow_subprocess_run;
DROP TABLE IF EXISTS flow_task;
DROP TABLE IF EXISTS flow_form;
DROP TABLE IF EXISTS flow_definition;
DROP TABLE IF EXISTS flow_instance;
DROP TABLE IF EXISTS flow_node;
DROP TABLE IF EXISTS flow_skip;

CREATE TABLE flow_node
(
    id              int8          NOT NULL,
    node_type       int2          NOT NULL,
    definition_id   int8          NOT NULL,
    node_code       varchar(96)   NOT NULL,
    node_name       varchar(100)  NULL,
    permission_flag varchar(200)  NULL,
    node_ratio      varchar(200) NULL,
    coordinate      varchar(100)  NULL,
    any_node_skip   varchar(100)  NULL,
    listener_type   varchar(100)  NULL,
    listener_path   varchar(400)  NULL,
    form_id       varchar(100)  NULL,
    "version"       varchar(20)   NOT NULL,
    created_at     timestamp     NULL,
    created_by       varchar(64)   NULL     DEFAULT '':: character varying,
    updated_at     timestamp     NULL,
    updated_by       varchar(64)   NULL     DEFAULT '':: character varying,
    ext             jsonb          NULL,
    deleted        bpchar(1)     NOT NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)   NULL,
    CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_skip
(
    id             int8         NOT NULL,
    definition_id  int8         NOT NULL,
    source_node_code  varchar(96) NOT NULL,
    source_node_type  int2         NULL,
    target_node_code varchar(96) NOT NULL,
    target_node_type int2         NULL,
    skip_name      varchar(100) NULL,
    skip_type      varchar(40)  NULL,
    skip_condition varchar(200) NULL,
    coordinate     varchar(100) NULL,
    created_at    timestamp    NULL,
    created_by      varchar(64)  NULL     DEFAULT '':: character varying,
    updated_at    timestamp    NULL,
    updated_by      varchar(64)  NULL     DEFAULT '':: character varying,
    deleted       bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id      varchar(40)  NULL,
    CONSTRAINT flow_skip_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_instance
(
    lifecycle_state VARCHAR(32),
    resubmission_context TEXT,
    id              int8         NOT NULL,
    definition_id   int8         NOT NULL,
    business_type   varchar(128) NOT NULL,
    business_id     varchar(128)  NOT NULL,
    node_type       int2         NOT NULL,
    node_code       varchar(96)  NOT NULL,
    node_name       varchar(100) NULL,
    variables        text         NULL,
    flow_status     varchar(20)  NOT NULL,
    activity_status int2         NOT NULL DEFAULT 1,
    def_json        text         NULL,
    created_at     timestamp    NULL,
    created_by       varchar(64)  NULL     DEFAULT '':: character varying,
    updated_at     timestamp    NULL,
    updated_by       varchar(64)  NULL     DEFAULT '':: character varying,
    ext             jsonb NULL,
    deleted        bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_instance_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_definition (
    id bigint PRIMARY KEY, flow_code varchar(40) NOT NULL, flow_name varchar(100) NOT NULL,
    category varchar(100), business_type varchar(128) NOT NULL, version varchar(20) NOT NULL,
    publish_status smallint NOT NULL DEFAULT 0, form_id varchar(100),
    activity_status smallint NOT NULL DEFAULT 1, listener_type varchar(100), listener_path varchar(400),
    ext jsonb, created_at timestamp, created_by varchar(64) DEFAULT '', updated_at timestamp,
    updated_by varchar(64) DEFAULT '', deleted char(1) NOT NULL DEFAULT '0', tenant_id varchar(40)
);

CREATE TABLE flow_form (
    id bigint PRIMARY KEY, form_code varchar(40) NOT NULL, form_name varchar(100) NOT NULL,
    version varchar(20) NOT NULL, publish_status smallint NOT NULL DEFAULT 0,
    form_content text, ext jsonb,
    created_at timestamp, created_by varchar(64) DEFAULT '', updated_at timestamp,
    updated_by varchar(64) DEFAULT '', deleted char(1) NOT NULL DEFAULT '0', tenant_id varchar(40)
);
CREATE INDEX idx_flow_form_code ON flow_form (tenant_id, form_code, deleted, publish_status, version);
CREATE INDEX idx_flow_form_published ON flow_form (tenant_id, publish_status, deleted, form_name);

CREATE TABLE flow_task (
    node_execution_id BIGINT,
    id bigint PRIMARY KEY, definition_id bigint NOT NULL, instance_id bigint NOT NULL,
    node_code varchar(100) NOT NULL, node_name varchar(100), node_type smallint NOT NULL,
    flow_status varchar(20) NOT NULL, form_id varchar(100),
    created_at timestamp, created_by varchar(64) DEFAULT '', updated_at timestamp,
    updated_by varchar(64) DEFAULT '', deleted char(1) NOT NULL DEFAULT '0', tenant_id varchar(40),
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
    initialized_at timestamp, completed_at timestamp, created_at timestamp, created_by varchar(64) DEFAULT '',
    updated_at timestamp, updated_by varchar(64) DEFAULT '', deleted char(1) NOT NULL DEFAULT '0',
    tenant_id varchar(40) NOT NULL DEFAULT '0', CONSTRAINT uk_subprocess_run_parent_task UNIQUE (tenant_id,parent_task_id)
);

CREATE TABLE flow_subprocess_child (
    id bigint PRIMARY KEY, run_id bigint NOT NULL, item_key varchar(200) NOT NULL, item_label varchar(200),
    child_business_key varchar(100) NOT NULL, child_flow_code varchar(100) NOT NULL,
    child_definition_id bigint NOT NULL, child_definition_version varchar(20) NOT NULL,
    child_instance_id bigint, child_status varchar(20) NOT NULL, outcome varchar(20),
    started_at timestamp, completed_at timestamp, created_at timestamp, created_by varchar(64) DEFAULT '',
    updated_at timestamp, updated_by varchar(64) DEFAULT '', deleted char(1) NOT NULL DEFAULT '0',
    tenant_id varchar(40) NOT NULL DEFAULT '0', CONSTRAINT uk_subprocess_child_item UNIQUE (tenant_id,run_id,item_key),
    CONSTRAINT uk_subprocess_child_instance UNIQUE (tenant_id,child_instance_id)
);

CREATE TABLE flow_subprocess_event (
    id bigint PRIMARY KEY, run_id bigint NOT NULL, child_id bigint, parent_instance_id bigint NOT NULL,
    child_instance_id bigint, parent_node_code varchar(100) NOT NULL, event_type varchar(50) NOT NULL,
    event_result varchar(30) NOT NULL, reason varchar(500), occurred_at timestamp NOT NULL,
    created_at timestamp, created_by varchar(64) DEFAULT '', updated_at timestamp, updated_by varchar(64) DEFAULT '',
    deleted char(1) NOT NULL DEFAULT '0', tenant_id varchar(40) NOT NULL DEFAULT '0'
);

-- 实际业务节点执行；网关不建立生命周期执行记录。
CREATE TABLE flow_node_execution (
    id BIGINT PRIMARY KEY,
    instance_id BIGINT NOT NULL,
    definition_id BIGINT NOT NULL,
    node_code VARCHAR(96) NOT NULL,
    node_type INTEGER NOT NULL,
    state VARCHAR(20) NOT NULL,
    entered_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    close_reason VARCHAR(20),
    version INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    tenant_id VARCHAR(40) DEFAULT '0' NOT NULL,
    deleted CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX idx_node_execution_active ON flow_node_execution (tenant_id,instance_id,deleted,state,entered_at,id);

CREATE TABLE flow_his_task
(
    node_execution_id BIGINT,
    id               int8         NOT NULL,
    definition_id    int8         NOT NULL,
    instance_id      int8         NOT NULL,
    task_id          int8         NOT NULL,
    node_code        varchar(96) NULL,
    node_name        varchar(100) NULL,
    node_type        int2         NULL,
    target_node_code varchar(96) NULL,
    target_node_name varchar(200) NULL,
    approver         varchar(40)  NULL,
    cooperation_type   int2         NOT NULL DEFAULT 0,
    collaborator     varchar(500)  NULL,
    skip_type        varchar(10)  NOT NULL,
    flow_status      varchar(20)  NOT NULL,
    form_id        varchar(100) NULL,
    ext              jsonb         NULL,
    message          varchar(500) NULL,
    variables         text         NULL,
    created_at      timestamp    NULL,
    updated_at      timestamp    NULL,
    deleted         bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id        varchar(40)  NULL,
    CONSTRAINT flow_his_task_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_user
(
    id           int8        NOT NULL,
    "type"       bpchar(1)   NOT NULL,
    processed_by varchar(80) NULL,
    task_id   int8        NOT NULL,
    created_at  timestamp    NULL,
    created_by    varchar(64)  NULL     DEFAULT '':: character varying,
    updated_at  timestamp    NULL,
    updated_by    varchar(64)  NULL     DEFAULT '':: character varying,
    deleted     bpchar(1)   NOT NULL DEFAULT '0':: character varying,
    tenant_id    varchar(40) NULL,
    CONSTRAINT flow_user_pk PRIMARY KEY (id)
);
