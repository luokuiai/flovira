## Why

Flovira 目前只能执行彼此独立的普通流程实例，无法在一个流程节点中根据运行时集合创建多个子流程、等待其汇聚，并统一处理撤回、驳回、终止、进度和日志，因此需要增加原生动态子流程能力。

## What Changes

- 新增原生 `SUB_PROCESS` 节点类型，节点只引用一个固定、已发布的子流程定义。
- 定义标准动态子项输入 `subprocessItems`；每项包含稳定键、显示名称和子流程变量。单次并行子实例数由全局配置 `flovira.subprocess-max-children` 控制，默认值为 128。
- 新增父子流程运行时聚合、子实例关系和编排事件模型，提供幂等初始化、定义版本固定、`ALL` 汇聚、失败恢复和父节点单次继续执行。
- 父流程撤回、驳回离开子流程节点或终止时，按作用域同步终止未完成子实例，同时保留历史运行记录。
- 在 core 门面中提供框架无关的子流程初始化、完成通知、取消、恢复和查询 API，并通过现有 ORM 抽象支持 MyBatis 和 MyBatis-Plus。
- 为 MySQL、PostgreSQL、Oracle 和 SQL Server 增加一致的子流程运行时表及索引；MySQL 和 PostgreSQL 直接提供 1.0.0 的 V1 初始化脚本。
- 在经典和仿钉钉 Vue 设计器中增加固定模板子流程节点，隐藏集合路径、字段映射、完成策略等引擎固定配置。
- 父流程进度节点增加可选汇总；子实例、子节点进度和父子合并日志按需加载，普通流程现有响应保持兼容。
- 第一阶段不支持递归子流程、按子项选择流程、`ANY`/比例汇聚、跨服务事务或运行时任意拓扑生成。

## Capabilities

### New Capabilities

- `dynamic-subprocess-runtime`: 动态子实例创建、版本固定、ALL 汇聚、取消、恢复、租户隔离和 ORM/数据库持久化契约。
- `subprocess-designer`: 固定子流程模板节点的设计、序列化、发布校验、循环依赖校验及双设计器展示契约。
- `hierarchical-process-observability`: 父节点汇总、子实例分页、子节点进度和父子合并流程日志契约。

### Modified Capabilities

<!-- Flovira 当前没有 OpenSpec 业务规格，本次仅新增能力。 -->

## Impact

- `flovira-core`：公共实体、枚举、服务、`FlowEngine` 门面和流程状态机增加兼容性扩展。
- `flovira-orm`：三个 ORM core 模块增加运行时实体、Mapper/DAO 和初始化接线；各 starter 继续复用对应 ORM core。
- `sql/`：四种数据库建表脚本增加三张 `flow_subprocess_*` 表；MySQL、PostgreSQL 以 `flovira-v1.sql` 作为全新安装入口，不继承旧升级链。
- `flovira-vue-designer`、`flovira-ui` 和 UI 插件静态资源：增加子流程节点与只读运行态展示。
- 公共 API 采用加法扩展；已有七张核心表、普通节点定义和运行中普通流程不改写。
- 子流程能力会把核心表数量从七张增加到十张，并要求使用新版本数据库脚本后才能启用 `SUB_PROCESS` 节点。
