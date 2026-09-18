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
package com.luokuiai.flovira.orm.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** 仅用于 ext：Java 保持 JSON 字符串，数据库使用原生 JSON 类型。 */
public final class ExtJsonTypeHandler extends BaseTypeHandler<String> {
    // OracleTypes.JSON；使用类型码避免给两个 ORM 引入 Oracle 驱动依赖。
    private static final int ORACLE_JSON = 2016;

    /** MyBatis-Plus 的实体条件；第二个参数已包含字段的 typeHandler 映射。 */
    public static final String CONDITION = "<choose>"
        + "<when test=\"@com.luokuiai.flovira.core.FlowEngine@dataSourceType() == 'oracle'\">"
        + "JSON_EQUAL(%1$s, #{%2$s} ERROR ON ERROR)</when>"
        + "<when test=\"@com.luokuiai.flovira.core.FlowEngine@dataSourceType() == 'mysql'\">"
        + "%1$s = CAST(#{%2$s} AS JSON)</when>"
        + "<otherwise>%1$s = #{%2$s}</otherwise></choose>";

    @Override
    public void setParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, sqlType(statement));
        } else {
            setNonNullParameter(statement, index, value, jdbcType);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        if (!value.trim().startsWith("{")) {
            throw new SQLException("ext must be a JSON object; use null for an absent value");
        }
        int type = sqlType(statement);
        if (type == Types.VARCHAR) {
            statement.setString(index, value);
        } else {
            statement.setObject(index, value, type);
        }
    }

    private int sqlType(PreparedStatement statement) throws SQLException {
        String database = statement.getConnection().getMetaData().getDatabaseProductName();
        if ("PostgreSQL".equals(database)) return Types.OTHER;
        if ("Oracle".equals(database)) return ORACLE_JSON;
        if ("MySQL".equals(database)) return Types.VARCHAR;
        throw new SQLException("Unsupported JSON database: " + database);
    }

    @Override
    public String getNullableResult(ResultSet result, String column) throws SQLException {
        return result.getString(column);
    }

    @Override
    public String getNullableResult(ResultSet result, int column) throws SQLException {
        return result.getString(column);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int column) throws SQLException {
        return statement.getString(column);
    }
}
