# Native lifecycle listener integration

The native listener replaces GlobalListener and the old start / assignment / create / finish dispatch. It exposes eight fact events and two optional pre-operation hooks, with automatic Spring Bean discovery, global subscriptions, and definition or node subscriptions by Bean name.

The project currently has no existing users requiring migration. New integrations should use the current API and fresh-install schemas. The migration sections below describe tooling already present in this implementation; they are not prerequisites for a new installation.

## Return to initiator and resubmission

An approval node with `nodeControlConfig.rejectStrategy=TO_INITIATOR` handles REJECT by entering AWAITING_RESUBMISSION and creating a runtime task owned only by the original initiator (`nodeType=9`). The current implementation also reads TO_DRAFT; the React designer normalizes it to TO_INITIATOR when saving. This runtime step is distinct from the start node and cannot be saved as a design node.

`resubmitStrategy` controls the route:

- RESTART_FROM_BEGINNING, the default, evaluates the route from the start node's outgoing edges using business variables.
- CONTINUE_FROM_REJECTED_NODE returns to the approval node that rejected the request, resolving its assignees again and creating a new task.

On return, the instance stores the strategy, source node, original task and initiator task ID in `resubmission_context`. This data is separate from business variables. Later definition edits or submitted variables cannot override the captured strategy. Other active tasks are cancelled, and child workflows use the existing cancellation path.

With the current actor supplied by the host's PermissionHandler, call:

```java
FlowEngine.taskService().resubmit(instanceId,
    FlowParams.build().variables(editedVariables));
```

Only the original initiator may resubmit; `ignore` cannot replace that identity. Ordinary skip, withdrawal, transfer and pending operations cannot substitute for resubmission. An awaiting instance may still be terminated under the existing permission rules. Resubmission keeps the instance ID, creates new task IDs, and does not call start or replay start-node history.

The event order is PROCESS_RESUBMITTED, initiator NODE_LEFT, then approval NODE_ENTERED. Native beforeOperation and beforeAssignment hooks support variable and assignment changes. Each node re-entry receives a new nodeExecutionId.

Task write operations use TransactionExecutor and an instance row lock. Both ORMs implement lockForUpdate after checking the instance's visibility, then restrict the lock by instance, tenant and deleted=0. MyBatis-Plus hosts must retain their ORM tenant isolation. Custom ORMs must implement FlowInstanceDao.lockForUpdate, invalidate stale query caches and return the state read after obtaining the lock. An outer rollback restores the initiator task and resubmission state and discards after-commit notifications. Listeners cannot synchronously advance the same instance recursively.

## Automatic Spring Bean discovery

Declare a listener Bean. Its Bean name is the subscription code stored in configuration. The engine retains the injected object and Spring proxy; no manual register call is required:

```java
@Bean
public WorkflowLifecycleListener businessListener(BusinessService businessService) {
    return new WorkflowLifecycleListener() {
        @Override
        public void onEvent(LifecycleEvent event, String parameters) {
            businessService.accept(event);
        }
    };
}

@Bean
public LifecycleSubscription businessProcessEnded() {
    return new LifecycleSubscription("businessListener", ListenerPoint.PROCESS_ENDED,
        DeliveryPhase.IN_TRANSACTION, 100, null);
}
```

The second Bean declares a global subscription. For selected definitions only, omit it and reference businessListener in those definitions.

Listeners and global subscriptions are installed atomically after singleton initialization. Conflicts or unknown references fail startup without leaving partial registrations. Context shutdown removes only objects installed by that context. Hosts should start handling workflows after application initialization completes.

Definition and node `ext` use the existing code/value array. The entry with code `lifecycle` stores the following JSON as its string value:

```json
{
  "schemaVersion": 1,
  "subscriptions": [
    {
      "code": "businessListener",
      "point": "PROCESS_ENDED",
      "phase": "IN_TRANSACTION",
      "order": 100,
      "parameters": null
    }
  ]
}
```

Process events allow definition or global scope. Node events also allow supported business-node scope. Individual approval and assignee-change events allow approval-node scope, in addition to definition and global scope. Gateways have no callbacks. Save, import and publish validate versions, codes, points, phases and cross-scope conflicts. Use JSON text in parameters when structured parameters are needed.

