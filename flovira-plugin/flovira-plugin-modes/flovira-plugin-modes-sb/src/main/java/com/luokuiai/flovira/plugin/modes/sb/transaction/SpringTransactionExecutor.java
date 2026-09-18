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
package com.luokuiai.flovira.plugin.modes.sb.transaction;

import com.luokuiai.flovira.core.transaction.TransactionCallback;
import com.luokuiai.flovira.core.transaction.TransactionExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring事务执行器
 *
 * @author warm
 */
public class SpringTransactionExecutor implements TransactionExecutor {

    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate afterCommitTemplate;
    private final ThreadLocal<Boolean> deliveringAfterCommit = new ThreadLocal<Boolean>();

    public SpringTransactionExecutor(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.afterCommitTemplate = new TransactionTemplate(transactionManager);
        this.afterCommitTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T execute(final TransactionCallback<T> callback) {
        boolean afterCommit = Boolean.TRUE.equals(deliveringAfterCommit.get());
        TransactionTemplate template = afterCommit ? afterCommitTemplate : transactionTemplate;
        return template.execute(status -> {
            deliveringAfterCommit.remove();
            try { return callback.execute(); }
            finally { if (afterCommit) deliveringAfterCommit.set(true); }
        });
    }

    @Override
    public boolean isTransactionActive() {
        return !Boolean.TRUE.equals(deliveringAfterCommit.get()) && TransactionSynchronizationManager.isActualTransactionActive()
            && TransactionSynchronizationManager.isSynchronizationActive();
    }

    @Override
    public void afterCommit(final Runnable callback) {
        // The original synchronization is still bound while afterCommit runs,
        // but registering another callback there would never deliver it.
        if (Boolean.TRUE.equals(deliveringAfterCommit.get())
            || !TransactionSynchronizationManager.isSynchronizationActive()) {
            callback.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Boolean previous = deliveringAfterCommit.get();
                deliveringAfterCommit.set(true);
                try { callback.run(); }
                finally {
                    if (previous == null) deliveringAfterCommit.remove();
                    else deliveringAfterCommit.set(previous);
                }
            }
        });
    }
}
