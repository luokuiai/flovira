## Context

Flovira persists every active executable step as a row in `flow_task`. Ordinary `BETWEEN` tasks wait for a human handler and `SUB_PROCESS` tasks wait for child aggregation, but the engine has no node that deliberately waits for an external business event. Node configuration already has a JSON `ext` field, while task rows are deleted when they become history.

The engine is an SDK used through three ORM families and Spring Boot 2/3/4 or Solon. Core must remain Java 8, framework-neutral, ORM-neutral and JSON-library-neutral. The fork starts at 1.0.0, so all database dialects use complete initialization schemas.

## Goals / Non-Goals

**Goals:**

- Add a first-class WAIT node whose current task is durable and has no human assignee semantics.
- Resume one exact WAIT task idempotently by task ID or by instance ID plus wait key.
- Configure fixed-duration timeout actions on BETWEEN and WAIT nodes.
- Freeze timeout deadline/action on the created task and execute due tasks safely in clustered deployments.
- Keep timeout execution disabled by default through an engine-level backend switch while preserving designer configuration.
- Keep React and Vue definition JSON interoperable with the core node `ext` format.

**Non-Goals:**

- A total lifetime limit for an entire process instance.
- Cron/calendar/business-day calculations or form-field-derived deadlines in the first version.
- Arbitrary user scripts as timeout actions.
- Message broker delivery, external distributed schedulers or exactly-once side effects outside the workflow transaction.

## Decisions

### 1. Append WAIT as node type 7

Append `WAIT(7, "wait")` without changing existing enum keys. WAIT is a work node so normal path selection creates one current `flow_task`, but it has no human permission list. Its versioned `waitConfig` in `Node.ext` contains a required normalized `waitKey` matching `^[A-Za-z][A-Za-z0-9_.:-]{0,127}$`.

Reusing `BETWEEN` with a magic handler was rejected because task lists, authorization, history and designers could not reliably distinguish human work from event waits.

### 2. Resume by task ID first, with an instance/key convenience API

`WaitService.resumeTask(taskId, variables)` is the precise primitive. `resume(instanceId, waitKey, variables)` searches only current WAIT tasks and requires one match. No match returns an idempotent `NOT_FOUND_OR_ALREADY_RESUMED` result; multiple matches fail without advancing either branch. Both APIs use the ordinary task passage path with system handling metadata so listeners, gateways, variables and history remain consistent.

The definition validator rejects duplicate wait keys that can be active together where it can determine that statically. Runtime duplicate detection remains mandatory because parallel topology can be complex.

### 3. Store timeout configuration in Node.ext and its snapshot on flow_task

`timeoutConfig` contains schema version, enabled flag, positive duration, unit and action. BETWEEN supports `AUTO_PASS` and `AUTO_REJECT`; WAIT supports `RESUME_WAIT`. Unsupported node/action combinations fail definition validation.

When a configured task is created and backend timeout execution is enabled, the engine calculates and stores `timeout_at`, `timeout_action`, serialized `timeout_config`, `timeout_status=PENDING` and an empty claim timestamp on that task. Later definition edits cannot change a running task. When the backend switch is disabled, configuration remains in `Node.ext` but timeout snapshot fields remain null.

A separate timeout table was rejected because `flow_task` is already the durable active-task aggregate. Successful or manually completed tasks naturally leave the scan set, and existing history captures the system action.

### 4. Claim due tasks atomically before execution

`FlowTaskDao` gains focused methods to list due candidates, atomically claim `PENDING` tasks, and release failed or stale claims. Only the claimant executes the action. This avoids duplicate execution when several application instances run the scheduler. A configurable claim timeout recovers work after process failure.

Timeout execution reuses `TaskService.skip`/`WaitService`; it never edits workflow state tables directly. Failures release the claim for a later retry and are surfaced in the returned batch result and logs.

A framework-neutral `TimeoutSchedulerLock` is checked before the due-task query. Spring automatically contributes a token-safe `StringRedisTemplate` implementation when that bean exists. Solon detects a Redisson client and contributes the same contract; hosts using another client can register their own implementation. A custom lock bean has priority. Redis lock contention skips the current scan. Redis failures are logged and fall back to the database claim path, so Redis reduces duplicate scans but is not the task-execution correctness boundary.

The Redis lock uses a configurable `schedulerLockKey` and the existing claim timeout as its lease. Token comparison is required when unlocking so an expired lock acquired by another instance is never deleted.

### 5. Keep scheduling outside core but start framework adapters with timeout execution

Core exposes `TimeoutService.executeDue(now, batchSize)` and contains all claiming and action semantics. `Flovira.timeout.enabled` defaults to false and also provides a `scanIntervalSeconds` interval (60 seconds by default), batch size and claim timeout defaults.

Spring and Solon modules register lightweight periodic invokers that call the core service only when timeout execution is enabled. The single `flovira.timeout.enabled` switch controls both timeout snapshots and the framework adapter's scheduler, avoiding a configuration state where executable deadlines are created without a scanner. Hosts that embed core without a framework adapter can still call the service from their own scheduler. Core creates no thread and imports no framework API.

### 6. Make designer configuration portable

React and Vue designers add WAIT to insertion menus and property panels. WAIT configuration uses a wait-key text field because a reusable SDK cannot know host business events; consumers can supply suggested keys where the designer provider contract supports it. BETWEEN and WAIT panels expose a timeout toggle, positive duration/unit controls and only valid actions. Import/export preserves unknown extension entries.

The designer shows that timeout configuration is runtime-dependent, but does not suppress it based on a transient backend capability response. Definitions therefore remain portable between deployments.

## Risks / Trade-offs

- [A custom wait key is never published by the host] -> Validate syntax, expose suggested keys, document the resume API and allow optional timeout recovery.
- [Manual completion races with timeout claiming] -> Re-read the current task after claim and treat a missing task as an idempotent completion.
- [A process dies after claiming] -> Store claim time and release claims older than the configured claim timeout.
- [Automatic rejection follows an unexpected edge] -> Reuse existing reject semantics and reject definitions whose node has no legal reject path.
- [Disabled timeout is later enabled while tasks are already active] -> Only tasks created after enablement receive snapshots; document this explicit non-retroactive behavior.
- [ORM implementations drift] -> Keep claim semantics in a dedicated DAO contract and compile/test all three adapters.
- [Redis becomes unavailable during scheduling] -> Log the lock failure and retain database claims as the final duplicate-execution guard.
- [Several applications share one Redis database] -> Allow the scheduler lock key to be namespaced per deployment.

## Migration Plan

1. Deploy complete 1.0.0 schemas containing the new task timeout columns and indexes before publishing WAIT/timeout definitions.
2. Deploy core, ORM and framework adapters with `flovira.timeout.enabled=false`.
3. Deploy designers and validate WAIT event integration through explicit resume calls.
4. Enable timeout execution per environment after scan interval, batch size and claim timeout are reviewed.
5. Rollback by disabling timeout execution and preventing new WAIT definitions; a compatible engine must remain until active WAIT tasks are resumed or cancelled.

## Open Questions

- A later release may add form-variable datetime deadlines and a wait-key provider SPI without changing the versioned extension envelope.
