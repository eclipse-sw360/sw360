/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.http.HttpClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the token-first {@link SW360ClientConfig.Builder} entry point.
 */
class SW360ClientConfigBuilderTest {
    private static final String BASE_URL = "https://sw360.example.org/resource/api";
    private static final String TOKEN = "a-bearer-token";

    @Test
    void shouldSelectTokenMode_whenTokenProvided() {
        SW360ClientConfig config = SW360ClientConfig.builder()
                .baseUrl(BASE_URL)
                .token(TOKEN)
                .build();

        assertThat(config.getAuthenticationMode()).isEqualTo(Sw360AuthenticationMode.TOKEN);
        assertThat(config.getToken()).isEqualTo(TOKEN);
    }

    @Test
    void shouldSelectCredentialsMode_whenTokenAbsent() {
        SW360ClientConfig config = SW360ClientConfig.builder()
                .baseUrl(BASE_URL)
                .authUrl("https://sw360.example.org/authorization/oauth/token")
                .clientId("client")
                .clientSecret("clientSecret")
                .build();

        assertThat(config.getAuthenticationMode()).isEqualTo(Sw360AuthenticationMode.CREDENTIALS);
    }

    @Test
    void shouldSelectBasicMode_whenUsernameAndPasswordProvided() {
        SW360ClientConfig config = SW360ClientConfig.builder()
                .baseUrl(BASE_URL)
                .username("user")
                .password("secret")
                .build();

        assertThat(config.getAuthenticationMode()).isEqualTo(Sw360AuthenticationMode.BASIC);
        assertThat(config.getUsername()).isEqualTo("user");
        assertThat(config.getPassword()).isEqualTo("secret");
        assertThat(config.getToken()).isEmpty();
    }

    @Test
    void shouldPreferToken_overBasicCredentials() {
        SW360ClientConfig config = SW360ClientConfig.builder()
                .baseUrl(BASE_URL)
                .token(TOKEN)
                .username("user")
                .password("secret")
                .build();

        assertThat(config.getAuthenticationMode()).isEqualTo(Sw360AuthenticationMode.TOKEN);
    }

    @Test
    void shouldBuildTokenFirstClientConfig_withSuppliedDependencies() {
        HttpClient httpClient = mock(HttpClient.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        SW360ClientConfig config = SW360ClientConfig.builder()
                .baseUrl(BASE_URL + "/")
                .token(TOKEN)
                .httpClient(httpClient)
                .objectMapper(mapper)
                .build();

        assertThat(config.getRestURL()).isEqualTo(BASE_URL);
        assertThat(config.getToken()).isEqualTo(TOKEN);
        assertThat(config.getHttpClient()).isSameAs(httpClient);
        assertThat(config.getObjectMapper()).isSameAs(mapper);
    }

    @Test
    void shouldCreateDefaultDependencies_whenNotSupplied() {
        SW360ClientConfig config = SW360ClientConfig.builder()
                .baseUrl(BASE_URL)
                .token(TOKEN)
                .build();

        assertThat(config.getHttpClient()).isNotNull();
        assertThat(config.getObjectMapper()).isNotNull();
    }
}
