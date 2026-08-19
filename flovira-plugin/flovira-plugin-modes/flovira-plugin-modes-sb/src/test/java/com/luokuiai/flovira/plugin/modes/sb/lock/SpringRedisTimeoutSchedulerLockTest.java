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

import org.junit.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Spring Redis超时任务调度锁测试
 *
 * @author warm
 */
public class SpringRedisTimeoutSchedulerLockTest {

    @Test
    public void shouldUseAtomicLeaseAndTokenSafeUnlock() {
        StubRedisTemplate redisTemplate = new StubRedisTemplate();
        SpringRedisTimeoutSchedulerLock schedulerLock = new SpringRedisTimeoutSchedulerLock(redisTemplate);

        assertTrue(schedulerLock.tryLock("flow:timeout", "owner-1", 3000L));
        schedulerLock.unlock("flow:timeout", "owner-1");

        assertEquals("flow:timeout", redisTemplate.lockArgs[0]);
        assertEquals("owner-1", redisTemplate.lockArgs[1]);
        assertEquals(3000L, redisTemplate.lockArgs[2]);
        assertSame(TimeUnit.MILLISECONDS, redisTemplate.lockArgs[3]);
        assertEquals("flow:timeout", redisTemplate.unlockKeys.get(0));
        assertEquals("owner-1", redisTemplate.unlockArgs[0]);
    }

    private static final class StubRedisTemplate extends StringRedisTemplate {
        private final Object[] lockArgs = new Object[4];
        private List<String> unlockKeys;
        private Object[] unlockArgs;

        @SuppressWarnings("unchecked")
        @Override
        public ValueOperations<String, String> opsForValue() {
            return (ValueOperations<String, String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(), new Class<?>[]{ValueOperations.class},
                (proxy, method, args) -> {
                    if ("setIfAbsent".equals(method.getName()) && args.length == 4) {
                        System.arraycopy(args, 0, lockArgs, 0, args.length);
                        return true;
                    }
                    return null;
                });
        }

        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            unlockKeys = keys;
            unlockArgs = args;
            return null;
        }
    }
}
