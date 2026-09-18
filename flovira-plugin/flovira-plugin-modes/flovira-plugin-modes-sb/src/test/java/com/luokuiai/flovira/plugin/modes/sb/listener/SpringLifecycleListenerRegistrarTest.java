/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luokuiai.flovira.plugin.modes.sb.listener;

import com.luokuiai.flovira.core.listener.lifecycle.*;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

public class SpringLifecycleListenerRegistrarTest {
    @Test
    public void discoversInjectedProxyAndInvokesWithoutSubscriptions() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        WorkflowLifecycleListener standalone = new WorkflowLifecycleListener() { };
        registry.register("standalone", standalone);
        try (AnnotationConfigApplicationContext context = context(registry, ValidConfiguration.class)) {
            WorkflowLifecycleListener listener = context.getBean("businessListener", WorkflowLifecycleListener.class);
            assertSame(listener, registry.getListeners().get("businessListener"));
            assertTrue(java.lang.reflect.Proxy.isProxyClass(listener.getClass()));
            registry.getListeners().get("businessListener").onEvent(null);
            assertEquals(1, context.getBean(AtomicInteger.class).get());
        }
        assertEquals(Collections.singletonMap("standalone", standalone), registry.getListeners());

    }

    @Test
    public void invalidListenerFailsStartupWithoutPartialRegistration() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        assertStartupFails(registry, InvalidConfiguration.class);
        assertTrue(registry.getListeners().isEmpty());
    }

    @Test
    public void collisionPreservesPreviouslyRegisteredListener() {
        LifecycleListenerRegistry registry = new LifecycleListenerRegistry();
        WorkflowLifecycleListener existing = new WorkflowLifecycleListener() { };
        registry.register("businessListener", existing);
        assertStartupFails(registry, ValidConfiguration.class);
        assertEquals(Collections.singletonMap("businessListener", existing), registry.getListeners());
    }

    private static void assertStartupFails(LifecycleListenerRegistry registry, Class<?> configuration) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            configure(context, registry, configuration);
            try {
                context.refresh();
                fail("Invalid listener configuration must fail startup");
            } catch (RuntimeException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    private static AnnotationConfigApplicationContext context(LifecycleListenerRegistry registry,
                                                               Class<?> configuration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        configure(context, registry, configuration);
        context.refresh();
        return context;
    }

    private static void configure(AnnotationConfigApplicationContext context,
                                  LifecycleListenerRegistry registry, Class<?> configuration) {
        context.getBeanFactory().registerSingleton("testRegistry", registry);
        context.register(RegistrarConfiguration.class, configuration);
    }

    @Configuration
    static class RegistrarConfiguration {
        @Bean
        SpringLifecycleListenerRegistrar registrar(org.springframework.beans.factory.ListableBeanFactory beans,
                                                   LifecycleListenerRegistry registry) {
            return new SpringLifecycleListenerRegistrar(beans, registry);
        }
    }

    @Configuration
    static class ValidConfiguration {
        @Bean
        AtomicInteger businessDependency() { return new AtomicInteger(); }

        @Bean
        WorkflowLifecycleListener businessListener(AtomicInteger businessDependency) {
            WorkflowLifecycleListener target = new WorkflowLifecycleListener() {
                @Override
                public void onEvent(LifecycleEvent event) {
                    businessDependency.incrementAndGet();
                }
            };
            return (WorkflowLifecycleListener) new ProxyFactory(target).getProxy();
        }

    }

    @Configuration
    static class InvalidConfiguration {
        @Bean
        WorkflowLifecycleListener candidateListener() { return new WorkflowLifecycleListener() { }; }

        @Bean
        WorkflowLifecycleListener invalidListener() {
            return new WorkflowLifecycleListener() {
                public DeliveryPhase getDeliveryPhase() { return null; }
            };
        }
    }
}
