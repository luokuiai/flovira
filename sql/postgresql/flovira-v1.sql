-- Flovira 1.0.0 PostgreSQL initialization schema.
-- Fresh-install baseline; migrate existing databases separately.

CREATE TABLE flow_definition
(
    id              int8         NOT NULL,
    flow_code       varchar(40)  NOT NULL,
    flow_name       varchar(100) NOT NULL,
    category        varchar(100) NULL,
    "version"       varchar(20)  NOT NULL,
    publish_status      int2         NOT NULL DEFAULT 0,
    form_id       varchar(100) NULL,
    activity_status int2         NOT NULL DEFAULT 1,
    listener_type   varchar(100) NULL,
    listener_path   varchar(400) NULL,
    ext             varchar(500) NULL,
    created_at     timestamp    NULL,
    created_by       varchar(64)  NULL     DEFAULT '':: character varying,
    updated_at     timestamp    NULL,
    updated_by       varchar(64)  NULL     DEFAULT '':: character varying,
    deleted        bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_definition IS '流程定义表';

COMMENT ON COLUMN flow_definition.id IS '主键id';
COMMENT ON COLUMN flow_definition.flow_code IS '流程编码';
COMMENT ON COLUMN flow_definition.flow_name IS '流程名称';
COMMENT ON COLUMN flow_definition.category IS '流程类别';
COMMENT ON COLUMN flow_definition."version" IS '流程版本';
COMMENT ON COLUMN flow_definition.publish_status IS '是否发布（0未发布 1已发布 9失效）';
COMMENT ON COLUMN flow_definition.form_id IS '外部业务表单标识';
COMMENT ON COLUMN flow_definition.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_definition.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_definition.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_definition.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_definition.created_at IS '创建时间';
COMMENT ON COLUMN flow_definition.created_by IS '创建人';
COMMENT ON COLUMN flow_definition.updated_at IS '更新时间';
COMMENT ON COLUMN flow_definition.updated_by IS '更新人';
COMMENT ON COLUMN flow_definition.deleted IS '删除标志';
COMMENT ON COLUMN flow_definition.tenant_id IS '租户id';
CREATE INDEX idx_flow_definition_lookup ON flow_definition (tenant_id, flow_code, deleted, publish_status);

CREATE TABLE flow_form
(
    id             int8         NOT NULL,
    form_code      varchar(40)  NOT NULL,
    form_name      varchar(100) NOT NULL,
    "version"      varchar(20)  NOT NULL,
    publish_status int2         NOT NULL DEFAULT 0,
    form_content   text         NULL,
    ext            varchar(500) NULL,
    created_at     timestamp    NULL,
    created_by     varchar(64)  NULL DEFAULT '':: character varying,
    updated_at     timestamp    NULL,
    updated_by     varchar(64)  NULL DEFAULT '':: character varying,
    deleted        bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id      varchar(40)  NULL,
    CONSTRAINT flow_form_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_form IS '流程表单表';
COMMENT ON COLUMN flow_form.id IS '主键id';
COMMENT ON COLUMN flow_form.form_code IS '表单编码';
COMMENT ON COLUMN flow_form.form_name IS '表单名称';
COMMENT ON COLUMN flow_form."version" IS '表单版本';
COMMENT ON COLUMN flow_form.publish_status IS '是否发布（0未发布 1已发布 9失效）';
COMMENT ON COLUMN flow_form.form_content IS '表单定义内容';
COMMENT ON COLUMN flow_form.ext IS '扩展字段';
COMMENT ON COLUMN flow_form.created_at IS '创建时间';
COMMENT ON COLUMN flow_form.created_by IS '创建人';
COMMENT ON COLUMN flow_form.updated_at IS '更新时间';
COMMENT ON COLUMN flow_form.updated_by IS '更新人';
COMMENT ON COLUMN flow_form.deleted IS '删除标志';
COMMENT ON COLUMN flow_form.tenant_id IS '租户id';
CREATE INDEX idx_flow_form_code ON flow_form (tenant_id, form_code, deleted, publish_status, "version");
CREATE INDEX idx_flow_form_published ON flow_form (tenant_id, publish_status, deleted, form_name);

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
    ext             text          NULL,
    deleted        bpchar(1)     NOT NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)   NULL,
    CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_node IS '流程节点表';

COMMENT ON COLUMN flow_node.id IS '主键id';
COMMENT ON COLUMN flow_node.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）';
COMMENT ON COLUMN flow_node.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_node.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_node.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_node.permission_flag IS '权限标识（权限类型:权限标识，可以多个，用@@隔开)';
COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';
COMMENT ON COLUMN flow_node.coordinate IS '坐标';
COMMENT ON COLUMN flow_node.any_node_skip IS '任意结点跳转';
COMMENT ON COLUMN flow_node.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_node.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_node.form_id IS '外部业务表单标识';
COMMENT ON COLUMN flow_node."version" IS '版本';
COMMENT ON COLUMN flow_node.created_at IS '创建时间';
COMMENT ON COLUMN flow_node.created_by IS '创建人';
COMMENT ON COLUMN flow_node.updated_at IS '更新时间';
COMMENT ON COLUMN flow_node.updated_by IS '更新人';
COMMENT ON COLUMN flow_node.ext IS '节点扩展属性';
COMMENT ON COLUMN flow_node.deleted IS '删除标志';
COMMENT ON COLUMN flow_node.tenant_id IS '租户id';
CREATE INDEX idx_flow_node_definition ON flow_node (tenant_id, definition_id, deleted, node_code);


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
COMMENT ON TABLE flow_skip IS '节点跳转关联表';

COMMENT ON COLUMN flow_skip.id IS '主键id';
COMMENT ON COLUMN flow_skip.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_skip.source_node_code IS '跳转来源节点编码';
COMMENT ON COLUMN flow_skip.source_node_type IS '跳转来源节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）';
COMMENT ON COLUMN flow_skip.target_node_code IS '跳转目标节点编码';
COMMENT ON COLUMN flow_skip.target_node_type IS '跳转目标节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）';
COMMENT ON COLUMN flow_skip.skip_name IS '跳转名称';
COMMENT ON COLUMN flow_skip.skip_type IS '跳转类型（PASS审批通过 REJECT退回）';
COMMENT ON COLUMN flow_skip.skip_condition IS '跳转条件';
COMMENT ON COLUMN flow_skip.coordinate IS '坐标';
COMMENT ON COLUMN flow_skip.created_at IS '创建时间';
COMMENT ON COLUMN flow_skip.created_by IS '创建人';
COMMENT ON COLUMN flow_skip.updated_at IS '更新时间';
COMMENT ON COLUMN flow_skip.updated_by IS '更新人';
COMMENT ON COLUMN flow_skip.deleted IS '删除标志';
COMMENT ON COLUMN flow_skip.tenant_id IS '租户id';
CREATE INDEX idx_flow_skip_definition ON flow_skip (tenant_id, definition_id, deleted, source_node_code);

CREATE TABLE flow_instance
(
    id              int8         NOT NULL,
    definition_id   int8         NOT NULL,
    business_type   varchar(64)  NOT NULL,
    business_id     varchar(40)  NOT NULL,
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
    ext             varchar(500) NULL,
    deleted        bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_instance_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_instance IS '流程实例表';

COMMENT ON COLUMN flow_instance.id IS '主键id';
COMMENT ON COLUMN flow_instance.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_instance.business_type IS '业务类型';
COMMENT ON COLUMN flow_instance.business_id IS '业务id';
COMMENT ON COLUMN flow_instance.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）';
COMMENT ON COLUMN flow_instance.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_instance.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_instance.variables IS '任务变量';
COMMENT ON COLUMN flow_instance.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_instance.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_instance.def_json IS '流程定义json';
COMMENT ON COLUMN flow_instance.created_at IS '创建时间';
COMMENT ON COLUMN flow_instance.created_by IS '创建人';
COMMENT ON COLUMN flow_instance.updated_at IS '更新时间';
COMMENT ON COLUMN flow_instance.updated_by IS '更新人';
COMMENT ON COLUMN flow_instance.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_instance.deleted IS '删除标志';
COMMENT ON COLUMN flow_instance.tenant_id IS '租户id';
CREATE INDEX idx_flow_instance_business ON flow_instance (tenant_id, business_type, business_id, deleted);
CREATE INDEX idx_flow_instance_definition ON flow_instance (tenant_id, definition_id, deleted);

CREATE TABLE flow_task
(
    id            int8         NOT NULL,
    definition_id int8         NOT NULL,
    instance_id   int8         NOT NULL,
    node_code     varchar(96) NOT NULL,
    node_name     varchar(100) NULL,
    node_type     int2         NOT NULL,
    flow_status      varchar(20)  NOT NULL,
    form_id     varchar(100) NULL,
    created_at   timestamp    NULL,
    created_by     varchar(64)  NULL     DEFAULT '':: character varying,
    updated_at   timestamp    NULL,
    updated_by     varchar(64)  NULL     DEFAULT '':: character varying,
    deleted      bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id     varchar(40)  NULL,
    timeout_at    timestamp    NULL,
    timeout_action varchar(32) NULL,
    timeout_config text        NULL,
    timeout_status varchar(16) NULL,
    timeout_claimed_at timestamp NULL,
    CONSTRAINT flow_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_task IS '待办任务表';

COMMENT ON COLUMN flow_task.id IS '主键id';
COMMENT ON COLUMN flow_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_task.node_code IS '节点编码';
COMMENT ON COLUMN flow_task.node_name IS '节点名称';
COMMENT ON COLUMN flow_task.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）';
COMMENT ON COLUMN flow_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_task.form_id IS '外部业务表单标识';
COMMENT ON COLUMN flow_task.created_at IS '创建时间';
COMMENT ON COLUMN flow_task.created_by IS '创建人';
COMMENT ON COLUMN flow_task.updated_at IS '更新时间';
COMMENT ON COLUMN flow_task.updated_by IS '更新人';
COMMENT ON COLUMN flow_task.deleted IS '删除标志';
COMMENT ON COLUMN flow_task.tenant_id IS '租户id';
COMMENT ON COLUMN flow_task.timeout_at IS '冻结的节点超时时间';
COMMENT ON COLUMN flow_task.timeout_action IS '节点超时动作';
COMMENT ON COLUMN flow_task.timeout_config IS '节点超时配置快照';
COMMENT ON COLUMN flow_task.timeout_status IS '节点超时状态';
COMMENT ON COLUMN flow_task.timeout_claimed_at IS '节点超时领取时间';
CREATE INDEX idx_flow_task_timeout_due ON flow_task (timeout_status, deleted, timeout_at, timeout_claimed_at);
CREATE INDEX idx_flow_task_instance_node ON flow_task (tenant_id, instance_id, deleted, node_type, node_code);

CREATE TABLE flow_his_task
(
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
    ext              text         NULL,
    message          varchar(500) NULL,
    variables         text         NULL,
    created_at      timestamp    NULL,
    updated_at      timestamp    NULL,
    deleted         bpchar(1)    NOT NULL DEFAULT '0':: character varying,
    tenant_id        varchar(40)  NULL,
    CONSTRAINT flow_his_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_his_task IS '历史任务记录表';

COMMENT ON COLUMN flow_his_task.id IS '主键id';
COMMENT ON COLUMN flow_his_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_his_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_his_task.task_id IS '对应flow_task表的id';
COMMENT ON COLUMN flow_his_task.node_code IS '开始节点编码';
COMMENT ON COLUMN flow_his_task.node_name IS '开始节点名称';
COMMENT ON COLUMN flow_his_task.node_type IS '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）';
COMMENT ON COLUMN flow_his_task.target_node_code IS '目标节点编码';
COMMENT ON COLUMN flow_his_task.target_node_name IS '结束节点名称';
COMMENT ON COLUMN flow_his_task.approver IS '审批者';
COMMENT ON COLUMN flow_his_task.cooperation_type IS '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)';
COMMENT ON COLUMN flow_his_task.collaborator IS '协作人';
COMMENT ON COLUMN flow_his_task.skip_type IS '流转类型（PASS通过 REJECT退回 NONE无动作）';
COMMENT ON COLUMN flow_his_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_his_task.form_id IS '外部业务表单标识';
COMMENT ON COLUMN flow_his_task.message IS '审批意见';
COMMENT ON COLUMN flow_his_task.variables IS '任务变量';
COMMENT ON COLUMN flow_his_task.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_his_task.created_at IS '任务开始时间';
COMMENT ON COLUMN flow_his_task.updated_at IS '审批完成时间';
COMMENT ON COLUMN flow_his_task.deleted IS '删除标志';
COMMENT ON COLUMN flow_his_task.tenant_id IS '租户id';
CREATE INDEX idx_flow_his_task_instance_time ON flow_his_task (tenant_id, instance_id, deleted, created_at);
CREATE INDEX idx_flow_his_task_task ON flow_his_task (tenant_id, task_id, deleted, cooperation_type);

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
CREATE INDEX idx_flow_user_processed ON flow_user (tenant_id, processed_by, deleted, type, task_id);
CREATE INDEX idx_flow_user_task ON flow_user (tenant_id, task_id, deleted, type, processed_by);
COMMENT ON TABLE flow_user IS '流程用户表';

COMMENT ON COLUMN flow_user.id IS '主键id';
COMMENT ON COLUMN flow_user."type" IS '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）';
COMMENT ON COLUMN flow_user.processed_by IS '权限人';
COMMENT ON COLUMN flow_user.task_id IS '任务表id';
COMMENT ON COLUMN flow_user.created_at IS '创建时间';
COMMENT ON COLUMN flow_user.created_by IS '创建人';
COMMENT ON COLUMN flow_user.updated_at IS '更新时间';
COMMENT ON COLUMN flow_user.updated_by IS '更新人';
COMMENT ON COLUMN flow_user.deleted IS '删除标志';
COMMENT ON COLUMN flow_user.tenant_id IS '租户id';
CREATE TABLE flow_subprocess_run (
    id bigint PRIMARY KEY, parent_instance_id bigint NOT NULL, parent_task_id bigint NOT NULL,
    parent_definition_id bigint NOT NULL, parent_node_code varchar(96) NOT NULL,
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
CREATE INDEX idx_subprocess_run_parent ON flow_subprocess_run
    (tenant_id,parent_instance_id,deleted,run_status,parent_node_code,id);
CREATE INDEX idx_subprocess_run_reconcile ON flow_subprocess_run (deleted,run_status,id);

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
CREATE INDEX idx_subprocess_child_page ON flow_subprocess_child (tenant_id,run_id,deleted,id);

CREATE TABLE flow_subprocess_event (
    id bigint PRIMARY KEY, run_id bigint NOT NULL, child_id bigint, parent_instance_id bigint NOT NULL,
    child_instance_id bigint, parent_node_code varchar(96) NOT NULL, event_type varchar(50) NOT NULL,
    event_result varchar(30) NOT NULL, reason varchar(500), occurred_at timestamp NOT NULL,
    created_at timestamp, created_by varchar(64) DEFAULT '', updated_at timestamp, updated_by varchar(64) DEFAULT '',
    deleted char(1) NOT NULL DEFAULT '0', tenant_id varchar(40) NOT NULL DEFAULT '0'
);
CREATE INDEX idx_subprocess_event_timeline ON flow_subprocess_event (tenant_id,run_id,deleted,occurred_at,id);
CREATE INDEX idx_subprocess_event_parent ON flow_subprocess_event (tenant_id,parent_instance_id,deleted,id);
