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
package com.luokuiai.flovira.core.entity;

import java.util.Date;

/**
 * 子流程实例关系
 *
 * @author warm
 */
public interface SubprocessChild extends RootEntity {

    Long getRunId();
    SubprocessChild setRunId(Long value);
    String getItemKey();
    SubprocessChild setItemKey(String value);
    String getItemLabel();
    SubprocessChild setItemLabel(String value);
    String getChildBusinessKey();
    SubprocessChild setChildBusinessKey(String value);
    String getChildFlowCode();
    SubprocessChild setChildFlowCode(String value);
    Long getChildDefinitionId();
    SubprocessChild setChildDefinitionId(Long value);
    String getChildDefinitionVersion();
    SubprocessChild setChildDefinitionVersion(String value);
    Long getChildInstanceId();
    SubprocessChild setChildInstanceId(Long value);
    String getChildStatus();
    SubprocessChild setChildStatus(String value);
    String getOutcome();
    SubprocessChild setOutcome(String value);
    Date getStartedAt();
    SubprocessChild setStartedAt(Date value);
    Date getCompletedAt();
    SubprocessChild setCompletedAt(Date value);
}
