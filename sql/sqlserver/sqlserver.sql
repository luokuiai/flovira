CREATE TABLE flow_definition (
    id bigint NOT NULL,
    flow_code nvarchar(40) NOT NULL,
    flow_name nvarchar(100) NOT NULL,
    model_value nvarchar(40) DEFAULT('CLASSICS') NOT NULL,
    category nvarchar(100) NULL,
    version nvarchar(20) NOT NULL,
    is_publish tinyint DEFAULT('0') NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    activity_status tinyint DEFAULT('1') NULL,
    listener_type nvarchar(100) NULL,
    listener_path nvarchar(400) NULL,
    ext nvarchar(500) NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(64) NULL,
    update_time datetime2(7)  NULL,
    update_by nvarchar(64) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_def__3213E83FEE39AE33 PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'flow_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'flow_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'model_value'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程类别',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'category'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程版本',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
'MS_Description', N'是否发布（0未发布 1已发布 9失效）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'is_publish'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程激活状态（0挂起 1激活）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'activity_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'listener_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'listener_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务详情 存业务表对象json字符串',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新人',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义表',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition'
GO

CREATE TABLE flow_node (
    id bigint NOT NULL,
    node_type tinyint NOT NULL,
    definition_id bigint NOT NULL,
    node_code nvarchar(100) NOT NULL,
    node_name nvarchar(100) NULL,
    permission_flag nvarchar(200) NULL,
    node_ratio nvarchar(200)  NULL,
    coordinate nvarchar(100) NULL,
    any_node_skip nvarchar(100) NULL,
    handler_type nvarchar(100) NULL,
    handler_path nvarchar(400) NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    version nvarchar(20) NOT NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(64) NULL,
    update_time datetime2(7)  NULL,
    update_by nvarchar(64) NULL,
    ext nvarchar(max) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_nod__3213E83F372470DE PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义id',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'权限标识（权限类型:权限标识，可以多个，用@@隔开)',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'permission_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程签署比例值',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_ratio'
GO

EXEC sp_addextendedproperty
'MS_Description', N'坐标',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'coordinate'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任意结点跳转',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'any_node_skip'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'listener_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'listener_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'处理器类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'handler_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'处理器路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'handler_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'版本',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新人',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点扩展属性',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'ext'
GO


EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点表',
'SCHEMA', N'dbo',
'TABLE', N'flow_node'
GO

CREATE TABLE flow_skip (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    now_node_code nvarchar(100) NOT NULL,
    now_node_type tinyint  NULL,
    next_node_code nvarchar(100) NOT NULL,
    next_node_type tinyint  NULL,
    skip_name nvarchar(100) NULL,
    skip_type nvarchar(40) NULL,
    skip_condition nvarchar(200) NULL,
    coordinate nvarchar(100) NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(64) NULL,
    update_time datetime2(7)  NULL,
    update_by nvarchar(64) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_ski__3213E83F073FEE6E PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义id',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'当前流程节点的编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'now_node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'now_node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'下一个流程节点的编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'next_node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'next_node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'跳转名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'skip_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'跳转类型（PASS审批通过 REJECT退回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'skip_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'跳转条件',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'skip_condition'
GO

EXEC sp_addextendedproperty
'MS_Description', N'坐标',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'coordinate'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新人',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点跳转关联表',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip'
GO

CREATE TABLE flow_instance (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    business_type nvarchar(64) NOT NULL,
    business_id nvarchar(40) NOT NULL,
    node_type tinyint NOT NULL,
    node_code nvarchar(40) NOT NULL,
    node_name nvarchar(100) NULL,
    variable nvarchar(max) NULL,
    flow_status nvarchar(20) NOT NULL,
    activity_status tinyint DEFAULT('1') NULL,
    def_json nvarchar(max) NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(64) NULL,
    update_time datetime2(7)  NULL,
    update_by nvarchar(64) NULL,
    ext nvarchar(500) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_ins__3213E83F5190FEE1 PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
TEXTIMAGE_ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX idx_flow_instance_business
ON flow_instance (tenant_id, business_type, business_id)
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_definition表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'business_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'business_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务变量',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'variable'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'flow_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程激活状态（0挂起 1激活）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'activity_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义json',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'def_json'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新人',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'扩展字段，预留给业务系统使用',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程实例表',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance'
GO

CREATE TABLE flow_task (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    node_code nvarchar(100) NOT NULL,
    node_name nvarchar(100) NULL,
    node_type tinyint NOT NULL,
    flow_status nvarchar(20) NOT NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(64) NULL,
    update_time datetime2(7)  NULL,
    update_by nvarchar(64) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    timeout_at datetime2(7) NULL,
    timeout_action nvarchar(32) NULL,
    timeout_config nvarchar(max) NULL,
    timeout_status nvarchar(16) NULL,
    timeout_claimed_at datetime2(7) NULL,
    CONSTRAINT PK__flow_tas__3213E83F5AE1F1BA PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX idx_flow_task_timeout_due
ON flow_task (timeout_status, timeout_at, timeout_claimed_at)
GO

CREATE NONCLUSTERED INDEX idx_flow_task_instance_node
ON flow_task (tenant_id, instance_id, node_type)
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_definition表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_instance表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'instance_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'flow_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新人',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'冻结的节点超时时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'timeout_at'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点超时动作',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'timeout_action'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点超时配置快照',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'timeout_config'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点超时状态',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'timeout_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点超时领取时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'timeout_claimed_at'
GO

EXEC sp_addextendedproperty
'MS_Description', N'待办任务表',
'SCHEMA', N'dbo',
'TABLE', N'flow_task'
GO

CREATE TABLE flow_his_task (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    task_id bigint NOT NULL,
    node_code nvarchar(200) NULL,
    node_name nvarchar(200) NULL,
    node_type tinyint  NULL,
    target_node_code nvarchar(100) NULL,
    target_node_name nvarchar(100) NULL,
    approver nvarchar(40) NULL,
    cooperate_type tinyint DEFAULT('0') NULL,
    collaborator nvarchar(500) NULL,
    skip_type nvarchar(10) NOT NULL,
    flow_status nvarchar(20) NOT NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    message nvarchar(500) NULL,
    variable nvarchar(max) NULL,
    ext nvarchar(max) NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_his__3213E83F67951564 PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX idx_flow_his_task_instance_time
ON flow_his_task (tenant_id, instance_id, create_time)
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_definition表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_instance表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'instance_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_task表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'task_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关 5包容网关 6子流程 7等待）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'目标节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'target_node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'结束节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'target_node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批者',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'approver'
GO

EXEC sp_addextendedproperty
'MS_Description', N'协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'cooperate_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'协作人',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'collaborator'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流转类型（PASS通过 REJECT退回 NONE无动作）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'skip_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'flow_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批意见',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'message'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务变量',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'variable'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务详情 存业务表对象json字符串',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务开始时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批完成时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'历史任务记录表',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task'
GO

CREATE TABLE flow_user (
    id bigint NOT NULL,
    type nchar(1) NOT NULL,
    processed_by nvarchar(80) NULL,
    associated bigint NOT NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(64) NULL,
    update_time datetime2(7)  NULL,
    update_by nvarchar(64) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_use__3213E83FFA38CA8B PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX user_processed_type ON flow_user (processed_by ASC, type ASC)
GO
CREATE NONCLUSTERED INDEX user_associated_idx ON flow_user (associated ASC)
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'权限人',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'processed_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务表id',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'associated'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新人',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程用户表',
'SCHEMA', N'dbo',
'TABLE', N'flow_user'
GO
CREATE TABLE flow_subprocess_run (
    id bigint NOT NULL PRIMARY KEY, parent_instance_id bigint NOT NULL, parent_task_id bigint NOT NULL,
    parent_definition_id bigint NOT NULL, parent_node_code nvarchar(100) NOT NULL,
    child_flow_code nvarchar(100) NOT NULL, child_definition_id bigint NOT NULL,
    child_definition_version nvarchar(20) NOT NULL, completion_policy nvarchar(20) NOT NULL DEFAULT 'ALL',
    collection_fingerprint char(64) NOT NULL, expected_count int NOT NULL DEFAULT 0,
    pending_count int NOT NULL DEFAULT 0, running_count int NOT NULL DEFAULT 0,
    completed_count int NOT NULL DEFAULT 0, failed_count int NOT NULL DEFAULT 0,
    cancelled_count int NOT NULL DEFAULT 0, run_status nvarchar(30) NOT NULL,
    failure_code nvarchar(100) NULL, lock_version int NOT NULL DEFAULT 0,
    initialized_at datetime2 NULL, completed_at datetime2 NULL, create_time datetime2 NULL,
    create_by nvarchar(64) DEFAULT '', update_time datetime2 NULL, update_by nvarchar(64) DEFAULT '',
    del_flag char(1) NOT NULL DEFAULT '0', tenant_id nvarchar(40) NOT NULL DEFAULT '0',
    CONSTRAINT uk_subprocess_run_parent_task UNIQUE (tenant_id,parent_task_id)
);
CREATE INDEX idx_subprocess_run_parent ON flow_subprocess_run (tenant_id,parent_instance_id,parent_node_code);
CREATE INDEX idx_subprocess_run_reconcile ON flow_subprocess_run (run_status,update_time);

CREATE TABLE flow_subprocess_child (
    id bigint NOT NULL PRIMARY KEY, run_id bigint NOT NULL, item_key nvarchar(200) NOT NULL,
    item_label nvarchar(200) NULL, child_business_key nvarchar(100) NOT NULL, child_flow_code nvarchar(100) NOT NULL,
    child_definition_id bigint NOT NULL, child_definition_version nvarchar(20) NOT NULL,
    child_instance_id bigint NULL, child_status nvarchar(20) NOT NULL, outcome nvarchar(20) NULL,
    started_at datetime2 NULL, completed_at datetime2 NULL, create_time datetime2 NULL,
    create_by nvarchar(64) DEFAULT '', update_time datetime2 NULL, update_by nvarchar(64) DEFAULT '',
    del_flag char(1) NOT NULL DEFAULT '0', tenant_id nvarchar(40) NOT NULL DEFAULT '0',
    CONSTRAINT uk_subprocess_child_item UNIQUE (tenant_id,run_id,item_key)
);
CREATE INDEX idx_subprocess_child_page ON flow_subprocess_child (tenant_id,run_id,id);
CREATE UNIQUE INDEX uk_subprocess_child_instance ON flow_subprocess_child (tenant_id,child_instance_id)
WHERE child_instance_id IS NOT NULL;

CREATE TABLE flow_subprocess_event (
    id bigint NOT NULL PRIMARY KEY, run_id bigint NOT NULL, child_id bigint NULL,
    parent_instance_id bigint NOT NULL, child_instance_id bigint NULL, parent_node_code nvarchar(100) NOT NULL,
    event_type nvarchar(50) NOT NULL, event_result nvarchar(30) NOT NULL, reason nvarchar(500) NULL,
    occurred_at datetime2 NOT NULL, create_time datetime2 NULL, create_by nvarchar(64) DEFAULT '',
    update_time datetime2 NULL, update_by nvarchar(64) DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0',
    tenant_id nvarchar(40) NOT NULL DEFAULT '0'
);
CREATE INDEX idx_subprocess_event_timeline ON flow_subprocess_event (tenant_id,run_id,occurred_at,id);
CREATE INDEX idx_subprocess_event_parent ON flow_subprocess_event (tenant_id,parent_instance_id);
