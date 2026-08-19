/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
import com.luokuiai.flovira.core.entity.SubprocessChild;
import com.luokuiai.flovira.core.entity.SubprocessEvent;
import com.luokuiai.flovira.core.entity.SubprocessRun;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.SubprocessChildStatus;
import com.luokuiai.flovira.core.enums.SubprocessRunStatus;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessChildDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessEventDao;
import com.luokuiai.flovira.core.orm.dao.FlowSubprocessRunDao;
import com.luokuiai.flovira.core.orm.dao.FlowTaskDao;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
            "--easy-query.database=pgsql",
            "--flovira.banner=false",
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
        jdbcTemplate = context.getBean(JdbcTemplate.class);
        transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        jdbcTemplate.update("delete from flow_subprocess_event");
        jdbcTemplate.update("delete from flow_subprocess_child");
        jdbcTemplate.update("delete from flow_subprocess_run");
        jdbcTemplate.update("delete from flow_task");
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
            .setTimeoutStatus("PENDING");
        root(task);
        assertEquals(1, taskDao.save(task));

        List<Task> due = taskDao.listDueTimeoutTasks(new Date(2000L), new Date(0L), 10);
        assertEquals(1, due.size());
        assertEquals("AUTO_PASS", due.get(0).getTimeoutAction());
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
        entity.setDelFlag("0");
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    @SpringBootApplication
    @EnableAutoConfiguration
    public static class TestApplication {
    }
}
