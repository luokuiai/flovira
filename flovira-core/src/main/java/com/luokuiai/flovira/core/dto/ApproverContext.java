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

import com.luokuiai.flovira.core.entity.Instance;
import com.luokuiai.flovira.core.entity.Node;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审批人解析上下文。预览必须只读；实例启动前 instance 可以为空。
 * 不推断提交人身份，由业务解析器按自己的业务契约获取。
 *
 * @author warm
 */
@Getter
@RequiredArgsConstructor
public class ApproverContext {
    private final Node node;
    private final ApproverRule rule;
    private final Instance instance;
    private final FlowParams flowParams;
    private final boolean preview;
}
