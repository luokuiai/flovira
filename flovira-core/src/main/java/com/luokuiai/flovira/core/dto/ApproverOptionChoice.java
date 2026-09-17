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
package com.luokuiai.flovira.core.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 办理人策略附加配置项的候选值。
 *
 * @author warm
 */
@Getter
@Setter
@Accessors(chain = true)
public class ApproverOptionChoice {

    private String value;
    private String label;
    private boolean disabled;
    /** 此选项调用业务选择器时使用的策略编码。 */
    private String selectionStrategy;
    /** 所选人员列表在所属规则 config 中的存储键。 */
    private String selectionConfigKey;
}
