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
package com.luokuiai.flovira.plugin.modes.sb.config;

import com.luokuiai.flovira.core.service.TimeoutService;
import com.luokuiai.flovira.core.lock.TimeoutSchedulerLock;
import com.luokuiai.flovira.plugin.modes.sb.lock.SpringRedisTimeoutSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Spring 节点超时调度配置
 *
 * @author warm
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "flovira.timeout", name = "enabled", havingValue = "true")
public class TimeoutSchedulingConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(TimeoutSchedulerLock.class)
    public static class RedisSchedulerLockConfiguration {

        @Bean
        public TimeoutSchedulerLock timeoutSchedulerLock(StringRedisTemplate redisTemplate) {
            return new SpringRedisTimeoutSchedulerLock(redisTemplate);
        }
    }

    @Bean
    public TimeoutSchedulerInvoker timeoutSchedulerInvoker(
        TimeoutService timeoutService, FloviraProperties properties) {
        return new TimeoutSchedulerInvoker(timeoutService, properties);
    }

    public static class TimeoutSchedulerInvoker {
        private final TimeoutService timeoutService;
        private final FloviraProperties properties;

        public TimeoutSchedulerInvoker(TimeoutService timeoutService, FloviraProperties properties) {
            this.timeoutService = timeoutService;
            this.properties = properties;
        }

        @Scheduled(fixedDelayString = "${flovira.timeout.scan-interval-seconds:60}", timeUnit = TimeUnit.SECONDS)
        public void scan() {
            timeoutService.executeDue(new Date(), properties.getTimeout().getBatchSize());
        }
    }
}
