## ADDED Requirements

### Requirement: Spring-only framework adapters
The distribution SHALL provide framework adapters for Spring Boot 2, Spring Boot 3, and Spring Boot 4, and SHALL NOT publish Solon-specific ORM, expression, scheduler, or Web adapter modules.

#### Scenario: Resolve supported framework modules
- **WHEN** a consumer selects a published Flovira framework adapter
- **THEN** the available adapters are limited to the supported Spring Boot variants

### Requirement: Framework-independent core
The core module SHALL remain independent of Spring and ORM implementation dependencies after Solon adapters are removed.

#### Scenario: Compile the core module
- **WHEN** the core module is compiled without any framework starter
- **THEN** compilation succeeds without Spring, Solon, or ORM implementation dependencies

### Requirement: Preserve independent JSON providers
The distribution SHALL retain Snack3 and Snack4 JSON providers even though their group namespace is `org.noear`, because they are independent serialization implementations rather than Solon framework adapters.

#### Scenario: Build JSON providers
- **WHEN** the JSON v1 plugin is compiled after Solon removal
- **THEN** Snack3 and Snack4 converters remain available
