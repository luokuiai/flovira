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
 * 子流程编排事件
 *
 * @author warm
 */
public interface SubprocessEvent extends RootEntity {

    Long getRunId();
    SubprocessEvent setRunId(Long value);
    Long getChildId();
    SubprocessEvent setChildId(Long value);
    Long getParentInstanceId();
    SubprocessEvent setParentInstanceId(Long value);
    Long getChildInstanceId();
    SubprocessEvent setChildInstanceId(Long value);
    String getParentNodeCode();
    SubprocessEvent setParentNodeCode(String value);
    String getEventType();
    SubprocessEvent setEventType(String value);
    String getEventResult();
    SubprocessEvent setEventResult(String value);
    String getReason();
    SubprocessEvent setReason(String value);
    Date getOccurredAt();
    SubprocessEvent setOccurredAt(Date value);
}
