# hierarchical-process-observability Specification

## Purpose
Define efficient, tenant-safe progress and history views for parent and child process hierarchies.

## Requirements

### Requirement: Parent progress shall expose an optional subprocess summary
A subprocess parent node SHALL expose run status and total, pending, running, completed, failed and cancelled counts. Ordinary nodes and callers that ignore the additive field SHALL remain compatible.

#### Scenario: Render partial subprocess progress
- **WHEN** two of five child instances completed and three remain active
- **THEN** parent progress reports completed two, active three and total five
- **AND** it does not load every child task history

### Requirement: Child summaries and progress shall load lazily
The engine SHALL provide tenant-scoped paged child summaries containing item identity, pinned definition identity, child instance, status, outcome and current node. Detailed child progress SHALL be fetched through the child instance only when requested.

#### Scenario: Expand one child
- **WHEN** a client expands one row in a subprocess node
- **THEN** the engine returns that child instance's actual progress nodes
- **AND** sibling child histories are not loaded

### Requirement: Combined history shall preserve one chronological timeline
The engine SHALL compose parent task history, subprocess orchestration events and child task history into a timestamp-ordered result. Entries SHALL identify their source, run, child and item label so a client can show one timeline with expandable subprocess nodes.

#### Scenario: Child completes before the whole subprocess
- **WHEN** one child task is completed while sibling children remain active
- **THEN** combined history immediately includes that child's handling result
- **AND** the client does not need to wait for the subprocess run to finish

### Requirement: Hierarchical queries shall not expose workflow variables
Summary and combined-history APIs SHALL exclude unrestricted process variables and form payloads. Tenant and host authorization hooks SHALL be applied before parent-child navigation is returned.

#### Scenario: Read subprocess history without form access
- **WHEN** an authorized workflow-history query is made
- **THEN** identifiers, node actions, outcomes and display snapshots are returned
- **AND** child variables and business form content are omitted
