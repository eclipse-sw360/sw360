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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Paging;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.ComponentSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.antenna.http.utils.HttpUtils.waitFor;

public class SW360ComponentClientIT extends AbstractMockServerTest {
    /**
     * An array with the names of test components contained in the test file.
     */
    private static final String[] TEST_COMPONENTS = {
            "jackson-annotations", "jakarta.validation-api", "jsoup"
    };

    /**
     * Name of the test file containing a single component.
     */
    private static final String FILE_COMPONENT = "component.json";

    private SW360ComponentClient componentClient;

    @Before
    public void setUp() {
        componentClient = new SW360ComponentClient(createClientConfig(), createMockTokenProvider());
        prepareAccessTokens(componentClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
    }

    /**
     * Checks whether a request yields the expected list of test components.
     *
     * @param components the list with components
     */
    private static void checkComponentsList(List<SW360SparseComponent> components) {
        assertThat(components).hasSize(TEST_COMPONENTS.length);
        List<String> componentNames = components.stream().map(SW360SparseComponent::getName)
                .collect(Collectors.toList());
        assertThat(componentNames).containsExactlyInAnyOrder(TEST_COMPONENTS);
        assertHasLinks(components);
    }

    /**
     * Returns a component instance that was read from the test JSON file.
     *
     * @return the component read from JSON
     * @throws IOException if an error occurs
     */
    private static SW360Component componentFromJson() throws IOException {
        return readTestJsonFile(resolveTestFileURL(FILE_COMPONENT), SW360Component.class);
    }

    @Test
    public void testSearchNoParameters() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/components"))
                .withQueryParams(Collections.emptyMap())
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_components.json")));

        PagingResult<SW360SparseComponent> result =
                waitFor(componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
        checkComponentsList(result.getResult());
        assertThat(result.getPaging()).isNull();
        assertThat(result.getPagingLinkObjects().getFirst()).isNull();
    }

    @Test
    public void testSearchNoBody() {
        wireMockRule.stubFor(get(urlPathEqualTo("/components"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)));

        extractException(componentClient.search(ComponentSearchParams.ALL_COMPONENTS), IOException.class);
    }

    @Test
    public void testSearchNoContent() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/components"))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

        PagingResult<SW360SparseComponent> result =
                waitFor(componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
        assertThat(result.getResult()).isEmpty();
    }

    @Test
    public void testSearchError() {
        wireMockRule.stubFor(get(urlPathEqualTo("/components"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception =
                expectFailedRequest(componentClient.search(ComponentSearchParams.ALL_COMPONENTS),
                        HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_GET_COMPONENTS);
    }

    @Test
    public void testSearchWithParameters() throws IOException {
        final String name = "desiredComponent";
        final SW360ComponentType componentType = SW360ComponentType.SERVICE;
        final int pageIndex = 42;
        final int pageSize = 11;
        wireMockRule.stubFor(get(urlPathEqualTo("/components"))
                .withQueryParam("name", equalTo(name))
                .withQueryParam("type", equalTo(componentType.name()))
                .withQueryParam("page", equalTo(String.valueOf(pageIndex)))
                .withQueryParam("page_entries", equalTo(String.valueOf(pageSize)))
                .withQueryParam("fields", equalTo("name,createdOn,type,releaseIds"))
                .withQueryParam("sort", equalTo("name,ASC,createdOn,DESC"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_components_paging.json")));

        ComponentSearchParams params = ComponentSearchParams.builder()
                .withName(name)
                .withComponentType(componentType)
                .withPage(pageIndex)
                .withPageSize(pageSize)
                .orderAscending("name")
                .orderDescending("createdOn")
                .retrieveFields("name", "createdOn", "type")
                .retrieveFields("releaseIds")
                .build();
        PagingResult<SW360SparseComponent> result = waitFor(componentClient.search(params));
        checkComponentsList(result.getResult());
        assertThat(result.getPaging()).isEqualTo(new Paging(5, 1, 12, 3));
        assertThat(result.getPagingLinkObjects().getFirst()).isNotNull();
    }

    @Test
    public void testGetComponent() throws IOException {
        final String componentId = "testComponentID";
        wireMockRule.stubFor(get(urlPathEqualTo("/components/" + componentId))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile(FILE_COMPONENT)));

        SW360Component component = waitFor(componentClient.getComponent(componentId));
        assertThat(component.getName()).isEqualTo("jackson-annotations");
        SW360ComponentEmbedded embedded = component.getEmbedded();
        assertThat(embedded.getCreatedBy().getEmail()).isEqualTo("osi9be@bosch.com");
        List<SW360SparseRelease> releases = embedded.getReleases();
        assertThat(releases).hasSize(10);
    }

    @Test
    public void testGetComponentNotFound() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_NOT_FOUND)));

        FailedRequestException exception =
                expectFailedRequest(componentClient.getComponent("unknownComponent"),
                        HttpConstants.STATUS_ERR_NOT_FOUND);
        assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_GET_COMPONENT);
    }

    @Test
    public void testGetComponentEmptyBody() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)));

        extractException(componentClient.getComponent("bar"), IOException.class);
    }

    @Test
    public void testCreateComponent() throws IOException {
        SW360Component component = componentFromJson();
        wireMockRule.stubFor(post(urlPathEqualTo("/components"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_CREATED)
                        .withBodyFile(FILE_COMPONENT)));

        SW360Component createdComponent = waitFor(componentClient.createComponent(component));
        assertThat(createdComponent).isEqualTo(component);
    }

    @Test
    public void testCreateComponentError() throws IOException {
        SW360Component component = componentFromJson();
        wireMockRule.stubFor(post(urlPathEqualTo("/components"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception =
                expectFailedRequest(componentClient.createComponent(component), HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_CREATE_COMPONENT);
    }

    @Test
    public void testPatchComponent() throws IOException {
        SW360Component component = componentFromJson();
        SW360Component componentUpdated = componentFromJson();
        component.setName("toBeUpdated");
        wireMockRule.stubFor(patch(urlPathEqualTo("/components/" + component.getId()))
                .withRequestBody(equalToJson(toJson(component)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile(FILE_COMPONENT)));

        SW360Component result = waitFor(componentClient.patchComponent(component));
        assertThat(result).isEqualTo(componentUpdated);
    }

    @Test
    public void testPatchComponentError() throws IOException {
        SW360Component component = componentFromJson();
        wireMockRule.stubFor(patch(urlPathEqualTo("/components/" + component.getId()))
                .withRequestBody(equalToJson(toJson(component)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_UNAUTHORIZED)));

        FailedRequestException exception =
                expectFailedRequest(componentClient.patchComponent(component), HttpConstants.STATUS_ERR_UNAUTHORIZED);
        assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_UPDATE_COMPONENT);
    }

    @Test
    public void testDeleteComponents() throws IOException {
        String compId1 = "res-1";
        String compId2 = "res-2";
        wireMockRule.stubFor(delete(urlPathEqualTo("/components/" + compId1 + "," + compId2))
                .willReturn(aJsonResponse(HttpConstants.STATUS_MULTI_STATUS)
                        .withBodyFile("multi_status_success.json")));

        MultiStatusResponse multiResponse = waitFor(componentClient.deleteComponents(Arrays.asList(compId1, compId2)));
        assertThat(multiResponse.responseCount()).isEqualTo(2);
        assertThat(multiResponse.getStatus("res-1")).isEqualTo(200);
        assertThat(multiResponse.getStatus("res-2")).isEqualTo(200);
    }

    @Test
    public void testDeleteComponentsUnexpectedStatus() {
        wireMockRule.stubFor(delete(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("multi_status_success.json")));

        FailedRequestException exception =
                expectFailedRequest(componentClient.deleteComponents(Collections.singletonList("c1")),
                        HttpConstants.STATUS_OK);
        assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_DELETE_COMPONENTS);
    }

    @Test
    public void testDeleteComponentsUnexpectedResponse() {
        wireMockRule.stubFor(delete(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_components.json")));

        extractException(componentClient.deleteComponents(Collections.singletonList("cDel")), IOException.class);
    }

    @Test
    public void testDeleteComponentsEmptyResponse() {
        wireMockRule.stubFor(delete(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_MULTI_STATUS)));

        extractException(componentClient.deleteComponents(Collections.singletonList("cDel")), IOException.class);
    }
}
