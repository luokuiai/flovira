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

import com.luokuiai.flovira.core.dto.ApproverContext;
import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.ApproverStrategyDefinition;
import java.util.List;

/**
 * 一个持久化策略编码对应一个业务解析器。配置校验与人员查询必须分离。
 *
 * @author warm
 */
public interface ApproverResolver {
    String getStrategy();

    /** 是否可用作开始节点的提交范围；自定义策略由业务方显式开启。 */
    default boolean supportsSubmission() {
        return "USER".equals(getStrategy()) || "ROLE".equals(getStrategy());
    }

    /** 描述前端选项；code 必须与 getStrategy 一致，版本对应持久化配置格式。 */
    ApproverStrategyDefinition getDefinition();

    /** 只校验配置，不能在保存或发布时查询、固化未来节点的人员。 */
    void validate(ApproverRule rule);

    /** 返回最终用户 ID，不返回角色、组织 ID 或待执行的表达式；预览不得产生副作用。 */
    List<String> resolve(ApproverContext context);
}
