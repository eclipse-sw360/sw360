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
import org.eclipse.sw360.antenna.http.utils.HttpUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>
 * A class with utility methods related to completable futures and error
 * handling in asynchronous calls.
 * </p>
 */
public class FutureUtils {
    /**
     * Private constructor to prevent instantiation.
     */
    private FutureUtils() {
    }

    /**
     * Blocks until the future specified has completed and returns the result.
     * This method is used by the adapter classes to map asynchronous client
     * methods to blocking calls. It works like the
     * {@link HttpUtils#waitFor(Future)} method, but also wraps
     * checked {@code IOException} exceptions into runtime exceptions.
     *
     * @param future the future to block for
     * @param <T>    the result type of the future
     * @return the result of the future if it completed successfully
     * @throws SW360ClientException if the future failed with a checked
     *                              exception
     */
    public static <T> T block(CompletableFuture<? extends T> future) {
        try {
            return HttpUtils.waitFor(future);
        } catch (IOException e) {
            if (e.getCause() instanceof SW360ClientException) {
                throw (SW360ClientException) e.getCause();
            }
            throw new SW360ClientException("Asynchronous call failed.", e);
        }
    }

    /**
     * Applies a callback to a future that becomes effective if and only if the
     * future fails with a specific exception. The exception to trigger the
     * callback is defined by a predicate passed to this method. If the future
     * fails with an exception matched by the predicate, another future is
     * returned that is obtained from the {@code fallback} supplier. Here an
     * alternative result value could be computed. If the future was successful
     * or failed with a different exception, the original result is passed to
     * the caller.
     *
     * @param future    the future to be decorated with a fallback
     * @param condition the predicate when to apply the fallback
     * @param fallback  a supplier for the fallback future
     * @param <T>       the result type of the future
     * @return the future with the conditional fallback
     */
    public static <T> CompletableFuture<T> wrapFutureForConditionalFallback(CompletableFuture<T> future,
                                                                            Predicate<? super Throwable> condition,
                                                                            Supplier<? extends CompletableFuture<T>> fallback) {
        return future.handle((result, exception) ->
                exceptionMatches(exception, condition) ?
                        Optional.<CompletableFuture<T>>empty() :
                        Optional.of(future))
                .thenCompose(optFuture -> optFuture.orElseGet(fallback));
    }

