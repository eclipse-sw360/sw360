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
package org.eclipse.sw360.clients.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.eclipse.sw360.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class SW360ClientConfigTest {
    private static final String REST_URL = "https://www.sw360.org/api";
    private static final String AUTH_URL = "https://auth.sw360.org/token";
    private static final String USER_TOKEN = "";
    private static final String CLIENT_ID = "myTestClientID";
    private static final String CLIENT_PASS = "secretClientPwd";

    /**
     * Mock for the HTTP client used within the configuration.
     */
    private HttpClient httpClient;

    /**
     * Mock for the JSON object mapper.
     */
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        httpClient = mock(HttpClient.class);
        mapper = mock(ObjectMapper.class);
    }

    @Test
    public void testNullRestUrlThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(null, AUTH_URL, CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, httpClient, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testEmptyRestUrlThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig("", AUTH_URL, CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, httpClient, mapper)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInvalidResUrlThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig("this is not a valid URL?!", AUTH_URL,
                CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullAuthUrlThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, null, CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, httpClient, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testEmptyAuthUrlThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, "", CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, httpClient, mapper)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullClientThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, AUTH_URL, null,
                CLIENT_PASS, USER_TOKEN, httpClient, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testEmptyClientThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, AUTH_URL, "",
                CLIENT_PASS, USER_TOKEN, httpClient, mapper)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullClientPasswordThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, AUTH_URL, CLIENT_ID,
                null, USER_TOKEN, httpClient, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testEmptyClientPasswordThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, AUTH_URL, CLIENT_ID,
                "", USER_TOKEN, httpClient, mapper)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullHttpClientThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, AUTH_URL, CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, null, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNullObjectMapperThrows() {
        assertThatThrownBy(() -> SW360ClientConfig.createConfig(REST_URL, AUTH_URL, CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, httpClient, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testCreateConfig() {
        SW360ClientConfig config =
                SW360ClientConfig.createConfig(REST_URL, AUTH_URL, CLIENT_ID, CLIENT_PASS,
                        USER_TOKEN, httpClient, mapper);

        assertThat(config.getRestURL()).isEqualTo(REST_URL);
        assertThat(config.getAuthURL()).isEqualTo(AUTH_URL);
        assertThat(config.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(config.getClientPassword()).isEqualTo(CLIENT_PASS);
        assertThat(config.getToken()).isEqualTo(USER_TOKEN);
        assertThat(config.getHttpClient()).isEqualTo(httpClient);
        assertThat(config.getObjectMapper()).isEqualTo(mapper);
        assertThat(config.getBaseURI().toString()).isEqualTo(REST_URL);
    }

    @Test
    public void testCreateConfigToken() {
        final String USER_TOKEN = "123token123";
        SW360ClientConfig config =
                SW360ClientConfig.createConfig(REST_URL, AUTH_URL, CLIENT_ID, CLIENT_PASS,
                        USER_TOKEN, httpClient, mapper);

        assertThat(config.getRestURL()).isEqualTo(REST_URL);
        assertThat(config.getAuthURL()).isEqualTo(AUTH_URL);
        assertThat(config.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(config.getClientPassword()).isEqualTo(CLIENT_PASS);
        assertThat(config.getToken()).isEqualTo(USER_TOKEN);
        assertThat(config.getHttpClient()).isEqualTo(httpClient);
        assertThat(config.getObjectMapper()).isEqualTo(mapper);
        assertThat(config.getBaseURI().toString()).isEqualTo(REST_URL);
    }

    @Test
    public void testTrailingSlashesFromURLsAreRemoved() {
        SW360ClientConfig config =
                SW360ClientConfig.createConfig(REST_URL + "/", AUTH_URL + "/",
                        CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);

        assertThat(config.getRestURL()).isEqualTo(REST_URL);
        assertThat(config.getAuthURL()).isEqualTo(AUTH_URL);
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(SW360ClientConfig.class)
                .withPrefabValues(ObjectMapper.class, new ObjectMapper(), new ObjectMapper())
                .suppress(Warning.NULL_FIELDS)
                .verify();
    }
}
