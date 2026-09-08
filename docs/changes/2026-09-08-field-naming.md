# 字段命名统一

本次按维护者确认的范围统一数据库、Java 属性和 JSON 字段。不修改字段类型、默认值、枚举值或流程业务含义。

| 原数据库列 | 新数据库列 | 原 Java/JSON 属性 | 新 Java/JSON 属性 |
| --- | --- | --- | --- |
| create_by | created_by | createBy | createdBy |
| create_time | created_at | createTime | createdAt |
| update_by | updated_by | updateBy | updatedBy |
| update_time | updated_at | updateTime | updatedAt |
| is_publish | publish_status | isPublish | publishStatus |
| now_node_code | source_node_code | nowNodeCode | sourceNodeCode |
| next_node_code | target_node_code | nextNodeCode | targetNodeCode |
| now_node_type | source_node_type | nowNodeType | sourceNodeType |
| next_node_type | target_node_type | nextNodeType | targetNodeType |
| cooperate_type | cooperation_type | cooperateType | cooperationType |
| variable | variables | variable | variables |
| associated | associated_id | associated | associatedId |
| del_flag | deleted | delFlag | deleted |

历史任务表的 created_at、updated_at 仍分别承载原任务开始、审批完成时间。本次按已确认的审计字段改名范围处理，不额外拆分时间字段。

processed_by、form_custom、def_json 等未批准调整的字段保留。CooperateType 枚举类统一更名为 CooperationType；listByTaskIdAndCooperateTypes 同步更名为 listByTaskIdAndCooperationTypes，覆盖 Service、DAO、两套 ORM 与 MyBatis XML。枚举常量及其数值不变，下游需同步更新 import 和方法调用。updateById 等 CRUD 操作名不变。

## 兼容性及接入变更

这是显式的破坏性字段重命名：新代码使用新 getter/setter、查询字段及 JSON 名称，未加入旧名称兼容别名。自定义 ORM 实体、数据填充处理器、监听器、外部 DTO 和前端消费者需同步调整。

FlowParams 的 variable(...)、getVariable()、getVariableStr() 调整为 variables(...)、getVariables()、getVariablesStr()；cooperateType(...) 调整为 cooperationType(...)。ListenerVariable 的变量属性及访问方法同步使用复数，类名保留。

Vue/React 设计器和演示数据均使用 sourceNodeCode、targetNodeCode。已保存的流程定义 JSON、实例 def_json 快照及外部导出文件也需要迁移相应结构键；不得对业务 variables/ext 中用户自定义的同名键进行无差别文本替换。

## 数据库处理

SQL Server 支持已移除，包括初始化脚本和专用锁定 SQL；当前维护 MySQL、PostgreSQL、Oracle。

MySQL/PostgreSQL V1 和 Oracle 初始化脚本已同步新列名，索引引用和 MyBatis XML 同步；没有创建历史升级链，也没有执行数据库 DDL。

已有数据库不能直接使用初始化脚本覆盖。应先备份并停写，在部署窗口按表执行列重命名，同时更新依赖视图、触发器、存储过程及 JSON 快照，完成后与匹配版本的应用一起上线。MySQL 的列重命名语法依版本选择 RENAME COLUMN 或 CHANGE COLUMN；PostgreSQL/Oracle 使用对应的 RENAME COLUMN。实际迁移语句需根据目标库版本与现有数据生成、审核后执行。

回滚须同时恢复旧应用、旧列名及旧 JSON 键；存在上线后写入时不能只回滚应用。应使用备份或经验证的逆向迁移方案。

## 验证结果

- `./gradlew compileJava compileTestJava --offline`：全部模块编译通过。
- `./gradlew test --offline`：最终逻辑删除字段调整后，78 项后端测试通过，无失败或跳过。
- `flovira-designer` 下 `bun run test`：Vue、React、Lumen/Antd 适配器共 82 项测试通过。
- `flovira-designer` 下 `bun run build`：组件库、适配器及六个示例全部构建通过；部分示例仍有包体积提示。
- MySQL、PostgreSQL、Oracle 三套 SQL 的旧列名残留和替换引用计数检查通过；11 个 MyBatis resultMap 对应的实体属性检查通过。
- 使用各模块自身依赖分别检查 Jackson 2、Jackson 3、Gson，新审计字段、variables、deleted、publishStatus、连线编码与类型、associatedId、cooperationType 序列化/反序列化通过。
- `git diff --check` 通过。

未执行生产数据库的实际列迁移；以上测试不代表现有数据库已经升级。

## 节点命名按场景区分

flow_skip 的 source/target 仅表示一条定义跳转关系的来源和目标，包含正常流转和退回，不表示整个流程的开始或结束。编码与类型均成对使用 sourceNodeCode/sourceNodeType、targetNodeCode/targetNodeType。

任务和实例的 nodeCode/nodeType 保留。拓扑前置/后置查询的基准参数使用 nodeCode；运行时 getNextNode/getNextNodeList 的当前节点编码使用 currentNodeCode。动态退回的目标可以来自历史记录或运行参数，不要求对应已有定义连线。

关联 ID 集合参数 associateds 统一为 associatedIds；getByAssociateds 统一为 getByAssociatedIds，下游调用需同步更新。
单个关联 ID 的 Service 查询统一为 listByAssociatedIdAndTypes；DAO、Mapper 与 XML 的集合查询统一为 listByAssociatedIdsAndTypes，集合参数统一为 associatedIds。

实例服务统一使用完整名称：InsService → InstanceService、InsServiceImpl → InstanceServiceImpl、FlowEngine.insService() → FlowEngine.instanceService()；Spring 装配和内部调用同步，下游同步更新 import 和门面调用。
