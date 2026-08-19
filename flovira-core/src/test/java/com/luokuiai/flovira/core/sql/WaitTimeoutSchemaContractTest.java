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
package com.luokuiai.flovira.core.sql;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * 四库等待节点和超时字段契约测试
 *
 * @author warm
 */
public class WaitTimeoutSchemaContractTest {

    @Test
    public void shouldProvideTimeoutColumnsAndDueIndexForEveryDatabase() throws IOException {
        List<String> scripts = Arrays.asList(
            "../sql/mysql/flovira-v1.sql",
            "../sql/postgresql/flovira-v1.sql",
            "../sql/oracle/oracle-wram-flow-all.sql",
            "../sql/sqlserver/sqlserver.sql"
        );
        for (String path : scripts) {
            String sql = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8)
                .toLowerCase();
            int taskTable = Math.max(sql.indexOf("create table `flow_task`"),
                Math.max(sql.indexOf("create table flow_task"), sql.indexOf("create table\nflow_task")));
            assertTrue(path + " missing flow_task", taskTable >= 0);
            String taskSection = sql.substring(taskTable, Math.min(sql.length(), taskTable + 2500));
            assertTrue(path + " flow_task missing timeout_at", taskSection.contains("timeout_at"));
            assertTrue(path + " flow_task missing timeout_action", taskSection.contains("timeout_action"));
            assertTrue(path + " flow_task missing timeout_config", taskSection.contains("timeout_config"));
            assertTrue(path + " flow_task missing timeout_status", taskSection.contains("timeout_status"));
            assertTrue(path + " flow_task missing timeout_claimed_at", taskSection.contains("timeout_claimed_at"));
            assertTrue(path + " missing timeout due index", sql.contains("idx_flow_task_timeout_due"));
        }
    }
}
