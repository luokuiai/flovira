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
package com.luokuiai.flovira.core.listener.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** 分派草案：引擎身份不可变，办理人变更完成后由引擎再次校验并持久化。 */
public final class AssignmentContext {
    private final Long instanceId;
    private final Long taskId;
    private final String nodeCode;
    private List<String> assignees;

    public AssignmentContext(Long instanceId, Long taskId, String nodeCode, List<String> assignees) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.taskId = taskId;
        this.nodeCode = Objects.requireNonNull(nodeCode, "nodeCode");
        setAssignees(assignees);
    }

    public Long getInstanceId() { return instanceId; }
    public Long getTaskId() { return taskId; }
    public String getNodeCode() { return nodeCode; }
    public List<String> getAssignees() { return assignees; }
    public void setAssignees(List<String> values) {
        Objects.requireNonNull(values, "assignees");
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        for (String value : values) {
            if (value == null || value.trim().isEmpty() || !value.equals(value.trim())) {
                throw new IllegalArgumentException("Invalid assignee ID");
            }
            unique.add(value);
        }
        assignees = Collections.unmodifiableList(new ArrayList<String>(unique));
    }
}
