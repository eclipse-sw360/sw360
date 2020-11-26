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
package org.eclipse.sw360.antenna.sw360.client.utils;

import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class FutureUtilsTest {
    /**
     * A defined result used by test futures.
     */
    private static final Integer RESULT = 42;

    /**
     * Checks that the given future has failed with the passed in exception.
     *
     * @param future    the future to be checked
     * @param exception the expected exception
     */
    private static void expectFailedFuture(CompletableFuture<?> future, Throwable exception) {
        try {
            FutureUtils.block(future);
            fail("No exception thrown!");
        } catch (SW360ClientException e) {
            assertThat(e.getCause()).isEqualTo(exception);
        }
    }

    @Test
    public void testBlockSuccessfulFuture() {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(RESULT);

        assertThat(FutureUtils.block(future)).isEqualTo(RESULT);
    }

    @Test
    public void testBlockFailedFuture() {
        IOException exception = new IOException("Failed future");
        CompletableFuture<Integer> future = FutureUtils.failedFuture(exception);

        expectFailedFuture(future, exception);
    }

    @Test
    public void testBlockFailedFutureSW360Exception() {
        SW360ClientException sw360Ex = new SW360ClientException("Failed miserably...");
        IOException ioEx = new IOException("Failed future", sw360Ex);
        CompletableFuture<String> future = FutureUtils.failedFuture(ioEx);

        try {
            FutureUtils.block(future);
            fail("No exception thrown!");
        } catch (SW360ClientException e) {
            assertThat(e).isEqualTo(sw360Ex);
        }
    }

    @Test
    public void testOptionalFutureSuccess() {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(RESULT);

        CompletableFuture<Optional<Integer>> optFuture = FutureUtils.optionalFuture(future);
        assertThat(FutureUtils.block(optFuture)).contains(RESULT);
    }

    @Test
    public void testOptionalFutureFailure() {
        FailedRequestException exception = new FailedRequestException("a tag", 500);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        CompletableFuture<Optional<Integer>> optFuture = FutureUtils.optionalFuture(future);
        expectFailedFuture(optFuture, exception);
    }

    @Test
    public void testOptionalFutureWithNotFoundFailure() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.completeExceptionally(new FailedRequestException("tag", HttpConstants.STATUS_ERR_NOT_FOUND));

        CompletableFuture<Optional<Integer>> optFuture = FutureUtils.optionalFuture(future);
        assertThat(FutureUtils.block(optFuture)).isNotPresent();
    }

    @Test
    public void testOrFallbackDefinedValue() {
        CompletableFuture<Optional<Integer>> future = CompletableFuture.completedFuture(Optional.of(RESULT));
        Supplier<CompletableFuture<Integer>> fallback = () -> {
            throw new UnsupportedOperationException("Unexpected");
        };

        CompletableFuture<Integer> definedFuture = FutureUtils.orFallback(future, fallback);
        assertThat(FutureUtils.block(definedFuture)).isEqualTo(RESULT);
    }

    @Test
    public void testOrFallbackUndefinedValue() {
        CompletableFuture<Optional<Integer>> future = CompletableFuture.completedFuture(Optional.empty());
        Supplier<CompletableFuture<Integer>> fallback = () -> CompletableFuture.completedFuture(RESULT);

        CompletableFuture<Integer> definedFuture = FutureUtils.orFallback(future, fallback);
        assertThat(FutureUtils.block(definedFuture)).isEqualTo(RESULT);
    }

    @Test
    public void testOrRetryDefinedValue() {
        CompletableFuture<Optional<Integer>> future = CompletableFuture.completedFuture(Optional.of(RESULT));
        Supplier<CompletableFuture<Optional<Integer>>> retry = () -> {
            throw new UnsupportedOperationException("Unexpected");
        };

        CompletableFuture<Optional<Integer>> retryFuture = FutureUtils.orRetry(future, retry);
        assertThat(FutureUtils.block(retryFuture)).isEqualTo(Optional.of(RESULT));
    }

    @Test
    public void testOrRetryUndefinedValue() {
        CompletableFuture<Optional<Integer>> future = CompletableFuture.completedFuture(Optional.empty());
        Supplier<CompletableFuture<Optional<Integer>>> retry =
                () -> CompletableFuture.completedFuture(Optional.of(RESULT));

        CompletableFuture<Optional<Integer>> retryFuture = FutureUtils.orRetry(future, retry);
        assertThat(FutureUtils.block(retryFuture)).isEqualTo(Optional.of(RESULT));
    }

    @Test
    public void testSequenceAllSuccess() {
        List<Integer> numbers = IntStream.range(1, 8)
                .boxed()
                .collect(Collectors.toList());
        List<CompletableFuture<Integer>> futures = numbers.stream()
                .map(CompletableFuture::completedFuture)
                .collect(Collectors.toList());

        CompletableFuture<Collection<Integer>> sequence = FutureUtils.sequence(futures, ex -> true);
        Collection<Integer> values = FutureUtils.block(sequence);
        assertThat(values).containsExactlyInAnyOrderElementsOf(numbers);
    }

    @Test
    public void testSequenceWithFailure() throws InterruptedException {
        Throwable exception = new Exception("Failed");
        List<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2), FutureUtils.failedFuture(exception));

        CompletableFuture<Collection<Integer>> sequence = FutureUtils.sequence(futures, ex -> true);
        try {
            sequence.get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isEqualTo(exception);
        }
    }

    @Test
    public void testSequenceIgnoreFailures() {
        List<CompletableFuture<Integer>> futures = Arrays.asList(CompletableFuture.completedFuture(1),
                FutureUtils.failedFuture(new RuntimeException("Ex1")),
                CompletableFuture.completedFuture(2),
                FutureUtils.failedFuture(new Exception("Ex2")));

        CompletableFuture<Collection<Integer>> sequence = FutureUtils.sequence(futures, ex -> false);
        Collection<Integer> values = FutureUtils.block(sequence);
        assertThat(values).containsOnly(1, 2);
    }

    @Test
    public void testWrapInFutureSuccess() {
        final Integer result = 42;
        Callable<Integer> action = () -> result;

        CompletableFuture<Integer> actionFuture = FutureUtils.wrapInFuture(action, "don't care");
        assertThat(FutureUtils.block(actionFuture)).isEqualTo(result);
    }

    @Test
    public void testWrapInFutureFailure() {
        String message = "My action failed?!";
        Exception exception = new RuntimeException("Action failed");
        Callable<Integer> action = () -> {
            throw exception;
        };

        CompletableFuture<Integer> actionFuture = FutureUtils.wrapInFuture(action, message);
        try {
            FutureUtils.block(actionFuture);
            fail("Future did not fail");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).isEqualTo(message);
            assertThat(e.getCause()).isEqualTo(exception);
        }
    }
}
