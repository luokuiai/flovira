# Flovira

Flovira 是一个轻量、可扩展的 Java 工作流引擎，提供流程定义、任务流转、
条件表达式、监听器、多租户、多 ORM 适配以及可嵌入的 Vue 3 流程设计器。

```text
    ______ _            _
   |  ____| |          (_)
   | |__  | | _____   ___ _ __ __ _
   |  __| | |/ _ \ \ / / | '__/ _` |
   | |    | | (_) \ V /| | | | (_| |
   |_|    |_|\___/ \_/ |_|_|  \__,_|
```

## 技术基线

- Java 8 源码兼容，支持在 Java 8、17 和 21 环境集成
- Spring Boot 2、3、4
- MyBatis、MyBatis-Plus、Easy-Query
- MySQL、Oracle、PostgreSQL、SQL Server
- Apache License 2.0

## 模块

| 模块 | 说明 |
| --- | --- |
| `flovira-core` | 框架和 ORM 无关的流程引擎核心 |
| `flovira-orm` | MyBatis、MyBatis-Plus、Easy-Query 适配 |
| `flovira-plugin` | 表达式、JSON 和流程设计器插件 |
| `flovira-designer/vue` | Vue 3 流程设计器组件库（`@luokuiai/flovira-vue-designer`） |
| `flovira-designer/react` | React 流程设计器组件库（`@luokuiai/flovira-react-designer`） |
| `flovira-designer/examples` | Vue / React 集成示例 |

## 构建

Jackson 3 和 Spring Boot 4 模块要求使用 JDK 17 或更高版本执行全量构建。

```bash
./gradlew clean build
bun install
bun run build
```

## Maven 坐标

当前制品使用 `com.luokuiai` groupId。仓库尚未发布到公共 Maven 仓库，需先安装到本地仓库。

Spring Boot 3 + MyBatis：

```xml
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-mybatis-sb3-starter</artifactId>
<version>1.0.0</version>
</dependency>
```

Spring Boot 3 + MyBatis-Plus：

```xml
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-mybatis-plus-sb3-starter</artifactId>
<version>1.0.0</version>
</dependency>
```

配置统一使用 `flovira` 前缀：

```yaml
flovira:
  enabled: true
  banner: true
  # 单个子流程节点每次最多启动的子实例数，默认 128，必须配置为正整数
  subprocess-max-children: 128
  timeout:
    enabled: false
    # 超时任务扫描间隔，单位秒
    scan-interval-seconds: 60
    batch-size: 100
    claim-timeout-millis: 300000
    # 多个应用共用 Redis 时应配置不同的锁键
    scheduler-lock-key: flovira:timeout:scheduler
```

启用超时任务后，Spring 环境存在 `StringRedisTemplate` Bean 时会自动优先使用 Redis
调度锁。Redis 未配置或暂时不可用时，引擎继续使用数据库原子任务领取，保证同一超时任务不会被重复执行。

Java 根包名为 `com.luokuiai.flovira`。

## 数据库

全量建表脚本位于 `sql/<database>/`。数据库表继续使用通用的 `flow_*` 前缀，
避免品牌变化影响既有流程数据结构。

## 上游项目

Flovira 基于 [Dromara WarmFlow](https://github.com/dromara/warm-flow) 开发，保留原项目
Git 历史、Apache 2.0 许可证、源文件版权声明和作者归属。WarmFlow 上游仓库在本地配置为
`upstream` remote，便于持续同步上游修复。

本 Fork 的品牌、Maven 坐标、Java 包名、配置前缀和设计器制品名已调整为 Flovira。
