/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.junit.Test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SyncClientAdapterHandlerTest {
    private static final int ANSWER = 42;

    /**
     * Returns an implementation of the asynchronous test interface.
     *
     * @return the asynchronous test implementation
     */
    private static DeepThoughtAsync createAsync() {
        return () -> CompletableFuture.completedFuture(ANSWER);
    }

    @Test
    public void testHandleAsyncResult() {
        DeepThoughtSync sync = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, createAsync());

        assertThat(sync.answerQuestionOfLifeUniverseAndEverything()).isEqualTo(ANSWER);
    }

    @Test
    public void testHandleSyncResult() {
        DeepThoughtSync syncDelegate = () -> ANSWER;
        DeepThoughtSync sync = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class, DeepThoughtSync.class,
                syncDelegate);

        assertThat(sync.answerQuestionOfLifeUniverseAndEverything()).isEqualTo(ANSWER);
    }

    @Test
    public void testHandleUnexpectedMethod() {
        DeepThoughtSync sync = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class, Object.class, new Object());

        try {
            sync.answerQuestionOfLifeUniverseAndEverything();
            fail("No exception thrown.");
        } catch (UndeclaredThrowableException e) {
            assertThat(e.getCause()).isInstanceOf(NoSuchMethodException.class);
        }
    }

    @Test
    public void testHandleMethodFromObject() {
        DeepThoughtAsync async = createAsync();
        DeepThoughtSync sync = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, async);

        assertThat(sync.hashCode()).isEqualTo(async.hashCode());
    }

    @Test
    public void testHandleEqualsTrue() {
        DeepThoughtAsync async = createAsync();
        DeepThoughtSync sync1 = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, async);
        DeepThoughtSync sync2 = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, async);

        assertThat(sync1).isEqualTo(sync2);
    }

    @Test
    public void testHandleEqualsFalse() {
        DeepThoughtSync sync1 = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, createAsync());
        DeepThoughtAsync otherAsync = () -> CompletableFuture.completedFuture(ANSWER - 1);
        DeepThoughtSync sync2 = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, otherAsync);

        assertThat(sync1).isNotEqualTo(sync2);
    }

    @Test
    public void testHandleEqualsOtherObject() {
        DeepThoughtSync sync = SyncClientAdapterHandler.newHandler(DeepThoughtSync.class,
                DeepThoughtAsync.class, createAsync());

        assertThat(sync).isNotEqualTo(new Object());
    }

    /**
     * An asynchronous test interface.
     */
    interface DeepThoughtAsync {
        CompletableFuture<Integer> answerQuestionOfLifeUniverseAndEverything();
    }

    /**
     * The corresponding synchronous test interface.
     */
    interface DeepThoughtSync {
        int answerQuestionOfLifeUniverseAndEverything();
    }
}