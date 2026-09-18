## 1. Transition contracts and test baseline

- [x] 1.1 Audit existing transitions, legacy listeners, and host integration scenarios. Deliver docs/lifecycle-transition-audit.md and the eight-event mapping in design.md; document gateway exclusions and host pre-operation responsibilities.
- [x] 1.2 Add event-sequence assertions for all eight events. Verify that serial approval, incomplete countersignature, transfer, and delegation return distinguish individual completion, assignment changes, and node closure.
- [x] 1.3 Preserve routing examples for backward transitions, parallel branches, and conditions. Verify that previews, gateways, and unselected branches emit no events and routing remains unchanged; record limitations without introducing join batches.

## 2. Execution identity and instance state

- [x] 2.1 Define supported node executions, DAOs, task/history associations, and ACTIVE / AWAITING_RESUBMISSION / ENDED instance states. Verify core compilation and distinct re-entry identities.
- [x] 2.2 Implement tenant isolation, logical deletion, activation/conditional closure, and participant consumption constraints in both ORMs. Verify that concurrent closure and consumption each have one successful winner.
- [x] 2.3 Synchronize all three fresh-install schemas and separate development-database migration/rollback documentation. Check fields, defaults, indexes, and ORM mappings; distinguish static checks from database execution.

## 3. Event contracts and transactional delivery

- [x] 3.1 Implement exactly eight events and immutable contexts, including operation correlation, participant relationships before/after, node work-item snapshots, and parent/child correlation. Verify required fields, copy isolation, and Java 8 compilation.
- [x] 3.2 Implement deterministic callback ordering, code registration and transaction delivery; cover duplicate names, callback failures and immutable dispatch snapshots with LifecycleListenerRegistryTest.
- [x] 3.3 Unify lifecycle transaction boundaries and same-instance recursion protection. Test missing transactions, authorization failures, synchronous listener failures, and host outer-transaction rollback.
- [x] 3.4 Deliver notifications after the outermost commit with independent failure reporting. Verify no notification on rollback, listener failure isolation, and accurate reporting of committed results through dispatcher tests and PostgreSQL contracts for both ORMs; native entry coverage is listed in section 4.
- [x] 3.5 Verify Boot 2 / 3 / 4 and custom transaction adapters; confirm that core has no framework dependency.
- [x] 3.6 Implement controlled mutable operation/assignment contexts and synchronous pre-hook delivery. Test variable-driven routing, persisted assignment changes, rollback, and immutable identity fields.
- [x] 3.7 Implement framework-independent callback registration. Test registration without Spring, persisted configuration or subscription declarations; cover multiple listeners and duplicate names.
- [x] 3.8 Discover Spring listener Beans directly. Verify proxy/injection preservation, atomic startup failure, context-owned cleanup, and consistent Boot 2 / 3 / 4 behavior using SpringLifecycleListenerRegistrarTest.

## 4. Engine integration

- [x] 4.1 Integrate process startup, actual start/end nodes, and terminal instance transitions. Test serial ordering, duplicate termination, and absence of fabricated end-node visits.
- [x] 4.2 Integrate APPROVAL_ACTION_COMPLETED and approval node departure. Cover countersignature, OR-signing, voting, delegation completion, approval/rejection, and system sources without closing below the threshold.
- [x] 4.3 Integrate ASSIGNEES_CHANGED. Verify transfer, delegation/return, signer snapshots, and ordering; exclude initial assignment, routine approval consumption, unchanged assignments, and closure cleanup.
- [x] 4.4 Integrate backward routing, withdrawal, and remaining-execution cleanup. Verify new execution identities, departure reasons, awaiting-resubmission snapshots, parallel work-item cancellation, and event ordering.
- [x] 4.5 Provide explicit same-instance resubmission and PROCESS_RESUBMITTED. Verify state gates, concurrent attempts, ordinary-approval bypass prevention, new-instance start behavior, and replacement of old call patterns. TaskService.resubmit supports both strategies and a separate initiator execution; both ORM contracts assert PROCESS_RESUBMITTED → initiator NODE_LEFT → approval NODE_ENTERED, including native hooks, instance locking, and concurrent attempts.
- [x] 4.6 Integrate wait handling, host-triggered timeout processing, and automatic carbon-copy advancement. Verify single closure under races, usable participant snapshots, no success notification on failure, and no fabricated approval actions.
- [x] 4.7 Integrate subprocess nodes and parent/child correlation. Verify that one child completion does not close the parent prematurely and that aggregation/cancellation/retry close it once; internal coordination remains independent of business callbacks.
- [x] 4.8 Apply and deduplicate pre-hooks across manual and automatic write entry points. Cover system timeout, wait, delegation, transfer, withdrawal, and startup.
- [x] 4.9 Implement TO_INITIATOR, separate initiator executions, and consistent terminology. Verify same-instance return/resubmission, work-item ownership, and ordering without replaying the start node.

## 5. Configuration, designers, and migration tooling

- [x] 5.1 Remove persisted lifecycle configuration and listener capability discovery. Keep legacy-field diagnostics separate from JSON object extension parsing.
- [x] 5.2 Replace GlobalListener and legacy dispatch, with explicit diagnostics for old configuration. Move assignment handling into the new listener and remove the obsolete FORM_LOAD callback and its dedicated expression dispatch.
- [x] 5.3 Remove callback configuration from Vue / React. Verify editing and JSON object round trips; hosts select business applicability in code.
- [x] 5.4 Provide active-instance migration tooling and host examples for individual completion, assignment differences, remaining-node cleanup, withdrawal/resubmission, phases, and outbox delivery. Check references and idempotency boundaries; reject ambiguous migration data explicitly.

## 6. Integration validation

- [x] 6.1 Run core, both ORMs, JSON, Boot compatibility checks, and ./gradlew clean check. Record actual results and database environments not exercised.
- [x] 6.2 Run bun run check and bun run build:demos; verify configuration interfaces and legacy-definition import diagnostics.
- [x] 6.3 Run openspec validate explicit-workflow-lifecycle --strict and git diff --check, check whitespace in untracked documentation separately, and map specification scenarios to evidence.

Implementation and scenario evidence: [lifecycle validation](../../../docs/lifecycle-validation.md). Host setup and existing migration tooling: [listener integration](../../../docs/lifecycle-listener-migration.md). Migration is not required for a new installation.

## 7. Simplify callback registration and correct extension format

- [x] 7.1 Remove persisted subscriptions, lifecycle ext parsing and designer callback controls; invoke code-registered listeners directly and preserve transaction behavior.
- [x] 7.2 Use JSON object extensions across engine, designers, examples and fixtures without an array compatibility path; verify nested business data round trips.
- [x] 7.3 Update host examples and specifications, run affected backend/frontend checks, and record actual results.
