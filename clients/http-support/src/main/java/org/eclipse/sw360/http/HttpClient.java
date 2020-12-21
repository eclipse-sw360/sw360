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
package org.eclipse.sw360.http;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * <p>
 * The central interface for the execution of HTTP requests.
 * </p>
 * <p>
 * An object implementing this interface allows the execution of asynchronous
 * HTTP requests. The interface supports lambda expressions for defining the
 * requests to be executed and consuming the responses. Requests are defined by
 * {@code Consumer} objects, which are invoked with a concrete
 * {@link RequestBuilder} to generate a proper request representation. This
 * allows for a declarative approach when specifying the properties of requests
 * as shown in the following example:
 * </p>
 * <pre>{@code
 * httpClient.execute(builder -> builder.method(RequestBuilder.Method.POST)
 *                                 .uri(endpointUri())
 *                                 .body(body -> body.string(CONTENT, CONTENT_TEXT_PLAIN)),
 *                                 ...);
 * }
 * </pre>
 * <p>
 * When the response arrives it is passed to a {@link ResponseProcessor}, which
 * can evaluate the data and generate a corresponding result object. Again, a
 * lambda expression can be provided to process the response.
 * </p>
 * <p>
 * Implementations of this interface are expected to be thread-safe. They are
 * usually stored centrally and shared by multiple components.
 * </p>
 */
public interface HttpClient {
    /**
     * Executes an HTTP request asynchronously and returns a future object with
     * the result. The method first invokes the {@code Consumer} to generate
     * the request to be executed. This request is then sent to the
     * server. If sending fails, e.g. because no connection could be
     * established, the resulting future is failed with the corresponding
     * exception. Otherwise, the {@code ResponseProcessor} is invoked with a
     * representation of the response; the outcome of this object is then used
     * to complete the result future.
     *
     * @param producer  the object to produce the request
     * @param processor the object to process the response
     * @param <T>       the type of the result produced by the {@code ResponseProcessor}
     * @return a future with the result of the execution
     */
    <T> CompletableFuture<T> execute(Consumer<? super RequestBuilder> producer,
                                     ResponseProcessor<? extends T> processor);
}