## Global registration without Spring

Core accepts listener objects directly, without Spring, container scanning, class-path configuration or persisted subscriptions:

```java
LifecycleListenerRegistry registry = FlowEngine.lifecycleListeners();
registry.register("business-sync", new WorkflowLifecycleListener() {
    @Override
    public void onEvent(LifecycleEvent event, String parameters) {
        // Update business state using the immutable event snapshot.
    }
});
registry.subscribeGlobally(new LifecycleSubscription(
    "business-sync", ListenerPoint.PROCESS_ENDED,
    DeliveryPhase.IN_TRANSACTION, 100, null));
```

These types belong to `com.luokuiai.flovira.core.listener.lifecycle`. Registration maps a stable code to an object; subscribeGlobally declares when to invoke it. Multiple objects and subscriptions are supported. Duplicate codes cannot replace existing objects. Order is deterministic by order, then code. Matching the same code, point and phase through multiple scopes invokes it once; conflicting parameters or order are rejected. Complete registration before processing workflows.

Global subscriptions do not require definition configuration. Local subscriptions reference the same registered objects. Database configuration cannot execute arbitrary class names. Spring discovery is an optional adapter.

## Hooks and delivery phases

- beforeOperation runs after task-operation authorization and before workflow writes. At initial start, it initializes business context before the submitter rule is resolved; submission denial still prevents instance/task persistence and rolls back transactional hook work. It supports business validation and permitted variable changes. Exceptions roll back the operation. BEFORE_OPERATION accepts only IN_TRANSACTION.
- beforeAssignment adjusts the resolved assignment draft before validation and persistence. It cannot change identity, tenant or authorization controls.
- onEvent receives eight immutable fact types, either in the transaction or after commit. After-commit failures are reported separately without presenting a committed operation as rolled back.

Hosts implement their own document checks and business locks. Transactional listeners may run again on transaction retry; network side effects cannot be rolled back. Reliable notification requires host-owned outbox, idempotency and retries.

Custom TransactionExecutor implementations must implement isTransactionActive(), returning true only inside a real transaction supporting outermost-commit synchronization. The default is false. Dispatch rejects missing transaction support. The Spring adapter checks both actual transaction and synchronization state; an outer rollback prevents delivery.

## Events and business todos

The envelope contains eventId, operationId, type, instanceId, occurredAt and contextJson. Context is serialized when the event is created. Objects parsed by listeners cannot modify engine state. Common fields include tenantId, definitionId, definitionVersion, businessType, businessId, actor, action, source, beforeState, state, variables, formData, opinion and destination nodes. Automatic operations use source=SYSTEM. Child instances also include parentInstanceId, parentTaskId and parentNodeExecutionId.

| Event | Host use |
| --- | --- |
| PROCESS_STARTED | Create the business-process association; task creation alone does not identify a new instance. |
| PROCESS_WITHDRAWN | Mark the instance awaiting resubmission without treating it as ended. |
| PROCESS_RESUBMITTED | Restore active handling; returnContext contains the captured return policy. |
| PROCESS_ENDED | Apply the terminal reason: COMPLETED, TERMINATED or CANCELLED. |
| NODE_ENTERED | Create todos from nodeExecutionId, tasks and assignees. Start/end nodes have empty task lists. |
| APPROVAL_ACTION_COMPLETED | Complete participants by participationId. Action is APPROVE or REJECT; nodeClosed distinguishes whole-node closure. |
| ASSIGNEES_CHANGED | Apply before/after differences using participationId, userId, type and createdBy, rather than comparing counts alone. |
| NODE_LEFT | Clear remaining todos using remainingAssignees and tasks. Reasons are COMPLETED, REJECTED, WITHDRAWN or CANCELLED. |

Serial approval emits individual completion, source departure, then destination entry. Delegation return emits individual completion followed by an assignee change without closing the node. An incomplete countersignature emits only individual completion. Initial assignment does not emit ASSIGNEES_CHANGED. All cancelled executions close before process end. Forced termination does not fabricate end-node entry.

The following example uses TodoProjection, a host-owned service rather than an SDK type:

