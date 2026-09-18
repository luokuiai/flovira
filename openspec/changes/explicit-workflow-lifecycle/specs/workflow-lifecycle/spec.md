## Purpose

Define eight workflow callbacks that distinguish process transitions, business node execution, individual approval outcomes and assignee changes, with explicit snapshots, transaction delivery and migration guarantees for host applications.

## ADDED Requirements

### Requirement: Bounded callback surface
The new public callback contract SHALL expose exactly PROCESS_STARTED, PROCESS_WITHDRAWN, PROCESS_RESUBMITTED, PROCESS_ENDED, NODE_ENTERED, NODE_LEFT, APPROVAL_ACTION_COMPLETED and ASSIGNEES_CHANGED. It MUST NOT introduce separate task creation, completion or cancellation events. The new listener SHALL also provide transactional pre-operation validation, variable editing and assignment adjustment methods, separate from these facts.

#### Scenario: Initial assignment
- **WHEN** an approval node is activated and its initial assignment is ready
- **THEN** NODE_ENTERED contains the assignment snapshot without additional task-created or assignee-changed events

### Requirement: Independent process lifecycle
PROCESS_STARTED SHALL describe a newly established instance with valid identity. PROCESS_ENDED SHALL describe an actual terminal transition with COMPLETED, TERMINATED or CANCELLED reason. Resumable withdrawal SHALL emit PROCESS_WITHDRAWN without a false terminal event. A distinct same-instance resubmission operation SHALL emit PROCESS_RESUBMITTED only from an explicit awaiting-resubmission state, without another PROCESS_STARTED. Legacy status strings alone MUST NOT determine these transitions.

#### Scenario: Ordinary completion
- **WHEN** a new instance starts and subsequently completes
- **THEN** PROCESS_STARTED precedes start-node entry and PROCESS_ENDED follows closure of all active supported node executions

#### Scenario: Withdrawal and resubmission
- **WHEN** an instance is withdrawn into an awaiting-resubmission state and explicitly resubmitted
- **THEN** it keeps its instance identity and emits WITHDRAWN and RESUBMITTED for the respective transitions
- **AND** it emits neither a false process end nor another process start or fabricated start-node activation

#### Scenario: Invalid or repeated resubmission
- **WHEN** resubmission is attempted on an instance not awaiting resubmission
- **THEN** the operation fails without a success callback
- **AND** ordinary approval cannot bypass the awaiting-resubmission state in the new lifecycle mode

### Requirement: Business node execution coverage
Actual activations of approval, wait, carbon-copy, start, end, initiator and subprocess nodes SHALL have distinct persistent execution identities and NODE_ENTERED / NODE_LEFT transitions. Gateways, previews, condition evaluation and unselected branches MUST NOT produce node callbacks. This capability MUST NOT introduce join batches or change routing rules. Departure reasons SHALL distinguish COMPLETED, REJECTED, WITHDRAWN and CANCELLED.

#### Scenario: Re-entry through existing routing
- **WHEN** routing returns to a previously visited supported node
- **THEN** the new activation has a new execution identity even if its node code is unchanged

#### Scenario: Traversing gateways
- **WHEN** existing routing traverses conditional, parallel or inclusive gateways
- **THEN** callbacks describe only actual supported-node activations on the resulting route
- **AND** no gateway callbacks or new join-batch guarantees are introduced

#### Scenario: Forced termination
- **WHEN** an instance is terminated with multiple active executions
- **THEN** each closes with CANCELLED before PROCESS_ENDED with TERMINATED reason
- **AND** unvisited end nodes receive no fabricated entry

### Requirement: Individual approval outcomes
APPROVAL_ACTION_COMPLETED SHALL describe an authorized and successfully applied approval or rejection, including an individual countersignature vote and delegated handling. It SHALL identify the participant, actor, applicable delegation relationship, action, source, result, opinion and applicable target node. It MUST distinguish individual completion from closure of the whole node. Unauthorized, failed or unaccepted attempts MUST NOT produce successful notifications.

