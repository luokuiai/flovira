# Flovira

Flovira is a lightweight and extensible Java workflow engine. It provides
process definitions, task transitions, conditional expressions, listeners,
multi-tenancy, multiple ORM integrations, and embeddable Vue and React process
designers.

Forked from [Dromara WarmFlow](https://github.com/dromara/warm-flow).

```text
    ______ _            _
   |  ____| |          (_)
   | |__  | | _____   ___ _ __ __ _
   |  __| | |/ _ \ \ / / | '__/ _` |
   | |    | | (_) \ V /| | | | (_| |
   |_|    |_|\___/ \_/ |_|_|  \__,_|
```

## Technology Baseline

- Java 8 source compatibility, with integration support for Java 8, 17, and 21
- Spring Boot 2.7.18, 3.5.16, and 4.0.2
- MyBatis and MyBatis-Plus
- MySQL, Oracle, and PostgreSQL
- Apache License 2.0

## Modules

| Module | Description |
| --- | --- |
| `flovira-core` | Framework-independent and ORM-independent workflow engine core |
| `flovira-orm` | MyBatis and MyBatis-Plus integrations |
| `flovira-plugin` | Expression, JSON, and process designer plugins |
| `flovira-designer/vue` | Vue 3 designer package (`@luokuiai/flovira-vue-designer`) |
| `flovira-designer/react` | React designer package (`@luokuiai/flovira-react-designer`) |
| `flovira-designer/examples` | Vue and React integration examples |
| `flovira-example` | Composable PostgreSQL/MySQL backends and React/Vue full-stack examples |

## Build

A full build requires JDK 17 or later because the Jackson 3 and Spring Boot 4
modules use a Java 17 baseline.

```bash
./gradlew clean build
cd flovira-designer
bun install
bun run build
```

For a runnable end-to-end matrix that combines either PostgreSQL or MySQL
with React + Lumen, React + Ant Design, or Vue + Ant Design Vue, see
[`flovira-example`](flovira-example/README.md).

## Maven Coordinates

All artifacts use the `com.luokuiai` group ID. Until a release is available
from Maven Central, publish the artifacts to your local Maven repository.

Spring Boot 3 with MyBatis:

```xml
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-mybatis-sb3-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot 3 with MyBatis-Plus:

```xml
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-mybatis-plus-sb3-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Select exactly one JSON provider. Framework and ORM starters do not select one
automatically:

```xml
<!-- Jackson 2: Spring Boot 2 and 3 -->
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-plugin-json-jackson</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Jackson 3: Spring Boot 4 -->
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-plugin-json-jackson3</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Gson: alternative for any supported framework -->
<dependency>
    <groupId>com.luokuiai</groupId>
    <artifactId>flovira-plugin-json-gson</artifactId>
    <version>1.0.0</version>
</dependency>
```

All configuration properties use the `flovira` prefix:

```yaml
flovira:
  enabled: true
  banner: true
  # Maximum child instances started by one subprocess node. Must be positive.
  subprocess-max-children: 128
  timeout:
    enabled: false
    # Execution requires a host scheduler or delayed-message consumer.
    batch-size: 100
    claim-timeout-millis: 300000
```

**You must integrate timeout scheduling in your host application.** Flovira does
not start background scans or register Redis scheduler locks. Setting
`flovira.timeout.enabled=true` enables timeout snapshots and execution APIs only;
without host calls, overdue tasks remain pending.

Call `FlowEngine.timeoutService().executeDue(new Date(), 100)` from your scheduler,
or `executeTimeout(taskId)` from a delayed-message consumer. Hosts own scheduling,
cluster coordination (for example ShedLock with Redis), retries and monitoring.
See [timeout integration and alpha migration](docs/timeout-integration.md) before
enabling this feature.

Workflow lifecycle callbacks use `WorkflowLifecycleListener` with Spring Bean
names or standalone registration. See [listener integration](docs/lifecycle-listener-migration.md)
for the eight events, global subscriptions, return-to-initiator handling and
host integration requirements.

The Java root package is `com.luokuiai.flovira`.

## Database

Full initialization scripts are available under `sql/<database>/`. Database
tables use the generic `flow_*` prefix to keep the persisted workflow schema
independent of application branding.
