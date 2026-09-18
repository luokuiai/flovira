## Why

The existing start / assignment / create / finish callbacks mix operations, node transitions, and process lifecycle changes. Applications cannot reliably synchronize business state or individual work items from these callbacks. During the current alpha stage, define explicit boundaries for process, node, and participant callbacks so consumers do not infer process completion from the old finish callback or a task's ended field.

## What Changes

- Provide eight fact events: PROCESS_STARTED, PROCESS_WITHDRAWN, PROCESS_RESUBMITTED, PROCESS_ENDED, NODE_ENTERED, NODE_LEFT, APPROVAL_ACTION_COMPLETED, and ASSIGNEES_CHANGED.
- Include reasons for process completion and node departure. Approval completion describes one participant's accepted action; assignee changes include before/after participants and relationships for transfers, delegation, and signer changes.
- Cover approval, wait, carbon-copy, start, end, initiator, and subprocess nodes. Persist execution identities and create a new identity on re-entry. Exclude gateway callbacks, join batches, and routing changes.
- Define transaction phases, immutable snapshots, causal ordering, failure reporting, and concurrent closure rules. Do not promise durable external delivery.
- Register host callback objects directly or discover Spring beans. Lifecycle selection is not stored in definitions or nodes and is not exposed by either designer.
- Do not add separate task creation/completion/cancellation events. The new listener also supports synchronous pre-operation validation, variable changes, and assignment changes through explicit contexts. Host applications retain ownership of business page routing.
- Replace GlobalListener and the old dispatch mechanism natively, without a dual compatibility mode. Document the capability mapping and existing migration tooling; migration is not a prerequisite for new integrations in this pre-adoption project.

## Capabilities

### New Capabilities

- `workflow-lifecycle`: Eight events, node execution identities, approval results, participant snapshots, delivery phases, code registration, and migration contracts.

### Modified Capabilities

None. Existing wait behavior, host timeout scheduling, countersignature thresholds, gateway routing, and subprocess aggregation remain unchanged; their actual transitions emit the new events.

## Impact

- Core: Listener contracts and registration, process start/withdrawal/resubmission/termination, node executions, approval and participant changes, automatic advancement, and subprocess coordination.
- Both ORMs and all three databases: Execution records, task/history associations, concurrency controls, fresh-install baselines, and separate development-database migration tooling.
- Spring Boot 2 / 3 / 4: Transaction and after-commit adapters. Core remains framework independent and Java 8 compatible.
- JSON / UI: Extensions use JSON objects. Remove lifecycle editors and persisted callback configuration from both designers.
- Host integration: Generic business mappings without a built-in external process portal, work-item service, or business locking policy.
- Validation: All eight events, negative cases, ordering, individual versus node cleanup, ORM concurrency and rollback, backend compatibility checks, and frontend checks.
