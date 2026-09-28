# 固定软删除与 UI 模块迁移

本次预发布配置收敛移除 `flovira.logic-delete`、
`flovira.logic-delete-value`、`flovira.logic-not-delete-value` 和 `flovira.ui`。
超时能力继续保留进程级全局开关，但改由宿主代码调用 `FlowEngine.setTimeoutEnabled(boolean)` 控制，不再绑定 `flovira.timeout.*` Spring Boot 属性。

## 升级

- 两套 ORM 均固定使用 `deleted='0'` 表示有效、`deleted='1'` 表示已删除。
  普通 MyBatis 不再执行物理删除。标准三库建表脚本无需修改，已有有效数据也无需迁移。
- 使用过其他有效标记的开发库，升级前先将有效行映射为 `0`。历史删除行可以保留
  `1` 或其他非 `0` 值；查询只读取 `0`，新删除统一写入 `1`。
- 删除三个 `logic-delete*` 配置。旧配置由 Spring 忽略，不再改变删除行为。
- 删除全部 `flovira.timeout.*` 配置，并在宿主初始化代码中调用
  `FlowEngine.setTimeoutEnabled(true)`；默认仍为关闭。批量大小直接传给
  `executeDue(now, batchSize)`。
- 原来使用 `flovira.ui=true` 的应用删除该配置即可；引入
  `flovira-plugin-ui-sb-web` 后默认 REST Controller 自动注册。
- 原来使用 `flovira.ui=false` 的应用必须在升级前移除
  `flovira-plugin-ui-sb-web`。若仍需设计器 DTO 和服务契约，只依赖
  `flovira-plugin-ui-core`，避免意外暴露 REST 端点。

## 回滚

回滚旧版本前，按旧版本要求恢复 `logic-delete: true`、删除值 `1`、有效值 `0`。
需要 REST 桥接开关的应用可恢复 `flovira.ui`；不需要端点的应用继续不引入
`flovira-plugin-ui-sb-web`。旧版本使用 `flovira.timeout.*`，回滚前恢复所需属性并删除对应的 Java 初始化调用。本次变更不删除或重写业务数据，回滚无需恢复数据库。
