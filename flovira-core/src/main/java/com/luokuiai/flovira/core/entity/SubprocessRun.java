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
 * 子流程运行聚合
 *
 * @author warm
 */
public interface SubprocessRun extends RootEntity {

    Long getParentInstanceId();
    SubprocessRun setParentInstanceId(Long value);
    Long getParentTaskId();
    SubprocessRun setParentTaskId(Long value);
    Long getParentDefinitionId();
    SubprocessRun setParentDefinitionId(Long value);
    String getParentNodeCode();
    SubprocessRun setParentNodeCode(String value);
    String getChildFlowCode();
    SubprocessRun setChildFlowCode(String value);
    Long getChildDefinitionId();
    SubprocessRun setChildDefinitionId(Long value);
    String getChildDefinitionVersion();
    SubprocessRun setChildDefinitionVersion(String value);
    String getCompletionPolicy();
    SubprocessRun setCompletionPolicy(String value);
    String getCollectionFingerprint();
    SubprocessRun setCollectionFingerprint(String value);
    Integer getExpectedCount();
    SubprocessRun setExpectedCount(Integer value);
    Integer getPendingCount();
    SubprocessRun setPendingCount(Integer value);
    Integer getRunningCount();
    SubprocessRun setRunningCount(Integer value);
    Integer getCompletedCount();
    SubprocessRun setCompletedCount(Integer value);
    Integer getFailedCount();
    SubprocessRun setFailedCount(Integer value);
    Integer getCancelledCount();
    SubprocessRun setCancelledCount(Integer value);
    String getRunStatus();
    SubprocessRun setRunStatus(String value);
    String getFailureCode();
    SubprocessRun setFailureCode(String value);
    Integer getLockVersion();
    SubprocessRun setLockVersion(Integer value);
    Date getInitializedAt();
    SubprocessRun setInitializedAt(Date value);
    Date getCompletedAt();
    SubprocessRun setCompletedAt(Date value);
}
