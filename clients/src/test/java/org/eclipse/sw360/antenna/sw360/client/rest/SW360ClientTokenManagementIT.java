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
package org.eclipse.sw360.antenna.sw360.client.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.auth.SW360AuthenticationClient;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.ProjectSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.antenna.http.utils.HttpUtils.waitFor;

/**
 * An integration test class that tests whether access tokens are correctly
 * managed by {@link SW360Client}. This test uses a real authentication client
 * that sends requests against the mock server.
 */
public class SW360ClientTokenManagementIT extends AbstractMockServerTest {

    /**
     * The endpoint queried by the test client.
     */
    private static final String ENDPOINT = "/projects";

    /**
     * The client to be tested.
     */
    private SW360ProjectClient client;

    @Before
    public void setUp() {
        SW360ClientConfig clientConfig = createClientConfig();
        SW360AuthenticationClient authClient = new SW360AuthenticationClient(clientConfig);
        AccessTokenProvider provider = new AccessTokenProvider(authClient);
        client = new SW360ProjectClient(clientConfig, provider);
    }

    /**
     * Generates the body of a response that returns the given token.
     *
     * @param token the access token
     * @return the body of this response
     */
    private static String accessTokenBody(String token) {
        return "{\"access_token\": \"" + token + "\"}";
    }

    /**
     * Tests the automatic refresh of access tokens if there are multiple
     * concurrent requests. Multiple threads are started that send a request.
     * The first access token is treated as expired, so it needs to be
     * refreshed. It is checked that only a single request for a refreshed
     * token is made.
     */
    @Test
    public void testAccessTokenManagementWithConcurrentRequests() throws InterruptedException {
        final int concurrentRequestCount = 4;
        final String expiredToken = "expired_access_token:-(";
        final String scenario = "multipleAccessTokens";
        final String stateRefreshed = "tokenRefreshed";
        wireMockRule.stubFor(authorized(get(urlPathEqualTo(ENDPOINT)), expiredToken)
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_ERR_UNAUTHORIZED)));
        wireMockRule.stubFor(authorized(get(urlPathEqualTo(ENDPOINT)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ACCEPTED)
                        .withBodyFile("all_projects.json")));

        // first token request returns the expired token
        wireMockRule.stubFor(post(urlPathEqualTo(TOKEN_ENDPOINT)).inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBody(accessTokenBody(expiredToken)))
                .willSetStateTo(stateRefreshed));
        // second token request returns the valid token
        wireMockRule.stubFor(post(urlPathEqualTo(TOKEN_ENDPOINT)).inScenario(scenario)
                .whenScenarioStateIs(stateRefreshed)
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBody(accessTokenBody(ACCESS_TOKEN.getToken()))));

        CyclicBarrier barrierStart = new CyclicBarrier(concurrentRequestCount);
        CountDownLatch latchCompletion = new CountDownLatch(concurrentRequestCount);
        for (int i = 0; i < concurrentRequestCount; i++) {
            new Thread(() -> {
                try {
                    // for maximum parallelism, wait for all threads to be started
                    barrierStart.await();
                    List<SW360Project> projects = waitFor(client.search(ProjectSearchParams.ALL_PROJECTS));
                    assertThat(projects).hasSize(4);
                    // thread completed successfully
                    latchCompletion.countDown();
                } catch (InterruptedException | IOException | BrokenBarrierException e) {
                    // in this case the test will fail as the latch is not triggered
                    e.printStackTrace();
                }
            }).start();
        }

        boolean success = latchCompletion.await(10, TimeUnit.SECONDS);
        assertThat(success).isTrue();
        WireMock.verify(2, postRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
    }
}