#### Scenario: Incomplete countersignature
- **WHEN** one participant approves but the node threshold is not met
- **THEN** APPROVAL_ACTION_COMPLETED identifies that participant and the node remains open without NODE_LEFT
- **AND** the consumer can complete only that participant personal todo

#### Scenario: Rejected authorization
- **WHEN** an unauthorized actor attempts approval
- **THEN** no approval-completed or node-transition event is produced

### Requirement: Assignee change facts
ASSIGNEES_CHANGED SHALL describe actual assignment relationship changes caused by transfer, delegation, delegation return, signer addition or signer removal on an active approval execution. It SHALL contain the operation, actor and immutable before/after assignment relationships including stable user and participation identities. Initial assignment, routine participant consumption by approval, closure cleanup and no-op changes MUST NOT generate redundant assignee-change events.

#### Scenario: Transfer without progression
- **WHEN** a task is transferred to another participant without routing onward
- **THEN** ASSIGNEES_CHANGED identifies the old and new recipients under the same execution identity
- **AND** neither NODE_LEFT nor a second NODE_ENTERED is emitted

#### Scenario: Delegated handling returns responsibility
- **WHEN** a delegate successfully handles an approval and responsibility returns to the principal while the node remains active
- **THEN** APPROVAL_ACTION_COMPLETED precedes ASSIGNEES_CHANGED
- **AND** the snapshots distinguish the delegate personal completion from the restored principal assignment

### Requirement: Causal event order and todo snapshots
Events SHALL preserve causal order within an operation and execution. NODE_ENTERED SHALL include the applicable persisted-task and resolved-assignee snapshot after existing assignment adjustments. NODE_LEFT SHALL identify affected tasks and remaining participants for cleanup. Nodes without tasks SHALL expose empty task and assignment collections. Unrelated parallel branches MUST NOT be advertised as having a global total order.

#### Scenario: Serial approval
- **WHEN** successful approval leaves one approval node and activates another
- **THEN** observers receive APPROVAL_ACTION_COMPLETED, source NODE_LEFT and destination NODE_ENTERED in that order
- **AND** personal completion and remaining-node cleanup are distinguishable without re-reading deleted tasks

#### Scenario: Withdrawal recreates a submission path
- **WHEN** withdrawal closes active executions and recreates supported nodes on the submission path
- **THEN** old NODE_LEFT events precede PROCESS_WITHDRAWN, which precedes the newly activated NODE_ENTERED events
- **AND** snapshots expose awaiting-resubmission state so consumers do not mistake pending submission for an approvable task

### Requirement: Automatic and nested execution
Automatic approvals and rejections SHALL follow the same callback rules with an explicit system source and actor. Wait resumption and carbon-copy progression SHALL emit actual node transitions without fabricating participant approvals. Child instances SHALL have independent process lifecycles and parent correlation; parent subprocess departure SHALL follow existing aggregation rules. Timeout scheduling SHALL remain host-owned, and unsuccessful timeout attempts MUST NOT become successful lifecycle notifications.

#### Scenario: Signal races with timeout
- **WHEN** a signal and timeout compete to resume one waiting execution
- **THEN** only the successful transition emits NODE_LEFT and no participant approval is fabricated

#### Scenario: One child completes
- **WHEN** a child ends while its parent subprocess execution still requires other children
- **THEN** the child emits its process end and the parent remains open

#### Scenario: Carbon-copy auto progression
- **WHEN** a carbon-copy node enters and automatically progresses
- **THEN** it has paired entry and departure with the applicable recipient snapshot and automatic source

### Requirement: Transactional delivery and errors
Listener implementations SHALL declare IN_TRANSACTION or AFTER_COMMIT fact delivery in code. Transaction-internal failures MUST abort and roll back the operation. After-commit delivery MUST wait for the outer transaction commit, report observer failures without misrepresenting committed state, and continue other notifications. Lifecycle-enabled writes MUST reject missing transaction support and recursive advancement of the same instance from a callback. Fact callbacks MUST remain distinct from mutable pre-operation methods.

