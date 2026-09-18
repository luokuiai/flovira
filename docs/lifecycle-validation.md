# Lifecycle implementation validation

Change: `explicit-workflow-lifecycle`. Validation date: 2026-09-18. The results below record implementation validation; translating this document does not rerun those checks.

## Commands and scope

- `rtk proxy ./gradlew clean check --max-workers=2`: Passed. Core: 108 tests; MyBatis / MyBatis-Plus: 45 PostgreSQL contract tests each; Jackson / Jackson 3 / Gson: 6 each; both ORM Boot 3 / 4 starters: 5 shared adapter tests each; Boot 2 modes: 7; UI core: 3. No failures or skipped tests.
- `rtk bun run check`: Vue, React, both React UI adapter tests, and all designer library builds passed.
- `rtk bun run build:demos`: All six consuming examples built. Some bundle-size warnings remain; third-party dependencies and build limits were not changed.
- `rtk proxy openspec validate explicit-workflow-lifecycle --strict`: Passed.
- `rtk git diff --check` and whitespace checks for new files: Passed.

Browser checks used local Chromium, actual consuming examples, and built packages. In React, an approval-node NODE_LEFT / AFTER_COMMIT subscription retained its Bean name and configuration after saving and reopening. In Vue, process callbacks survived switching between basic-information and design panels; clicking the end node opened its properties and allowed adding a callback. Neither example reported browser script errors. Component tests additionally cover round trips, read-only behavior, and phase restrictions.

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
| Overlapping subscription deduplication, ordering, conflict rejection | `LifecycleListenerRegistryTest` and shared `LifecycleJsonContractTest` across three JSON providers |
| Framework-independent registration, Bean discovery, proxies, atomic startup | `LifecycleListenerRegistryTest`, shared `SpringLifecycleListenerRegistrarTest` across Boot adapters |
| Legacy finish rejection and ambiguous active-instance migration rejection | `oldLifecycleSubscriptionsAreRejectedBeforeMutation`, `explicitActiveMigrationHasNoHistoricalEventsAndRejectsChangedTaskSet` |
| UI capabilities, round trips, scope/phase restrictions, old-import diagnostics | UI `ApproverCapabilitiesTest`; React `LifecycleEditor.test.tsx`; Vue `lifecycle.test.ts`; configuration contracts for three JSON providers |
| Durable external delivery and required host timeout scheduling | [Integration documentation](lifecycle-listener-migration.md): outbox, idempotency keys, scheduling, and multiple instances; no new engine scanner |

## Integration boundaries

LifecycleTransition creates executions and facts only within authorized instance transitions; it does not route gateways. Approval, transfer/delegation, pending, withdrawal, termination, resubmission, and startup use real transactions. Wait, timeout, carbon-copy, and subprocess paths reuse native system entry points. Start/end nodes create and close separate executions; the initiator step uses runtime type 9.

GlobalListener and old start / assignment / finish / create dispatch have been removed. Legacy configuration produces explicit diagnostics; Listener / ListenerUtil retain only independent formLoad handling. Core introduces no Spring, ORM, or concrete JSON dependency and retains Java 8 compatibility.
