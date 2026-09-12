-- Flovira 1.0.0 MySQL initialization schema.
-- Fresh-install baseline; migrate existing databases separately.

CREATE TABLE `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `publish_status`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_id`       varchar(100)             DEFAULT NULL COMMENT '外部业务表单标识',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `created_at`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `created_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `updated_at`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `updated_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `deleted`        char(1)         NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_definition_lookup` (`tenant_id`, `flow_code`, `deleted`, `publish_status`)
) ENGINE = InnoDB COMMENT ='流程定义表';

CREATE TABLE `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(96)     NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200) DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_id`       varchar(100)  DEFAULT NULL COMMENT '外部业务表单标识',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `created_at`     datetime      DEFAULT NULL COMMENT '创建时间',
    `created_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `updated_at`     datetime      DEFAULT NULL COMMENT '更新时间',
    `updated_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             text          COMMENT '节点扩展属性',
    `deleted`        char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_node_definition` (`tenant_id`, `definition_id`, `deleted`, `node_code`)
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `source_node_code`  varchar(96)     NOT NULL COMMENT '跳转来源节点编码',
    `source_node_type`  tinyint(1)   DEFAULT NULL COMMENT '跳转来源节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `target_node_code` varchar(96)     NOT NULL COMMENT '跳转目标节点编码',
    `target_node_type` tinyint(1)   DEFAULT NULL COMMENT '跳转目标节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `created_at`    datetime     DEFAULT NULL COMMENT '创建时间',
    `created_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `updated_at`    datetime     DEFAULT NULL COMMENT '更新时间',
    `updated_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `deleted`       char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_skip_definition` (`tenant_id`, `definition_id`, `deleted`, `source_node_code`)
) ENGINE = InnoDB COMMENT ='节点跳转关联表';

CREATE TABLE `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_type`   varchar(64) NOT NULL COMMENT '业务类型',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `node_code`       varchar(96) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variables`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `created_at`     datetime             DEFAULT NULL COMMENT '创建时间',
    `created_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `updated_at`     datetime             DEFAULT NULL COMMENT '更新时间',
    `updated_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `deleted`        char(1)     NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_instance_business` (`tenant_id`, `business_type`, `business_id`, `deleted`),
    KEY `idx_flow_instance_definition` (`tenant_id`, `definition_id`, `deleted`)
) ENGINE = InnoDB COMMENT ='流程实例表';

CREATE TABLE `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(96) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_id`     varchar(100) DEFAULT NULL COMMENT '外部业务表单标识',
    `created_at`   datetime     DEFAULT NULL COMMENT '创建时间',
    `created_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `updated_at`   datetime     DEFAULT NULL COMMENT '更新时间',
    `updated_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `deleted`      char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    `timeout_at`    datetime     DEFAULT NULL COMMENT '冻结的节点超时时间',
    `timeout_action` varchar(32) DEFAULT NULL COMMENT '节点超时动作',
    `timeout_config` text        DEFAULT NULL COMMENT '节点超时配置快照',
    `timeout_status` varchar(16) DEFAULT NULL COMMENT '节点超时状态',
    `timeout_claimed_at` datetime DEFAULT NULL COMMENT '节点超时领取时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_task_timeout_due` (`timeout_status`, `deleted`, `timeout_at`, `timeout_claimed_at`),
    KEY `idx_flow_task_instance_node` (`tenant_id`, `instance_id`, `deleted`, `node_type`, `node_code`)
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20)                   NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)                   NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)                   NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)                   NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(96)                  DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `target_node_code` varchar(96)                  DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批人',
    `cooperation_type`   tinyint(1)                   NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(500)                  DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)                  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)                  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_id`        varchar(100)                 DEFAULT NULL COMMENT '外部业务表单标识',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variables`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              TEXT                         DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `created_at`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `updated_at`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `deleted`         char(1)             NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_his_task_instance_time` (`tenant_id`, `instance_id`, `deleted`, `created_at`),
    KEY `idx_flow_his_task_task` (`tenant_id`, `task_id`, `deleted`, `cooperation_type`)
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `task_id`   bigint          NOT NULL COMMENT '任务表id',
    `created_at`  datetime    DEFAULT NULL COMMENT '创建时间',
    `created_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `updated_at`  datetime    DEFAULT NULL COMMENT '更新时间',
    `updated_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `deleted`     char(1)     NOT NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_user_processed` (`tenant_id`, `processed_by`, `deleted`, `type`, `task_id`),
    KEY `idx_flow_user_task` (`tenant_id`, `task_id`, `deleted`, `type`, `processed_by`)
) ENGINE = InnoDB COMMENT ='流程用户表';

