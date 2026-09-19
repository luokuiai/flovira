/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luokuiai.flovira.ui.service;

import com.luokuiai.flovira.core.dto.WorkflowPackage;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.service.DefService;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class WorkflowPackageApiTest {
    @After
    public void resetBeanLookup() {
        FrameInvoker.setBeanFunction(type -> null);
    }

    @Test
    public void shouldExposeFullFormContent() {
        String content = "{\"schemaVersion\":\"1\",\"fields\":[{\"key\":\"reason\","
            + "\"label\":\"申请原因\",\"dataType\":\"string\"}]}";
        WorkflowPackage bundle = new WorkflowPackage().setForms(Collections.singletonList(
            new WorkflowPackage.PackagedForm().setReference("12").setFormCode("expense")
                .setFormName("报销表单").setVersion("1").setFormContent(content)));
        DefService service = (DefService) Proxy.newProxyInstance(DefService.class.getClassLoader(),
            new Class<?>[] {DefService.class}, (proxy, method, args) -> {
                if ("exportPackage".equals(method.getName())) {
                    assertEquals(11L, args[0]);
                    return bundle;
                }
                throw new AssertionError("Must use the complete package API: " + method.getName());
            });
        FrameInvoker.setBeanFunction(type -> DefService.class.equals(type) ? service : null);

        WorkflowPackage exported = (WorkflowPackage) FloviraService.exportData("package", 11L).getData();
        assertEquals(content, exported.getForms().get(0).getFormContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingExportType() {
        FloviraService.exportData(null, 11L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnknownImportType() {
        FloviraService.importData("unknown", Collections.emptyMap());
    }
}
