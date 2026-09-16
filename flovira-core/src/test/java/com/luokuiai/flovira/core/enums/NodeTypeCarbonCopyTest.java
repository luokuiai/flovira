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
package com.luokuiai.flovira.core.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 抄送节点类型兼容测试。
 *
 * @author warm
 */
public class NodeTypeCarbonCopyTest {

    @Test
    public void shouldAppendCarbonCopyWithoutChangingExistingKeys() {
        assertEquals(Integer.valueOf(7), NodeType.WAIT.getKey());
        assertEquals(Integer.valueOf(8), NodeType.CARBON_COPY.getKey());
        assertEquals("carbonCopy", NodeType.getValueByKey(8));
        assertTrue(NodeType.isCarbonCopy(8));
        assertTrue(NodeType.isWorkNode(8));
        assertFalse(NodeType.isBetween(8));
        assertEquals("4", UserType.CARBON_COPY.getKey());
    }
}
