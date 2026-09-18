# Lifecycle implementation validation

Change: `explicit-workflow-lifecycle`. Validation date: 2026-09-18. The original implementation checks are historical; the callback configuration they exercised has since been removed. Current correction checks are recorded separately below.

## Original implementation checks (historical)

- `rtk proxy ./gradlew clean check --max-workers=2`: Passed. Core: 108 tests; MyBatis / MyBatis-Plus: 45 PostgreSQL contract tests each; Jackson / Jackson 3 / Gson: 6 each; both ORM Boot 3 / 4 starters: 5 shared adapter tests each; Boot 2 modes: 7; UI core: 3. No failures or skipped tests.
- `rtk bun run check`: Vue, React, both React UI adapter tests, and all designer library builds passed.
- `rtk bun run build:demos`: All six consuming examples built. Some bundle-size warnings remain; third-party dependencies and build limits were not changed.
- `rtk proxy openspec validate explicit-workflow-lifecycle --strict`: Passed.
- `rtk git diff --check` and whitespace checks for new files: Passed.

Historical browser checks used local Chromium, actual consuming examples, and built packages. In React, an approval-node NODE_LEFT / AFTER_COMMIT subscription retained its Bean name and configuration after saving and reopening. In Vue, process callbacks survived switching between basic-information and design panels; clicking the end node opened its properties and allowed adding a callback. Neither example reported browser script errors. Component tests additionally cover round trips, read-only behavior, and phase restrictions.

PostgreSQL contracts use temporary Testcontainers databases for real transactions, row locks, races, and rollback. No host database migration was executed. MySQL and Oracle received static checks of fresh-install schemas, migration scripts, and entity mappings only. Gateway convergence remains graph-state based; this change adds no join batches and does not claim to solve existing gateway loops or complex concurrent joins. AFTER_COMMIT is not a durable message queue.

## Specification scenarios and evidence

Both ORMs share `flovira-orm/src/contractTest/java/com/luokuiai/flovira/orm/contract/SubprocessPersistenceContractTest.java`. Unqualified method names below refer to that file; native event coverage is exercised against both ORMs.

| Scenario | Evidence |
| --- | --- |
| Exactly eight facts, no initial-assignment change event, normal completion ordering | `serialLifecycleHasPersistentExecutionsAndCausalOrder`: start/end executions, event sequence, required envelope fields, history references |
| Withdrawal, return to initiator, same-instance resubmission, re-entry identity | `returnResubmitAndWithdrawKeepDistinctExecutionIdentities`; resubmission contracts cover both strategies, captured policy despite later definition changes, initiator permissions, and ordinary-operation rejection |
| Invalid/duplicate resubmission and outer rollback | `resubmissionRollsBackWithHostTransactionAndOnlyOneConcurrentAttemptWins`: initiator work items and lifecycle state restore together |
| Backward routing without start replay | `backwardRouteReentersApprovalWithoutReplayingStart` |
| No preview, gateway, or unselected-branch facts | `conditionalGatewayAndPreviewOnlyReportActualBusinessNodes`: actual preview and execution select the same node |
| Separate parallel executions and complete withdrawal cleanup | `parallelBranchesHaveSeparateExecutionsAndWithdrawTogether`: no gateway execution |
| Countersignature/vote threshold behavior | `countersignatureVoteAndUnauthorizedAttemptHaveDifferentEffects`, `voteThresholdClosesOnceAndRetainsRemainingAssignees` |
| Concurrent consumption of one participant | `concurrentApprovalConsumesParticipantOnlyOnce`: one success, one history record, one event; execution remains active |
| Delegation return, transfer, signer changes | `participantChangesAndDelegationDoNotCloseExecution`, `timeoutApprovalAndSignerChangesUseNativeEvents`: snapshots, completion before assignment changes, no premature closure |
| Forced termination/cancellation without fabricated end-node traversal | `forcedTerminationDoesNotVisitEndNodeAndOuterRollbackPublishesNothing`; parallel/subprocess cancellation tests |
| Wait signal/timeout race and carbon-copy advancement | `nativeWaitSignalAndTimeoutRaceEmitOnlyOneCompletion`, `waitAndCarbonCopyProduceOnlyActualNodeTransitions`: only the winner emits closure/terminal facts |
| System timeout and one authorized pre-hook invocation | `beforeOperationRunsOncePerAuthorizedNativeEntry`: start, delegation, delegation return, withdrawal, resubmission, timeout; unauthorized attempts do not invoke the hook |
| Mutable variables/assignment, rollback, recursion protection | Native resubmission contracts, `LifecycleListenerRegistryTest`, `listenerFailureRollsBackExecutionAndAssignmentTogether` |
| Child completion, final aggregation, parent cancellation | `childLifecyclesCorrelateParentAndOnlyLastChildClosesParentExecution`: correlation, last-child parent resumption, two children each cancelled once with no active executions remaining |
| Subprocess retries and failure boundaries | Core `SubprocessLifecycleTest`, `SubprocessUnusedRegressionTest`: coordination does not require business subscriptions |
| Immutable queued snapshots despite nested caller mutation | `deferredEventsKeepCreationTimeSnapshotsAfterCallerMutation`: mutate the original nested Map before outer commit; events retain creation-time values |
| Synchronous failure, outer rollback, after-commit failure isolation | Both ORM contracts `lifecycleDeliveryWaitsForOuterCommitAndRejectsMissingTransaction`, `lifecycleSynchronousFailureRollsBackDatabaseMutation`; core dispatcher isolation tests |
| Boot 2 / 3 / 4 transaction adapters | Shared `SpringTransactionExecutorTest`: outer transaction participation, rollback suppression, new transactions for writes after commit, immediate invocation when registering inside an already committed callback |
| Code-only registration, deterministic ordering and immutable callback snapshots | `LifecycleListenerRegistryTest`; no persisted callback configuration |
| Framework-independent registration, Bean discovery, proxies, atomic startup | `LifecycleListenerRegistryTest`, shared `SpringLifecycleListenerRegistrarTest` across Boot adapters |
| Legacy finish rejection and ambiguous active-instance migration rejection | `oldLifecycleSubscriptionsAreRejectedBeforeMutation`, `explicitActiveMigrationHasNoHistoricalEventsAndRejectsChangedTaskSet` |
| No designer callback selection, JSON object round trips, old-import diagnostics | React `CodeCallbacks.test.tsx`; Vue `legacyListeners.test.ts`; extension contracts across three JSON providers |
| Durable external delivery and required host timeout scheduling | [Integration documentation](lifecycle-listener-migration.md): outbox, idempotency keys, scheduling, and multiple instances; no new engine scanner |