    /**
     * Maps the given future to a new future that returns an empty
     * {@code Optional} if the original future completes with an exception
     * indicating a 404 NOT FOUND failure. If the future completes normally,
     * the resulting future completes with a corresponding defined
     * {@code Optional}. In all other cases, the resulting future completes
     * with the same exception as the original future. This method is used by
     * adapter classes to map specific failed REST calls on the client layer to
     * optional results.
     *
     * @param future the future to be decorated
     * @param <T>    the result type of the future
     * @return a future returning an optional value
     */
    public static <T> CompletableFuture<Optional<T>> optionalFuture(CompletableFuture<? extends T> future) {
        CompletableFuture<Optional<T>> optFuture = future.thenApply(Optional::of);
        return wrapFutureForConditionalFallback(optFuture,
                FutureUtils::resourceNotFound,
                () -> CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * Maps a future that returns an optional value to one that always returns
     * a value by applying a fallback if necessary. The resulting future checks
     * whether the given future completes with a value or fails with an
     * exception; if so, it is returned as is. If it completes with an empty
     * {@code Optional}, the future provided by the fallback supplier is used
     * instead. This function is useful in cases where different alternatives
     * are possible. If one attempt does not yield a result, a retry with
     * another strategy may be successful.
     *
     * @param future   the original future yielding an optional result
     * @param fallback a supplier for a future yielding a fallback value
     * @param <T>      the result type of the original future
     * @return a future yielding a defined value
     */
    public static <T> CompletableFuture<T> orFallback(CompletableFuture<Optional<T>> future,
                                                      Supplier<? extends CompletableFuture<T>> fallback) {
        return future.thenCompose(optResult ->
                optResult.map(CompletableFuture::completedFuture).orElseGet(fallback));
    }

    /**
     * Retries a future that returns an optional value by applying a retry
     * strategy if necessary. The original future is checked whether it
     * completes with a value or fails with an exception; if so, it is returned
     * as is. If it completes with an empty {@code Optional}, the future
     * provided by the retry supplier is used instead. This function is similar
     * to {@link #orFallback(CompletableFuture, Supplier)}, but the alternative
     * future again yields an optional value.
     *
     * @param future the original future yielding an optional result
     * @param retry  a supplier for a future to try an alternative
     * @param <T>    the result type of the future
     * @return a future that retries the operation if necessary
     */
    public static <T> CompletableFuture<Optional<T>> orRetry(CompletableFuture<Optional<T>> future,
                                                             Supplier<? extends CompletableFuture<Optional<T>>> retry) {
        return future.thenCompose(optResult -> optResult.isPresent() ?
                CompletableFuture.completedFuture(optResult) : retry.get());
    }

    /**
     * Tests whether the given exception is a {@link FailedRequestException}
     * with the passed in status code.
     *
     * @param exception  the exception to be checked
     * @param statusCode the status code
     * @return <strong>true</strong> if the exception indicates a failed
     * request with this status code; <strong>false</strong> otherwise
     */
    public static boolean isFailedRequestWithStatus(Throwable exception, int statusCode) {
        return exception instanceof FailedRequestException &&
                ((FailedRequestException) exception).getStatusCode() == statusCode;
    }

    /**
     * Returns a future of the given type that fails with the exception
     * specified. Note that this method may become obsolete with later Java
     * versions, but in JDK8, there is not straight-forward way to create such
     * a future.
     *
     * @param ex  the exception causing the future to fail
     * @param <T> the result type of the future
     * @return the future failing with the given exception
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    /**
     * Converts the given collection of futures to a single one that completes
     * when all the provided futures have completed. This function is similar
     * to the {@code allOf()} method of {@code CompletableFuture}, but access
     * to the results of the futures is directly available. For each future
     * that fails, the given exception handler function is invoked. If it
     * returns <strong>true</strong>, the resulting future fails (with one of
     * the exceptions thrown by one of the futures). If the exception handler
     * function returns <strong>false</strong>, the exception is ignored; the
     * resulting collection will then not contain an element for this failing
     * future. Note that the order in the resulting collection is not related
     * to the original order.
     *
     * @param futures          the collection with futures
     * @param exceptionHandler the exception handler function
     * @param <T>              the type of the futures
     * @return a future with a list of all results
     */
    public static <T> CompletableFuture<Collection<T>> sequence(Collection<CompletableFuture<T>> futures,
                                                                Function<Throwable, Boolean> exceptionHandler) {
        final ConcurrentMap<T, Boolean> results = new ConcurrentHashMap<>();
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        CompletableFuture<?>[] futuresArray = futures.stream().map(f -> f.handle((result, ex) -> {
            if (ex != null) {
                if (exceptionHandler.apply(ex)) {
                    exception.compareAndSet(null, ex);
                }
            } else {
                results.put(result, Boolean.TRUE);
            }
            return result;
        })).toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futuresArray).thenCompose(v -> {
            Throwable failure = exception.get();
            return failure != null ? failedFuture(failure) :
                    CompletableFuture.completedFuture(results.keySet());
        });
    }

    /**
     * Executes the given action and wraps its result in a future. If the
     * action fails, the resulting future also fails with an
     * {@link SW360ClientException} whose cause is the original exception and
     * whose error message is the given message. This method is useful to chain
     * synchronous operations that may fail with asynchronous executions.
     *
     * @param action the action to be executed
     * @param errMsg an error message to be used in case of a failure
     * @param <T>    the result type of the action
     * @return a future with the result of the action
     */
    public static <T> CompletableFuture<T> wrapInFuture(Callable<? extends T> action, String errMsg) {
        try {
            return CompletableFuture.completedFuture(action.call());
        } catch (Exception e) {
            return failedFuture(new SW360ClientException(errMsg, e));
        }
    }

    /**
     * Checks whether the passed in exception fulfills the given predicate. If
     * the exception is not <strong>null</strong>, it is unwrapped and passed
     * to the predicate.
     *
     * @param exception the exception to be checked
     * @param condition the predicate to check the exception
     * @return a flag whether the exception is not <strong>null</strong> and is
     * matched by the predicate
     */
    private static boolean exceptionMatches(Throwable exception, Predicate<? super Throwable> condition) {
        return exception != null && condition.test(HttpUtils.unwrapCompletionException(exception));
    }

    /**
     * Checks whether the given exception represents a request that failed with
     * a 404 NOT FOUND status.
     *
     * @param exception the exception to be checked
     * @return a flag whether the exception is a 404 request
     */
    private static boolean resourceNotFound(Throwable exception) {
        return isFailedRequestWithStatus(exception, HttpConstants.STATUS_ERR_NOT_FOUND);
    }
}
