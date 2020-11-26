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
package org.eclipse.sw360.antenna.sw360.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.eclipse.sw360.antenna.http.HttpClient;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SW360ClientConfigTest {
    private static final String REST_URL = "https://www.sw360.org/api";
    private static final String AUTH_URL = "https://auth.sw360.org/token";
    private static final String USER = "scott";
    private static final String PASSWORD = "tiger";
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

    @Before
    public void setUp() {
        httpClient = mock(HttpClient.class);
        mapper = mock(ObjectMapper.class);
    }

    @Test(expected = NullPointerException.class)
    public void testNullRestUrlThrows() {
        SW360ClientConfig.createConfig(null, AUTH_URL, USER, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyRestUrlThrows() {
        SW360ClientConfig.createConfig("", AUTH_URL, USER, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidResUrlThrows() {
        SW360ClientConfig.createConfig("this is not a valid URL?!", AUTH_URL, USER, PASSWORD, CLIENT_ID,
                CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullAuthUrlThrows() {
        SW360ClientConfig.createConfig(REST_URL, null, USER, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyAuthUrlThrows() {
        SW360ClientConfig.createConfig(REST_URL, "", USER, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullUserThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, null, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyUserThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, "", PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullPasswordThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, null, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPasswordThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, "", CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullClientThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, null, CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyClientThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, "", CLIENT_PASS, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullClientPasswordThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, CLIENT_ID, null, USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyClientPasswordThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, CLIENT_ID, "", USER_TOKEN, httpClient, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullHttpClientThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, null, mapper);
    }

    @Test(expected = NullPointerException.class)
    public void testNullObjectMapperThrows() {
        SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, CLIENT_ID, CLIENT_PASS, USER_TOKEN, httpClient, null);
    }

    @Test
    public void testCreateConfig() {
        SW360ClientConfig config =
                SW360ClientConfig.createConfig(REST_URL, AUTH_URL, USER, PASSWORD, CLIENT_ID, CLIENT_PASS,
                        USER_TOKEN, httpClient, mapper);

        assertThat(config.getRestURL()).isEqualTo(REST_URL);
        assertThat(config.getAuthURL()).isEqualTo(AUTH_URL);
        assertThat(config.getUser()).isEqualTo(USER);
        assertThat(config.getPassword()).isEqualTo(PASSWORD);
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
                SW360ClientConfig.createConfig(REST_URL, AUTH_URL, "", "", CLIENT_ID, CLIENT_PASS,
                        USER_TOKEN, httpClient, mapper);

        assertThat(config.getRestURL()).isEqualTo(REST_URL);
        assertThat(config.getAuthURL()).isEqualTo(AUTH_URL);
        assertThat(config.getUser()).isEqualTo("");
        assertThat(config.getPassword()).isEqualTo("");
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
                SW360ClientConfig.createConfig(REST_URL + "/", AUTH_URL + "/", USER, PASSWORD,
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