## Integration with the latest develop branch

Before PR creation, develop commit `857d81a7` was integrated. `rtk proxy ./gradlew check --max-workers=2` passed after resolving overlapping approver-policy changes. Both PostgreSQL ORM suites passed 46 tests each, including `automaticApproverPolicyPreservesLifecycleAndAssignmentOverride`: an explicit empty-approver skip retains native lifecycle events, while assignment hooks can supply recipients and prevent the automatic skip. Resubmission also applies the integrated automatic approver policies.

After integration, `rtk bun run check` and `rtk bun run build:demos` also passed, including all six consuming examples. Existing bundle-size warnings remain. OpenSpec strict validation and staged whitespace checks passed. The browser smoke checks above were performed before this integration; they were not repeated for the merge.

## Integration boundaries

LifecycleTransition creates executions and facts only within authorized instance transitions; it does not route gateways. Approval, transfer/delegation, pending, withdrawal, termination, resubmission, and startup use real transactions. Wait, timeout, carbon-copy, and subprocess paths reuse native system entry points. Start/end nodes create and close separate executions; the initiator step uses runtime type 9.

GlobalListener and old start / assignment / finish / create dispatch have been removed. Legacy configuration produces explicit diagnostics. The old formLoad dispatch and its Listener / ListenerUtil / expression strategy have also been removed; form reads return stored references and data without callbacks. Core introduces no Spring, ORM, or concrete JSON dependency and retains Java 8 compatibility.

## Code callbacks and JSON object correction

The current implementation removes persisted lifecycle subscriptions, callback selectors and capability fields. Host callback objects run upon registration; business filtering lives in host code. Definition/node extensions use JSON objects with no array compatibility branch. The existing node execution, transaction and event ordering rules remain.

Fresh checks for this correction:

- Core tests passed (121 in the full run); the final strengthened rollback/after-commit assertion passed in a focused lifecycle-registry rerun.
- Both ORM PostgreSQL contract suites passed 48 tests each, with zero failures or skipped tests.
- Jackson, Jackson 3 and Gson passed 5 tests each, including object extensions and rejection of array roots; Spring modes passed 7 and UI core passed 2.
- Both ORM Boot 3 / 4 starters passed their shared adapter tests (5 per starter); the common backend example compiled.
- Frontend tests passed: Vue 43, React 140, Lumen adapter 10 and Ant Design adapter 7.
- Local Chromium smoke checks passed against built React and Vue examples: approval properties had no callback/listener tab, React node edits committed, and both pages reported zero script errors.
- Vue / React libraries, both React adapters and all six consuming examples built. React declaration generation initially exposed a missing type assertion in the JSON object reader; the corrected build passed. Existing bundle-size warnings remain.

No database schema or host data migration was executed. See [extension data format](extension-json-object.md) for development-data conversion.

OpenSpec strict validation and tracked/new-file whitespace checks passed.

## Removal of obsolete form-load callbacks

The formLoad callback and its Listener, ListenerVariable, ValueHolder, ListenerUtil,
ListenerStrategy and Spring expression implementation were removed. Task form reads
retain their formId/data response; historical reads still use history snapshots.
Definition and node validation in core, Vue and React rejects all nonempty persisted
listener settings, including formLoad, without compatibility execution.

Validation after removal:
- Core: 124 tests passed, including a task-form read that fails if listener configuration is accessed and definition/node rejection of formLoad.
- Spring modes: 7 tests passed; UI core: 2 tests passed; both ORM cores compiled.
- Frontend workspace tests passed (Vue 43, React 141, Lumen adapter 10, Ant Design adapter 7).
- Full frontend library, adapter and six consuming-example builds passed; only bundle-size warnings remained.
- Strict OpenSpec validation and git diff whitespace checks passed.

No schema changes or database migrations were executed. Existing development data
with listener settings must be cleared as described in lifecycle-listener-migration.md.