```java
@Bean
public WorkflowLifecycleListener todoListener(TodoProjection todos) {
    return new WorkflowLifecycleListener() {
        @Override
        public void onEvent(LifecycleEvent event, String parameters) {
            Map<String, Object> snapshot = FlowEngine.jsonConvert.strToMap(event.getContextJson());
            switch (event.getType()) {
                case NODE_ENTERED:
                    todos.createForExecution(snapshot); // tasks and assignees
                    break;
                case APPROVAL_ACTION_COMPLETED:
                    todos.completeParticipants(snapshot); // Only the completed participants.
                    break;
                case ASSIGNEES_CHANGED:
                    todos.applyParticipantDifference(snapshot); // before and after
                    break;
                case NODE_LEFT:
                    todos.clearRemaining(snapshot); // remainingAssignees
                    break;
                default:
                    todos.updateProcessState(event.getType(), snapshot);
            }
        }
    };
}
```

Declare a LifecycleSubscription Bean for each required point. When projection tables share the engine database and transaction manager, use IN_TRANSACTION so both writes roll back together. Todo idempotency keys should include tenant, instance, nodeExecutionId and participationId. A nodeCode alone cannot distinguish re-entry. eventId deduplicates repeated delivery of one event; retrying a rolled-back operation creates new event IDs.

## Reliable external delivery and timeout scheduling

AFTER_COMMIT waits for the outer transaction to commit, but does not provide a persistent queue. A process crash may lose notifications. For reliable delivery, hosts must implement an outbox and retries. Write outbox records through an IN_TRANSACTION subscription:

```java
@Bean
public WorkflowLifecycleListener workflowOutbox(OutboxRepository outbox) {
    return new WorkflowLifecycleListener() {
        @Override
        public void onEvent(LifecycleEvent event, String parameters) {
            outbox.insert("workflowOutbox", event.getEventId(), event.getOperationId(),
                event.getType().name(), event.getContextJson());
        }
    };
}

@Bean
public LifecycleSubscription endedOutboxSubscription() {
    return new LifecycleSubscription("workflowOutbox", ListenerPoint.PROCESS_ENDED,
        DeliveryPhase.IN_TRANSACTION, 100, null);
}
```

OutboxRepository must join the engine transaction. Deduplicate by subscription code and eventId. A host worker delivers committed records, acknowledges success and retries failure; receivers must also be idempotent. Writing an outbox in AFTER_COMMIT does not make it atomic with the workflow transaction. Business listeners cannot synchronously advance the same instance, including after-commit listeners. Queue follow-up operations for execution after the current call returns.

**Hosts must integrate timeout scheduling themselves.** The engine supplies timeoutService query/execution methods and waitService resumption methods, but no scheduled scanner. Multi-instance hosts may use an existing scheduler or Redis coordination. Instance locks, task claims and conditional execution closure prevent duplicate successful handling; they do not replace scheduling, failure tracking or retries.

## Existing migration tooling

This section records the current implementation, not a deployment requirement for this pre-adoption project.

GlobalListener is no longer a dispatch source. The old start validation/variable hook corresponds to beforeOperation; assignment corresponds to beforeAssignment. Former finish/create business logic maps to the appropriate process, node, individual approval or assignee event. FORM_LOAD remains a separate extension. New contexts do not permit arbitrary replacement of identity, tenant, graph structure or authorization flags.

The implementation includes independent development scripts for [MySQL](migrations/explicit-workflow-lifecycle/mysql.sql), [PostgreSQL](migrations/explicit-workflow-lifecycle/postgresql.sql) and [Oracle](migrations/explicit-workflow-lifecycle/oracle.sql). They add flow_node_execution, instance lifecycle_state/resubmission_context, and task/history node_execution_id. They also widen flow_instance.business_id to 128 characters because existing subprocess keys contain two full IDs and a digest. MySQL uses VARCHAR and DATETIME(3); PostgreSQL uses VARCHAR and TIMESTAMP; Oracle uses VARCHAR2, NUMBER, CLOB and TIMESTAMP. No foreign keys are added. Tenant and logical-deletion boundaries are retained, and execution closure compares and increments version.

