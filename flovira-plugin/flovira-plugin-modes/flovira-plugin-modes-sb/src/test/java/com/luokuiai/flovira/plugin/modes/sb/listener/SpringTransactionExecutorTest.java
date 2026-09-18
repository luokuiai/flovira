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

import com.luokuiai.flovira.plugin.modes.sb.transaction.SpringTransactionExecutor;
import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** Shared by the Boot 2 / 3 / 4 test matrix. */
public class SpringTransactionExecutorTest {
    @Test
    public void outerCommitDeliversOnceAndFollowUpWorkCommitsInNewTransaction() {
        RecordingManager manager = new RecordingManager();
        SpringTransactionExecutor executor = new SpringTransactionExecutor(manager);
        List<String> facts = new ArrayList<String>();
        new TransactionTemplate(manager).execute(status -> {
            executor.execute(() -> {
                assertTrue(executor.isTransactionActive());
                executor.afterCommit(() -> {
                    assertFalse(executor.isTransactionActive());
                    assertEquals(1, manager.commits);
                    facts.add("outer");
                    executor.execute(() -> {
                        assertTrue(executor.isTransactionActive());
                        executor.execute(() -> {
                            assertEquals(2, manager.begins);
                            return null;
                        });
                        executor.afterCommit(() -> facts.add("followUp"));
                        return null;
                    });
                    executor.afterCommit(() -> facts.add("alreadyCommitted"));
                });
                return null;
            });
            assertTrue(facts.isEmpty());
            return null;
        });
        assertEquals(Arrays.asList("outer", "followUp", "alreadyCommitted"), facts);
        assertEquals(2, manager.begins);
        assertEquals(2, manager.commits);
        assertFalse(executor.isTransactionActive());
    }

    @Test
    public void outerRollbackDiscardsAllJoinedCallbacks() {
        RecordingManager manager = new RecordingManager();
        SpringTransactionExecutor executor = new SpringTransactionExecutor(manager);
        List<String> facts = new ArrayList<String>();
        new TransactionTemplate(manager).execute(status -> {
            executor.execute(() -> { executor.afterCommit(() -> facts.add("unexpected")); return null; });
            status.setRollbackOnly();
            return null;
        });
        assertTrue(facts.isEmpty());
        assertEquals(0, manager.commits);
        assertEquals(1, manager.rollbacks);
    }

    private static final class RecordingManager extends AbstractPlatformTransactionManager {
        private Object bound;
        private int begins, commits, rollbacks;
        @Override protected Object doGetTransaction() { return new Object[] { bound }; }
        @Override protected boolean isExistingTransaction(Object transaction) { return ((Object[]) transaction)[0] != null; }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            bound = new Object(); ((Object[]) transaction)[0] = bound; begins++;
        }
        @Override protected Object doSuspend(Object transaction) { Object old = bound; bound = null; return old; }
        @Override protected void doResume(Object transaction, Object suspendedResources) { bound = suspendedResources; }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
        @Override protected void doCleanupAfterCompletion(Object transaction) { bound = null; }
    }
}
