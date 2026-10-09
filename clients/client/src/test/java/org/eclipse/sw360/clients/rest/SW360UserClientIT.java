/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest;

import org.eclipse.sw360.http.utils.FailedRequestException;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.eclipse.sw360.clients.adapter.SW360ConnectionFactory;
import org.eclipse.sw360.clients.rest.resource.users.SW360User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;

public class SW360UserClientIT extends AbstractMockServerTest {
    private SW360UserClient userClient;

    @BeforeEach
    public void setUp() {
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ConnectionFactory scf = new SW360ConnectionFactory();
            userClient = scf.newConnection(createClientConfig()).getUserAdapterAsync().getUserClient();
        } else {
            userClient = new SW360UserClient(createClientConfig(), createMockTokenProvider());
            prepareAccessTokens(userClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
        }
    }

    @Test
    public void testGetUserProfile() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(get(urlPathEqualTo("/users/profile"))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                            .withBody("{\"email\":\"admin@sw360.org\",\"department\":\"SW360\"," +
                                    "\"fullName\":\"John Doe\",\"userGroup\":\"ADMIN\"}")));
        }

        SW360User profile = waitFor(userClient.getUserProfile());
        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isNotEmpty();
        if (!RUN_REST_INTEGRATION_TEST) {
            assertThat(profile.getEmail()).isEqualTo("admin@sw360.org");
            assertThat(profile.getFullName()).isEqualTo("John Doe");
        }
    }

    @Test
    public void testGetUsers() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                            .withBody("{\"_embedded\":{\"sw360:users\":[" +
                                    "{\"email\":\"a@sw360.org\"},{\"email\":\"b@sw360.org\"}]}}")));

            List<SW360User> users = waitFor(userClient.getUsers());
            assertThat(users).hasSize(2);
            assertThat(users).extracting(SW360User::getEmail)
                    .containsExactlyInAnyOrder("a@sw360.org", "b@sw360.org");
        }
    }

    @Test
    public void testGetUsersNoContent() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                    .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

            List<SW360User> users = waitFor(userClient.getUsers());
            assertThat(users).isEmpty();
        }
    }

    @Test
    public void testCreateToken() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(post(urlPathEqualTo("/users/tokens"))
                    .willReturn(aResponse().withStatus(HttpConstants.STATUS_CREATED)
                            .withBody("generated-token-value")));

            String token = waitFor(userClient.createToken("ci-token", Set.of("READ"), "2026-12-31"));
            assertThat(token).isEqualTo("generated-token-value");
        }
    }

    @Test
    public void testRevokeToken() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(delete(urlPathEqualTo("/users/tokens"))
                    .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

            Integer status = waitFor(userClient.revokeToken("ci-token"));
            assertThat(status).isEqualTo(HttpConstants.STATUS_NO_CONTENT);
        }
    }

    @Test
    public void testGetUserProfileError() {
        wireMockRule.stubFor(get(urlPathEqualTo("/users/profile"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_SERVER)));

        if (!RUN_REST_INTEGRATION_TEST) {
            FailedRequestException exception =
                    expectFailedRequest(userClient.getUserProfile(), HttpConstants.STATUS_ERR_SERVER);
            assertThat(exception.getTag()).isEqualTo(SW360UserClient.TAG_GET_USER_PROFILE);
        }
    }
}
