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
package com.luokuiai.flovira.core.handler;

import com.luokuiai.flovira.core.dto.BusinessRelationQuery;
import com.luokuiai.flovira.core.dto.BusinessSubject;

import java.util.List;

/**
 * 业务组织关系查询扩展点，由审批人策略调用，流程执行语义由 Flovira 负责。
 *
 * @author warm
 */
public interface BusinessRelationProvider {

    String DEPARTMENT_LEADER = "DEPARTMENT_LEADER";
    String SUPERVISING_LEADER = "SUPERVISING_LEADER";
    String ROLE_MEMBERS = "ROLE_MEMBERS";
    String ORGANIZATION_CHAIN = "ORGANIZATION_CHAIN";

    List<BusinessSubject> resolveRelationship(BusinessRelationQuery query);
}
