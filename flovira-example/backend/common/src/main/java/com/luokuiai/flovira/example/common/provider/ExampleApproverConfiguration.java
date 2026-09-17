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

package com.luokuiai.flovira.example.common.provider;

import com.luokuiai.flovira.core.dto.ApproverContext;
import com.luokuiai.flovira.core.dto.ApproverRule;
import com.luokuiai.flovira.core.dto.BusinessSubject;
import com.luokuiai.flovira.core.handler.AbstractInitiatorResolver;
import com.luokuiai.flovira.core.handler.AbstractRoleResolver;
import com.luokuiai.flovira.core.handler.AbstractUserResolver;
import com.luokuiai.flovira.example.common.repository.ExampleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 示例业务自行定义人员来源；引擎不提供默认人员查询。 */
@Configuration
public class ExampleApproverConfiguration {

    @Bean
    public AbstractUserResolver userApproverResolver(ExampleRepository repository) {
        return new AbstractUserResolver() {
            public void validate(ApproverRule rule) { validateSubjects(rule, "USER"); }
            public List<String> resolve(ApproverContext context) {
                List<String> result = new ArrayList<>();
                for (BusinessSubject subject : context.getRule().getSubjects()) {
                    result.add(repository.findIdentity(subject.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + subject.getId())).id());
                }
                return result;
            }
        };
    }

    @Bean
    public AbstractRoleResolver roleApproverResolver(ExampleRepository repository) {
        return new AbstractRoleResolver() {
            public void validate(ApproverRule rule) { validateSubjects(rule, "ROLE"); }
            public List<String> resolve(ApproverContext context) {
                List<String> result = new ArrayList<>();
                for (BusinessSubject subject : context.getRule().getSubjects()) {
                    repository.findByRole(subject.getId()).forEach(identity -> result.add(identity.id()));
                }
                return result;
            }
        };
    }

    @Bean
    public AbstractInitiatorResolver initiatorApproverResolver(ExampleRepository repository) {
        return new AbstractInitiatorResolver() {
            public void validate(ApproverRule rule) {
                if (rule.getSubjects() != null && !rule.getSubjects().isEmpty()) {
                    throw new IllegalArgumentException("INITIATOR does not accept selected subjects");
                }
            }
            public List<String> resolve(ApproverContext context) {
                // 定义预览尚无实例，业务通过 purchaseId 指定采购单；运行时使用实例的业务主键。
                Object purchaseId = context.getInstance() == null
                    ? context.getFlowParams().getVariables().get("purchaseId")
                    : context.getInstance().getBusinessId();
                if (purchaseId == null) throw new IllegalArgumentException("Purchase ID is required");
                return Collections.singletonList(repository.findPurchase(String.valueOf(purchaseId))
                    .orElseThrow(() -> new IllegalArgumentException("Purchase not found")).applicantId());
            }
        };
    }

    private static void validateSubjects(ApproverRule rule, String type) {
        if (rule.getSubjects() == null || rule.getSubjects().isEmpty()) {
            throw new IllegalArgumentException(type + " subjects are required");
        }
        for (BusinessSubject subject : rule.getSubjects()) {
            if (subject == null || !type.equals(subject.getType())
                || subject.getId() == null || subject.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid " + type + " subject");
            }
        }
    }
}
