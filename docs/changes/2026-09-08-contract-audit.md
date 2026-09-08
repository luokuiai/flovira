# 已确认改动的契约复核

范围：已授权字段命名统一、CooperationType 与关联 ID 方法补齐，以及移除设计器模式。未新增其他字段改名；随后按用户要求移除 SQL Server 支持，详见取消支持说明。

## 检查结果

- 对 core、ORM、插件、前端和三套 SQL 扫描，已改字段与访问方法无旧名残留。迁移说明中的旧名、Vue v-model 的通用 modelValue 属性、普通局部变量 variable 不属于遗漏。
- DefJson、NodeJson、SkipJson 的字段及 DefJson 双向实体转换已核对；DTO 原本未携带的实体属性不额外扩充。发布导入的既有行为未改动。
- Jackson 2、Jackson 3、Gson 下验证 DTO、FlowParams、ListenerVariable 序列化与反序列化；新属性名和业务扩展内部同名键保留检查通过。
- MyBatis 实际解析全部 11 份 Mapper XML；关联 ID 单值/集合查询和协作类型查询的动态 SQL 与参数绑定通过。
- 11 个 resultMap 与 MyBatis、MyBatis-Plus 两套实体核对通过；排除 5 个非持久化/联表属性后，映射列与初始化表一致。
- MySQL、PostgreSQL、Oracle 均为 10 张表，字段集合一致。既有 flow_form Mapper 没有对应初始化表，这是原有基线，不是本轮字段更名导致。
- 本次复核后，SQL Server 支持按用户要求移除。

## 验证

- `./gradlew clean build --offline`：通过，108 个任务（101 执行、7 up-to-date）。
- `bun run test`：通过，Vue 8、React 60、Lumen 8、Antd 7，共 83 项。
- 前端源码本轮未更改，沿用上一轮针对当前源码通过的 `bun run build` 结果（全部组件库、适配器、六个示例）。
- 最后仅修正 TaskService/InstanceService 中 FlowParams.variables 的旧属性说明，以及 MySQL flow_user.updated_by 的错误注释；`git diff --check` 通过。

未执行真实数据库的 DDL 或端到端数据库迁移。已有数据库和存量 JSON 仍需按字段命名与统一设计器说明迁移，不能将编译通过视作数据库已升级。
