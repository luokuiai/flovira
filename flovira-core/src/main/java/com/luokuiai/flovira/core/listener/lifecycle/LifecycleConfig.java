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
package com.luokuiai.flovira.core.listener.lifecycle;

import java.util.List;
import lombok.Data;

/** 存储于流程／节点 ext 的版本化订阅配置。 */
@Data
public class LifecycleConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private Integer schemaVersion;
    private List<Entry> subscriptions;

    @Data
    public static class Entry {
        private String code;
        private String point;
        private String phase;
        private Integer order;
        private String parameters;
    }
}
