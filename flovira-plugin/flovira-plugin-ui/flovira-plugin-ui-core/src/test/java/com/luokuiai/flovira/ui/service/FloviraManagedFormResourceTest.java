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
package com.luokuiai.flovira.ui.service;

import com.luokuiai.flovira.core.entity.Form;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.service.FormService;
import com.luokuiai.flovira.core.utils.page.Page;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.vo.DesignerResourcePage;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** Flovira 管理表单的设计器资源回退测试。 @author warm */
public class FloviraManagedFormResourceTest {

    @After
    public void resetBeanLookup() {
        FrameInvoker.setBeanFunction(type -> null);
    }

    @Test
    public void shouldReturnManagedFormsWhenHostProviderIsAbsent() {
        Form form = proxy(Form.class, (method, args) -> {
            if ("getId".equals(method)) return 12L;
            if ("getFormCode".equals(method)) return "expense";
            if ("getFormName".equals(method)) return "Expense";
            if ("getVersion".equals(method)) return "2";
            return null;
        });
        FormService service = proxy(FormService.class, (method, args) -> {
            if ("publishedPage".equals(method)) {
                return new Page<Form>(Collections.singletonList(form), 1L);
            }
            return null;
        });
        FrameInvoker.<Object>setBeanFunction(type -> FormService.class.getName().equals(type.getName())
            ? service : null);

        DesignerResourcePage page = FloviraService.queryResources(new DesignerResourceQuery()
            .setResourceType("FORM").setKeyword("Exp")).getData();

        assertEquals(1L, page.getTotal());
        assertEquals("12", page.getItems().get(0).getId());
        assertEquals("expense", page.getItems().get(0).getCode());
        assertEquals("2", page.getItems().get(0).getMetadata().get("version"));
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        Object value = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (proxy, method, args) -> invocation.invoke(method.getName(), args));
        return type.cast(value);
    }

    private interface Invocation {
        Object invoke(String method, Object[] args);
    }
}
