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

import com.luokuiai.flovira.core.constant.ApproverStrategy;
import com.luokuiai.flovira.core.dto.ApproverStrategyDefinition;
import com.luokuiai.flovira.core.utils.ApproverPolicyUtil;

/**
 * 角色成员策略的业务实现基类。不提供人员来源，不自动注册。
 *
 * @author warm
 */
public abstract class AbstractRoleResolver implements ApproverResolver {
    @Override
    public final String getStrategy() {
        return ApproverStrategy.ROLE;
    }

    @Override
    public ApproverStrategyDefinition getDefinition() {
        return new ApproverStrategyDefinition().setCode(getStrategy()).setName("角色成员")
            .setSelectionType("RESOURCE").setEditorType("DIALOG")
            .setResourceType("ROLE").setMultiple(true)
            .setResultCardinality("ZERO_OR_MORE")
            .setOptions(ApproverPolicyUtil.options());
    }
}
