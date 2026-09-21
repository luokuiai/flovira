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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.exception.FlowException;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class FlowConfigUtilNodeKeyTest {

    @Test
    public void shouldNormalizeOptionalNodeKeyAndRejectInvalidOrDuplicateValues() {
        Set<String> keys = new HashSet<String>();
        Node first = TestEntityFactory.create(Node.class).setNodeKey(" approval_review ");
        Node absent = TestEntityFactory.create(Node.class).setNodeKey(" ");

        FlowConfigUtil.checkNodeKey(first, "test", keys);
        FlowConfigUtil.checkNodeKey(absent, "test", keys);

        assertEquals("approval_review", first.getNodeKey());
        assertNull(absent.getNodeKey());
        assertThrows(FlowException.class, () -> FlowConfigUtil.checkNodeKey(
            TestEntityFactory.create(Node.class).setNodeKey("approval_review"), "test", keys));
        assertThrows(FlowException.class, () -> FlowConfigUtil.checkNodeKey(
            TestEntityFactory.create(Node.class).setNodeKey("approval.review"), "test", new HashSet<String>()));
    }
}
