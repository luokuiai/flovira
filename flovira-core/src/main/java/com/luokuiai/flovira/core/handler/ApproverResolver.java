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
package com.luokuiai.flovira.core.handler;

import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Node;

import java.util.List;

/**
 * 按策略编码解析节点办理人的扩展点。
 *
 * @author warm
 */
public interface ApproverResolver {

    /**
     * @return 与设计器策略配置一致的唯一编码
     */
    String getStrategy();

    /**
     * 解析器同时负责校验自定义策略的配置参数。
     *
     * @return 解析后的用户 ID
     */
    List<String> resolve(Node node, ApproverRule rule, FlowParams flowParams);
}
