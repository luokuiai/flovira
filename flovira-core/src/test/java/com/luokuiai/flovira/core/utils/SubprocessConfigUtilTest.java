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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.dto.SubprocessConfig;
import org.junit.Test;

/**
 * 子流程节点配置契约测试
 *
 * @author warm
 */
public class SubprocessConfigUtilTest {

    @Test
    public void shouldAcceptCurrentFixedFlowConfig() {
        SubprocessConfig config = new SubprocessConfig();
        config.setFixedChildFlowCode("expense-review");
        SubprocessConfigUtil.validate(config);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectUnknownSchemaVersion() {
        SubprocessConfig config = new SubprocessConfig();
        config.setSchemaVersion(SubprocessConfig.CURRENT_SCHEMA_VERSION + 1);
        config.setFixedChildFlowCode("expense-review");
        SubprocessConfigUtil.validate(config);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectMissingFixedFlow() {
        SubprocessConfigUtil.validate(new SubprocessConfig());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectUnsupportedCompletionPolicy() {
        SubprocessConfig config = new SubprocessConfig();
        config.setFixedChildFlowCode("expense-review");
        config.setCompletionPolicy("ANY");
        SubprocessConfigUtil.validate(config);
    }
}
