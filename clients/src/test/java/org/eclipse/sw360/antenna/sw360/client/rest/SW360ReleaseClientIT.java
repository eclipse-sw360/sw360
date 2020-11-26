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

import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.antenna.http.utils.HttpUtils.waitFor;

public class SW360ReleaseClientIT extends AbstractMockServerTest {
    /**
     * Defines the expected test release data from the test JSON file.
     */
    private static final String[][] TEST_RELEASE_DATA = {
            {"handlebars", "4.0.1"},
            {"springdoc-openapi-ui", "1.1.45"},
            {"resteasy-rxjava2", "3.7.0.Final"},
            {"akka-actor_2.11", "2.4.12"},
            {"spring-boot-starter-web", "2.2.1.RELEASE"},
            {"jackson-module-kotlin", "2.9.8"}
    };

    private SW360ReleaseClient releaseClient;

    @Before
    public void setUp() {
        releaseClient = new SW360ReleaseClient(createClientConfig(), createMockTokenProvider());
        prepareAccessTokens(releaseClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
    }

    /**
     * Checks whether a request for multiple releases yields the expected
     * result.
     *
     * @param releases the list with releases returned by the test client
     */
    private static void checkReleaseData(List<SW360SparseRelease> releases) {
        assertThat(releases).hasSize(TEST_RELEASE_DATA.length);
        List<String> expData = Arrays.stream(TEST_RELEASE_DATA)
                .map(release -> release[0] + ":" + release[1])
                .collect(Collectors.toList());
        List<String> releaseData = releases.stream()
                .map(release -> release.getName() + ":" + release.getVersion())
                .collect(Collectors.toList());
        assertThat(releaseData).isEqualTo(expData);

        assertHasLinks(releases);
    }

    @Test
    public void testGetRelease() throws IOException {
        final String releaseId = "testRelease";
        wireMockRule.stubFor(get(urlPathEqualTo("/releases/" + releaseId))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("release.json")));

        SW360Release release = waitFor(releaseClient.getRelease(releaseId));
        assertThat(release.getName()).isEqualTo("akka-actor_2.11");
        assertThat(release.getVersion()).isEqualTo("2.4.12");
        assertThat(release.getExternalIds()).contains(new AbstractMap.SimpleEntry<>("hash_1", "501887b9053ef9f4341a"));

        Set<SW360SparseAttachment> attachments = release.getEmbedded().getAttachments();
        assertThat(attachments).hasSize(1);
        SW360SparseAttachment attachment = attachments.iterator().next();
        assertThat(attachment.getAttachmentType()).isEqualTo(SW360AttachmentType.SOURCE);
        assertThat(attachment.getFilename()).isEqualTo("artifact-sources.jar");
        assertThat(attachment.getSha1()).isEqualTo("9fa75ed24ee85514f63046a39697509c78f536de");
    }

    @Test
    public void testGetReleaseEmptyBody() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)));

        extractException(releaseClient.getRelease("foo"), IOException.class);
    }

    @Test
    public void testGetReleasesByExternalIds() throws IOException {
        Map<String, Object> idMap = new LinkedHashMap<>();
        idMap.put("id 1", "testRelease");
        idMap.put("id2", "otherFilter");
        wireMockRule.stubFor(get(urlPathEqualTo("/releases/searchByExternalIds"))
                .withQueryParam("id+1", equalTo("testRelease"))
                .withQueryParam("id2", equalTo("otherFilter"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_releases.json")));

        List<SW360SparseRelease> releases = waitFor(releaseClient.getReleasesByExternalIds(idMap));
        checkReleaseData(releases);
    }

    @Test
    public void testGetReleasesByExternalIdsStatusNoContent() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/releases/searchByExternalIds"))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

        List<SW360SparseRelease> releases = waitFor(releaseClient.getReleasesByExternalIds(new HashMap<>()));
        assertThat(releases).isEmpty();
    }

    @Test
    public void testGetReleasesByExternalIdsError() {
        wireMockRule.stubFor(get(urlPathEqualTo("/releases/searchByExternalIds"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception =
                expectFailedRequest(releaseClient.getReleasesByExternalIds(new HashMap<>()),
                        HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_GET_RELEASES_BY_EXTERNAL_IDS);
    }

    @Test
    public void testCreateRelease() throws IOException {
        SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        String releaseJson = toJson(release);
        wireMockRule.stubFor(post(urlEqualTo("/releases"))
                .withRequestBody(equalToJson(releaseJson))
                .willReturn(aJsonResponse(HttpConstants.STATUS_CREATED)
                        .withBody(releaseJson)));

        SW360Release createdRelease = waitFor(releaseClient.createRelease(release));
        assertThat(createdRelease).isEqualTo(release);
    }

    @Test
    public void testCreateReleaseError() throws IOException {
        SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        wireMockRule.stubFor(post(urlEqualTo("/releases"))
                .withRequestBody(equalToJson(toJson(release)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception =
                expectFailedRequest(releaseClient.createRelease(release), HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_CREATE_RELEASE);
    }

    @Test
    public void testPatchRelease() throws IOException {
        SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        String releaseJson = toJson(release);
        wireMockRule.stubFor(patch(urlEqualTo("/releases/" + release.getId()))
                .withRequestBody(equalToJson(releaseJson))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ACCEPTED)
                        .withBody(releaseJson)));

        SW360Release patchedRelease = waitFor(releaseClient.patchRelease(release));
        assertThat(patchedRelease).isEqualTo(release);
    }

    @Test
    public void testPatchReleaseError() throws IOException {
        SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        wireMockRule.stubFor(patch(urlEqualTo("/releases/" + release.getId()))
                .withRequestBody(equalToJson(toJson(release)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception =
                expectFailedRequest(releaseClient.patchRelease(release), HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_UPDATE_RELEASE);
    }

    @Test
    public void testDeleteReleases() throws IOException {
        String relId1 = "res-1";
        String relId2 = "res-2";
        wireMockRule.stubFor(delete(urlPathEqualTo("/releases/" + relId1 + "," + relId2))
                .willReturn(aJsonResponse(HttpConstants.STATUS_MULTI_STATUS)
                        .withBodyFile("multi_status_success.json")));

        MultiStatusResponse multiResponse = waitFor(releaseClient.deleteReleases(Arrays.asList(relId1, relId2)));
        assertThat(multiResponse.responseCount()).isEqualTo(2);
        assertThat(multiResponse.getStatus("res-1")).isEqualTo(200);
        assertThat(multiResponse.getStatus("res-2")).isEqualTo(200);
    }

    @Test
    public void testDeleteReleasesFailure() {
        wireMockRule.stubFor(delete(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_SERVER)));

        FailedRequestException exception =
                expectFailedRequest(releaseClient.deleteReleases(Collections.singleton("relDelFail")),
                        HttpConstants.STATUS_ERR_SERVER);
        assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_DELETE_RELEASES);
    }
}