CREATE TABLE `flow_subprocess_run` (
    `id` bigint NOT NULL,
    `parent_instance_id` bigint NOT NULL,
    `parent_task_id` bigint NOT NULL,
    `parent_definition_id` bigint NOT NULL,
    `parent_node_code` varchar(96) NOT NULL,
    `child_flow_code` varchar(100) NOT NULL,
    `child_definition_id` bigint NOT NULL,
    `child_definition_version` varchar(20) NOT NULL,
    `completion_policy` varchar(20) NOT NULL DEFAULT 'ALL',
    `collection_fingerprint` char(64) NOT NULL,
    `expected_count` int NOT NULL DEFAULT 0,
    `pending_count` int NOT NULL DEFAULT 0,
    `running_count` int NOT NULL DEFAULT 0,
    `completed_count` int NOT NULL DEFAULT 0,
    `failed_count` int NOT NULL DEFAULT 0,
    `cancelled_count` int NOT NULL DEFAULT 0,
    `run_status` varchar(30) NOT NULL,
    `failure_code` varchar(100) DEFAULT NULL,
    `lock_version` int NOT NULL DEFAULT 0,
    `initialized_at` datetime DEFAULT NULL,
    `completed_at` datetime DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(64) DEFAULT '',
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(64) DEFAULT '',
    `deleted` char(1) NOT NULL DEFAULT '0',
    `tenant_id` varchar(40) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subprocess_run_parent_task` (`tenant_id`, `parent_task_id`),
    KEY `idx_subprocess_run_parent` (`tenant_id`, `parent_instance_id`, `deleted`, `run_status`, `parent_node_code`, `id`),
    KEY `idx_subprocess_run_reconcile` (`deleted`, `run_status`, `id`)
) ENGINE=InnoDB COMMENT='子流程运行聚合表';

CREATE TABLE `flow_subprocess_child` (
    `id` bigint NOT NULL,
    `run_id` bigint NOT NULL,
    `item_key` varchar(200) NOT NULL,
    `item_label` varchar(200) DEFAULT NULL,
    `child_business_key` varchar(100) NOT NULL,
    `child_flow_code` varchar(100) NOT NULL,
    `child_definition_id` bigint NOT NULL,
    `child_definition_version` varchar(20) NOT NULL,
    `child_instance_id` bigint DEFAULT NULL,
    `child_status` varchar(20) NOT NULL,
    `outcome` varchar(20) DEFAULT NULL,
    `started_at` datetime DEFAULT NULL,
    `completed_at` datetime DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(64) DEFAULT '',
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(64) DEFAULT '',
    `deleted` char(1) NOT NULL DEFAULT '0',
    `tenant_id` varchar(40) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subprocess_child_item` (`tenant_id`, `run_id`, `item_key`),
    UNIQUE KEY `uk_subprocess_child_instance` (`tenant_id`, `child_instance_id`),
    KEY `idx_subprocess_child_page` (`tenant_id`, `run_id`, `deleted`, `id`)
) ENGINE=InnoDB COMMENT='子流程实例关系表';

CREATE TABLE `flow_subprocess_event` (
    `id` bigint NOT NULL,
    `run_id` bigint NOT NULL,
    `child_id` bigint DEFAULT NULL,
    `parent_instance_id` bigint NOT NULL,
    `child_instance_id` bigint DEFAULT NULL,
    `parent_node_code` varchar(96) NOT NULL,
    `event_type` varchar(50) NOT NULL,
    `event_result` varchar(30) NOT NULL,
    `reason` varchar(500) DEFAULT NULL,
    `occurred_at` datetime NOT NULL,
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(64) DEFAULT '',
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(64) DEFAULT '',
    `deleted` char(1) NOT NULL DEFAULT '0',
    `tenant_id` varchar(40) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_subprocess_event_timeline` (`tenant_id`, `run_id`, `deleted`, `occurred_at`, `id`),
    KEY `idx_subprocess_event_parent` (`tenant_id`, `parent_instance_id`, `deleted`, `id`)
) ENGINE=InnoDB COMMENT='子流程编排事件表';
