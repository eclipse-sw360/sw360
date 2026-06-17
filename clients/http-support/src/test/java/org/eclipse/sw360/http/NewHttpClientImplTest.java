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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@code NewHttpClientImpl}.
 */
public class NewHttpClientImplTest {

    private HttpClient client;
    private NewHttpClientImpl httpClient;

    @Before
    public void setUp() {
        client = mock(HttpClient.class);
        httpClient = new NewHttpClientImpl(client, new ObjectMapper());
    }

    @Test
    public void testExecuteDoesNotBlockBeforeResponseIsAvailable() throws Exception {
        CompletableFuture<HttpResponse<String>> pendingResponse = new CompletableFuture<>();
        when(client.sendAsync(any(), any(HttpResponse.BodyHandler.class))).thenReturn(pendingResponse);

        @SuppressWarnings("unchecked")
        ResponseProcessor<String> processor = mock(ResponseProcessor.class);
        when(processor.process(any())).thenReturn("processed");

        CompletableFuture<CompletableFuture<String>> executeCall = CompletableFuture.supplyAsync(
                () -> httpClient.execute(builder -> builder.uri("http://test.org/foo")
                        .method(RequestBuilder.Method.POST)
                        .body(body -> body.string("payload", "text/plain")), processor));

        CompletableFuture<String> resultFuture;
        try {
            resultFuture = executeCall.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            fail("execute() blocked waiting for HTTP response");
            return;
        }

        assertThat(resultFuture.isDone()).isFalse();

        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn("body");
        pendingResponse.complete(response);

        assertThat(resultFuture.get(1, TimeUnit.SECONDS)).isEqualTo("processed");
        verify(processor, times(1)).process(any());
    }

    @Test
    public void testExecuteCompletesExceptionallyWhenProcessorFails() {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn("body");

        CompletableFuture<HttpResponse<String>> doneResponse = CompletableFuture.completedFuture(response);
        when(client.sendAsync(any(), any(HttpResponse.BodyHandler.class))).thenReturn(doneResponse);

        @SuppressWarnings("unchecked")
        ResponseProcessor<String> processor = mock(ResponseProcessor.class);
        IOException expected = new IOException("boom");
        try {
            when(processor.process(any())).thenThrow(expected);
        } catch (IOException e) {
            throw new AssertionError("Unexpected checked exception while setting up mock", e);
        }

        CompletableFuture<String> result = httpClient.execute(builder -> builder.uri("http://test.org/foo")
                .method(RequestBuilder.Method.POST)
                .body(body -> body.string("payload", "text/plain")), processor);

        try {
            result.join();
            fail("No exception thrown!");
        } catch (CompletionException ex) {
            assertThat(ex.getCause()).isEqualTo(expected);
        }
    }

    @Test
    public void testExecutePreservesOriginalIOExceptionForGet() throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn("body");

        CompletableFuture<HttpResponse<String>> doneResponse = CompletableFuture.completedFuture(response);
        when(client.sendAsync(any(), any(HttpResponse.BodyHandler.class))).thenReturn(doneResponse);

        @SuppressWarnings("unchecked")
        ResponseProcessor<String> processor = mock(ResponseProcessor.class);
        IOException expected = new IOException("boom");
        try {
            when(processor.process(any())).thenThrow(expected);
        } catch (IOException e) {
            throw new AssertionError("Unexpected checked exception while setting up mock", e);
        }

        CompletableFuture<String> result = httpClient.execute(builder -> builder.uri("http://test.org/foo")
                .method(RequestBuilder.Method.POST)
                .body(body -> body.string("payload", "text/plain")), processor);

        try {
            result.get(1, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch (java.util.concurrent.ExecutionException ex) {
            assertThat(ex.getCause()).isEqualTo(expected);
        }
    }
}