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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 四库业务关联字段和索引契约测试
 *
 * @author warm
 */
public class BusinessCorrelationSchemaContractTest {

    @Test
    public void shouldKeepBusinessKeyOnInstanceOnlyForEveryDatabase() throws IOException {
        List<String> scripts = Arrays.asList(
            "../sql/mysql/flovira-v1.sql",
            "../sql/postgresql/flovira-v1.sql",
            "../sql/oracle/oracle-wram-flow-all.sql",
            "../sql/sqlserver/sqlserver.sql"
        );
        for (String path : scripts) {
            String sql = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8)
                .toLowerCase();
            String instance = tableSection(sql, "flow_instance");
            String task = tableSection(sql, "flow_task");
            String history = tableSection(sql, "flow_his_task");

            assertTrue(path + " instance missing business_type", instance.contains("business_type"));
            assertTrue(path + " instance missing business_id", instance.contains("business_id"));
            assertFalse(path + " task duplicates business_type", task.contains("business_type"));
            assertFalse(path + " task duplicates business_id", task.contains("business_id"));
            assertFalse(path + " history duplicates business_type", history.contains("business_type"));
            assertFalse(path + " history duplicates business_id", history.contains("business_id"));
            assertTrue(path + " missing instance business index", sql.contains("idx_flow_instance_business"));
            assertTrue(path + " missing history lookup index", sql.contains("idx_flow_his_task_instance_time"));
        }
    }

    private String tableSection(String sql, String tableName) {
        int start = sql.indexOf("create table `" + tableName + "`");
        if (start < 0) {
            start = sql.indexOf("create table " + tableName);
        }
        assertTrue("missing table " + tableName, start >= 0);
        int nextTable = sql.indexOf("create table", start + 12);
        return sql.substring(start, nextTable < 0 ? sql.length() : nextTable);
    }
}
