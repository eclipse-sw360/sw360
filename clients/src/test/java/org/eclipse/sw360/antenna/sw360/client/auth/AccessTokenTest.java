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
package org.eclipse.sw360.antenna.sw360.client.auth;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.sw360.antenna.http.RequestBuilder;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.junit.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AccessTokenTest {
    /**
     * A test access token.
     */
    private static final String TOKEN = "<access_token>";

    /**
     * The expected value of an Authorization header with the test token.
     */
    private static final String AUTH_HEADER_VALUE = "Bearer " + TOKEN;

    @Test(expected = NullPointerException.class)
    public void testNullTokenIsRejected() {
        new AccessToken(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyTokenIsRejected() {
        new AccessToken("");
    }

    @Test
    public void testTokenIsAddedToRequestBuilder() {
        RequestBuilder builder = mock(RequestBuilder.class);
        RequestBuilder builderResult = mock(RequestBuilder.class);
        when(builder.header(HttpConstants.HEADER_AUTHORIZATION, AUTH_HEADER_VALUE))
                .thenReturn(builderResult);

        AccessToken accessToken = new AccessToken(TOKEN);
        assertThat(accessToken.addToken(builder)).isEqualTo(builderResult);
    }

    @Test
    public void testRequestProducerAddingTokenCanBeCreated() {
        RequestBuilder builder = mock(RequestBuilder.class);
        RequestBuilder builderResult = mock(RequestBuilder.class);
        @SuppressWarnings("unchecked")
        Consumer<RequestBuilder> wrappedProducer = mock(Consumer.class);
        when(builder.header(HttpConstants.HEADER_AUTHORIZATION, AUTH_HEADER_VALUE))
                .thenReturn(builderResult);

        AccessToken accessToken = new AccessToken(TOKEN);
        Consumer<RequestBuilder> producer = accessToken.tokenProducer(wrappedProducer);
        producer.accept(builder);
        verify(builder).header(HttpConstants.HEADER_AUTHORIZATION, AUTH_HEADER_VALUE);
        verify(wrappedProducer).accept(builderResult);
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(AccessToken.class)
                .withNonnullFields("token")
                .verify();
    }
}