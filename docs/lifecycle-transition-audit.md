# Lifecycle transition audit

This implementation baseline for `explicit-workflow-lifecycle` records behavior before the overhaul and explains integration decisions. See [lifecycle validation](lifecycle-validation.md) for the completed implementation. Source paths are relative to `flovira-core/src/main/java/com/luokuiai/flovira/core/`.

## Transitions and legacy listeners

| Operation / entry point | Original behavior | New event boundary |
| --- | --- | --- |
| Start: `InstanceServiceImpl.start` | Old start runs with a null instance; instance, initial history, and downstream tasks are then created, assigned, saved, and followed by finish/create | Emit PROCESS_STARTED only after identity exists; give the start node its own execution; capture downstream NODE_ENTERED after assignment |
| Approval/rejection: `TaskServiceImpl.skip` | start precedes authorization and voting; delegation and incomplete countersignature can return early; routing saves history, deletes old tasks, creates downstream tasks, then calls finish/create | Emit APPROVAL_ACTION_COMPLETED only for an authorized, accepted action; close the execution only when transition conditions are met |
| Delegation completion: `TaskServiceImpl.handleDepute` | Save history, remove delegate, restore principal when applicable; do not advance the node | Record accepted participant completion while retaining execution identity |
| Countersignature/voting: `TaskServiceImpl.cooperate` | Below threshold, save participant history and remove the actor; route at threshold, last participant, or applicable veto | Actions share one execution; history count does not determine NODE_LEFT |
| Transfer/delegation/signer changes: `TaskServiceImpl.updateHandler` | start precedes authorization; update participants/history, then finish; task remains | Emit ASSIGNEES_CHANGED for actual relationship changes, without node transitions |
| Pending: `TaskServiceImpl.pending` | start, authorization, history, PENDING state, finish; task remains | No separate fact among these eight; old finish does not imply node closure |
| Withdrawal: `TaskServiceImpl.revoke` | Default CANCEL state; delete old tasks and recreate start successors in the same instance; finish/create runs per old task | Emit resumable PROCESS_WITHDRAWN; close each old execution without multiplying downstream entry events |
| Resubmission | start always creates a new instance; withdrawn instances continue through existing tasks; RE_START alone does not establish an API | Add explicit same-instance resubmission and awaiting-resubmission state; do not relabel ordinary start |
| Task take-back: `TaskServiceImpl.taskBack` | Select target from the actor's latest approval history, then call skip with REJECT, TASK_BACK, and check-bypass parameters | Preserve actual backward routing; allocate a new execution identity on target re-entry |
| Normal completion: `TaskServiceImpl.setInsFinishInfo`, `handUndoneTask` | Remove END placeholder from tasks to save; store end-node display information; delete remaining tasks/users | Emit end-node transitions only for actual traversal; cancel remaining executions before PROCESS_ENDED(COMPLETED) |
| Termination: `TaskServiceImpl.termination` | Authorize, cancel child runs, set end-node display information, save selected history, delete remaining tasks; finish only for selected task | Close all active executions; do not fabricate end-node entry from display information; finish with PROCESS_ENDED(TERMINATED) |
| Veto cleanup: `TaskServiceImpl.oneVoteVeto` | Delete tasks/users by successor nodeCode without per-task history | Enumerate affected executions and record cancellation, not only the main task |
| Deletion: `InstanceServiceImpl.remove`, `toRemoveTask` | Delete instance, tasks, history, and participants | CRUD deletion does not automatically emit PROCESS_ENDED(CANCELLED) |
| Enable/disable: `InstanceServiceImpl.active`, `unActive` | Change availability | Availability is not process start/end |

Callers can customize FlowStatus, and the original withdrawal CANCEL state does not prevent later actions. Derive lifecycle from actual transitions, not status codes, method names, or old listener types.

## Automatic advancement and special nodes

| Entry point | Existing boundary | Integration requirement |
| --- | --- | --- |
| `WaitServiceImpl.doResumeTask`, `resumeTimeoutTask` | Claim and advancement share a transaction and call skipSystemTask | Signal/timeout race winner closes once; record WAIT_RESUME or WAIT_TIMEOUT |
| `TimeoutServiceImpl.executeDue`, `executeTimeout` | Host-triggered; no internal scanner; claim and advancement share a transaction | Preserve host scheduling and mark automatic approval/rejection as timeout sourced |
| `CarbonCopyUtil.advanceTasks` | Record recipients and automatically advance system tasks | Emit paired node entry/exit even for immediate advancement |
| `SubprocessServiceImpl.onTasksCreated` | Initialize child runs after commit | Internal coordination must remain independent of business subscriptions |
| `SubprocessServiceImpl.beforeTaskLeave` | Restrict ordinary approval; cancel linked child runs on rejection | Satisfy existing coordination before closing the parent execution |
| `SubprocessServiceImpl.onInstanceTerminal`, `resumeReadyRun` | Aggregate after child commit and resume parent according to run state | Child termination is not parent-node termination; aggregation closes the parent once |
| `SubprocessServiceImpl.cancelByParent`, `cancelByTask` | Lock and cancel runs transactionally, with retry support | Preserve ordering, idempotency, and rollback while adding correlated facts |

