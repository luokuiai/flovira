# spring-framework-support Specification

## Purpose
Define the supported Spring adapter matrix while preserving framework-independent core and JSON provider choices.

## Requirements

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
The distribution SHALL publish each supported JSON provider as an independent module without binding a provider to a framework starter.

#### Scenario: Build JSON providers
- **WHEN** a consumer selects a JSON provider
- **THEN** only the selected provider and its JSON library are required
