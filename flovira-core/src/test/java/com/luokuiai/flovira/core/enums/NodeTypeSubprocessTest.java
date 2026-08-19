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
package com.luokuiai.flovira.core.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 子流程节点类型兼容测试
 *
 * @author warm
 */
public class NodeTypeSubprocessTest {

    @Test
    public void shouldAppendSubprocessWithoutChangingExistingKeys() {
        assertEquals(Integer.valueOf(0), NodeType.START.getKey());
        assertEquals(Integer.valueOf(5), NodeType.INCLUSIVE.getKey());
        assertEquals(Integer.valueOf(6), NodeType.SUB_PROCESS.getKey());
        assertEquals("subProcess", NodeType.getValueByKey(6));
        assertTrue(NodeType.isWorkNode(6));
        assertFalse(NodeType.isBetween(6));
    }
}