The scripts do not infer lifecycle states, replay events or migrate active instances. They do not contain IF NOT EXISTS and must not be blindly rerun. Applying them to a database with data requires paused writers, a backup and inspection of existing columns and indexes. Do not rerun fresh-install schemas over existing data.

The current explicit maintenance API accepts the complete reviewed active-task set:

```java
LifecycleMigration.migrateActive(instanceId,
    new HashSet<Long>(Arrays.asList(reviewedTaskId1, reviewedTaskId2)));
```

It locks the instance, creates a distinct execution per active task, associates history for the same taskId and marks the instance ACTIVE in one transaction. It emits no historical events. Changed task sets, missing creation times, inconsistent node types, existing associations, partial active execution data, terminal instances and already migrated instances are rejected. Historical terminal rows do not receive guessed execution IDs. Old return-to-initiator state cannot be inferred from a business status string.

If this tooling is used, migrate old subscriptions explicitly and remove globalListenerPath. Only formLoad may remain in the old listener fields. Unsupported old callbacks fail save, import, publish and execution validation. Verify approval, return, withdrawal, resubmission and subprocess coordination before resuming writers and host schedulers.

Rollback must restore matching code and data. Once new executions, initiator tasks or history exist, dropping columns or downgrading jars alone is insufficient. Never truncate subprocess business keys to fit the old 40-character column. MySQL and Oracle DDL may commit implicitly, so transaction rollback is not a schema recovery plan. No host database changes were executed as part of this work.

### Operation context and trust boundaries

A lifecycle hook needs engine-owned runtime facts before persistence, not an ID
that assumes a database row already exists. The operation model therefore keeps
four sources separate:

| Source | API | Contract |
| --- | --- | --- |
| Runtime facts | `getDefinition()`, `getInstance()`, `getInitiatorId()`, `getActor()` | Read-only snapshots from engine entities; always available, including first start. The original initiator is independent of the current actor. |
| Current input | `getInputVariables()` | Read-only, unmerged variables supplied for this operation. Input cannot establish a tenant or replace initiation identity. |
| Stored business context | `getPersistedVariables()` | Read-only variables from before this operation, including nested maps/lists. Empty for a new instance. Input and hooks cannot rewrite this snapshot. |
| Working variables | `getVariables()`, `setVariable`, `removeVariable` | Stored variables overlaid with input, then validated/adjusted by hooks. Only this result proceeds to resolvers and persistence. |

`getDefinition()` provides definition ID, tenant ID, flow code/name, version and
business type. `getInstance()` provides instance ID, definition ID, tenant ID,
original creator and business key. `isNewInstance()` explicitly identifies initial
creation; instance information is never hidden behind a null value. Constructors
require the runtime definition and instance; there is no ID-only context variant.

Hosts validate employment/company relationships using these facts. At initial
start, validate the selected employment from input against the definition tenant
and original initiator, then write the validated business snapshot through
`setVariable`. On later operations, validate and restore that snapshot from
`getPersistedVariables()`, not the overlaid working variables. Use the runtime
initiator to check snapshot ownership; never infer it from the current actor.
Restore or remove related visible input fields in the working variables as needed.
The engine does not interpret host-specific organization keys or infer employment.

The order is runtime context creation → transactional `beforeOperation` validation
and initialization → submitter authorization (initial start) → resolver/route
execution → persistence → lifecycle facts. Exceptions abort the transition.
`PROCESS_STARTED` is a fact after creation, so it cannot initialize context needed
by the first resolver. For existing-task operations, task authorization still runs
before the hook. Variable merging happens once at the operation-context boundary;
callers must not pre-merge input and history before constructing a transition.

This corrects a design omission in the initial native lifecycle implementation:
its ID/actor/merged-variable-only context discarded authoritative information
required for business validation. No schema changes or special Guardian adapter
are required. Applications implement the native hook contract and own their
business-context validation, including old instances without a snapshot and
validated parent-context inheritance for subprocesses.

## Validation scope

See [lifecycle validation](lifecycle-validation.md) for commands and scenario evidence. Both ORMs were tested with real PostgreSQL transactions. MySQL and Oracle received static schema/mapping checks only.
