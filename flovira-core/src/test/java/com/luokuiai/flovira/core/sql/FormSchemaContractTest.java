/*
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

/** 三库表单结构契约测试。 @author warm */
public class FormSchemaContractTest {

    @Test
    public void shouldKeepManagedFormAndOpaqueReferencesForEveryDatabase() throws IOException {
        List<String> scripts = Arrays.asList(
            "../sql/mysql/flovira-v1.sql",
            "../sql/postgresql/flovira-v1.sql",
            "../sql/oracle/flovira-v1.sql"
        );
        for (String path : scripts) {
            String sql = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8)
                .toLowerCase();
            String form = tableSection(sql, "flow_form");
            for (String column : Arrays.asList("form_code", "form_name", "version", "publish_status",
                "form_content", "deleted", "tenant_id")) {
                assertTrue(path + " flow_form missing " + column, form.contains(column));
            }
            assertFalse(path + " flow_form must not contain form_type", form.contains("form_type"));
            assertFalse(path + " flow_form must not contain form_path", form.contains("form_path"));
            assertTrue(path + " flow_form.deleted must be non-null", deletedIsNotNull(form));
            assertTrue(path + " missing form lookup index", sql.contains("idx_flow_form_code"));
            assertTrue(path + " missing published form index", sql.contains("idx_flow_form_published"));
            assertFalse(path + " must not restore form_custom", sql.contains("form_custom"));

            for (String table : Arrays.asList("flow_definition", "flow_node", "flow_task", "flow_his_task")) {
                String referenceTable = tableSection(sql, table);
                assertTrue(path + " " + table + " missing form_id", referenceTable.contains("form_id"));
                assertFalse(path + " " + table + " restored form_path", referenceTable.contains("form_path"));
            }
        }
    }

    private boolean deletedIsNotNull(String table) {
        int column = table.indexOf("deleted");
        if (column < 0) {
            return false;
        }
        int end = table.indexOf('\n', column);
        String definition = table.substring(column, end < 0 ? table.length() : end);
        return definition.contains("not null") && definition.contains("default");
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
