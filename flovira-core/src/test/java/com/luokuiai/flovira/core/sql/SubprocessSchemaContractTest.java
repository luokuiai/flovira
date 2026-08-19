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
 * 四库子流程表结构契约测试
 *
 * @author warm
 */
public class SubprocessSchemaContractTest {

    @Test
    public void shouldProvideRequiredTablesAndIndexesForEveryDatabase() throws IOException {
        List<String> scripts = Arrays.asList(
            "../sql/mysql/flovira-v1.sql",
            "../sql/postgresql/flovira-v1.sql",
            "../sql/oracle/oracle-wram-flow-all.sql",
            "../sql/sqlserver/sqlserver.sql"
        );
        for (String path : scripts) {
            String sql = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8)
                .toLowerCase();
            assertContains(sql, "flow_subprocess_run", path);
            assertContains(sql, "flow_subprocess_child", path);
            assertContains(sql, "flow_subprocess_event", path);
            assertContains(sql, "uk_subprocess_run_parent_task", path);
            assertContains(sql, "uk_subprocess_child_item", path);
            assertContains(sql, "uk_subprocess_child_instance", path);
            assertContains(sql, "idx_subprocess_run_reconcile", path);
            assertContains(sql, "idx_subprocess_child_page", path);
            assertContains(sql, "idx_subprocess_event_timeline", path);
            assertContains(sql, "tenant_id", path);
            assertContains(sql, "lock_version", path);
        }
    }

    private void assertContains(String sql, String token, String path) {
        assertTrue(path + " missing " + token, sql.contains(token));
    }
}
