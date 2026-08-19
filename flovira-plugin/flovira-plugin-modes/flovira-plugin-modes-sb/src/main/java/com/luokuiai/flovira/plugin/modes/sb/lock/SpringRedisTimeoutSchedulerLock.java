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
package com.luokuiai.flovira.plugin.modes.sb.lock;

import com.luokuiai.flovira.core.lock.TimeoutSchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Spring Redis超时任务调度锁
 *
 * @author warm
 */
public class SpringRedisTimeoutSchedulerLock implements TimeoutSchedulerLock {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<Long>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    public SpringRedisTimeoutSchedulerLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String key, String owner, long leaseMillis) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
            .setIfAbsent(key, owner, leaseMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public void unlock(String key, String owner) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), owner);
    }
}
