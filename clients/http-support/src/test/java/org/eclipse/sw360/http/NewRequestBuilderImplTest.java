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

import java.net.http.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test class for {@code NewRequestBuilderImpl}. This class focuses on
 * request-building semantics for the native HttpClient variant.
 */
public class NewRequestBuilderImplTest {
    /**
     * The builder to be tested.
     */
    private NewRequestBuilderImpl requestBuilder;

    @Before
    public void setUp() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        requestBuilder = new NewRequestBuilderImpl(mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUriRejected() {
        requestBuilder.uri("://invalid-uri");
    }

    @Test
    public void testBuildGetWithoutBody() {
        requestBuilder.uri("http://example.org/releases");
        HttpRequest request = requestBuilder.build();

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.headers().firstValue("Content-Type")).isEmpty();
    }

    @Test
    public void testBuildPostWithoutBody() {
        requestBuilder.uri("http://example.org/releases");
        requestBuilder.method(RequestBuilder.Method.POST);
        HttpRequest request = requestBuilder.build();

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.headers().firstValue("Content-Type")).isEmpty();
    }

    @Test
    public void testDoesNotOverrideExplicitContentType() {
        requestBuilder.uri("http://example.org/releases");
        requestBuilder.header("Content-Type", "text/plain");
        requestBuilder.body(body -> body.string("payload", "text/plain"));
        HttpRequest request = requestBuilder.build();

        assertThat(request.headers().firstValue("Content-Type")).hasValue("text/plain");
    }
}