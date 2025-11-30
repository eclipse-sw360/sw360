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
package org.eclipse.sw360.clients.rest;

import org.eclipse.sw360.http.utils.FailedRequestException;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.eclipse.sw360.clients.adapter.SW360ComponentClientAdapterAsync;
import org.eclipse.sw360.clients.adapter.SW360ConnectionFactory;
import org.eclipse.sw360.clients.adapter.SW360ReleaseClientAdapterAsync;
import org.eclipse.sw360.clients.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.clients.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.clients.rest.resource.components.ComponentSearchParams;
import org.eclipse.sw360.clients.rest.resource.components.SW360Component;
import org.eclipse.sw360.clients.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.clients.rest.resource.releases.SW360Release;
import org.eclipse.sw360.clients.rest.resource.releases.SW360SparseRelease;
import org.junit.Before;
import org.junit.Ignore;
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
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


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
    private SW360ReleaseClient releaseClientAlt;
    private SW360ComponentClient componentClient;
    private static final String FILE_COMPONENT = "component.json";

    @Before
    public void setUp() {
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ConnectionFactory scf = new SW360ConnectionFactory();
            SW360ReleaseClientAdapterAsync releaseClientAsync = scf.newConnection(createClientConfig())
                    .getReleaseAdapterAsync();
            SW360ComponentClientAdapterAsync componentClientAsync = scf.newConnection(createClientConfig())
                    .getComponentAdapterAsync();
            SW360ReleaseClientAdapterAsync releaseClientAsyncAlt = scf.newConnection(createClientConfigAlt())
                    .getReleaseAdapterAsync();
            componentClient = componentClientAsync.getComponentClient();
            releaseClient = releaseClientAsync.getReleaseClient();
            releaseClientAlt = releaseClientAsyncAlt.getReleaseClient();
        } else {
            releaseClient = new SW360ReleaseClient(createClientConfig(), createMockTokenProvider());
            releaseClientAlt = new SW360ReleaseClient(createClientConfigAlt(), createMockTokenProvider());
            prepareAccessTokens(releaseClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
            prepareAccessTokens(releaseClientAlt.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
        }
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
        if(!RUN_REST_INTEGRATION_TEST) {
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
        } else {
            cleanupComponent(componentClient);
            SW360Component component = componentFromJsonForIntegrationTest();
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            SW360Release release = waitFor(releaseClient.createRelease(sw360Release));
            SW360Release get_release = waitFor(releaseClient.getRelease(release.getId()));
            assertNotNull(get_release);
            assertThat(release.getName()).isEqualTo(get_release.getName());
            assertThat(release.getVersion()).isEqualTo(get_release.getVersion());
            cleanupRelease(release, releaseClient);
            cleanupComponent(componentClient);
        }
    }

    @Test
    public void testGetReleaseEmptyBody() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)));

        extractException(releaseClient.getRelease("foo"), IOException.class);
    }

    @Test
    public void testGetReleasesByExternalIds() throws IOException {
        if(!RUN_REST_INTEGRATION_TEST) {
            Map<String, Object> idMap = new LinkedHashMap<>();
            idMap.put("id1", "testRelease");
            idMap.put("id2", "otherFilter");
            wireMockRule.stubFor(get(urlPathEqualTo("/releases/searchByExternalIds"))
                    .withQueryParam("id1", equalTo("testRelease"))
                    .withQueryParam("id2", equalTo("otherFilter"))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                            .withBodyFile("all_releases.json")));
            
            List<SW360SparseRelease> releases = waitFor(releaseClient.getReleasesByExternalIds(idMap));
            checkReleaseData(releases);
        } else {
            Map<String, List<String>> externalIds = new LinkedHashMap<>();
            externalIds.put("id1", List.of("testRelease"));
            externalIds.put("id2", List.of("otherFilter"));
            Map<String, String> idMap = new LinkedHashMap<>();
            idMap.put("id1", "testRelease");
            idMap.put("id2", "otherFilter");
            cleanupComponent(componentClient);
            SW360Component component = componentFromJsonForIntegrationTest();
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setExternalIds(idMap);
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            SW360Release release = waitFor(releaseClient.createRelease(sw360Release));
            List<SW360SparseRelease> releases = waitFor(releaseClient.getReleasesByExternalIds(idMap));
            assertEquals(releases.size(), 1);
            cleanupRelease(release, releaseClient);
            cleanupComponent(componentClient);
        }
    }

    @Test
    public void testGetReleasesByExternalIdsStatusNoContent() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/releases/searchByExternalIds"))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

        List<SW360SparseRelease> releases = waitFor(releaseClient.getReleasesByExternalIds(new HashMap<>()));
        assertThat(releases).isEmpty();
    }

    @Test
    public void testGetReleasesByExternalIdsError() throws IOException {
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.stubFor(get(urlPathEqualTo("/releases/searchByExternalIds"))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

            FailedRequestException exception = expectFailedRequest(
                    releaseClient.getReleasesByExternalIds(new HashMap<>()), HttpConstants.STATUS_ERR_BAD_REQUEST);
            assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_GET_RELEASES_BY_EXTERNAL_IDS);
        } else {
            cleanupComponent(componentClient);
            List<SW360SparseRelease> releases = waitFor(releaseClient.getReleasesByExternalIds(new HashMap<>()));
            assertEquals(releases.size(), 0);
        }
    }

    @Test
    public void testCreateRelease() throws IOException {
        if(!RUN_REST_INTEGRATION_TEST) {
            SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
            String releaseJson = toJson(release);
            wireMockRule.stubFor(post(urlEqualTo("/releases"))
                    .withRequestBody(equalToJson(releaseJson))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_CREATED)
                            .withBody(releaseJson)));

            SW360Release createdRelease = waitFor(releaseClient.createRelease(release));
            assertThat(createdRelease).isEqualTo(release);
        } else {
            cleanupComponent(componentClient);
            SW360Component component = componentFromJsonForIntegrationTest();
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            SW360Release release = waitFor(releaseClient.createRelease(sw360Release));
            assertNotNull(release);
            cleanupRelease(release, releaseClient);
            cleanupComponent(componentClient);
        }
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
        if (!RUN_REST_INTEGRATION_TEST) {
            SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
            String releaseJson = toJson(release);
            wireMockRule
                    .stubFor(patch(urlEqualTo("/releases/" + release.getId())).withRequestBody(equalToJson(releaseJson))
                            .willReturn(aJsonResponse(HttpConstants.STATUS_ACCEPTED).withBody(releaseJson)));

            SW360Release patchedRelease = waitFor(releaseClient.patchRelease(release));
            assertThat(patchedRelease).isEqualTo(release);
        } else {
            cleanupComponent(componentClient);
            SW360Component component = componentFromJsonForIntegrationTest();
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            SW360Release release = waitFor(releaseClient.createRelease(sw360Release));
            SW360Release patchedRelease = waitFor(releaseClient.patchRelease(release));
            assertEquals(release.getName(), patchedRelease.getName());
            cleanupRelease(release, releaseClient);
            cleanupComponent(componentClient);
        }
    }

    @Test
    public void testPatchReleaseError() throws IOException {
        if(!RUN_REST_INTEGRATION_TEST) {
            SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
            wireMockRule.stubFor(patch(urlEqualTo("/releases/" + release.getId()))
                    .withRequestBody(equalToJson(toJson(release)))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

            FailedRequestException exception =
                    expectFailedRequest(releaseClient.patchRelease(release), HttpConstants.STATUS_ERR_BAD_REQUEST);
            assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_UPDATE_RELEASE);
        } else {
            cleanupComponent(componentClient);
            SW360Component component = componentFromJsonForIntegrationTest();
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            SW360Release release = waitFor(releaseClient.createRelease(sw360Release));
            release.setComponentId("Blabla");
            FailedRequestException exception = expectFailedRequest(releaseClient.patchRelease(release),
                    HttpConstants.STATUS_ERR_BAD_REQUEST);
            assertThat(exception.getTag()).isEqualTo(SW360ReleaseClient.TAG_UPDATE_RELEASE);
            cleanupRelease(release, releaseClient);
            cleanupComponent(componentClient);
        }
    }

    public static void cleanupComponent(SW360ComponentClient componentClient) throws IOException {
        PagingResult<SW360SparseComponent> allComponentsWithPaging = waitFor(
                componentClient.search(ComponentSearchParams.ALL_COMPONENTS.builder().build()));
        List<SW360SparseComponent> allComponents = allComponentsWithPaging.getResult();
        List<String> componentIds = allComponents.stream().map(x -> x.getId()).collect(Collectors.toList());
        if (!componentIds.isEmpty()) {
            waitFor(componentClient.deleteComponents(componentIds));
        }
    }

    public static void cleanupRelease(SW360Release release, SW360ReleaseClient releaseClient) throws IOException {
        waitFor(releaseClient.deleteReleases(Collections.singleton(release.getId())));
    }

    @Test
    public void testDeleteReleases() throws IOException {
        if(!RUN_REST_INTEGRATION_TEST) {
            String relId1 = "res-1";
            String relId2 = "res-2";
            wireMockRule.stubFor(delete(urlPathEqualTo("/releases/" + relId1 + "," + relId2))
                    .willReturn(aJsonResponse(HttpConstants.STATUS_MULTI_STATUS)
                            .withBodyFile("multi_status_success.json")));

            MultiStatusResponse multiResponse = waitFor(releaseClient.deleteReleases(Arrays.asList(relId1, relId2)));
            assertThat(multiResponse.responseCount()).isEqualTo(2);
            assertThat(multiResponse.getStatus("res-1")).isEqualTo(200);
            assertThat(multiResponse.getStatus("res-2")).isEqualTo(200);
        } else {
            cleanupComponent(componentClient);
            SW360Component component = componentFromJsonForIntegrationTest();
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            SW360Release sw360Release1 = new SW360Release();
            sw360Release1.setComponentId(createdComponent.getId());
            sw360Release1.setVersion("1.2");
            SW360Release release = waitFor(releaseClient.createRelease(sw360Release));
            SW360Release release1 = waitFor(releaseClient.createRelease(sw360Release1));
            MultiStatusResponse multiResponse = waitFor(releaseClient.deleteReleases(Arrays.asList(release.getId(), release1.getId())));
            assertThat(multiResponse.responseCount()).isEqualTo(2);
            assertThat(multiResponse.getStatus(release.getId())).isEqualTo(200);
            assertThat(multiResponse.getStatus(release1.getId())).isEqualTo(200);
            cleanupComponent(componentClient);
        }
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

    /**
     * Returns a component instance that was read from the test JSON file.
     *
     * @return the component read from JSON
     * @throws IOException if an error occurs
     */
    public static SW360Component componentFromJsonForIntegrationTest() throws IOException {
        return readTestJsonFile(resolveTestFileURLForRealDB(FILE_COMPONENT), SW360Component.class);
    }
}