#### Scenario: Outer rollback
- **WHEN** the host rolls back after an engine operation
- **THEN** state changes roll back and none of its after-commit notifications execute

#### Scenario: Callback rejects transaction
- **WHEN** a transaction-internal callback throws
- **THEN** the operation, execution records and associated assignment changes roll back together

#### Scenario: Notification fails after commit
- **WHEN** an after-commit observer throws
- **THEN** committed state remains committed, the configured error path reports the failure and other observers continue

### Requirement: Stable context and honest guarantees
Events SHALL expose detached immutable snapshots with event and operation identities, occurrence time, tenant, process and applicable node-execution/task identities, business correlation, actor, action, reason, source, state and applicable assignment details. Concurrent attempts MUST NOT commit duplicate closure of an execution or duplicate consumption of the same participant action. Retried transactions and post-commit crashes MUST be documented without claiming reliable external exactly-once delivery.

#### Scenario: Later object mutation
- **WHEN** later routing mutates live entities or assignment collections
- **THEN** earlier event snapshots retain their original values

#### Scenario: Competing completion
- **WHEN** two attempts compete to consume the same participant action or close the same execution
- **THEN** only the successful state transition creates committed success notifications

#### Scenario: External delivery requirements
- **WHEN** hosts require reliable external side effects
- **THEN** documentation explains transaction retry and after-commit crash limits and describes host-managed outbox and idempotency

### Requirement: Code-owned callback registration
Lifecycle listeners SHALL be registered as callback objects in host code or discovered as Spring beans. Registration alone SHALL activate the callbacks. Hosts SHALL filter events and business applicability in their implementation. The engine MUST NOT read lifecycle configuration from definition/node ext or expose dynamic callback selection in designers or capability APIs. Extensions SHALL be JSON objects, with no code/value array compatibility branch.

#### Scenario: Business extensions do not select callbacks
- **WHEN** a definition or node contains a lifecycle-named business JSON property
- **THEN** lifecycle dispatch does not parse, validate, or execute that property
- **AND** registered code callbacks still run

#### Scenario: JSON object round trip
- **WHEN** a designer imports and exports nested business JSON extensions
- **THEN** objects, arrays, booleans and numeric values inside the extension object are preserved without converting the root to an array

### Requirement: Explicit legacy and active-instance migration
Legacy callbacks MUST NOT be silently reinterpreted. Old definitions SHALL require migration; the old global listener and dispatch mechanism SHALL be replaced without a compatibility switch. Assignment editing SHALL move to the new listener; form loading SHALL read stored references and data without callbacks. Migration SHALL map active supported executions and process lifecycle state only when unambiguous, without replaying historical start or entry events. Hosts SHALL implement their own business validation and lock behavior through the new pre-operation methods or their service layer; page routing and timeout audit remain host responsibilities.

#### Scenario: Old finish subscription
- **WHEN** an old finish subscription is migrated
- **THEN** its individual approval, assignee change, node departure or process end meaning is explicitly selected rather than automatically renamed

#### Scenario: Ambiguous active data
- **WHEN** an active instance cannot be unambiguously mapped to lifecycle state and execution identities
- **THEN** migration is blocked or the instance finishes before upgrading, without guessed associations or fabricated historical events

### Requirement: Native mutable pre-operation handling
The new listener SHALL support synchronous pre-operation methods after authorization and before workflow mutation, plus assignment adjustment after resolution and before persistence. It SHALL invoke registered listeners in deterministic order; any business filtering belongs in callback code. Variable edits MUST reach routing and persistence. Assignment edits MUST be validated and reflected in persisted participants and entry snapshots. Identity, tenant and permission-bypass controls MUST NOT be mutable through these contexts.

