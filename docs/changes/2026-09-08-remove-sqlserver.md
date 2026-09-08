# 取消 SQL Server 支持

按用户最终要求，SQL Server 不再属于支持数据库。此前“保留原脚本、仅排除字段迁移”的处理已被此决定替代。

- 删除 sql/sqlserver/sqlserver.sql 及空目录。
- 删除 MyBatis XML 中的 SQL Server updlock/rowlock 分支，保留三库使用的 for update。
- 删除 MyBatis-Plus 的 SQL Server 专用 Mapper 方法及 DAO 分支。
- 数据库契约测试仅检查 MySQL、PostgreSQL、Oracle。
- README、根与模块 AGENTS、CLAUDE、Cursor 规则及本轮说明同步支持范围。
- 已归档 OpenSpec 中的历史记录保留，不作为当前支持声明。

验证：./gradlew clean build --offline 通过（101 个任务全部执行）；源码、测试、当前支持声明中无 SQL Server 专用引用残留；git diff --check 通过。

未执行数据库 DDL，也未修改或删除任何实际数据库数据。
