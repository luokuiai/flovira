-- Flovira 1.0.0 MySQL initialization schema.
-- This is a fresh-install baseline and is not an upgrade from Warm-Flow or earlier Flovira releases.

CREATE TABLE `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `model_value`            varchar(40)     NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)                  DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程定义表';

CREATE TABLE `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100)    NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200) DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             text          COMMENT '节点扩展属性',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100)    NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `next_node_code` varchar(100)    NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='节点跳转关联表';

CREATE TABLE `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_type`   varchar(64) NOT NULL COMMENT '业务类型',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_instance_business` (`tenant_id`, `business_type`, `business_id`)
) ENGINE = InnoDB COMMENT ='流程实例表';

CREATE TABLE `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    `timeout_at`    datetime     DEFAULT NULL COMMENT '冻结的节点超时时间',
    `timeout_action` varchar(32) DEFAULT NULL COMMENT '节点超时动作',
    `timeout_config` text        DEFAULT NULL COMMENT '节点超时配置快照',
    `timeout_status` varchar(16) DEFAULT NULL COMMENT '节点超时状态',
    `timeout_claimed_at` datetime DEFAULT NULL COMMENT '节点超时领取时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_task_timeout_due` (`timeout_status`, `timeout_at`, `timeout_claimed_at`),
    KEY `idx_flow_task_instance_node` (`tenant_id`, `instance_id`, `node_type`)
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20)                   NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)                   NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)                   NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)                   NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)                 DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
    `target_node_code` varchar(200)                 DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   tinyint(1)                   NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(500)                  DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)                  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)                  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`      char(1)                      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)                 DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variable`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              TEXT                         DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)                      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_his_task_instance_time` (`tenant_id`, `instance_id`, `create_time`)
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程用户表';

CREATE TABLE `flow_subprocess_run` (
    `id` bigint NOT NULL,
    `parent_instance_id` bigint NOT NULL,
    `parent_task_id` bigint NOT NULL,
    `parent_definition_id` bigint NOT NULL,
    `parent_node_code` varchar(100) NOT NULL,
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
    `create_time` datetime DEFAULT NULL,
    `create_by` varchar(64) DEFAULT '',
    `update_time` datetime DEFAULT NULL,
    `update_by` varchar(64) DEFAULT '',
    `del_flag` char(1) NOT NULL DEFAULT '0',
    `tenant_id` varchar(40) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subprocess_run_parent_task` (`tenant_id`, `parent_task_id`),
    KEY `idx_subprocess_run_parent` (`tenant_id`, `parent_instance_id`, `parent_node_code`),
    KEY `idx_subprocess_run_reconcile` (`run_status`, `update_time`)
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
    `create_time` datetime DEFAULT NULL,
    `create_by` varchar(64) DEFAULT '',
    `update_time` datetime DEFAULT NULL,
    `update_by` varchar(64) DEFAULT '',
    `del_flag` char(1) NOT NULL DEFAULT '0',
    `tenant_id` varchar(40) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subprocess_child_item` (`tenant_id`, `run_id`, `item_key`),
    UNIQUE KEY `uk_subprocess_child_instance` (`tenant_id`, `child_instance_id`),
    KEY `idx_subprocess_child_page` (`tenant_id`, `run_id`, `id`)
) ENGINE=InnoDB COMMENT='子流程实例关系表';

CREATE TABLE `flow_subprocess_event` (
    `id` bigint NOT NULL,
    `run_id` bigint NOT NULL,
    `child_id` bigint DEFAULT NULL,
    `parent_instance_id` bigint NOT NULL,
    `child_instance_id` bigint DEFAULT NULL,
    `parent_node_code` varchar(100) NOT NULL,
    `event_type` varchar(50) NOT NULL,
    `event_result` varchar(30) NOT NULL,
    `reason` varchar(500) DEFAULT NULL,
    `occurred_at` datetime NOT NULL,
    `create_time` datetime DEFAULT NULL,
    `create_by` varchar(64) DEFAULT '',
    `update_time` datetime DEFAULT NULL,
    `update_by` varchar(64) DEFAULT '',
    `del_flag` char(1) NOT NULL DEFAULT '0',
    `tenant_id` varchar(40) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_subprocess_event_timeline` (`tenant_id`, `run_id`, `occurred_at`, `id`),
    KEY `idx_subprocess_event_parent` (`tenant_id`, `parent_instance_id`)
) ENGINE=InnoDB COMMENT='子流程编排事件表';
