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
import com.luokuiai.flovira.core.FlowEngine;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** 仅用于 ext：Java 保持 JSON 字符串，数据库使用原生 JSON 类型。 */
public final class ExtJsonTypeHandler extends BaseTypeHandler<String> {
    /** MyBatis-Plus 的实体条件；参数映射由实体提供，业务值仍使用绑定参数。 */
    public static final String CONDITION = "${@com.luokuiai.flovira.orm.type.ExtJsonTypeHandler@comparison('%1$s', '%2$s')}";

    /** 两套 ORM 共用的动态条件入口；仅由可信映射传入列名和参数映射。 */
    public static String comparison(String column, String parameterMapping) {
        return ExtJsonAdapters.dialect(FlowEngine.dataSourceType())
            .comparison(column, "#{" + parameterMapping + "}");
    }

    @Override
    public void setParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        if (value != null && !value.trim().startsWith("{")) {
            throw new SQLException("ext must be a JSON object; use null for an absent value");
        }
        ExtJsonAdapters.database(statement.getConnection().getMetaData().getDatabaseProductName())
            .bind(statement, index, value);
    }

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        setParameter(statement, index, value, jdbcType);
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
