/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.http;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * This class implements the java native client library.
 *
 * @author smruti.sahoo@siemens.com
 *
 */
public class NewHttpClientImpl implements org.eclipse.sw360.http.HttpClient {

    /**
     * The underlying HTTP client.
     */
    private final HttpClient client;

    /**
     * The JSON object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * Creates a new instance of {@code NewHttpClientImpl} with the dependencies passed
     * in.
     * @param client the underlying HTTP client
     * @param mapper the JSON object mapper
     */
    public NewHttpClientImpl(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public <T> CompletableFuture<T> execute(Consumer<? super RequestBuilder> producer,
            ResponseProcessor<? extends T> processor) {
        CompletableFuture<HttpResponse<String>> asyncResponse = null;
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        NewRequestBuilderImpl builder = new NewRequestBuilderImpl(getMapper());
        producer.accept(builder);
        HttpRequest request = builder.build();
        asyncResponse = getClient().sendAsync(request, BodyHandlers.ofString());
        try {
            HttpResponse<String> response = asyncResponse.get();
            T result = processor.process(new NewResponseImpl<>(response));
            resultFuture.complete(result);
        } catch (InterruptedException | ExecutionException | IOException e) {
            resultFuture.completeExceptionally(e);
        }
        return resultFuture;
    }

    /**
     * Returns a reference to the JSON mapper used by this client.
     *
     * @return the JSON mapper
     */
    ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Returns a reference to the underlying {@code HttpClient}.
     *
     * @return the underlying client
     */
    HttpClient getClient() {
        return client;
    }

}
