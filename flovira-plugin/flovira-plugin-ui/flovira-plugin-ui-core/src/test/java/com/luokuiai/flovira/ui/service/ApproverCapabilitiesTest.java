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

import com.luokuiai.flovira.core.dto.ApproverContext;
import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.handler.AbstractRoleResolver;
import com.luokuiai.flovira.core.handler.ApproverResolver;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import org.junit.After;
import org.junit.Test;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class ApproverCapabilitiesTest {
    @After public void reset() {
        FrameInvoker.setBeansFunction(type -> Collections.emptyList());
        FrameInvoker.setBeanFunction(type -> null);
    }

    @Test public void exposesOnlyRegisteredResolversWithoutResolvingPeople() {
        FrameInvoker.setBeanFunction(type -> null);
        FrameInvoker.setBeansFunction(type -> Collections.emptyList());
        assertTrue(FloviraService.capabilities().getData().getApproverStrategies().isEmpty());
        ApproverResolver resolver = new AbstractRoleResolver() {
            public void validate(ApproverRule rule) { throw new AssertionError("Not configuring a node"); }
            public List<String> resolve(ApproverContext context) { throw new AssertionError("No personnel lookup"); }
        };
        FrameInvoker.<ApproverResolver>setBeansFunction(type -> Collections.singletonList(resolver));
        assertEquals(1, FloviraService.capabilities().getData().getApproverStrategies().size());
        assertEquals("ROLE", FloviraService.capabilities().getData().getApproverStrategies().get(0).getCode());
    }
}
