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
package com.luokuiai.flovira.core.utils;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.dto.FlowParams;
import com.luokuiai.flovira.core.entity.Task;
import com.luokuiai.flovira.core.enums.FlowStatus;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.SkipType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抄送节点自动流转工具。
 *
 * @author warm
 * @since 2026/8/26
 */
public final class CarbonCopyUtil {

    public static final String SYSTEM_HANDLER = "flovira:carbon-copy";

    private CarbonCopyUtil() {
    }

    public static void advanceTasks(List<Task> tasks, Map<String, Object> variables) {
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (Task task : new ArrayList<Task>(tasks)) {
            if (!NodeType.isCarbonCopy(task.getNodeType())) {
                continue;
            }
            FlowParams flowParams = FlowParams.build()
                .skipType(SkipType.PASS.getKey())
                .flowStatus(FlowStatus.AUTO_PASS.getKey())
                .handler(SYSTEM_HANDLER)
                .message("Carbon copy completed")
                .hisTaskExt(history(task))
                .variable(variables);
            FlowEngine.taskService().skipSystemTask(flowParams, task);
        }
    }

    private static String history(Task task) {
        Map<String, Object> history = new LinkedHashMap<String, Object>();
        history.put("action", "CARBON_COPY");
        history.put("recipients", CollUtil.isEmpty(task.getPermissionList())
            ? new ArrayList<String>() : new ArrayList<String>(task.getPermissionList()));
        return FlowEngine.jsonConvert.objToStr(history);
    }
}
