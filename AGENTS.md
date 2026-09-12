# AGENTS.md

This is the primary source of project instructions for coding agents working on Flovira. Keep project instructions in English; preserve the language of user-facing documentation and source comments unless requested otherwise.

## Project

Flovira is a lightweight workflow engine / SDK maintained in `luokuiai/flovira`, with Maven group `com.luokuiai`. Applications integrate the backend through jars and the Vue / React designers through separate npm packages. Describe the current project as Flovira; preserve upstream attribution without presenting Flovira as an upstream community project.

Core constraints: Java 8 source compatibility, framework / ORM / JSON independence, and deliberate management of public contracts.

- `flovira-core`: entities, services, abstract DAOs, workflow state transitions, handlers, listeners, expression strategies, ID generation and SPI.
- `flovira-orm`: MyBatis and MyBatis-Plus implementations. Each has an ORM core plus Spring Boot 2 / 3 / 4 starters (`sb` / `sb3` / `sb4`).
- `flovira-plugin`: Spring expressions and integration (`modes`), separate Jackson / Jackson 3 / Gson providers (`json`), and designer backend APIs (`ui`). No bundled frontend pages.
- `flovira-designer`: Bun workspace with independent Vue / React packages, optional React UI adapters and consuming examples.
- `sql/mysql`, `sql/postgresql`, `sql/oracle`: complete `flovira-v1.sql` fresh-install schemas. SQL Server is unsupported. Do not restore its scripts or dialect branches.
- Tests exist in backend `src/test`, shared ORM `src/contractTest`, and frontend test files. External integration suites may supplement these; do not claim this repository has no tests.
- Flovira may manage versioned form metadata and content in `flow_form`; host applications may also supply forms. Workflow definitions, nodes, tasks and history store opaque string `formId` references and approval data snapshots. Keep host page routing outside `flow_form`; do not restore `form_custom`, `form_type`, `form_path`, numeric-only form references, bundled rendering pages or designer mode switching.

## Instruction hierarchy and maintenance

1. Current explicit user instructions.
2. Applicable module `AGENTS.md`.
3. This root `AGENTS.md`.
4. Root `CLAUDE.md` and `.cursor/rules` summaries.
5. Optional local architecture references.

Read root and applicable module instructions before editing. Module files contain only module-specific rules. Update this file first when changing lasting project rules, then synchronize summaries as needed. Explain conflicts and follow the user's explicit instructions. Do not turn temporary task context into permanent policy.

`.qoder/repowiki` is optional local, ignored documentation; never require it for contributors. Keep temporary investigations and decisions in `.codex/` or `docs/`, outside source packages. Remove unreferenced temporary code and backups from the source tree. README and `flovira.com` are user-facing documentation sources.

## Working rules

- Identify target files, expected behavior, verification and risks before nontrivial edits.
- Prefer the smallest correct change consistent with existing patterns. Avoid unrelated formatting, renaming or refactoring.
- Check `git status --short` before substantial changes. Preserve user work; never revert changes you did not make without authorization.
- Resolve uncertainty from code. State assumptions or ask a concise question when workflow semantics cannot be determined safely.
- Do not hide failures with fabricated defaults, swallowed exceptions or simulated success.
- Report success only with fresh command output or direct inspection. Report failed checks and their practical limits.
- Treat public APIs, entities, schemas, configuration, SPI, state transitions and cross-ecosystem changes as high risk.
- Use `rg` for literal searches and available CodeGraph tools for structural queries. Do not use Python for ordinary file reads or searches.
- Prefix shell commands with `rtk`; use `rtk proxy` when unfiltered output is needed.

## Architecture and extension points

