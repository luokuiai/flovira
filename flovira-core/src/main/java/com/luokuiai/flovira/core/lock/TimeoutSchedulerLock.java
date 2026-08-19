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
package com.luokuiai.flovira.core.lock;

/**
 * 超时任务调度锁
 *
 * @author warm
 */
public interface TimeoutSchedulerLock {

    /**
     * 尝试获取调度锁
     *
     * @param key 锁键
     * @param owner 本次调用的唯一标识
     * @param leaseMillis 锁租约毫秒数
     * @return 是否获取成功
     */
    boolean tryLock(String key, String owner, long leaseMillis);

    /**
     * 仅释放当前调用持有的调度锁
     *
     * @param key 锁键
     * @param owner 本次调用的唯一标识
     */
    void unlock(String key, String owner);
}
