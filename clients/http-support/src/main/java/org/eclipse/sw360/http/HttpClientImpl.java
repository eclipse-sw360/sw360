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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class HttpClientImpl implements HttpClient {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientImpl.class);

    /**
     * The underlying HTTP client.
     */
    private final OkHttpClient client;

    /**
     * The JSON object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * Creates a new instance of {@code HttpClientImpl} with the dependencies
     * passed in.
     * @param client the underlying HTTP client
     * @param mapper the JSON object mapper
     */
    public HttpClientImpl(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public <T> CompletableFuture<T> execute(Consumer<? super RequestBuilder> producer,
                                            ResponseProcessor<? extends T> processor) {
        RequestBuilderImpl builder = new RequestBuilderImpl(getMapper());
        producer.accept(builder);
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        Request request = builder.build();
        LOG.debug("HTTP request {} {}", request.method(), request.url());
        getClient().newCall(request).enqueue(createCallback(processor, resultFuture));
        return resultFuture;
    }

    /**
     * Returns a callback to be notified by the underlying HTTP client with the
     * result of the asynchronous request execution. This callback is
     * responsible of completing the result future either with the object
     * created by the {@code ResponseProcessor} or with an exception.
     *
     * @param processor    the object to process the response
     * @param resultFuture the result future
     * @param <T>          the type of the result object
     * @return the callback for asynchronous request execution
     */
    <T> Callback createCallback(ResponseProcessor<? extends T> processor, CompletableFuture<T> resultFuture) {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                resultFuture.completeExceptionally(e);
                LOG.error("Failed HTTP request {} {}", call.request().method(), call.request().url(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    LOG.debug("HTTP response {} - {} {}", response.code(), response.request().method(),
                            response.request().url());
                    T result = processor.process(new ResponseImpl(response));
                    resultFuture.complete(result);
                } catch (Exception e) {
                    // we really need to catch all exceptions here; otherwise, a client waiting for the
                    // future to complete will wait forever
                    resultFuture.completeExceptionally(e);
                    LOG.error("Failed HTTP request {} {}", call.request().method(), call.request().url(), e);
                } finally {
                    response.close();
                }
            }
        };
    }

    /**
     * Returns a reference to the underlying {@code OkHttpClient}.
     * @return the underlying client
     */
    OkHttpClient getClient() {
        return client;
    }

    /**
     * Returns a reference to the JSON mapper used by this client.
     * @return the JSON mapper
     */
    ObjectMapper getMapper() {
        return mapper;
    }
}