## Legacy listener scope and purpose

The original `utils/ListenerUtil.java` executeListener dispatches the same callback at node, definition, then global scope. Configuration placement does not change event meaning. execute parses comma-separated types and @@-separated paths, supports expressions/container objects, and exposes mutable context.

- executeStart is a pre-operation extension, sometimes before authorization and before startup identity exists. It cannot simply become PROCESS_STARTED or NODE_ENTERED.
- executeAssignment is a mutable extension before downstream persistence; retain it as a separate typed hook rather than an immutable fact.
- executeFinish describes an old operation, including participant-only changes; it cannot simply become PROCESS_ENDED or NODE_LEFT.
- endCreateListener calls finish, then create for non-END successors, locating tasks by nodeCode and reusing mutable context. It cannot distinguish repeated activations and skips end-node create.
- FORM_LOAD in TaskServiceImpl.load is a form-loading extension, separate from lifecycle.

Repository call sites and tests explain engine behavior, not the business meaning of listeners in external applications. Existing migration tooling cannot infer those meanings automatically.

## Gateway convergence and scope

An earlier proposal to associate executions with existing join batches did not match the implementation.

TaskServiceImpl.isGenerateNewTask reads graph state from instance.defJson, keyed by nodeCode. It examines predecessor state around the last parallel/inclusive gateway and clears downstream tasks/truncates the path when its branch-wait condition is met. There is no independent persisted batch ID or branch-arrival record.

NodeServiceImpl.getNextByCheckGateway recursively selects successors through serial conditions, inclusive filtering, and parallel traversal. Gateways have no work items. Preview reuses this method with null PathWayData; callbacks cannot be attached to every recursive call.

The graph snapshot does not establish separate convergence identities for repeated activations of one nodeCode or provide concurrent-arrival deduplication. This is a structural limitation, not a claim that a particular concurrency failure was reproduced.

This change excludes gateway callbacks, join batches, and routing modifications. Actual supported business-node executions emit events; existing gateway loop/concurrency limits remain. The earlier join-batch association requirement was removed.

## Host integration mapping

These scenarios are generic and require no particular host application or handler.

| Scenario | New integration |
| --- | --- |
| External initiated-process records, resubmission, current node, final state | Four process events plus NODE_ENTERED; do not infer instance termination from task ended |
| Approval work items and individual completion | NODE_ENTERED creates, APPROVAL_ACTION_COMPLETED completes one participant, NODE_LEFT cleans up remaining items |
| Transfer, delegation, signer changes | ASSIGNEES_CHANGED with before/after relationship differences |
| Business document stage/final state | Node departure reason and process state, not old callback names |
| Empty approver handling | Host strategies and assignment hooks; no extra fact event |
| Timeout snapshots and audit | Actual transitions synchronize state; hosts provide scheduling and failure audit |
| Material validation and business locks | Optional beforeOperation handling; no built-in business policy |

The change's design.md and specification define the complete eight-event contract.

## Original validation baseline and gaps

The following tests supplied the pre-overhaul baseline. Their existing coverage did not replace new event-sequence assertions. Current coverage is recorded in [lifecycle validation](lifecycle-validation.md).

| Test | Original coverage | Required additions |
| --- | --- | --- |
| `BusinessCorrelationServiceTest` | Business-type validation, correlation persistence/query | Valid startup identity; no event on failed startup |
| `WaitTimeoutServiceTest` | Wait/timeout handling, races, rollback | No loser event; one winner closure; no notification after outer rollback |
| `CarbonCopyUtilTest` | Carbon-copy-only advancement and recipient recording | Paired node facts and automatic source |
| `SubprocessLifecycleTest` | Initialization retry, missing-work coordination, one aggregation resume, resume retry, cancellation order/rollback, failed aggregation | Parent/child snapshots, cancellation ordering, one parent closure |
| `SubprocessUnusedRegressionTest` | Ordinary tasks/instances do not start subprocess transactions | Lifecycle integration must not introduce subprocess coordination on ordinary paths |

Additional requirements covered serial sequences, incomplete countersignature, transfer/delegation/signer changes, authorization failure, backward re-entry, withdrawal/resubmission, duplicate termination, parallel cleanup, and unchanged gateway routing without gateway events. Transaction tests needed to distinguish synchronous rollback, outer rollback, and after-commit failure rather than merely counting callbacks.

Historical baseline command: `./gradlew :flovira-core:test --no-daemon --no-parallel --max-workers=2` succeeded as UP-TO-DATE, reusing existing outputs. This historical record does not claim a fresh test run or represent final acceptance.
