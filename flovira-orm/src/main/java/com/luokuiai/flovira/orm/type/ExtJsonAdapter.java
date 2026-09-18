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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ext 原生 JSON 的 JDBC 绑定及等值比较扩展，通过 Java SPI 注册。
 * 实现须线程安全；不接收业务值来拼接 SQL。
 */
public interface ExtJsonAdapter {
    /** 匹配 JDBC DatabaseMetaData.getDatabaseProductName() 返回的产品名。 */
    boolean supportsDatabase(String databaseProductName);

    /** 匹配 Flovira dataSourceType 配置，用于两套 ORM 的查询条件。 */
    boolean supportsDialect(String dataSourceType);

    /** 绑定 JSON 对象字符串或 SQL NULL；实现可使用业务方自己的 JDBC 驱动类型。 */
    void bind(PreparedStatement statement, int index, String value) throws SQLException;

    /**
     * column 为映射提供的列名，parameter 为带 typeHandler 的 MyBatis 绑定占位符。
     * 必须保留参数绑定，不可将 JSON 值插入 SQL。
     */
    default String comparison(String column, String parameter) {
        return column + " = " + parameter;
    }
}
