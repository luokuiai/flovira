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
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.orm.type.ExtJsonTypeHandler;
import com.luokuiai.flovira.orm.type.ExtJsonAdapter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExtJsonTypeHandlerTest {
    @Test
    public void bindsOnlyNativeJsonAndUsesDialectNullTypes() throws Exception {
        String[] databases = {"PostgreSQL", "MySQL", "Oracle"};
        int[] types = {Types.OTHER, Types.VARCHAR, 2016};
        for (int i = 0; i < databases.length; i++) {
            List<Object[]> calls = new ArrayList<>();
            PreparedStatement statement = statement(databases[i], calls);
            ExtJsonTypeHandler handler = new ExtJsonTypeHandler();
            handler.setParameter(statement, 1, "{\"nested\":{\"enabled\":false}}", null);
            assertEquals(i == 1 ? "setString" : "setObject", calls.get(0)[0]);
            if (i != 1) assertEquals(types[i], calls.get(0)[3]);
            handler.setParameter(statement, 2, null, null);
            assertEquals("setNull", calls.get(1)[0]);
            assertEquals(types[i], calls.get(1)[2]);
            for (String invalid : new String[] {"", " ", "[]", "null", "plain"}) {
                assertThrows(SQLException.class, () -> handler.setParameter(statement, 3, invalid, null));
            }
            assertEquals(2, calls.size());
        }
    }

    @Test
    public void producesJsonComparisonWithBoundParametersForEachDialect() {
        Flovira previous = FlowEngine.getFlowConfig();
        try {
            for (String dialect : new String[] {"postgresql", "mysql", "oracle"}) {
                Flovira config = new Flovira();
                config.setDataSourceType(dialect);
                FlowEngine.setFlowConfig(config);
                String statement = "<script>SELECT id FROM flow_form WHERE "
                    + String.format(ExtJsonTypeHandler.CONDITION, "ext", "ext,typeHandler="
                        + ExtJsonTypeHandler.class.getName()) + "</script>";
                BoundSql sql = new XMLLanguageDriver().createSqlSource(new Configuration(), statement, Map.class)
                    .getBoundSql(Collections.singletonMap("ext", "{}"));
                String expected = "oracle".equals(dialect) ? "JSON_EQUAL(ext, ? ERROR ON ERROR)"
                    : "mysql".equals(dialect) ? "ext = CAST(? AS JSON)" : "ext = ?";
                assertTrue(sql.getSql().contains(expected));
                assertEquals(1, sql.getParameterMappings().size());
                assertTrue(sql.getParameterMappings().get(0).getTypeHandler() instanceof ExtJsonTypeHandler);
            }
        } finally {
            FlowEngine.setFlowConfig(previous);
        }
    }

    @Test
    public void readsJsonAsTextWithoutWrappingOrReserializing() throws Exception {
        ResultSet result = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {ResultSet.class}, (proxy, method, args) -> {
                if (method.getName().equals("getString")) return "{\"a\":1}";
                throw new AssertionError(method);
            });
        assertEquals("{\"a\":1}", new ExtJsonTypeHandler().getNullableResult(result, "ext"));
    }

    @Test
    public void discoversHostAdapterAndDelegatesNullAndDriverSpecificBinding() throws Exception {
        List<Object[]> calls = new ArrayList<>();
        PreparedStatement statement = statement("KingbaseTest", calls);
        ExtJsonTypeHandler handler = new ExtJsonTypeHandler();
        handler.setParameter(statement, 1, "{}", null);
        assertEquals("setObject", calls.get(0)[0]);
        assertTrue(calls.get(0)[2] instanceof DriverJson);
        assertEquals("{}", ((DriverJson) calls.get(0)[2]).value);
        handler.setParameter(statement, 2, null, null);
        assertEquals("setNull", calls.get(1)[0]);
        assertEquals(Types.OTHER, calls.get(1)[2]);
        // 扩展不能绕过 ext 必须为对象的入口检查。
        assertThrows(SQLException.class, () -> handler.setParameter(statement, 3, "[]", null));
        assertEquals(2, calls.size());
    }

    @Test
    public void usesHostComparisonWithoutInterpolatingBusinessJson() {
        Flovira previous = FlowEngine.getFlowConfig();
        try {
            Flovira config = new Flovira();
            config.setDataSourceType("kingbase-test");
            FlowEngine.setFlowConfig(config);
            String script = "<script>SELECT id FROM flow_form WHERE "
                + String.format(ExtJsonTypeHandler.CONDITION, "ext", "ext,typeHandler="
                    + ExtJsonTypeHandler.class.getName()) + "</script>";
            String value = "{\"sql\":\"' OR 1=1 --\"}";
            BoundSql sql = new XMLLanguageDriver().createSqlSource(new Configuration(), script, Map.class)
                .getBoundSql(Collections.singletonMap("ext", value));
            assertEquals("SELECT id FROM flow_form WHERE ext = CAST(? AS jsonb)", sql.getSql());
            assertEquals(1, sql.getParameterMappings().size());
            assertTrue(sql.getParameterMappings().get(0).getTypeHandler() instanceof ExtJsonTypeHandler);
            assertFalse(sql.getSql().contains(value));
        } finally {
            FlowEngine.setFlowConfig(previous);
        }
    }

    @Test
    public void rejectsUnknownOrAmbiguousDialectsWithoutBinding() throws Exception {
        ExtJsonTypeHandler handler = new ExtJsonTypeHandler();
        for (String product : new String[] {"Unknown", "Ambiguous"}) {
            List<Object[]> calls = new ArrayList<>();
            SQLException failure = assertThrows(SQLException.class, () ->
                handler.setParameter(statement(product, calls), 1, "{}", null));
            assertTrue(failure.getMessage().contains(product));
            assertTrue(calls.isEmpty());
        }
        Flovira previous = FlowEngine.getFlowConfig();
        try {
            for (String name : new String[] {"Unknown", "Ambiguous"}) {
                Flovira config = new Flovira();
                config.setDataSourceType(name);
                FlowEngine.setFlowConfig(config);
                assertThrows(IllegalStateException.class, () -> ExtJsonTypeHandler.comparison("ext", "ext"));
            }
        } finally {
            FlowEngine.setFlowConfig(previous);
        }
    }

    /** 只验证 SPI/驱动对象委托，不声称是真实 Kingbase 驱动适配。 */
    public static class HostAdapter implements ExtJsonAdapter {
        public boolean supportsDatabase(String product) {
            return "KingbaseTest".equals(product) || "Ambiguous".equals(product);
        }
        public boolean supportsDialect(String name) {
            return "kingbase-test".equals(name) || "Ambiguous".equals(name);
        }
        public void bind(PreparedStatement statement, int index, String value) throws SQLException {
            if (value == null) statement.setNull(index, Types.OTHER);
            else statement.setObject(index, new DriverJson(value));
        }
        public String comparison(String column, String parameter) {
            return column + " = CAST(" + parameter + " AS jsonb)";
        }
    }

    public static class DuplicateAdapter extends HostAdapter {
        public boolean supportsDatabase(String product) { return "Ambiguous".equals(product); }
        public boolean supportsDialect(String name) { return "Ambiguous".equals(name); }
    }

    private static final class DriverJson {
        private final String value;
        private DriverJson(String value) { this.value = value; }
    }

    private PreparedStatement statement(String database, List<Object[]> calls) {
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {DatabaseMetaData.class}, (proxy, method, args) -> {
                if (method.getName().equals("getDatabaseProductName")) return database;
                throw new AssertionError(method);
            });
        Connection connection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                if (method.getName().equals("getMetaData")) return metadata;
                throw new AssertionError(method);
            });
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                if (method.getName().equals("getConnection")) return connection;
                if (method.getName().startsWith("set")) {
                    Object[] call = new Object[args.length + 1];
                    call[0] = method.getName();
                    System.arraycopy(args, 0, call, 1, args.length);
                    calls.add(call);
                    return null;
                }
                throw new AssertionError(method);
            });
    }
}