- Obtain services and entities through `FlowEngine.xxxService()` / `FlowEngine.newXxx()`. Do not instantiate concrete ORM entities or service implementations in engine logic.
- `FrameInvoker.setBeanFunction` / `setCfgFunction` bridge framework bean lookup and configuration. Core must not depend on a container.
- `Flovira` configuration initializes handlers, banner and SPI. Preserve existing tenant, data-fill, permission, listener, ID, deletion and datasource extension points.
- JSON providers implement `JsonConvert` and register through `META-INF/services/com.luokuiai.flovira.core.json.JsonConvert`; consumers select one provider. Preserve `ServiceLoaderUtil` loading behavior.
- Spring Boot 2 uses `META-INF/spring.factories`; Boot 3 / 4 use `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Core defines `FloviraDao`, `FloviraQuery`, service abstractions and entity interfaces. ORM modules implement them and attach suppliers through starters.
- New JSON / ORM / framework support must use these extension points and the existing starter matrix. New database support needs a full schema plus dialect, pagination and datasource verification.

## Compatibility and dependencies

- Shared Java conventions target Java 8. Jackson 3 and Spring Boot 4 modules use the separate Java 17 convention.
- Java 8 modules must not use `var`, records, sealed classes, switch expressions, text blocks, pattern matching, `List.of` / `Map.of` / `Set.of`, `Optional.isEmpty`, `Stream.toList`, `String.isBlank` / `strip`, `Files.readString`, or other Java 9+ APIs.
- Use `Arrays.asList`, `Collections`, `Collectors.toList` and existing `com.luokuiai.flovira.core.utils` alternatives.
- Core must not import Spring, MyBatis-Plus or concrete framework / ORM / JSON implementations. Use adapters, `FrameInvoker` and SPI.
- Assess downstream effects before changing public classes, method signatures, fields, enum names / codes / ordering, configuration or SPI. Prefer additive evolution for released stable contracts and a deprecation period when retiring them.
- During the current pre-1.0 stabilization, explicitly requested contract changes may be applied directly. Document code and development-data migration; do not label every such change as a stable-release breaking change.
- Keep Spring Boot 2 / 3 / 4 and both ORM implementations aligned where affected.
- Dependency versions belong in `gradle/libs.versions.toml` and shared `buildSrc` conventions; do not independently pin versions in child modules. Inspect these files for the actual supported versions.
- Use Lombok and `slf4j-api` without binding a logging implementation. Avoid heavyweight dependencies in core.

## Java style and workflow semantics

- Follow `.editorconfig`: UTF-8, LF, final newline, no trailing whitespace, established indentation. No blanket reformatting; no Checkstyle / Spotless assumptions.
- Preserve entity interfaces, service interfaces plus `service.impl`, abstract DAOs and ORM implementations. Do not introduce unrelated architectural layers.
- Use Lombok for entity / DTO accessors and existing constructor injection patterns for components.
- Log with SLF4J placeholders. Do not use `printStackTrace`, `System.out.println` or log sensitive data in production code.
- Reuse existing utilities. Comments explain intent and non-obvious workflow behavior. Preserve class documentation, original `@author` and `@since` values.
- Conditions, approver expressions, vote signing and listeners are extension points; follow existing `condition`, `strategy`, `listener` and plugin expression implementations.
- Read relevant services, strategies, handlers, listeners and enums before changing approval, rejection, jumping, transfer, delegation, added / removed signers, termination, withdrawal, voting or branch behavior.
- CRUD uses `FloviraDao`. Entity changes must reach both ORM implementations, serialization, DTO conversion and supported SQL schemas.
- Preserve tenant isolation and logical deletion in both engine-managed and ORM-managed paths.

## Branding and attribution

- Preserve Flovira names, `com.luokuiai` packages / group, modules, banner, project links and author attribution unless explicitly authorized otherwise.
- Keep Apache 2.0 headers in Java files and do not alter `LICENSE` or weaken the project's free/open-source commitments.
- Existing or derived upstream code retains `Copyright 2024-2025, Warm-Flow (290631660@qq.com).` and other original attribution.
- Independently authored LuokuiAI Java code uses `Copyright 2026, LuokuiAI (luokuiai@gmail.com).` Mixed substantive code keeps the original notice and adds LuokuiAI attribution.
- Preserve truthful README fork provenance and upstream developer attribution. These are not current project branding.
- Frontend package licenses are separate; do not replace MIT frontend notices with backend Apache headers.
- Preserve Chinese README content and source comments unless the user asks for translation. Agent instruction files use English.

## SQL and migration

- Synchronize all three schemas: `sql/mysql/flovira-v1.sql`, `sql/postgresql/flovira-v1.sql`, `sql/oracle/flovira-v1.sql`.
- Do not define foreign keys in Flovira schemas. Protect internal relationships through engine transactions and verification; use indexes and unique constraints where appropriate.
- Every Flovira table must define `deleted` as `NOT NULL DEFAULT '0'`. Align indexes with tenant isolation, logical-deletion filters and actual DAO query predicates.
- Maintain complete V1 fresh-install baselines during 1.0.0 development; do not restore an inherited historical upgrade chain.
- Respect dialect differences in types, sequences / identity, pagination, case and reserved words. Check column comments and indexes against actual columns.
- Document migration purpose, affected contracts, data mapping, database differences and rollback. Never execute destructive database changes without explicit authorization.
- Existing development databases must be migrated separately; do not rerun fresh-install schemas over them.

## Task scope and verification

Use the lightest sufficient workflow:

- L0: small local fixes, documentation or comments; inspect, edit and run focused checks.
- L1: changes within a module, services, utilities, one starter or one database; collect context and compile / test affected modules.
- L2: public core contracts, framework / ORM matrices, SPI, state machines, multi-database schemas or publishing; inspect call paths and record decisions / validation when useful.

Gradle project names may differ from filesystem paths. Inspect `settings.gradle` before selecting task paths.

```bash
rtk proxy ./gradlew clean build
rtk proxy ./gradlew :flovira-core:compileJava
rtk proxy ./gradlew :flovira-mybatis-core:compileJava
# From flovira-designer:
rtk bun install
rtk bun run test
rtk bun run build
```

- Documentation / comments: `rtk git diff --check`.
- Core changes: relevant tests, core compilation and at least one downstream ORM / plugin compilation.
- ORM changes: test each affected ORM; starter changes need affected ecosystem checks.
- SPI / auto-configuration: verify registrations and implementation classes together.
- SQL changes: compare all supported schemas and ORM mappings; distinguish static checks from execution against actual databases.
- Frontend changes: relevant tests, library builds and consuming example builds. Check relevant UI interactions for visual changes.
- If blocked, report the command, failure, likely cause, impact and next step. Do not remove build plugins, weaken checks or change the JDK baseline to hide failures.

## Git and pull requests

- Default branch base is `develop`; other bases require explicit direction.
- Branch names use `<type>/<kebab-case-topic>`, with one clear goal. Use existing types such as `feat`, `fix`, `refactor`, `docs`, `perf`, `test`, `build`, `ci`, `update`, `upgrade`, `revert`.
- Commits use English `<type>: <imperative summary>`, no scope by default, lowercase initial, no final period, preferably <=50 characters and at most 72.
- Existing commit types include `init`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `update`, `upgrade`. Split unrelated logical changes.
- Omit bodies when the subject suffices. Otherwise explain intent or migration in English, with lines <=72 characters. Stable-release incompatible changes need explicit migration communication; follow current user instructions for pre-release labeling.
- Inspect staged diffs, run `git diff --cached --check`, and exclude artifacts, temporary files and unrelated work.
- PRs target `develop` by default, use an English Conventional Commit title, and follow `.github/pull_request_template.md`.
- Keep `## Summary` and `## Changes`. Summary is one short English paragraph; Changes has 3-7 concrete English bullets, including actual verification.
- Before creating a PR, verify the branch is pushed, workspace is clean and commits / diff are appropriate. Use Draft when work or verification remains incomplete.

## Publishing and permissions

- `publishToMavenLocal` publishes locally; remote `publish` requires configured repository, credentials, signing and Central publishing setup. Never commit credentials.
- `.github/workflows/publish.yml` publishes snapshots on `develop` and releases for version tags. npm releases use `.github/workflows/publish-npm.yml` and Trusted Publishing configured separately for each public package. Inspect workflows before changing publishing behavior.
- Without user authorization: no commit / push, branch deletion, force push, history rewrite, remote publishing, credential rotation, destructive out-of-scope filesystem actions, destructive database execution, lower JDK baselines or weakened licensing.
- Existing session authorization persists; do not repeatedly ask for already-authorized actions.
- Reading, searching, local builds / tests, scoped edits and git status / diff are allowed during relevant tasks.

## Responses

Be concise and concrete. Explain changes and verification, link useful files, and report uncertainty or incomplete work honestly. Do not claim failed or unrun checks passed.
