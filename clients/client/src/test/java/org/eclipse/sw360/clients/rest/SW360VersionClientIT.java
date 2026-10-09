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
import org.eclipse.sw360.clients.rest.resource.version.VersionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;

public class SW360VersionClientIT extends AbstractMockServerTest {
    private SW360VersionClient versionClient;

    @BeforeEach
    public void setUp() {
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ConnectionFactory scf = new SW360ConnectionFactory();
            versionClient = scf.newConnection(createClientConfig()).getVersionAdapter().getVersionClient();
        } else {
            versionClient = new SW360VersionClient(createClientConfig(), createMockTokenProvider());
            prepareAccessTokens(versionClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
        }
    }

    @Test
    public void testGetVersion() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(get(urlPathEqualTo("/version"))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                            .withBody("{\"apiVersion\":\"1.0.0\",\"buildTime\":\"0\",\"buildNumber\":\"unknown\"," +
                                    "\"sw360Version\":\"20.1.0\",\"gitBranch\":\"main\"}")));
        }

        VersionData version = waitFor(versionClient.getVersion());
        assertThat(version).isNotNull();
        assertThat(version.getApiVersion()).isNotEmpty();
        if (!RUN_REST_INTEGRATION_TEST) {
            assertThat(version.getSw360Version()).isEqualTo("20.1.0");
            assertThat(version.getGitBranch()).isEqualTo("main");
        }
    }

    @Test
    public void testGetVersionError() {
        wireMockRule.stubFor(get(urlPathEqualTo("/version"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_SERVER)));

        CompletableFuture<VersionData> versionFuture;
        if (RUN_REST_INTEGRATION_TEST) {
            versionFuture = CompletableFuture.supplyAsync(() -> {
                throw new CompletionException(new FailedRequestException(SW360VersionClient.TAG_GET_VERSION,
                        HttpConstants.STATUS_ERR_SERVER));
            });
        } else {
            versionFuture = versionClient.getVersion();
        }

        FailedRequestException exception = expectFailedRequest(versionFuture, HttpConstants.STATUS_ERR_SERVER);
        assertThat(exception.getTag()).isEqualTo(SW360VersionClient.TAG_GET_VERSION);
    }
}
