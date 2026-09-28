/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luokuiai.flovira.orm.contract;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.entity.RootEntity;
import com.luokuiai.flovira.core.orm.dao.*;
import com.luokuiai.flovira.core.utils.page.Page;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.junit.Assert.*;

/** 两套 ORM 使用真实建表基线，验证所有 ext 列的原生 JSON 读写。 */
public class ExtJsonPersistenceContractTest {
    @Test
    public void postgresNativeJson() throws Exception {
        try (PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:16-alpine")) {
            database.start();
            verify("postgresql", database.getJdbcUrl(), database.getUsername(), database.getPassword());
        }
    }

    @Test
    public void mysqlNativeJson() throws Exception {
        try (GenericContainer<?> database = new GenericContainer<>("mysql:8.4")
                .withEnv("MYSQL_ROOT_PASSWORD", "contract-test")
                .withEnv("MYSQL_DATABASE", "flovira")
                .withEnv("MYSQL_USER", "flovira").withEnv("MYSQL_PASSWORD", "contract-test")
                .withExposedPorts(3306)
                .waitingFor(Wait.forLogMessage(".*ready for connections.*port: 3306.*\\n", 1))) {
            database.start();
            verify("mysql", "jdbc:mysql://" + database.getHost() + ":" + database.getMappedPort(3306)
                + "/flovira?allowPublicKeyRetrieval=true&useSSL=false", "flovira", "contract-test");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void verify(String dialect, String url, String user, String password) throws Exception {
        // The engine owns a singleton context bridge; isolate successive test applications.
        java.lang.reflect.Field bridge = com.luokuiai.flovira.plugin.modes.sb.utils.SpringUtil.class
            .getDeclaredField("applicationContext");
        bridge.setAccessible(true);
        bridge.set(null, null);
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.addInitializers(app -> {
            // Pagination is host-configured in the Plus integration.
            try {
                Class<?> interceptorType = Class.forName("com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor");
                Object interceptor = interceptorType.getDeclaredConstructor().newInstance();
                Class<?> innerType = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor");
                Object pagination = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor")
                    .getDeclaredConstructor().newInstance();
                interceptorType.getMethod("addInnerInterceptor", innerType).invoke(interceptor, pagination);
                app.getBeanFactory().registerSingleton("jsonContractPagination", interceptor);
            } catch (ClassNotFoundException classicMyBatis) {
                // Classic MyBatis performs pagination directly in its mapper SQL.
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
        });
        try (ConfigurableApplicationContext context = application.run(
                "--spring.main.banner-mode=off", "--spring.datasource.url=" + url,
                "--spring.datasource.username=" + user, "--spring.datasource.password=" + password,
                "--spring.sql.init.mode=always",
                "--spring.sql.init.schema-locations=classpath:" + dialect + "/flovira-v1.0.0.sql",
                "--flovira.banner=false", "--flovira.data-source-type=" + dialect)) {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            Map<String, Object> business = new LinkedHashMap<>();
            business.put("text", String.join("", Collections.nCopies(40000, "中")));
            business.put("tags", Arrays.asList("a,b", "二"));
            business.put("enabled", false);
            Map<String, Object> ext = new LinkedHashMap<>();
            ext.put("business", business);
            String json = FlowEngine.jsonConvert.objToStr(ext);
            Class<?>[] types = {FlowDefinitionDao.class, FlowFormDao.class, FlowNodeDao.class,
                FlowInstanceDao.class, FlowHisTaskDao.class};
            String[] tables = {"flow_definition", "flow_form", "flow_node", "flow_instance", "flow_his_task"};
            for (int i = 0; i < types.length; i++) {
                FloviraDao<RootEntity> dao = (FloviraDao<RootEntity>) context.getBean(types[i]);
                RootEntity first = record(dao, 101L, json);
                RootEntity absent = record(dao, 102L, null);
                dao.saveBatch(Arrays.asList(first, absent));
                RootEntity saved = dao.selectById(101L);
                assertEquals(ext, FlowEngine.jsonConvert.strToMap(readExt(saved)));
                if ("flow_node".equals(tables[i])) {
                    assertEquals("node-key-101", saved.getClass().getMethod("getNodeKey").invoke(saved));
                }
                assertNull(readExt(dao.selectById(102L)));
                String sqlType = jdbc.queryForObject("select data_type from information_schema.columns "
                    + "where table_name = ? and column_name = 'ext'", String.class, tables[i]);
                assertEquals("postgresql".equals(dialect) ? "jsonb" : "json", sqlType);
                RootEntity query = dao.newEntity();
                set(query, "Ext", json);
                assertEquals(1L, dao.selectCount(query));
                assertEquals(1, dao.selectList(query, null).size());
                assertEquals(1L, dao.selectPage(query, new Page<RootEntity>(1, 10).setOrderBy("id")).getTotal());
                set(first, "Ext", "{\"b\":2,\"a\":1}");
                assertEquals(1, dao.updateById(first));
                set(query, "Ext", "{ \"a\": 1, \"b\": 2 }");
                assertEquals(1L, dao.selectCount(query));
                for (String invalid : Arrays.asList("", "[]", "not-json", "{broken}")) {
                    set(first, "Ext", invalid);
                    assertThrows(RuntimeException.class, () -> dao.updateById(first));
                }
                assertEquals(1, dao.delete(query));
                assertEquals(0L, dao.selectCount(query));
                assertEquals("1", jdbc.queryForObject("select deleted from " + tables[i]
                    + " where id = 101", String.class));
                dao.deleteById(102L);
                assertEquals("1", jdbc.queryForObject("select deleted from " + tables[i]
                    + " where id = 102", String.class));
                jdbc.update("update " + tables[i] + " set deleted = '2' where id = 102");
                assertNull(dao.selectById(102L));
            }
        } finally {
            bridge.set(null, null);
        }
    }

    private RootEntity record(FloviraDao<RootEntity> dao, Long id, String ext) throws Exception {
        RootEntity value = dao.newEntity();
        value.setId(id);
        set(value, "Ext", ext);
        // Each entity receives only its existing required fields.
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("FlowCode", "flow-" + id); fields.put("FlowName", "JSON contract");
        fields.put("FormCode", "form-" + id); fields.put("FormName", "JSON contract");
        fields.put("Version", "1"); fields.put("BusinessType", "contract");
        fields.put("BusinessId", "business-" + id); fields.put("NodeType", 1);
        fields.put("NodeCode", "node-" + id); fields.put("NodeName", "JSON contract");
        fields.put("NodeKey", "node-key-" + id);
        fields.put("DefinitionId", 1L); fields.put("InstanceId", 1L); fields.put("TaskId", id);
        fields.put("FlowStatus", "1"); fields.put("SkipType", "PASS");
        fields.put("PublishStatus", 0); fields.put("ActivityStatus", 1);
        fields.put("CooperationType", 0); fields.put("TenantId", "0"); fields.put("Deleted", "0");
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            try {
                value.getClass().getMethod("set" + field.getKey(), field.getValue().getClass())
                    .invoke(value, field.getValue());
            } catch (NoSuchMethodException ignored) {
                // Field belongs to another entity in this shared contract.
            }
        }
        return value;
    }

    private static void set(Object entity, String field, String value) throws Exception {
        entity.getClass().getMethod("set" + field, String.class).invoke(entity, value);
    }

    private static String readExt(Object entity) throws Exception {
        return (String) entity.getClass().getMethod("getExt").invoke(entity);
    }

    @Configuration
    @EnableAutoConfiguration
    public static class Application { }
}
