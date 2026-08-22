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
- MySQL, Oracle, PostgreSQL, and SQL Server
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

## Build

A full build requires JDK 17 or later because the Jackson 3 and Spring Boot 4
modules use a Java 17 baseline.

```bash
./gradlew clean build
bun install
bun run build
```

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

All configuration properties use the `flovira` prefix:

```yaml
flovira:
  enabled: true
  banner: true
  # Maximum child instances started by one subprocess node. Must be positive.
  subprocess-max-children: 128
  timeout:
    enabled: false
    # Timeout scan interval in seconds.
    scan-interval-seconds: 60
    batch-size: 100
    claim-timeout-millis: 300000
    # Use a distinct lock key when multiple applications share Redis.
    scheduler-lock-key: flovira:timeout:scheduler
```

When timeout processing is enabled, a Spring application automatically prefers
a Redis scheduler lock if a `StringRedisTemplate` bean is available. If Redis
is not configured or temporarily unavailable, the engine continues to use
atomic database task claiming to prevent duplicate timeout processing.

The Java root package is `com.luokuiai.flovira`.

## Database

Full initialization scripts are available under `sql/<database>/`. Database
tables use the generic `flow_*` prefix to keep the persisted workflow schema
independent of application branding.
