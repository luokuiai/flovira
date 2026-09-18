/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *    Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.luokuiai.flovira.orm.contract;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.entity.NodeExecution;
import com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao;
import java.util.Map;
import com.luokuiai.flovira.core.listener.lifecycle.*;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import com.luokuiai.flovira.core.dto.DefJson;
import com.luokuiai.flovira.core.dto.WorkflowPackage;
import com.luokuiai.flovira.core.dto.WorkflowImportResult;
import com.luokuiai.flovira.core.exception.FlowException;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.orm.dao.FlowInstanceDao;
import com.luokuiai.flovira.core.orm.dao.FlowDefinitionDao;
import com.luokuiai.flovira.core.entity.SubprocessChild;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.SubprocessRun;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.core.enums.SubprocessChildStatus;
import com.luokuiai.flovira.core.enums.SubprocessRunStatus;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
import com.luokuiai.flovira.core.orm.dao.FlowFormDao;
import com.luokuiai.flovira.core.handler.TenantHandler;
import com.luokuiai.flovira.core.service.FormService;
import com.luokuiai.flovira.core.service.TaskService;
import com.luokuiai.flovira.core.service.impl.TaskServiceImpl;
import com.luokuiai.flovira.core.service.impl.TimeoutServiceImpl;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.utils.page.Page;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.postgresql.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * 三种 ORM 共用的子流程持久化契约测试
 *
 * @author warm
 */
public class SubprocessPersistenceContractTest {

    @ClassRule
    public static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("flovira").withUsername("flovira").withPassword("flovira");

    private static ConfigurableApplicationContext context;