#### Scenario: Business validation rejects an operation
- **WHEN** a pre-operation listener throws during a manual or system operation
- **THEN** the whole operation rolls back without success events or after-commit notifications

#### Scenario: Variable adjustment affects routing
- **WHEN** a pre-operation listener changes a permitted business variable
- **THEN** this operation evaluates conditions and persists state using the adjusted value

#### Scenario: Assignment adjustment
- **WHEN** a listener changes a permitted assignment draft
- **THEN** validated final recipients are persisted and included in NODE_ENTERED without a redundant ASSIGNEES_CHANGED event

#### Scenario: Pre-hooks remain synchronous
- **WHEN** a listener declares AFTER_COMMIT for facts
- **THEN** its pre-operation and assignment methods still execute synchronously inside the workflow transaction

### Requirement: Return to initiator

TO_INITIATOR SHALL mean return to the instance initiator for modification and resubmission, using an initiator execution distinct from the start event. All user-facing terminology SHALL consistently use 发起人.

Resubmission routing SHALL follow the existing node-control resubmitStrategy: RESTART_FROM_BEGINNING resumes sequential routing, while CONTINUE_FROM_REJECTED_NODE returns to the approval node that performed the rejection. The engine SHALL persist the selected strategy and rejection-source correlation when returning to the initiator and use that snapshot on resubmission. It MUST NOT introduce a separate global routing choice or replay process-start or start-node events for either strategy.

#### Scenario: Configured resubmission route
- **WHEN** an initiator resubmits after a return using either configured resubmission strategy
- **THEN** routing follows the strategy captured for that return and creates new execution identities for entered approval nodes
- **AND** later definition edits do not change the captured route policy

#### Scenario: Return and resubmit
- **WHEN** approval returns to the initiator
- **THEN** the current approval completes with rejection, its node leaves with REJECTED and an initiator execution enters awaiting resubmission
- **AND** no withdrawal, process end or repeated start-node event is fabricated
- **WHEN** the initiator resubmits
- **THEN** PROCESS_RESUBMITTED precedes initiator departure and subsequent node entry under the same instance

### Requirement: Framework-independent registration
Hosts SHALL register multiple listener instances using the core API without persisted listener configuration. Duplicate registration names MUST fail without replacing an existing object. Callbacks SHALL run in getOrder() order, with registration names breaking ties.

#### Scenario: Standalone host
- **WHEN** a host registers callback objects using only the core API
- **THEN** pre-operation hooks and facts execute without a framework bean container, subscription declarations or database listener configuration

#### Scenario: Registration collision
- **WHEN** another listener is registered under an existing name
- **THEN** registration fails without replacing the existing listener

### Requirement: Automatic Spring bean discovery
Spring integration SHALL discover WorkflowLifecycleListener beans and retain injected dependencies and proxies. Hosts MUST NOT need manual registration or subscription beans. Batch installation MUST reject invalid listeners and conflicts atomically. Context shutdown MUST remove only registrations owned by that context.

#### Scenario: Listener bean
- **WHEN** a host provides a listener bean
- **THEN** the injected bean receives callbacks without workflow configuration or manual registration

#### Scenario: Invalid startup registration
- **WHEN** discovered names conflict or a listener declares an invalid fact-delivery phase
- **THEN** startup fails and no partial batch remains installed

### Requirement: Form reads without callbacks
Task and historical form reads SHALL return stored form references and approval data without listener or expression dispatch. Persisted listener configuration, including formLoad, MUST be rejected when validating definitions and nodes.

#### Scenario: Load a task form
- **WHEN** a host reads a task form
- **THEN** the response uses the task formId and stored instance formData without reading listener configuration
- **AND** historical reads use the history snapshot

#### Scenario: Removed form callback configuration
- **WHEN** a definition or node contains a formLoad listener configuration
- **THEN** validation rejects it rather than executing or converting it
