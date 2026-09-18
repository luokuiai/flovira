/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.core.service;

import com.luokuiai.flovira.core.dto.TimeoutExecutionResult;

import java.util.Date;

/**
 * 节点超时服务。宿主必须自行接入定时任务或延迟消息，Flovira 不自动调度。
 *
 * @author warm
 */
public interface TimeoutService {

    /**
     * 由宿主调度执行一批到期任务；单任务失败记录到结果并继续处理其他任务。
     * 宿主应配置租户上下文、监控失败结果，并安排后续重试。
     * 不应将整批调用放在宿主的一个外层事务中。
     *
     * @param now 到期截止时间，null 使用当前时间
     * @param batchSize 批次大小，非正数使用配置值
     * @return 扫描、抢占、成功和失败数量
     */
    TimeoutExecutionResult executeDue(Date now, int batchSize);

    /**
     * 由宿主消息或调度器触发指定任务；按当前时间重新校验截止时间。
     * 抢占和推进在同一事务内执行，执行失败抛出异常并回滚，供宿主重试。
     * 提前到达的消息返回 false，宿主必须重新安排到期触发或提供补漏扫描。
     *
     * @param taskId 待办任务 ID，不可为 null
     * @return 成功推进返回 true；超时关闭、任务不存在、尚未到期或未抢到返回 false
     */
    boolean executeTimeout(Long taskId);
}