    private FlowSubprocessRunDao<SubprocessRun> runDao;
    private FlowSubprocessChildDao<SubprocessChild> childDao;
    private FlowSubprocessEventDao<SubprocessEvent> eventDao;
    private FlowTaskDao<Task> taskDao;
    private FlowFormDao<Form> formDao;
    private FlowDefinitionDao<Definition> definitionDao;
    private FormService formService;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @BeforeClass
    public static void startApplication() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        context = application.run(
            "--spring.main.banner-mode=off",
            "--spring.datasource.url=" + DATABASE.getJdbcUrl(),
            "--spring.datasource.username=" + DATABASE.getUsername(),
            "--spring.datasource.password=" + DATABASE.getPassword(),
            "--spring.datasource.driver-class-name=" + Driver.class.getName(),
            "--spring.sql.init.mode=always",
            "--spring.sql.init.schema-locations=classpath:subprocess-contract-schema.sql",
            "--flovira.banner=false",
            "--flovira.timeout.enabled=true",
            "--flovira.logic-delete=true",
            "--flovira.tenant-handler-path=" + ContractTenantHandler.class.getName(),
            "--flovira.data-source-type=postgresql"
        );
    }

    @AfterClass
    public static void stopApplication() {
        if (context != null) context.close();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        runDao = (FlowSubprocessRunDao<SubprocessRun>) context.getBean(FlowSubprocessRunDao.class);
        childDao = (FlowSubprocessChildDao<SubprocessChild>) context.getBean(FlowSubprocessChildDao.class);
        eventDao = (FlowSubprocessEventDao<SubprocessEvent>) context.getBean(FlowSubprocessEventDao.class);
        taskDao = (FlowTaskDao<Task>) context.getBean(FlowTaskDao.class);
        formDao = (FlowFormDao<Form>) context.getBean(FlowFormDao.class);
        definitionDao = (FlowDefinitionDao<Definition>) context.getBean(FlowDefinitionDao.class);
        formService = context.getBean(FormService.class);
        jdbcTemplate = context.getBean(JdbcTemplate.class);
        transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        jdbcTemplate.update("delete from flow_node_execution");
        jdbcTemplate.update("delete from flow_user");
        jdbcTemplate.update("delete from flow_his_task");
        jdbcTemplate.update("delete from flow_subprocess_event");
        jdbcTemplate.update("delete from flow_subprocess_child");
        jdbcTemplate.update("delete from flow_subprocess_run");
        jdbcTemplate.update("delete from flow_task");
        jdbcTemplate.update("delete from flow_form");
        jdbcTemplate.update("delete from flow_definition");
        jdbcTemplate.update("delete from flow_instance");
        jdbcTemplate.update("delete from flow_skip");
        jdbcTemplate.update("delete from flow_node");
    }

    @Test
    public void resubmissionFollowsCapturedStrategyUnderSameInstance() {
        for (String strategy : java.util.Arrays.asList("RESTART_FROM_BEGINNING", "CONTINUE_FROM_REJECTED_NODE")) {
            setUp();
            Instance instance = returnedInstance(strategy);
            Long instanceId = instance.getId();
            Task initiator = FlowEngine.taskService().getByInsId(instanceId).get(0);
            assertEquals(Integer.valueOf(9), initiator.getNodeType());
            assertEquals("AWAITING_RESUBMISSION", instance.getLifecycleState());
            assertEquals("initiator", FlowEngine.userService().listByTaskIdAndTypes(initiator.getId()).get(0).getProcessedBy());
            // 设计变更不应覆盖这次退回保存的策略。
            jdbcTemplate.update("update flow_node set ext = CAST(? AS jsonb) where node_code = 'second'", controlExt(
                strategy.equals("RESTART_FROM_BEGINNING") ? "CONTINUE_FROM_REJECTED_NODE" : "RESTART_FROM_BEGINNING"));
            Instance resumed = FlowEngine.taskService().resubmit(instanceId,
                FlowParams.build().handler("initiator").variables(java.util.Collections.singletonMap("edited", "yes")));
            assertEquals(instanceId, resumed.getId());
            assertEquals("ACTIVE", resumed.getLifecycleState());
            Task next = FlowEngine.taskService().getByInsId(instanceId).get(0);
            assertEquals(strategy.equals("RESTART_FROM_BEGINNING") ? "first" : "second", next.getNodeCode());
            assertNotEquals(initiator.getId(), next.getId());
            assertEquals("yes", FlowEngine.instanceService().getById(instanceId).getVariableMap().get("edited"));
            DefJson chart = FlowEngine.jsonConvert.strToBean(resumed.getDefJson(), DefJson.class);
            assertEquals(java.util.Collections.singletonList(next.getNodeCode()), chart.getNodeList().stream()
                .filter(node -> Integer.valueOf(1).equals(node.getStatus())).map(com.luokuiai.flovira.core.dto.NodeJson::getNodeCode)
                .collect(java.util.stream.Collectors.toList()));
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject("select count(*) from flow_instance", Integer.class));
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "select count(*) from flow_his_task where node_code = 'start'", Integer.class));
        }
    }

    @Test
    public void resubmissionPreHooksPersistVariableEditsAndFinalAssignment() throws Exception {
        Instance instance = returnedInstance("CONTINUE_FROM_REJECTED_NODE");
        instance.setVariables("{\"removeMe\":true}");
        FlowEngine.instanceService().updateById(instance);
        java.util.List<String> operations = new java.util.ArrayList<>();
        WorkflowLifecycleListener listener = new WorkflowLifecycleListener() {
            public void beforeOperation(OperationContext operation) {
                operation.removeVariable("removeMe");
                operation.setVariable("businessChecked", true);
                operations.add(operation.getOperationId());
            }
            public void beforeAssignment(AssignmentContext assignment) {
                assignment.setAssignees(java.util.Collections.singletonList("adjusted-reviewer"));
            }
            public void onEvent(LifecycleEvent event) {
                assertEquals(operations.get(0), event.getOperationId());
            }
        };
        try (AutoCloseable ignored = FlowEngine.lifecycleListeners().install(
                java.util.Collections.singletonMap("resubmitHooks", listener))) {
            FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
        }
        assertEquals(1, operations.size());
        Instance resumed = FlowEngine.instanceService().getById(instance.getId());
        assertEquals(Boolean.TRUE, resumed.getVariableMap().get("businessChecked"));
        org.junit.Assert.assertFalse(resumed.getVariableMap().containsKey("removeMe"));
        Task next = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        assertEquals("adjusted-reviewer", FlowEngine.userService().listByTaskIdAndTypes(next.getId()).get(0).getProcessedBy());
    }

    @Test
    public void resubmissionEventWaitsForCommitAndUsesDetachedSnapshot() throws Exception {
        Instance instance = returnedInstance("CONTINUE_FROM_REJECTED_NODE");
        java.util.List<LifecycleEvent> events = new java.util.ArrayList<>();
        WorkflowLifecycleListener listener = new WorkflowLifecycleListener() {
            public DeliveryPhase getDeliveryPhase() { return DeliveryPhase.AFTER_COMMIT; }
            public void onEvent(LifecycleEvent event) {
                if (event.getType() == LifecycleEventType.PROCESS_RESUBMITTED) events.add(event);
            }
        };
        try (AutoCloseable ignored = FlowEngine.lifecycleListeners().install(
                java.util.Collections.singletonMap("resubmitObserver", listener))) {
            transactionTemplate.execute(status -> {
                FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
                assertTrue(events.isEmpty());
                status.setRollbackOnly();
                return null;
            });
            assertTrue(events.isEmpty());
            Instance resumed = FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
            assertEquals(1, events.size());
            assertEquals(instance.getId(), events.get(0).getInstanceId());
            String snapshot = events.get(0).getContextJson();
            resumed.setLifecycleState("CHANGED_AFTER_DELIVERY");
            assertEquals("ACTIVE", FlowEngine.jsonConvert.strToMap(snapshot).get("state"));
            assertTrue(snapshot.contains("CONTINUE_FROM_REJECTED_NODE"));
            assertEquals(snapshot, events.get(0).getContextJson());
        }
    }

    @Test
    public void recursiveResubmissionListenerRollsBackAndReleasesGuard() throws Exception {
        Instance instance = returnedInstance("CONTINUE_FROM_REJECTED_NODE");
        WorkflowLifecycleListener listener = new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event) {
                Task next = FlowEngine.taskService().getByInsId(event.getInstanceId()).get(0);
                FlowEngine.taskService().skip(next.getId(), approval("PASS"));
            }
        };
        try (AutoCloseable ignored = FlowEngine.lifecycleListeners().install(
                java.util.Collections.singletonMap("recursiveObserver", listener))) {
            IllegalStateException failure = org.junit.Assert.assertThrows(IllegalStateException.class,
                () -> FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator")));
            assertTrue(failure.getMessage().contains("Recursive"));
            assertEquals("AWAITING_RESUBMISSION", FlowEngine.instanceService().getById(instance.getId()).getLifecycleState());
        }
        assertEquals("ACTIVE", FlowEngine.taskService().resubmit(instance.getId(),
            FlowParams.build().handler("initiator")).getLifecycleState());
    }

    @Test
    public void returnCancelsParallelTasksAndRollbackRestoresOriginalApproval() {
        Instance instance = returnedInstance("RESTART_FROM_BEGINNING");
        FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
        Task original = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        Task parallel = FlowEngine.taskService().addTask(FlowEngine.nodeService()
            .getByDefIdAndNodeCode(81000L, "second"), instance, FlowEngine.defService().getById(81000L), approval("PASS"));
        NodeExecution execution = FlowEngine.newNodeExecution();
        root(execution);
        FlowEngine.dataFillHandler().idFill(execution);
        execution.setInstanceId(instance.getId()).setDefinitionId(instance.getDefinitionId()).setNodeCode(parallel.getNodeCode())
            .setNodeType(parallel.getNodeType()).setState("ACTIVE").setVersion(0).setEnteredAt(new Date());
        ((FlowNodeExecutionDao<NodeExecution>) context.getBean(FlowNodeExecutionDao.class)).save(execution);
        parallel.setNodeExecutionId(execution.getId());
        FlowEngine.taskService().save(parallel);
        FlowEngine.userService().saveBatch(FlowEngine.userService().taskAddUsers(java.util.Collections.singletonList(parallel)));
        transactionTemplate.execute(status -> {
            FlowEngine.taskService().skip(original.getId(), approval("REJECT"));
            assertEquals(1, FlowEngine.taskService().getByInsId(instance.getId()).size());
            status.setRollbackOnly();
            return null;
        });
        assertEquals(2, FlowEngine.taskService().getByInsId(instance.getId()).size());
        FlowEngine.taskService().skip(original.getId(), approval("REJECT"));
        List<Task> tasks = FlowEngine.taskService().getByInsId(instance.getId());
        assertEquals(1, tasks.size());
        assertEquals(Integer.valueOf(9), tasks.get(0).getNodeType());
        assertTrue(FlowEngine.userService().listByTaskIdAndTypes(parallel.getId()).isEmpty());
        assertTrue(FlowEngine.userService().listByTaskIdAndTypes(original.getId()).isEmpty());
    }

    @Test
    public void resubmissionRejectsWrongActorOrdinaryApprovalAndTargetOverride() {
        Instance instance = returnedInstance("CONTINUE_FROM_REJECTED_NODE");
        Long taskId = FlowEngine.taskService().getByInsId(instance.getId()).get(0).getId();
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService()
            .resubmit(instance.getId(), FlowParams.build().handler("intruder").ignore(true)));
        org.junit.Assert.assertThrows(IllegalArgumentException.class, () -> FlowEngine.taskService()
            .resubmit(instance.getId(), FlowParams.build().handler("initiator").nodeCode("end")));
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService()
            .skip(taskId, FlowParams.build().handler("initiator").skipType("PASS").ignore(true)));
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService()
            .revoke(instance.getId(), FlowParams.build().handler("initiator")));
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService()
            .updateHandler(taskId, FlowParams.build().handler("initiator").ignore(true)));
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService()
            .pending(taskId, FlowParams.build().handler("initiator").ignore(true)));
        assertEquals(taskId, FlowEngine.taskService().getByInsId(instance.getId()).get(0).getId());
    }

    @Test
    public void initiatorCanTerminateWhileAwaitingResubmission() {
        Instance instance = returnedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        Instance ended = FlowEngine.taskService().termination(task.getId(), FlowParams.build().handler("initiator")
            .permissionFlag(java.util.Collections.singletonList("initiator")));
        assertEquals("ENDED", ended.getLifecycleState());
        assertTrue(FlowEngine.taskService().getByInsId(instance.getId()).isEmpty());
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService()
            .resubmit(instance.getId(), FlowParams.build().handler("initiator")));
    }

    @Test
    public void resubmissionRollsBackWithHostTransactionAndOnlyOneConcurrentAttemptWins() throws Exception {
        Instance instance = returnedInstance("CONTINUE_FROM_REJECTED_NODE");
        Long initiatorTask = FlowEngine.taskService().getByInsId(instance.getId()).get(0).getId();
        transactionTemplate.execute(status -> {
            FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
            status.setRollbackOnly();
            return null;
        });
        assertEquals("AWAITING_RESUBMISSION", FlowEngine.instanceService().getById(instance.getId()).getLifecycleState());
        assertEquals(initiatorTask, FlowEngine.taskService().getByInsId(instance.getId()).get(0).getId());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> submit = () -> {
            ready.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timed out");
            try {
                FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
                return true;
            } catch (IllegalStateException expected) {
                assertEquals("Instance is not awaiting resubmission", expected.getMessage());
                return false;
            }
        };
        try {
            Future<Boolean> first = executor.submit(submit);
            Future<Boolean> second = executor.submit(submit);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            release.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
        assertEquals(1, FlowEngine.taskService().getByInsId(instance.getId()).size());
    }


    private AutoCloseable observe(java.util.List<LifecycleEvent> events) {
        WorkflowLifecycleListener observer = new WorkflowLifecycleListener() {
            public DeliveryPhase getDeliveryPhase() { return DeliveryPhase.AFTER_COMMIT; }
            public void onEvent(LifecycleEvent event) { events.add(event); }
        };
        return FlowEngine.lifecycleListeners().install(java.util.Collections.singletonMap("sequence", observer));
    }

    private void assertEvents(List<LifecycleEvent> events, String... expected) {
        assertEquals(java.util.Arrays.asList(expected), events.stream().map(event -> event.getType().name())
            .collect(java.util.stream.Collectors.toList()));
        for (LifecycleEvent event : events) {
            Map<String, Object> value = FlowEngine.jsonConvert.strToMap(event.getContextJson());
            assertNotNull(event.getEventId());
            assertNotNull(event.getOperationId());
            assertTrue(event.getOccurredAt() > 0);
            assertEquals(event.getInstanceId().toString(), value.get("instanceId").toString());
            assertNotNull(value.get("definitionId"));
            assertNotNull(value.get("definitionVersion"));
            assertNotNull(value.get("source"));
            assertNotNull(value.get("state"));
            assertEquals("business-1", value.get("businessId"));
            if (event.getType().name().startsWith("NODE_")) assertNotNull(value.get("nodeExecutionId"));
        }
    }

    @Test
    public void automaticApproverPolicyPreservesLifecycleAndAssignmentOverride() throws Exception {
        for (boolean override : new boolean[] {false, true}) {
            setUp();
            Map<String, String> rule = new java.util.LinkedHashMap<>();
            rule.put("approverRule", "{\"schemaVersion\":1,\"strategyVersion\":1,\"strategy\":\"USER\","
                + "\"config\":{\"contractEmpty\":true,\"emptyPolicy\":\"SKIP\"}}");
            String ext = FlowEngine.jsonConvert.objToStr(rule);
            List<LifecycleEvent> events = new java.util.ArrayList<>();
            WorkflowLifecycleListener hook = new WorkflowLifecycleListener() {
                public void beforeAssignment(AssignmentContext assignment) {
                    if (override && "second".equals(assignment.getNodeCode())) {
                        assignment.setAssignees(java.util.Collections.singletonList("replacement"));
                    }
                }
            };
            try (AutoCloseable observer = observe(events); AutoCloseable installed = FlowEngine.lifecycleListeners().install(
                    java.util.Collections.singletonMap("policyOverride", hook))) {
                Instance instance = startedInstance("RESTART_FROM_BEGINNING", 1, ext);
                events.clear();
                Instance result = FlowEngine.taskService().skip(FlowEngine.taskService().getByInsId(instance.getId()).get(0).getId(), approval("PASS"));
                if (override) {
                    assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
                    Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
                    assertEquals("second", task.getNodeCode());
                    assertEquals("replacement", FlowEngine.userService().listByTaskIdAndTypes(task.getId()).get(0).getProcessedBy());
                } else {
                    assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED",
                        "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
                    assertEquals("SYSTEM", FlowEngine.jsonConvert.strToMap(events.get(3).getContextJson()).get("source"));
                    assertEquals("ENDED", result.getLifecycleState());
                    assertTrue(FlowEngine.taskService().getByInsId(instance.getId()).isEmpty());
                }
            }
        }
    }

    @Test
    public void serialLifecycleHasPersistentExecutionsAndCausalOrder() throws Exception {
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            Instance instance = startedInstance("RESTART_FROM_BEGINNING");
            assertEvents(events, "PROCESS_STARTED", "NODE_ENTERED", "NODE_LEFT", "NODE_ENTERED");
            Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            assertNotNull(first.getNodeExecutionId());
            events.clear();
            FlowEngine.taskService().skip(first.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
            assertTrue(events.get(1).getContextJson().contains("approver"));
            assertEquals(1, events.stream().map(LifecycleEvent::getOperationId).distinct().count());
            events.clear();
            Task second = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            assertNotEquals(first.getNodeExecutionId(), second.getNodeExecutionId());
            FlowEngine.taskService().skip(second.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
            assertEquals("ENDED", FlowEngine.instanceService().getById(instance.getId()).getLifecycleState());
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_node_execution where state='ACTIVE'", Integer.class));
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_his_task where node_execution_id is null", Integer.class));
        }
    }

    @Test
    public void returnResubmitAndWithdrawKeepDistinctExecutionIdentities() throws Exception {
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            Instance instance = startedInstance("CONTINUE_FROM_REJECTED_NODE");
            Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            events.clear();
            FlowEngine.taskService().skip(first.getId(), approval("REJECT"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
            assertTrue(events.get(1).getContextJson().contains("REJECTED"));
            events.clear();
            FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
            assertEvents(events, "PROCESS_RESUBMITTED", "NODE_LEFT", "NODE_ENTERED");
            Task again = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            assertEquals(first.getNodeCode(), again.getNodeCode());
            assertNotEquals(first.getNodeExecutionId(), again.getNodeExecutionId());
            events.clear();
            FlowEngine.taskService().revoke(instance.getId(), FlowParams.build().handler("initiator"));
            assertEvents(events, "NODE_LEFT", "PROCESS_WITHDRAWN", "NODE_ENTERED");
            assertTrue(events.get(0).getContextJson().contains("WITHDRAWN"));
            assertEquals("AWAITING_RESUBMISSION", FlowEngine.instanceService().getById(instance.getId()).getLifecycleState());
        }
    }

    @Test
    public void participantChangesAndDelegationDoNotCloseExecution() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.taskService().depute(task.getId(), approval("PASS").addHandlers(java.util.Collections.singletonList("delegate")));
            assertEvents(events, "ASSIGNEES_CHANGED");
            events.clear();
            FlowEngine.taskService().skip(task.getId(), FlowParams.build().handler("delegate").skipType("PASS")
                .permissionFlag(java.util.Collections.singletonList("delegate")));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "ASSIGNEES_CHANGED");
            assertTrue(events.get(1).getContextJson().contains("delegate"));
            assertTrue(events.get(1).getContextJson().contains("approver"));
            assertEquals(task.getNodeExecutionId(), FlowEngine.taskService().getById(task.getId()).getNodeExecutionId());
            events.clear();
            FlowEngine.taskService().transfer(task.getId(), approval("PASS").addHandlers(java.util.Collections.singletonList("replacement")));
            assertEvents(events, "ASSIGNEES_CHANGED");
        }
    }

    @Test
    public void countersignatureVoteAndUnauthorizedAttemptHaveDifferentEffects() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        jdbcTemplate.update("update flow_node set node_ratio='100' where node_code='first'");
        FlowEngine.userService().save(FlowEngine.userService().structureUser(task.getId(), "second-reviewer", "1", "initiator"));
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            org.junit.Assert.assertThrows(com.luokuiai.flovira.core.exception.FlowException.class, () -> FlowEngine.taskService()
                .skip(task.getId(), FlowParams.build().handler("intruder").skipType("PASS").permissionFlag(java.util.Collections.singletonList("intruder"))));
            assertTrue(events.isEmpty());
            FlowEngine.taskService().skip(task.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED");
            assertEquals(Boolean.FALSE, FlowEngine.jsonConvert.strToMap(events.get(0).getContextJson()).get("nodeClosed"));
            events.clear();
            org.junit.Assert.assertThrows(com.luokuiai.flovira.core.exception.FlowException.class,
                () -> FlowEngine.taskService().skip(task.getId(), approval("PASS")));
            assertTrue(events.isEmpty());
            assertEquals(task.getNodeExecutionId(), FlowEngine.taskService().getById(task.getId()).getNodeExecutionId());
        }
    }

    @Test
    public void forcedTerminationDoesNotVisitEndNodeAndOuterRollbackPublishesNothing() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            transactionTemplate.execute(status -> {
                FlowEngine.taskService().termination(task.getId(), approval("PASS"));
                status.setRollbackOnly();
                return null;
            });
            assertTrue(events.isEmpty());
            assertNotNull(FlowEngine.taskService().getById(task.getId()));
            FlowEngine.taskService().termination(task.getId(), approval("PASS"));
            assertEvents(events, "NODE_LEFT", "PROCESS_ENDED");
            assertTrue(events.get(0).getContextJson().contains("CANCELLED"));
            assertTrue(events.get(1).getContextJson().contains("TERMINATED"));
            events.clear();
            org.junit.Assert.assertThrows(com.luokuiai.flovira.core.exception.FlowException.class,
                () -> FlowEngine.taskService().termination(task.getId(), approval("PASS")));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void waitAndCarbonCopyProduceOnlyActualNodeTransitions() throws Exception {
        String wait = "{\"waitConfig\":{\"schemaVersion\":1,\"waitKey\":\"ready\"}}";
        Instance instance = startedInstance("RESTART_FROM_BEGINNING", 7, wait);
        Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        FlowEngine.taskService().skip(first.getId(), approval("PASS"));
        Task waiting = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.waitService().resumeTask(waiting.getId(), java.util.Collections.emptyMap());
            assertEvents(events, "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
            assertEquals("WAIT_RESUME", FlowEngine.jsonConvert.strToMap(events.get(0).getContextJson()).get("action"));
            assertEquals("SYSTEM", FlowEngine.jsonConvert.strToMap(events.get(0).getContextJson()).get("source"));
            events.clear();
            FlowEngine.waitService().resumeTimeoutTask(waiting.getId());
            assertTrue(events.isEmpty());
        }
        setUp();
        String copy = "{\"carbonCopyRule\":{\"schemaVersion\":1,\"strategy\":\"USER\",\"strategyVersion\":1,\"selectionType\":\"RESOURCE\",\"subjects\":[{\"id\":\"approver\",\"type\":\"USER\"}]}}";
        instance = startedInstance("RESTART_FROM_BEGINNING", 8, copy);
        first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        events.clear();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.taskService().skip(first.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED",
                "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
            assertTrue(events.get(2).getContextJson().contains("approver"));
            assertEquals("SYSTEM", FlowEngine.jsonConvert.strToMap(events.get(3).getContextJson()).get("source"));
        }
    }

    @Test
    public void timeoutApprovalAndSignerChangesUseNativeEvents() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.taskService().addSignature(task.getId(), approval("PASS").addHandlers(java.util.Collections.singletonList("extra")));
            assertEvents(events, "ASSIGNEES_CHANGED");
            events.clear();
            FlowEngine.taskService().reductionSignature(task.getId(), approval("PASS").reductionHandlers(java.util.Collections.singletonList("extra")));
            assertEvents(events, "ASSIGNEES_CHANGED");
            events.clear();
            jdbcTemplate.update("update flow_task set timeout_at=?,timeout_action='AUTO_PASS',timeout_status='PENDING' where id=?", new Date(1), task.getId());
            assertTrue(FlowEngine.timeoutService().executeTimeout(task.getId()));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
            assertEquals("SYSTEM", FlowEngine.jsonConvert.strToMap(events.get(0).getContextJson()).get("source"));
            events.clear();
            assertEquals(false, FlowEngine.timeoutService().executeTimeout(task.getId()));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void listenerFailureRollsBackExecutionAndAssignmentTogether() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        WorkflowLifecycleListener failing = new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event) {
                if (event.getType() == LifecycleEventType.NODE_LEFT) throw new IllegalStateException("observer rejected");
            }
        };
        List<LifecycleEvent> committed = new java.util.ArrayList<>();
        try (AutoCloseable observer = observe(committed); AutoCloseable failure = FlowEngine.lifecycleListeners().install(
                java.util.Collections.singletonMap("failure", failing))) {
            org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService().skip(task.getId(), approval("PASS")));
            assertTrue(committed.isEmpty());
            assertEquals(task.getNodeExecutionId(), FlowEngine.taskService().getById(task.getId()).getNodeExecutionId());
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject("select count(*) from flow_node_execution where state='ACTIVE'", Integer.class));
            assertEquals(1, FlowEngine.userService().listByTaskIdAndTypes(task.getId()).size());
        }
    }

    @Test
    public void explicitActiveMigrationHasNoHistoricalEventsAndRejectsChangedTaskSet() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        jdbcTemplate.update("delete from flow_node_execution where id=?", task.getNodeExecutionId());
        jdbcTemplate.update("update flow_task set node_execution_id=null where id=?", task.getId());
        jdbcTemplate.update("update flow_instance set lifecycle_state=null where id=?", instance.getId());
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.taskService().skip(task.getId(), approval("PASS")));
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> com.luokuiai.flovira.core.listener.lifecycle.LifecycleMigration
            .migrateActive(instance.getId(), java.util.Collections.singleton(-1L)));
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            com.luokuiai.flovira.core.listener.lifecycle.LifecycleMigration.migrateActive(instance.getId(), java.util.Collections.singleton(task.getId()));
            assertTrue(events.isEmpty());
            assertEquals("ACTIVE", FlowEngine.instanceService().getById(instance.getId()).getLifecycleState());
            assertNotEquals(task.getNodeExecutionId(), FlowEngine.taskService().getById(task.getId()).getNodeExecutionId());
            FlowEngine.taskService().skip(task.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
        }
    }

    @Test
    public void oldLifecycleSubscriptionsAreRejectedBeforeMutation() {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        jdbcTemplate.update("update flow_definition set listener_type='finish',listener_path='oldBusiness' where id=?", instance.getDefinitionId());
        IllegalArgumentException failure = org.junit.Assert.assertThrows(IllegalArgumentException.class,
            () -> FlowEngine.taskService().skip(task.getId(), approval("PASS")));
        assertTrue(failure.getMessage().contains("clear listenerType and listenerPath"));
        assertNotNull(FlowEngine.taskService().getById(task.getId()));
    }

    @Test
    public void childLifecyclesCorrelateParentAndOnlyLastChildClosesParentExecution() throws Exception {
        String config = "{\"subprocessConfig\":{\"schemaVersion\":1,\"fixedChildFlowCode\":\"child-contract\",\"completionPolicy\":\"ALL\"}}";
        Definition child = FlowEngine.newDef().setId(82000L).setFlowCode("child-contract").setFlowName("Child")
            .setBusinessType("child").setVersion("1").setPublishStatus(1).setActivityStatus(1);
        root(child);
        FlowEngine.defService().save(child);
        String[] codes = {"child-start", "child-approval", "child-end"};
        for (int i = 0; i < 3; i++) {
            com.luokuiai.flovira.core.entity.Node node = FlowEngine.newNode().setId(82100L + i).setDefinitionId(child.getId())
                .setNodeCode(codes[i]).setNodeName(codes[i]).setNodeType(i).setNodeRatio("0").setVersion("1");
            if (i == 1) node.setExt(controlExt("RESTART_FROM_BEGINNING"));
            root(node);
            FlowEngine.nodeService().save(node);
            if (i > 0) {
                com.luokuiai.flovira.core.entity.Skip edge = FlowEngine.newSkip().setId(82200L + i).setDefinitionId(child.getId())
                    .setSourceNodeCode(codes[i - 1]).setTargetNodeCode(codes[i]).setSourceNodeType(i - 1).setTargetNodeType(i).setSkipType("PASS");
                root(edge);
                FlowEngine.skipService().save(edge);
            }
        }
        Instance parent = startedInstance("RESTART_FROM_BEGINNING", 6, config);
        Task first = FlowEngine.taskService().getByInsId(parent.getId()).get(0);
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (String key : java.util.Arrays.asList("one", "two")) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("itemKey", key);
            item.put("variables", java.util.Collections.emptyMap());
            items.add(item);
        }
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.taskService().skip(first.getId(), approval("PASS").variables(java.util.Collections.singletonMap("subprocessItems", items)));
            Task subprocess = FlowEngine.taskService().getByInsId(parent.getId()).get(0);
            assertEquals(Integer.valueOf(6), subprocess.getNodeType());
            List<Long> children = jdbcTemplate.queryForList("select child_instance_id from flow_subprocess_child order by id", Long.class);
            assertEquals(2, children.size());
            List<LifecycleEvent> starts = events.stream().filter(event -> event.getType().name().equals("PROCESS_STARTED"))
                .collect(java.util.stream.Collectors.toList());
            assertEquals(2, starts.size());
            assertTrue(starts.get(0).getContextJson().contains("parentNodeExecutionId"));
            events.clear();
            Task childTask = FlowEngine.taskService().getByInsId(children.get(0)).get(0);
            FlowEngine.taskService().skip(childTask.getId(), approval("PASS"));
            assertNotNull(FlowEngine.taskService().getById(subprocess.getId()));
            assertEquals(0, events.stream().filter(event -> event.getInstanceId().equals(parent.getId())).count());
            events.clear();
            childTask = FlowEngine.taskService().getByInsId(children.get(1)).get(0);
            FlowEngine.taskService().skip(childTask.getId(), approval("PASS"));
            List<LifecycleEvent> parentEvents = events.stream().filter(event -> event.getInstanceId().equals(parent.getId()))
                .collect(java.util.stream.Collectors.toList());
            assertEvents(parentEvents, "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
            assertEquals("ENDED", FlowEngine.instanceService().getById(parent.getId()).getLifecycleState());
            Instance cancelledParent = FlowEngine.instanceService().start("business-2", FlowParams.build()
                .flowCode("resubmit-contract").handler("initiator").variables(new java.util.HashMap<>()));
            Task cancelledFirst = FlowEngine.taskService().getByInsId(cancelledParent.getId()).get(0);
            FlowEngine.taskService().skip(cancelledFirst.getId(), approval("PASS").variables(java.util.Collections.singletonMap("subprocessItems", items)));
            Task cancelledSubprocess = FlowEngine.taskService().getByInsId(cancelledParent.getId()).get(0);
            events.clear();
            FlowEngine.taskService().termination(cancelledSubprocess.getId(), approval("PASS").ignore(true));
            assertEquals(3, events.stream().filter(event -> event.getType() == LifecycleEventType.PROCESS_ENDED).count());
            assertEquals(2, events.stream().filter(event -> event.getType() == LifecycleEventType.PROCESS_ENDED
                && "CANCELLED".equals(FlowEngine.jsonConvert.strToMap(event.getContextJson()).get("reason"))).count());
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_node_execution where state='ACTIVE'", Integer.class));

        }
    }

    @Test
    public void concurrentApprovalConsumesParticipantOnlyOnce() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        jdbcTemplate.update("update flow_node set node_ratio='100' where node_code='first'");
        FlowEngine.userService().save(FlowEngine.userService().structureUser(task.getId(), "second-reviewer", "1", "initiator"));
        List<LifecycleEvent> events = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), release = new CountDownLatch(1);
        try (AutoCloseable ignored = observe(events)) {
            java.util.concurrent.Callable<Boolean> approve = () -> {
                ready.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timed out");
                try { FlowEngine.taskService().skip(task.getId(), approval("PASS")); return true; }
                catch (com.luokuiai.flovira.core.exception.FlowException consumed) { return false; }
            };
            Future<Boolean> a = executor.submit(approve), b = executor.submit(approve);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            release.countDown();
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED");
            assertEquals(1, FlowEngine.hisTaskService().listByTaskId(task.getId()).size());
            assertEquals(task.getNodeExecutionId(), FlowEngine.taskService().getById(task.getId()).getNodeExecutionId());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    public void voteThresholdClosesOnceAndRetainsRemainingAssignees() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING");
        Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        jdbcTemplate.update("update flow_node set node_ratio='60' where node_code='first'");
        for (String reviewer : java.util.Arrays.asList("second-reviewer", "third-reviewer")) {
            FlowEngine.userService().save(FlowEngine.userService().structureUser(task.getId(), reviewer, "1", "initiator"));
        }
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.taskService().skip(task.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED");
            events.clear();
            FlowEngine.taskService().skip(task.getId(), approval("PASS").handler("second-reviewer")
                .permissionFlag(java.util.Collections.singletonList("second-reviewer")));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
            Map<String, Object> left = FlowEngine.jsonConvert.strToMap(events.get(1).getContextJson());
            List<?> remaining = (List<?>) left.get("remainingAssignees");
            assertEquals(1, remaining.size());
            assertEquals("third-reviewer", ((Map<?, ?>) remaining.get(0)).get("userId"));
        }
    }

    @Test
    public void conditionalGatewayAndPreviewOnlyReportActualBusinessNodes() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING", 3, null, () -> {
            jdbcTemplate.update("update flow_skip set skip_condition='eq@@route|selected' where id=81203");
            com.luokuiai.flovira.core.entity.Node unused = FlowEngine.newNode().setId(81104L).setDefinitionId(81000L)
                .setVersion("1").setNodeCode("unused").setNodeName("unused").setNodeType(1).setNodeRatio("0")
                .setExt(controlExt("RESTART_FROM_BEGINNING"));
            root(unused); FlowEngine.nodeService().save(unused);
            com.luokuiai.flovira.core.entity.Skip fallback = FlowEngine.newSkip().setId(81204L).setDefinitionId(81000L)
                .setSourceNodeCode("second").setSourceNodeType(3).setTargetNodeCode("unused").setTargetNodeType(1).setSkipType("PASS");
            root(fallback); FlowEngine.skipService().save(fallback);
        });
        Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        Map<String, Object> variables = new java.util.HashMap<>(); variables.put("route", "selected");
        try (AutoCloseable ignored = observe(events)) {
            List<com.luokuiai.flovira.core.entity.Node> preview = FlowEngine.nodeService()
                .getNextNodeList(81000L, "first", null, "PASS", variables);
            assertEquals("end", preview.get(0).getNodeCode());
            assertTrue(events.isEmpty());
            FlowEngine.taskService().skip(first.getId(), approval("PASS").variables(variables));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                "select count(*) from flow_node_execution where node_code in ('second','unused')", Integer.class));
        }
    }

    @Test
    public void parallelBranchesHaveSeparateExecutionsAndWithdrawTogether() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING", 4, null, () -> {
            jdbcTemplate.update("update flow_skip set target_node_code='left',target_node_type=1 where id=81203");
            for (int i = 0; i < 2; i++) {
                String code = i == 0 ? "left" : "right";
                com.luokuiai.flovira.core.entity.Node node = FlowEngine.newNode().setId(81104L + i).setDefinitionId(81000L)
                    .setNodeCode(code).setNodeName(code).setNodeType(1).setNodeRatio("0").setVersion("1")
                    .setExt(controlExt("RESTART_FROM_BEGINNING"));
                root(node); FlowEngine.nodeService().save(node);
                com.luokuiai.flovira.core.entity.Skip edge = FlowEngine.newSkip().setId(81205L + i).setDefinitionId(81000L)
                    .setSourceNodeCode(code).setSourceNodeType(1).setTargetNodeCode("end").setTargetNodeType(2).setSkipType("PASS");
                root(edge); FlowEngine.skipService().save(edge);
            }
            com.luokuiai.flovira.core.entity.Skip fork = FlowEngine.newSkip().setId(81204L).setDefinitionId(81000L)
                .setSourceNodeCode("second").setSourceNodeType(4).setTargetNodeCode("right").setTargetNodeType(1).setSkipType("PASS");
            root(fork); FlowEngine.skipService().save(fork);
        });
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            FlowEngine.taskService().skip(first.getId(), approval("PASS"));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED", "NODE_ENTERED");
            List<Task> branches = FlowEngine.taskService().getByInsId(instance.getId());
            assertEquals(2, branches.size());
            assertNotEquals(branches.get(0).getNodeExecutionId(), branches.get(1).getNodeExecutionId());
            events.clear();
            FlowEngine.taskService().revoke(instance.getId(), FlowParams.build().handler("initiator"));
            assertEvents(events, "NODE_LEFT", "NODE_LEFT", "PROCESS_WITHDRAWN", "NODE_ENTERED");
            for (int i = 0; i < 2; i++) assertEquals("WITHDRAWN", FlowEngine.jsonConvert.strToMap(events.get(i).getContextJson()).get("reason"));
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject("select count(*) from flow_node_execution where state='ACTIVE'", Integer.class));
        }
    }

    @Test
    public void backwardRouteReentersApprovalWithoutReplayingStart() throws Exception {
        Instance instance = startedInstance("RESTART_FROM_BEGINNING", 1,
            controlExt("RESTART_FROM_BEGINNING").replace("TO_INITIATOR", "TO_PREVIOUS"));
        Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        FlowEngine.taskService().skip(first.getId(), approval("PASS"));
        Task second = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable ignored = observe(events)) {
            FlowEngine.taskService().skip(second.getId(), approval("REJECT").nodeCode("first").ignore(true));
            assertEvents(events, "APPROVAL_ACTION_COMPLETED", "NODE_LEFT", "NODE_ENTERED");
            Task again = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            assertEquals("first", again.getNodeCode());
            assertNotEquals(first.getNodeExecutionId(), again.getNodeExecutionId());
        }
    }

    @Test
    public void nativeWaitSignalAndTimeoutRaceEmitOnlyOneCompletion() throws Exception {
        String wait = "{\"waitConfig\":{\"schemaVersion\":1,\"waitKey\":\"ready\"}}";
        Instance instance = startedInstance("RESTART_FROM_BEGINNING", 7, wait);
        FlowEngine.taskService().skip(FlowEngine.taskService().getByInsId(instance.getId()).get(0).getId(), approval("PASS"));
        Task waiting = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        List<LifecycleEvent> events = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), release = new CountDownLatch(1);
        try (AutoCloseable ignored = observe(events)) {
            Future<?> a = executor.submit(() -> {
                ready.countDown(); await(release);
                return FlowEngine.waitService().resumeTask(waiting.getId(), java.util.Collections.emptyMap());
            });
            Future<?> b = executor.submit(() -> {
                ready.countDown(); await(release);
                return FlowEngine.waitService().resumeTimeoutTask(waiting.getId());
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS)); release.countDown();
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
            assertEvents(events, "NODE_LEFT", "NODE_ENTERED", "NODE_LEFT", "PROCESS_ENDED");
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timed out"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }

    @Test
    public void beforeOperationRunsOncePerAuthorizedNativeEntry() throws Exception {
        List<String> calls = new java.util.ArrayList<>();
        WorkflowLifecycleListener hook = new WorkflowLifecycleListener() {
            public void beforeOperation(OperationContext operation) {
                assertNotNull(operation.getInstanceId());
                calls.add(operation.getAction());
            }
        };
        try (AutoCloseable installed = FlowEngine.lifecycleListeners().install(java.util.Collections.singletonMap("hook", hook))) {
            Instance instance = startedInstance("RESTART_FROM_BEGINNING");
            assertEquals(java.util.Collections.singletonList("START"), calls);
            Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            org.junit.Assert.assertThrows(com.luokuiai.flovira.core.exception.FlowException.class, () -> FlowEngine.taskService()
                .skip(first.getId(), approval("PASS").handler("intruder").permissionFlag(java.util.Collections.singletonList("intruder"))));
            assertEquals(1, calls.size());
            FlowEngine.taskService().depute(first.getId(), approval("PASS").addHandlers(java.util.Collections.singletonList("delegate")));
            assertEquals(2, calls.size());
            FlowEngine.taskService().skip(first.getId(), approval("PASS").handler("delegate")
                .permissionFlag(java.util.Collections.singletonList("delegate")));
            assertEquals(3, calls.size());
            FlowEngine.taskService().revoke(instance.getId(), FlowParams.build().handler("initiator"));
            assertEquals(4, calls.size());
            FlowEngine.taskService().resubmit(instance.getId(), FlowParams.build().handler("initiator"));
            assertEquals(5, calls.size());
            Task again = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            jdbcTemplate.update("update flow_task set timeout_at=?,timeout_action='AUTO_PASS',timeout_status='PENDING' where id=?", new Date(1), again.getId());
            FlowEngine.timeoutService().executeTimeout(again.getId());
            assertEquals(6, calls.size());
            assertEquals("APPROVAL_TIMEOUT", calls.get(5));
        }
    }

    @Test
    public void deferredEventsKeepCreationTimeSnapshotsAfterCallerMutation() throws Exception {
        Map<String, Object> nested = new java.util.LinkedHashMap<>(); nested.put("amount", 10);
        WorkflowLifecycleListener hook = new WorkflowLifecycleListener() {
            public void beforeOperation(OperationContext operation) { operation.setVariable("invoice", nested); }
        };
        List<LifecycleEvent> events = new java.util.ArrayList<>();
        try (AutoCloseable observer = observe(events); AutoCloseable installed = FlowEngine.lifecycleListeners().install(
                java.util.Collections.singletonMap("snapshot", hook))) {
            transactionTemplate.execute(status -> {
                startedInstance("RESTART_FROM_BEGINNING");
                nested.put("amount", 999);
                assertTrue(events.isEmpty());
                return null;
            });
            assertEvents(events, "PROCESS_STARTED", "NODE_ENTERED", "NODE_LEFT", "NODE_ENTERED");
            for (LifecycleEvent event : events) {
                Map<?, ?> variables = (Map<?, ?>) FlowEngine.jsonConvert.strToMap(event.getContextJson()).get("variables");
                assertEquals("10", ((Map<?, ?>) variables.get("invoice")).get("amount").toString());
            }
        }
    }

    private Instance returnedInstance(String strategy) {
        Instance instance = startedInstance(strategy);
        Task first = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        FlowEngine.taskService().skip(first.getId(), approval("PASS"));
        Task second = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
        assertEquals("second", second.getNodeCode());
        return FlowEngine.taskService().skip(second.getId(), approval("REJECT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void trustedInitiationContextSurvivesLaterActorsAndForgedRequestVariables() throws Exception {
        List<OperationContext> calls = new java.util.ArrayList<>();
        WorkflowLifecycleListener contextValidator = new WorkflowLifecycleListener() {
            public void beforeOperation(OperationContext operation) {
                calls.add(operation);
                assertEquals("tenant-a", operation.getDefinition().getTenantId());
                assertEquals(Long.valueOf(81000L), operation.getDefinition().getId());
                assertEquals("resubmit-contract", operation.getDefinition().getFlowCode());
                assertEquals("1", operation.getDefinition().getVersion());
                assertEquals("initiator", operation.getInitiatorId());
                Map<String, Object> trusted;
                assertNotNull(operation.getInstance());
                if (operation.isNewInstance()) {
                    assertEquals("START", operation.getAction());
                    assertEquals("initiator", operation.getActor());
                    assertTrue(operation.getPersistedVariables().isEmpty());
                    assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_instance", Integer.class));
                    trusted = new java.util.LinkedHashMap<>();
                    trusted.put("tenantId", operation.getDefinition().getTenantId());
                    trusted.put("initiatorId", operation.getInitiatorId());
                    trusted.put("organizationId", "department-a");
                    trusted.put("companyId", "company-a");
                    operation.setVariable("historyOnly", true);
                } else {
                    assertEquals("approver", operation.getActor());
                    assertEquals("initiator", operation.getInstance().getCreatedBy());
                    assertEquals(operation.getDefinition().getTenantId(), operation.getInstance().getTenantId());
                    assertTrue(operation.getPersistedVariables().containsKey("historyOnly"));
                    assertTrue(!operation.getInputVariables().containsKey("historyOnly"));
                    assertEquals("attacker", ((Map<?, ?>) operation.getInputVariables().get("host.approverContext")).get("initiatorId"));
                    trusted = (Map<String, Object>) operation.getPersistedVariables().get("host.approverContext");
                    assertEquals("department-a", trusted.get("organizationId"));
                    assertEquals("company-a", trusted.get("companyId"));
                    assertEquals("initiator", trusted.get("initiatorId"));
                    org.junit.Assert.assertThrows(UnsupportedOperationException.class, () ->
                        operation.getPersistedVariables().put("host.approverContext", "forged"));
                    org.junit.Assert.assertThrows(UnsupportedOperationException.class, () -> trusted.put("initiatorId", "forged"));
                }
                operation.setVariable("host.approverContext", trusted);
                operation.setVariable("workflowOrganizationId", trusted.get("organizationId"));
            }
        };
        try (AutoCloseable installed = FlowEngine.lifecycleListeners().install(java.util.Collections.singletonMap("contextValidator", contextValidator))) {
            Instance instance = startedInstance("RESTART_FROM_BEGINNING", 1, controlExt("RESTART_FROM_BEGINNING"), () -> {
                com.luokuiai.flovira.core.entity.Node start = FlowEngine.nodeService().getByDefIdAndNodeCode(81000L, "start");
                start.setExt(submitterExt("initiator", true));
                FlowEngine.nodeService().updateById(start);
                for (String code : java.util.Arrays.asList("first", "second")) {
                    com.luokuiai.flovira.core.entity.Node node = FlowEngine.nodeService().getByDefIdAndNodeCode(81000L, code);
                    node.setExt(node.getExt().replace("\\\"strategy\\\":\\\"USER\\\"", "\\\"strategy\\\":\\\"USER\\\",\\\"config\\\":{\\\"requireTrustedContext\\\":true}"));
                    FlowEngine.nodeService().updateById(node);
                }
            });
            Map<String, Object> forged = new java.util.LinkedHashMap<>();
            forged.put("host.approverContext", java.util.Collections.singletonMap("initiatorId", "attacker"));
            forged.put("workflowOrganizationId", "department-b");
            forged.put("tenantId", "tenant-b");
            Task task = FlowEngine.taskService().getByInsId(instance.getId()).get(0);
            FlowEngine.taskService().skip(task.getId(), approval("PASS").variables(forged));
            Instance saved = FlowEngine.instanceService().getById(instance.getId());
            assertEquals("initiator", saved.getCreatedBy());
            assertEquals("department-a", saved.getVariableMap().get("workflowOrganizationId"));
            Map<String, Object> snapshot = (Map<String, Object>) saved.getVariableMap().get("host.approverContext");
            assertEquals("initiator", snapshot.get("initiatorId"));
            assertEquals("company-a", snapshot.get("companyId"));
            assertEquals(2, calls.size());
        }
    }

    @Test
    public void restrictedSubmissionRejectsBothStartEntrypointsWithoutWorkflowWrites() {
        IllegalStateException rejected = org.junit.Assert.assertThrows(IllegalStateException.class, () ->
            startedInstance("RESTART_FROM_BEGINNING", 1, controlExt("RESTART_FROM_BEGINNING"), () -> {
                com.luokuiai.flovira.core.entity.Node start = FlowEngine.nodeService().getByDefIdAndNodeCode(81000L, "start");
                start.setExt(submitterExt("someone-else", false));
                FlowEngine.nodeService().updateById(start);
            }));
        assertEquals("User is not allowed to submit this workflow", rejected.getMessage());
        org.junit.Assert.assertThrows(IllegalStateException.class, () -> FlowEngine.instanceService()
            .startByDefinitionId("business-2", 81000L, FlowParams.build().handler("initiator")));
        for (String table : java.util.Arrays.asList("flow_instance", "flow_task", "flow_his_task", "flow_node_execution")) {
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class));
        }
    }

    private String submitterExt(String user, boolean trusted) {
        com.luokuiai.flovira.core.dto.ApproverRule rule = new com.luokuiai.flovira.core.dto.ApproverRule()
            .setStrategy("USER").setSelectionType("RESOURCE")
            .setSubjects(java.util.Collections.singletonList(new com.luokuiai.flovira.core.dto.BusinessSubject().setId(user).setType("USER")))
            .setConfig(java.util.Collections.<String, Object>singletonMap("requireTrustedContext", trusted));
        Map<String, String> ext = new java.util.LinkedHashMap<>();
        ext.put("submitterRule", FlowEngine.jsonConvert.objToStr(rule));
        return FlowEngine.jsonConvert.objToStr(ext);
    }

    private Instance startedInstance(String strategy) {
        return startedInstance(strategy, 1, controlExt(strategy));
    }

    private Instance startedInstance(String strategy, int secondType, String secondExt) {
        return startedInstance(strategy, secondType, secondExt, () -> { });
    }

    private Instance startedInstance(String strategy, int secondType, String secondExt, Runnable configure) {
        Definition definition = FlowEngine.newDef().setId(81000L).setFlowCode("resubmit-contract")
            .setFlowName("Resubmission contract").setBusinessType("contract").setVersion("1")
            .setPublishStatus(1).setActivityStatus(1);
        root(definition);
        FlowEngine.defService().save(definition);
        String[] codes = {"start", "first", "second", "end"};
        int[] types = {0, 1, secondType, 2};
        for (int i = 0; i < codes.length; i++) {
            com.luokuiai.flovira.core.entity.Node node = FlowEngine.newNode().setId(81100L + i)
                .setDefinitionId(81000L).setVersion("1").setNodeCode(codes[i]).setNodeName(codes[i])
                .setNodeType(types[i]).setNodeRatio("0");
            if (types[i] == 1) node.setExt(controlExt(strategy));
            if (i == 2) node.setExt(secondExt);
            root(node);
            FlowEngine.nodeService().save(node);
            if (i > 0) {
                com.luokuiai.flovira.core.entity.Skip skip = FlowEngine.newSkip().setId(81200L + i)
                    .setDefinitionId(81000L).setSourceNodeCode(codes[i - 1]).setTargetNodeCode(codes[i])
                    .setSourceNodeType(types[i - 1]).setTargetNodeType(types[i]).setSkipType("PASS");
                root(skip);
                FlowEngine.skipService().save(skip);
            }
        }
        configure.run();
        return FlowEngine.instanceService().start("business-1",
            FlowParams.build().flowCode("resubmit-contract").handler("initiator").variables(new java.util.HashMap<>()));
    }

    private FlowParams approval(String action) {
        return FlowParams.build().handler("approver").permissionFlag(java.util.Collections.singletonList("approver"))
            .skipType(action).variables(new java.util.HashMap<>());
    }

    private String controlExt(String strategy) {
        java.util.Map<String, String> control = new java.util.LinkedHashMap<>();
        control.put("nodeControlConfig", "{\"schemaVersion\":1,\"allowRollback\":true,\"rejectStrategy\":\"TO_INITIATOR\",\"resubmitStrategy\":\"" + strategy + "\"}");
        java.util.Map<String, String> rule = new java.util.LinkedHashMap<>();
        rule.put("approverRule", "{\"schemaVersion\":1,\"strategyVersion\":1,\"strategy\":\"USER\"}");
        control.putAll(rule);
        return FlowEngine.jsonConvert.objToStr(control);
    }

    @Test
    public void lifecycleDeliveryWaitsForOuterCommitAndRejectsMissingTransaction() {
        TransactionExecutor transactions = context.getBean(TransactionExecutor.class);
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        java.util.List<String> calls = new java.util.ArrayList<>();
        registry.register("observer", new WorkflowLifecycleListener() {
            public DeliveryPhase getDeliveryPhase() { return DeliveryPhase.AFTER_COMMIT; }
            public void onEvent(LifecycleEvent event) { calls.add("committed"); }
        });
        LifecycleDispatcher dispatcher = new LifecycleDispatcher(registry, (code, event, failure) -> fail());
        LifecycleEvent event = new LifecycleEvent("event", "operation", LifecycleEventType.NODE_ENTERED,
            1L, System.currentTimeMillis(), "{}");
        try {
            dispatcher.emit(event, transactions);
            fail("Delivery outside a transaction must fail");
        } catch (IllegalStateException expected) {
            assertTrue(calls.isEmpty());
        }
        transactionTemplate.execute(status -> {
            transactions.execute(() -> { dispatcher.emit(event, transactions); return null; });
            assertTrue(calls.isEmpty());
            status.setRollbackOnly();
            return null;
        });
        assertTrue(calls.isEmpty());
        transactionTemplate.execute(status -> {
            transactions.execute(() -> { dispatcher.emit(event, transactions); return null; });
            assertTrue(calls.isEmpty());
            return null;
        });
        assertEquals(java.util.Collections.singletonList("committed"), calls);
    }

    @Test
    public void lifecycleSynchronousFailureRollsBackDatabaseMutation() {
        TransactionExecutor transactions = context.getBean(TransactionExecutor.class);
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        registry.register("reject", new WorkflowLifecycleListener() {
            public void onEvent(LifecycleEvent event) {
                throw new IllegalStateException("business rejection");
            }
        });
        LifecycleDispatcher dispatcher = new LifecycleDispatcher(registry, (code, event, failure) -> fail());
        try {
            transactions.execute(() -> {
                jdbcTemplate.update("insert into flow_instance(id, definition_id, business_type, business_id, "
                    + "node_type, node_code, flow_status, lifecycle_state) values (?, ?, ?, ?, ?, ?, ?, ?)",
                    98001L, 98000L, "contract", "business-1", 1, "approval", "1", "ACTIVE");
                dispatcher.emit(new LifecycleEvent("event", "operation", LifecycleEventType.NODE_ENTERED,
                    98001L, System.currentTimeMillis(), "{}"), transactions);
                return null;
            });
            fail("Synchronous listener failure must abort the transaction");
        } catch (IllegalStateException expected) {
            assertEquals("business rejection", expected.getMessage());
        }
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
            "select count(*) from flow_instance where id = 98001", Integer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nodeExecutionClosesOnceAndHonorsTenantDeletionAndRollback() throws Exception {
        com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao<com.luokuiai.flovira.core.entity.NodeExecution> dao =
            context.getBean(com.luokuiai.flovira.core.orm.dao.FlowNodeExecutionDao.class);
        com.luokuiai.flovira.core.entity.NodeExecution execution = FlowEngine.newNodeExecution();
        execution.setId(90001L);
        execution.setTenantId("tenant-a");
        execution.setDeleted("0");
        execution.setInstanceId(900L).setDefinitionId(901L).setNodeCode("approve").setNodeType(1)
            .setState("ACTIVE").setVersion(0).setEnteredAt(new Date());
        assertEquals(1, dao.save(execution));
        assertNull(dao.get("tenant-b", 90001L));
        assertEquals(0, dao.close("tenant-b", 90001L, 0, "COMPLETED", new Date()));
        assertEquals(1, dao.listActive("tenant-a", 900L).size());
        transactionTemplate.execute(status -> {
            assertEquals(1, dao.close("tenant-a", 90001L, 0, "COMPLETED", new Date()));
            status.setRollbackOnly();
            return null;
        });
        assertEquals("ACTIVE", dao.get("tenant-a", 90001L).getState());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        java.util.concurrent.Callable<Integer> close = () -> {
            ready.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timed out");
            return transactionTemplate.execute(status -> dao.close("tenant-a", 90001L, 0, "COMPLETED", new Date()));
        };
        try {
            Future<Integer> first = executor.submit(close);
            Future<Integer> second = executor.submit(close);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            release.countDown();
            assertEquals(1, first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
        assertEquals(Integer.valueOf(1), dao.get("tenant-a", 90001L).getVersion());
        assertTrue(dao.listActive("tenant-a", 900L).isEmpty());
        jdbcTemplate.update("update flow_node_execution set deleted='1' where id=90001");
        assertNull(dao.get("tenant-a", 90001L));
        assertEquals(0, dao.close("tenant-a", 90001L, 1, "CANCELLED", new Date()));
    }

    @Test
    public void shouldRoundTripWorkflowPackageWithFormsAndSubprocesses() throws Exception {
        WorkflowPackage input = workflowPackage();
        WorkflowImportResult imported = FlowEngine.defService().importPackage(input, java.util.Collections.emptyMap());
        assertEquals(2, imported.getDefinitionIds().size());
        assertEquals(1, imported.getFormReferences().size());
        assertNotEquals(Long.valueOf(999999L), imported.getRootDefinitionId());
        String newFormId = imported.getFormReferences().get("source-form-1");
        assertNotEquals("source-form-1", newFormId);
        Definition parent = FlowEngine.defService().getAllDataDefinition(imported.getRootDefinitionId());
        assertEquals(newFormId, parent.getFormId());
        // 节点不再保存表单覆盖，导入后仅流程定义持有表单引用。
        assertNull(parent.getNodeList().stream().filter(n -> "sub".equals(n.getNodeCode()))
            .findFirst().get().getFormId());
        assertEquals(Integer.valueOf(0), parent.getPublishStatus());
        assertEquals("1", parent.getVersion());
        assertEquals("tenant-a", parent.getTenantId());
        assertEquals("source-form-1", input.getDefinitions().get(1).getFormId());
        assertEquals(Long.valueOf(999999L), input.getDefinitions().get(1).getId());
        Form form = FlowEngine.formService().getById(Long.valueOf(newFormId));
        assertEquals(input.getForms().get(0).getFormContent(), form.getFormContent());
        assertEquals(Integer.valueOf(0), form.getPublishStatus());

        // 固定子流程按已发布版本解析，先发布依赖，再导出父流程。
        assertTrue(FlowEngine.defService().publish(imported.getDefinitionIds().get("package_child")));
        WorkflowPackage exported = FlowEngine.defService().exportPackage(imported.getRootDefinitionId());
        assertEquals(2, exported.getDefinitions().size());
        assertEquals(1, exported.getForms().size());
        assertTrue(exported.getExternalFormIds().isEmpty());
        assertNull(exported.getDefinitions().get(1).getId());
        assertEquals("1", exported.getForms().get(0).getVersion());
        assertEquals(form.getFormContent(), exported.getForms().get(0).getFormContent());
        WorkflowPackage decoded = FlowEngine.jsonConvert.strToBean(
            FlowEngine.jsonConvert.objToStr(exported), WorkflowPackage.class);
        WorkflowImportResult second = FlowEngine.defService().importPackage(decoded, java.util.Collections.emptyMap());
        assertNotEquals(imported.getRootDefinitionId(), second.getRootDefinitionId());
        assertEquals("2", FlowEngine.defService().getById(second.getRootDefinitionId()).getVersion());
        assertNotEquals(newFormId, second.getFormReferences().get(newFormId));
    }

    @Test
    public void shouldRequireExplicitExternalFormMappings() throws Exception {
        WorkflowPackage input = workflowPackage();
        input.getDefinitions().get(1).setFormId("host:purchase");
        input.getExternalFormIds().add("host:purchase");
        try {
            FlowEngine.defService().importPackage(input, java.util.Collections.emptyMap());
            fail("Missing external mapping must fail");
        } catch (FlowException expected) {
            assertTrue(expected.getMessage().contains("外部表单映射"));
        }
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_form", Integer.class));
        WorkflowImportResult result = FlowEngine.defService().importPackage(input,
            java.util.Collections.singletonMap("host:purchase", "target:purchase"));
        assertEquals("target:purchase", FlowEngine.defService().getById(result.getRootDefinitionId()).getFormId());
    }

    @Test
    public void shouldRollbackFormsAndDefinitionsWhenNodePersistenceFails() throws Exception {
        WorkflowPackage input = workflowPackage();
        input.getDefinitions().get(1).getNodeList().get(0).setNodeName(repeat('x', 101));
        try {
            FlowEngine.defService().importPackage(input, java.util.Collections.emptyMap());
            fail("Oversized node name must fail in database");
        } catch (org.springframework.dao.DataIntegrityViolationException expected) {
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_form", Integer.class));
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_definition", Integer.class));
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_node", Integer.class));
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_skip", Integer.class));
        }
    }

    @Test
    public void shouldRejectIncompletePackageBeforeWriting() throws Exception {
        WorkflowPackage input = workflowPackage();
        input.getDefinitions().remove(0);
        try {
            FlowEngine.defService().importPackage(input, java.util.Collections.emptyMap());
            fail("Missing subprocess must fail");
        } catch (FlowException expected) {
            assertTrue(expected.getMessage().contains("子流程"));
        }
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject("select count(*) from flow_form", Integer.class));
    }

    @Test
    public void shouldNotExportAnotherTenantsDefinition() throws Exception {
        WorkflowImportResult result = FlowEngine.defService().importPackage(workflowPackage(), java.util.Collections.emptyMap());
        jdbcTemplate.update("update flow_definition set tenant_id = 'tenant-b' where id = ?", result.getRootDefinitionId());
        try {
            FlowEngine.defService().exportPackage(result.getRootDefinitionId());
            fail("Other tenant definition must be hidden");
        } catch (FlowException expected) {
            assertTrue(expected.getMessage().contains("不存在"));
        }
    }

    private WorkflowPackage workflowPackage() throws Exception {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
            getClass().getResourceAsStream("/workflow-package.json"), java.nio.charset.StandardCharsets.UTF_8))) {
            return FlowEngine.jsonConvert.strToBean(reader.lines().collect(java.util.stream.Collectors.joining("\n")),
                WorkflowPackage.class);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPersist128CharacterInstanceBusinessType() {
        FlowInstanceDao<Instance> dao = (FlowInstanceDao<Instance>) context.getBean(FlowInstanceDao.class);
        String businessType = repeat('b', 128);
        Instance instance = FlowEngine.newIns().setId(70L).setDefinitionId(60L)
            .setBusinessType(businessType).setBusinessId("1001").setNodeType(1)
            .setNodeCode("APPROVE").setNodeName("Approve").setFlowStatus("1").setActivityStatus(1);
        root(instance);
        assertEquals(1, dao.save(instance));
        assertEquals(businessType, dao.selectById(70L).getBusinessType());
    }

    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void shouldRejectNullDefinitionBusinessTypeInDatabase() {
        jdbcTemplate.update("insert into flow_definition (id, flow_code, flow_name, version) "
            + "values (80, 'missing-type', 'Missing type', '1')");
    }

    @Test
    public void shouldPersistAndCopyDefinitionBusinessType() {
        String businessType = repeat('a', 128);
        Definition definition = FlowEngine.newDef().setId(60L).setFlowCode("purchase")
            .setFlowName("Purchase").setVersion("1").setBusinessType(businessType)
            .setPublishStatus(0).setActivityStatus(1);
        root(definition);
        assertEquals(1, definitionDao.save(definition));
        Definition stored = definitionDao.selectById(60L);
        assertEquals(businessType, stored.getBusinessType());
        assertEquals(businessType, stored.copy().getBusinessType());
        assertEquals(businessType, DefJson.copyDef(DefJson.copyDef(stored)).getBusinessType());

        stored.setBusinessType("EXPENSE");
        definitionDao.updateById(stored);
        assertEquals("EXPENSE", definitionDao.selectById(60L).getBusinessType());
        assertEquals(1, definitionDao.selectList(FlowEngine.newDef().setBusinessType("EXPENSE"), null).size());
        assertEquals(0, definitionDao.selectList(FlowEngine.newDef().setBusinessType(businessType), null).size());

        Definition copy = stored.copy().setId(61L).setVersion("2").setPublishStatus(0).setActivityStatus(1);
        root(copy);
        definitionDao.saveBatch(java.util.Collections.singletonList(copy));
        assertEquals("EXPENSE", definitionDao.selectById(61L).getBusinessType());
    }

    @Test
    public void shouldPersistLockClaimAndIsolateRunByTenant() {
        SubprocessRun run = run(1L, 10L, SubprocessRunStatus.READY_TO_RESUME.name());
        assertEquals(1, runDao.save(run));
        assertNotNull(runDao.findByParentTask("tenant-a", 10L));
        assertNull(runDao.findByParentTask("tenant-b", 10L));
        SubprocessRun locked = transactionTemplate.execute(status -> runDao.lockById("tenant-a", 1L));
        assertNotNull(locked);
        assertEquals(1, runDao.claimReadyToResume("tenant-a", 1L));
        assertEquals(0, runDao.claimReadyToResume("tenant-a", 1L));
        assertEquals(SubprocessRunStatus.RESUMING.name(), runDao.findById("tenant-a", 1L).getRunStatus());

        run.setRunStatus(SubprocessRunStatus.RUNNING.name());
        runDao.updateById(run);
        assertEquals(1, runDao.lockActiveByParent("tenant-a", 100L).size());
        assertEquals(1, runDao.findReconcileCandidates(1).size());
    }

    @Test
    public void shouldPersistPageAndOrderChildrenAndEvents() {
        runDao.save(run(2L, 20L, SubprocessRunStatus.RUNNING.name()));
        childDao.save(child(11L, 2L, "b", 211L));
        childDao.save(child(10L, 2L, "a", 210L));
        childDao.save(child(12L, 2L, "c", 212L));

        assertNotNull(childDao.findByRunAndItem("tenant-a", 2L, "a"));
        assertNotNull(childDao.findByChildInstanceId("tenant-a", 210L));
        assertNull(childDao.findByChildInstanceId("tenant-b", 210L));
        List<SubprocessChild> locked = transactionTemplate.execute(
            status -> childDao.lockByRunId("tenant-a", 2L));
        assertEquals(Long.valueOf(10L), locked.get(0).getId());
        Page<SubprocessChild> page = childDao.pageByRunId("tenant-a", 2L, new Page<SubprocessChild>(1, 2));
        assertEquals(3L, page.getTotal());
        assertEquals(2, page.getList().size());

        eventDao.save(event(31L, 2L, new Date(3000L)));
        eventDao.save(event(30L, 2L, new Date(1000L)));
        List<SubprocessEvent> events = eventDao.listByRunId("tenant-a", 2L);
        assertEquals(Long.valueOf(30L), events.get(0).getId());
        assertEquals(Long.valueOf(31L), events.get(1).getId());
    }

    @Test
    public void shouldPersistScanClaimRecoverAndReleaseTimeoutTask() {
        Task task = FlowEngine.newTask();
        task.setId(40L).setDefinitionId(1000L).setInstanceId(100L).setNodeCode("APPROVE")
            .setNodeName("Approve").setNodeType(1).setFlowStatus("1").setTimeoutAt(new Date(1000L))
            .setTimeoutAction("AUTO_PASS").setTimeoutConfig("{\"schemaVersion\":1}")
            .setTimeoutStatus("PENDING").setFormId("expense:v2");
        root(task);
        assertEquals(1, taskDao.save(task));

        List<Task> due = taskDao.listDueTimeoutTasks(new Date(2000L), new Date(0L), 10);
        assertEquals(1, due.size());
        assertEquals("AUTO_PASS", due.get(0).getTimeoutAction());
        assertEquals("expense:v2", due.get(0).getFormId());
        assertEquals(1, taskDao.claimTimeout(40L, new Date(3000L), new Date(2500L)));
        assertEquals(0, taskDao.claimTimeout(40L, new Date(3500L), new Date(2500L)));
        assertEquals(1, taskDao.claimTimeout(40L, new Date(5000L), new Date(4000L)));
        assertEquals(1, taskDao.releaseTimeout(40L));

        Task released = taskDao.selectById(40L);
        assertEquals("PENDING", released.getTimeoutStatus());
        assertNull(released.getTimeoutClaimedAt());

        Task wait = FlowEngine.newTask();
        wait.setId(41L).setDefinitionId(1000L).setInstanceId(100L).setNodeCode("WAIT")
            .setNodeName("Wait").setNodeType(7).setFlowStatus("1");
        root(wait);
        assertEquals(1, taskDao.save(wait));
        assertEquals(1, taskDao.claimWait(41L, new Date(6000L)));
        assertEquals(0, taskDao.claimWait(41L, new Date(7000L)));
    }

    @Test
    public void shouldNotEnableSchedulingWhenTimeoutsAreEnabled() {
        assertTrue(FlowEngine.getFlowConfig().getTimeout().isEnabled());
        assertTrue(context.getBeansOfType(
            org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class).isEmpty());
        assertNotNull(FlowEngine.timeoutService());
    }

    @Test
    public void shouldRollbackTimeoutClaimAndTransitionTogether() {
        timeoutTask();
        boolean enabled = FlowEngine.getFlowConfig().getTimeout().isEnabled();
        FlowEngine.getFlowConfig().getTimeout().setEnabled(true);
        try {
            installTimeoutTransition(() -> {
                jdbcTemplate.update("delete from flow_task where id = 42");
                throw new IllegalStateException("transition failed");
            });
            TimeoutServiceImpl service = new TimeoutServiceImpl();
            try {
                service.executeTimeout(42L);
                fail("transition must fail");
            } catch (IllegalStateException expected) {
                assertEquals("transition failed", expected.getMessage());
            }
            assertEquals("PENDING", taskDao.selectById(42L).getTimeoutStatus());
            assertNull(taskDao.selectById(42L).getTimeoutClaimedAt());
            installTimeoutTransition(() -> jdbcTemplate.update("delete from flow_task where id = 42"));
            assertTrue(service.executeTimeout(42L));
            assertNull(taskDao.selectById(42L));
            org.junit.Assert.assertFalse(service.executeTimeout(42L));
        } finally {
            FrameInvoker.setBeanFunction(context::getBean);
            FlowEngine.getFlowConfig().getTimeout().setEnabled(enabled);
        }
    }

    @Test
    public void shouldHoldTimeoutClaimUntilTransitionCommits() throws Exception {
        timeoutTask();
        boolean enabled = FlowEngine.getFlowConfig().getTimeout().isEnabled();
        FlowEngine.getFlowConfig().getTimeout().setEnabled(true);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch competitor = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            installTimeoutTransition(() -> {
                entered.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                jdbcTemplate.update("delete from flow_task where id = 42");
            });
            TimeoutServiceImpl service = new TimeoutServiceImpl();
            Future<Boolean> first = workers.submit(() -> service.executeTimeout(42L));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            // 即使扫描时间已超过旧租约，也不能在执行事务提交前偷走抢占。
            Future<Integer> second = workers.submit(() -> {
                competitor.countDown();
                return service.executeDue(new Date(System.currentTimeMillis() + 600000L), 10).getSucceeded();
            });
            assertTrue(competitor.await(10, TimeUnit.SECONDS));
            try {
                second.get(200, TimeUnit.MILLISECONDS);
                fail("competing timeout must wait for the task row lock");
            } catch (TimeoutException expected) {
                // 第一执行者仍持有行锁。
            }
            release.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(0), second.get(10, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            FrameInvoker.setBeanFunction(context::getBean);
            FlowEngine.getFlowConfig().getTimeout().setEnabled(enabled);
        }
    }

    private void timeoutTask() {
        Instance instance = FlowEngine.newIns().setId(100L).setDefinitionId(1000L).setBusinessType("contract").setBusinessId("timeout")
            .setNodeType(1).setNodeCode("APPROVE").setFlowStatus("1");
        root(instance);
        FlowEngine.instanceService().save(instance);
        Task task = FlowEngine.newTask().setId(42L).setDefinitionId(1000L).setInstanceId(100L)
            .setNodeCode("APPROVE").setNodeName("Approve").setNodeType(1).setFlowStatus("1")
            .setTimeoutAt(new Date(1L)).setTimeoutAction("AUTO_PASS").setTimeoutStatus("PENDING");
        root(task);
        taskDao.save(task);
    }

    private void installTimeoutTransition(final Runnable transition) {
        TaskService service = new TaskServiceImpl() {
            @Override
            public Instance skipSystemTask(FlowParams params, Task task) {
                transition.run();
                return null;
            }
        }.setDao(taskDao);
        FrameInvoker.setBeanFunction(type -> TaskService.class.equals(type) ? service : context.getBean(type));
    }

    @Test
    public void shouldManageFormsAndIsolateThemByTenant() {
        Form form = FlowEngine.newForm();
        form.setId(50L).setFormCode("expense").setFormName("Expense")
            .setPublishStatus(PublishStatus.UNPUBLISHED.getKey())
            .setFormContent("{\"schemaVersion\":\"1\"}");
        assertTrue(formService.save(form));
        assertEquals("1", form.getVersion());
        assertEquals("tenant-a", form.getTenantId());

        assertTrue(formService.saveContent(50L, "{\"fields\":[]}"));
        assertEquals("{\"fields\":[]}", formService.getByCode("expense", "1").getFormContent());
        assertTrue(formService.publish(50L));
        assertEquals(1L, formService.publishedPage("Expense", 1, 20).getTotal());
        assertTrue(formService.copyForm(50L));
        Form copy = formService.getByCode("expense", "2");
        assertEquals(PublishStatus.UNPUBLISHED.getKey(),
            copy.getPublishStatus());
        assertTrue(formService.removeById(copy.getId()));
        assertNull(formService.getById(copy.getId()));

        jdbcTemplate.update("insert into flow_form "
                + "(id, form_code, form_name, version, publish_status, deleted, tenant_id) "
                + "values (?, ?, ?, ?, ?, ?, ?)",
            51L, "expense", "Other tenant", "1", 1, "0", "tenant-b");
        assertNull(formService.getById(51L));
        assertEquals(1, formDao.queryByCodeList(java.util.Collections.singletonList("expense")).size());
    }

    private SubprocessRun run(Long id, Long taskId, String status) {
        SubprocessRun run = FlowEngine.newSubprocessRun();
        run.setId(id);
        run.setParentInstanceId(100L);
        run.setParentTaskId(taskId);
        run.setParentDefinitionId(1000L);
        run.setParentNodeCode("SUB");
        run.setChildFlowCode("child");
        run.setChildDefinitionId(2000L);
        run.setChildDefinitionVersion("1");
        run.setCompletionPolicy("ALL");
        run.setCollectionFingerprint(repeat('a', 64));
        run.setExpectedCount(3);
        run.setPendingCount(0);
        run.setRunningCount(3);
        run.setCompletedCount(0);
        run.setFailedCount(0);
        run.setCancelledCount(0);
        run.setRunStatus(status);
        run.setLockVersion(0);
        root(run);
        return run;
    }

    private SubprocessChild child(Long id, Long runId, String itemKey, Long instanceId) {
        SubprocessChild child = FlowEngine.newSubprocessChild();
        child.setId(id);
        child.setRunId(runId);
        child.setItemKey(itemKey);
        child.setItemLabel(itemKey);
        child.setChildBusinessKey("business-" + itemKey);
        child.setChildFlowCode("child");
        child.setChildDefinitionId(2000L);
        child.setChildDefinitionVersion("1");
        child.setChildInstanceId(instanceId);
        child.setChildStatus(SubprocessChildStatus.RUNNING.name());
        root(child);
        return child;
    }

    private SubprocessEvent event(Long id, Long runId, Date occurredAt) {
        SubprocessEvent event = FlowEngine.newSubprocessEvent();
        event.setId(id);
        event.setRunId(runId);
        event.setParentInstanceId(100L);
        event.setParentNodeCode("SUB");
        event.setEventType("TEST");
        event.setEventResult("SUCCEEDED");
        event.setOccurredAt(occurredAt);
        root(event);
        return event;
    }

    private void root(com.luokuiai.flovira.core.entity.RootEntity entity) {
        entity.setTenantId("tenant-a");
        entity.setDeleted("0");
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    @SpringBootApplication
    @EnableAutoConfiguration
    public static class TestApplication {
        @org.springframework.context.annotation.Bean
        public com.luokuiai.flovira.core.handler.ApproverResolver contractUsers() {
            return new com.luokuiai.flovira.core.handler.AbstractUserResolver() {
                public void validate(com.luokuiai.flovira.core.dto.ApproverRule rule) { }
                public List<String> resolve(com.luokuiai.flovira.core.dto.ApproverContext context) {
                    if (context.getRule().getConfig() != null
                            && Boolean.TRUE.equals(context.getRule().getConfig().get("requireTrustedContext"))) {
                        Map<?, ?> trusted = (Map<?, ?>) context.getFlowParams().getVariables().get("host.approverContext");
                        assertNotNull(trusted);
                        assertEquals("initiator", trusted.get("initiatorId"));
                        assertEquals("department-a", trusted.get("organizationId"));
                    }
                    if (Integer.valueOf(0).equals(context.getNode().getNodeType())) {
                        return context.getRule().getSubjects().stream().map(com.luokuiai.flovira.core.dto.BusinessSubject::getId)
                            .collect(java.util.stream.Collectors.toList());
                    }
                    if (context.getRule().getConfig() != null
                            && Boolean.TRUE.equals(context.getRule().getConfig().get("contractEmpty"))) {
                        return java.util.Collections.emptyList();
                    }
                    return java.util.Collections.singletonList("approver");
                }
            };
        }
    }

    public static class ContractTenantHandler implements TenantHandler {
        @Override
        public String getTenantId() {
            return "tenant-a";
        }
    }
}
