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

import com.luokuiai.flovira.core.utils.ServiceLoaderUtil;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 两套 ORM 共用的 JSON 方言选择；业务 SPI 优先于内置实现，匹配冲突明确报错。 */
final class ExtJsonAdapters {
    private ExtJsonAdapters() { }

    private static final class Providers {
        private static final List<ExtJsonAdapter> CUSTOM = load();
        private static List<ExtJsonAdapter> load() {
            List<ExtJsonAdapter> providers = new ArrayList<>();
            // 注册错误必须传播，不能静默跳过业务方的驱动适配。
            for (ExtJsonAdapter provider : ServiceLoaderUtil.load(ExtJsonAdapter.class)) providers.add(provider);
            return Collections.unmodifiableList(providers);
        }
    }

    private static final List<ExtJsonAdapter> BUILT_INS = Arrays.asList(
        new NativeAdapter("postgresql", Types.OTHER),
        new NativeAdapter("mysql", Types.VARCHAR),
        new NativeAdapter("oracle", 2016)); // OracleTypes.JSON，不引入驱动依赖。

    static ExtJsonAdapter database(String product) throws SQLException {
        try {
            return resolve(product, true);
        } catch (IllegalStateException failure) {
            throw new SQLException(failure.getMessage(), failure);
        }
    }

    static ExtJsonAdapter dialect(String name) {
        return resolve(name, false);
    }

    private static ExtJsonAdapter resolve(String name, boolean database) {
        ExtJsonAdapter selected = null;
        for (ExtJsonAdapter provider : Providers.CUSTOM) {
            if (matches(provider, name, database)) {
                if (selected != null) throw new IllegalStateException("Multiple ext JSON adapters match: " + name);
                selected = provider;
            }
        }
        if (selected != null) return selected;
        for (ExtJsonAdapter provider : BUILT_INS) if (matches(provider, name, database)) return provider;
        throw new IllegalStateException("Unsupported ext JSON database/dialect: " + name
            + "; register an ExtJsonAdapter through Java SPI");
    }

    private static boolean matches(ExtJsonAdapter provider, String name, boolean database) {
        return database ? provider.supportsDatabase(name) : provider.supportsDialect(name);
    }

    private static final class NativeAdapter implements ExtJsonAdapter {
        private final String name;
        private final int type;
        private NativeAdapter(String name, int type) { this.name = name; this.type = type; }
        public boolean supportsDatabase(String product) { return name.equalsIgnoreCase(product); }
        public boolean supportsDialect(String dialect) { return name.equalsIgnoreCase(dialect); }
        public void bind(PreparedStatement statement, int index, String value) throws SQLException {
            if (value == null) statement.setNull(index, type);
            else if (type == Types.VARCHAR) statement.setString(index, value);
            else statement.setObject(index, value, type);
        }
        public String comparison(String column, String parameter) {
            if ("oracle".equals(name)) return "JSON_EQUAL(" + column + ", " + parameter + " ERROR ON ERROR)";
            if ("mysql".equals(name)) return column + " = CAST(" + parameter + " AS JSON)";
            return ExtJsonAdapter.super.comparison(column, parameter);
        }
    }
}
