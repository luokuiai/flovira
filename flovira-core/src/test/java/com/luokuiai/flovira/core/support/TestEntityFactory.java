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
package com.luokuiai.flovira.core.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试实体代理工厂
 *
 * @author warm
 */
public final class TestEntityFactory {

    private TestEntityFactory() {
    }

    public static <T> T create(Class<T> type) {
        InvocationHandler handler = new BeanInvocationHandler(type);
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    public static void put(Object bean, String property, Object value) {
        BeanInvocationHandler handler = (BeanInvocationHandler) Proxy.getInvocationHandler(bean);
        handler.values.put(property, value);
    }

    private static final class BeanInvocationHandler implements InvocationHandler {

        private final Class<?> type;
        private final Map<String, Object> values = new HashMap<String, Object>();

        private BeanInvocationHandler(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.startsWith("set") && args != null && args.length == 1) {
                values.put(name.substring(3), args[0]);
                return proxy;
            }
            if (name.startsWith("get") && (args == null || args.length == 0)) {
                return values.get(name.substring(3));
            }
            if (name.startsWith("is") && (args == null || args.length == 0)) {
                Object value = values.get(name.substring(2));
                return value == null ? Boolean.FALSE : value;
            }
            if ("toString".equals(name)) {
                return type.getSimpleName() + values;
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) return false;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            return null;
        }
    }
}
