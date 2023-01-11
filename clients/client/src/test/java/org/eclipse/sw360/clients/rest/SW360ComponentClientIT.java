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

import org.assertj.core.api.Assertions;
import org.eclipse.sw360.http.utils.FailedRequestException;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.eclipse.sw360.clients.adapter.SW360ComponentClientAdapterAsync;
import org.eclipse.sw360.clients.adapter.SW360ConnectionFactory;
import org.eclipse.sw360.clients.rest.resource.Paging;
import org.eclipse.sw360.clients.rest.resource.components.ComponentSearchParams;
import org.eclipse.sw360.clients.rest.resource.components.SW360Component;
import org.eclipse.sw360.clients.rest.resource.components.SW360ComponentEmbedded;
import org.eclipse.sw360.clients.rest.resource.components.SW360ComponentType;
import org.eclipse.sw360.clients.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.clients.rest.resource.releases.SW360SparseRelease;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

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
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class SW360ComponentClientIT extends AbstractMockServerTest {
	/**
	 * An array with the names of test components contained in the test file.
	 */
	private static final String[] TEST_COMPONENTS = {"jackson-annotations", "jakarta.validation-api", "jsoup"};

	/**
	 * Name of the test file containing a single component.
	 */
	private static final String FILE_COMPONENT = "component.json";

	private static final String FILE_COMPONENT_ALL = "all_components.json";

	private SW360ComponentClient componentClient;

	@Before
	public void setUp() {
		if (RUN_REST_INTEGRATION_TEST) {
			SW360ConnectionFactory scf = new SW360ConnectionFactory();
			SW360ComponentClientAdapterAsync componentClientAsync = scf.newConnection(createClientConfig())
					.getComponentAdapterAsync();
			componentClient = componentClientAsync.getComponentClient();
		} else {
			componentClient = new SW360ComponentClient(createClientConfig(), createMockTokenProvider());
			prepareAccessTokens(componentClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
		}
	}

	/**
	 * Checks whether a request yields the expected list of test components.
	 *
	 * @param components
	 *            the list with components
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
	 * @throws IOException
	 *             if an error occurs
	 */
	private static SW360Component componentFromJson() throws IOException {
		return readTestJsonFile(resolveTestFileURL(FILE_COMPONENT), SW360Component.class);
	}

	/**
	 * Returns a component instance that was read from the test JSON file.
	 *
	 * @return the component read from JSON
	 * @throws IOException
	 *             if an error occurs
	 */
	private static List<SW360Component> multipleComponentsFromJson() throws IOException {
		return readTestJsonFile(resolveTestFileURLForRealDB(FILE_COMPONENT_ALL),
				new TypeReference<List<SW360Component>>() {
				});
	}

	/**
	 * Returns a component instance that was read from the test JSON file.
	 *
	 * @return the component read from JSON
	 * @throws IOException
	 *             if an error occurs
	 */
	private static SW360Component componentFromJsonForIntegrationTest() throws IOException {
		return readTestJsonFile(resolveTestFileURLForRealDB(FILE_COMPONENT), SW360Component.class);
	}

	@Test
	public void testSearchNoParameters() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(get(urlPathEqualTo("/components")).withQueryParams(Collections.emptyMap())
					.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("all_components.json")));
			PagingResult<SW360SparseComponent> result = waitFor(
					componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
			checkComponentsList(result.getResult());
			assertThat(result.getPaging()).isNull();
			Assertions.assertThat(result.getPagingLinkObjects().getFirst()).isNull();
		} else {
			cleanup();
			List<SW360Component> components = multipleComponentsFromJson();
			components.stream().forEach(component -> {
				try {
					waitFor(componentClient.createComponent(component));
				} catch (IOException e) {
					System.err.println("Error creating test component");
				}
			});
			PagingResult<SW360SparseComponent> result = waitFor(
					componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
			checkComponentsList(result.getResult());
			assertThat(result.getPaging()).isNull();
			Assertions.assertThat(result.getPagingLinkObjects().getFirst()).isNull();
		}
	}

	@Test
	public void testSearchNoBody() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(get(urlPathEqualTo("/components")).willReturn(aJsonResponse(HttpConstants.STATUS_OK)));
			extractException(componentClient.search(ComponentSearchParams.ALL_COMPONENTS), IOException.class);
		} else {
			cleanup();
			PagingResult<SW360SparseComponent> components = waitFor(
					componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
			assertEquals(components.getResult().size(), 0);
		}
	}

	@Test
	public void testSearchNoContent() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(get(urlPathEqualTo("/components"))
					.willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));
			PagingResult<SW360SparseComponent> result = waitFor(
					componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
			assertThat(result.getResult()).isEmpty();
		}
		cleanup();
		PagingResult<SW360SparseComponent> result = waitFor(
				componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
		assertThat(result.getResult()).isEmpty();
	}

	@Test
	public void testSearchError() {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(
					get(urlPathEqualTo("/components")).willReturn(aJsonResponse(HttpConstants.STATUS_ERR_NOT_FOUND)));
		}

		FailedRequestException exception = expectFailedRequest(
				componentClient.search(ComponentSearchParams.ALL_COMPONENTS.builder().withPage(5).build()),
				HttpConstants.STATUS_ERR_NOT_FOUND);
		assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_GET_COMPONENTS);
	}

	@Test
	public void testSearchWithParameters() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			final String name = "desiredComponent";
			final SW360ComponentType componentType = SW360ComponentType.SERVICE;
			final int pageIndex = 42;
			final int pageSize = 11;
			wireMockRule.stubFor(get(urlPathEqualTo("/components")).withQueryParam("name", equalTo(name))
					.withQueryParam("type", equalTo(componentType.name()))
					.withQueryParam("page", equalTo(String.valueOf(pageIndex)))
					.withQueryParam("page_entries", equalTo(String.valueOf(pageSize)))
					.withQueryParam("fields", equalTo("name,createdOn,type,releaseIds"))
					.withQueryParam("sort", equalTo("name,ASC,createdOn,DESC"))
					.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("all_components_paging.json")));

			ComponentSearchParams params = ComponentSearchParams.builder().withName(name)
					.withComponentType(componentType).withPage(pageIndex).withPageSize(pageSize).orderAscending("name")
					.orderDescending("createdOn").retrieveFields("name", "createdOn", "type")
					.retrieveFields("releaseIds").build();
			PagingResult<SW360SparseComponent> result = waitFor(componentClient.search(params));
			checkComponentsList(result.getResult());
			assertThat(result.getPaging()).isEqualTo(new Paging(5, 1, 12, 3));
			Assertions.assertThat(result.getPagingLinkObjects().getFirst()).isNotNull();
		} else {
			cleanup();
			List<SW360Component> components = multipleComponentsFromJson();
			components.stream().forEach(component -> {
				try {
					waitFor(componentClient.createComponent(component));
				} catch (IOException e) {
					System.err.println("Error creating test component");
				}
			});
			final SW360ComponentType componentType = SW360ComponentType.OSS;
			final int pageIndex = 0;
			final int pageSize = 3;
			ComponentSearchParams params = ComponentSearchParams.builder().withComponentType(componentType)
					.withPage(pageIndex).withPageSize(pageSize).orderAscending("name").orderDescending("createdOn")
					.retrieveFields("name", "createdOn", "type").retrieveFields("releaseIds").build();
			PagingResult<SW360SparseComponent> result = waitFor(componentClient.search(params));
			checkComponentsList(result.getResult());
			assertThat(result.getPaging()).isEqualTo(new Paging(3, 0, 3, 1));
			Assertions.assertThat(result.getPagingLinkObjects().getFirst()).isNotNull();

		}
	}

	@Test
	public void testGetComponent() throws IOException {
		SW360Component component = null;
		SW360Component createdComponent = null;
		SW360Component get_component;
		final String componentId = "testComponentID";
		if (!RUN_REST_INTEGRATION_TEST) {
			component = componentFromJson();
			wireMockRule.stubFor(get(urlPathEqualTo("/components/" + componentId))
					.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile(FILE_COMPONENT)));
			get_component = waitFor(componentClient.getComponent(componentId));
			assertThat(get_component.getName()).isEqualTo("jackson-annotations");
			SW360ComponentEmbedded embedded = get_component.getEmbedded();
			assertThat(embedded.getCreatedBy().getEmail()).isEqualTo("osi9be@bosch.com");
			List<SW360SparseRelease> releases = embedded.getReleases();
			assertThat(releases).hasSize(10);
		} else {
			component = componentFromJsonForIntegrationTest();
			createdComponent = waitFor(componentClient.createComponent(component));
			get_component = waitFor(componentClient.getComponent(createdComponent.getId()));
			assertThat(get_component.getName()).isEqualTo("jackson-annotations");
			List<SW360SparseRelease> releases = get_component.getEmbedded().getReleases();
			assertThat(releases).hasSize(0);
		}
	}

	@Test
	public void testGetComponentNotFound() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(get(anyUrl()).willReturn(aJsonResponse(HttpConstants.STATUS_ERR_NOT_FOUND)));

			FailedRequestException exception = expectFailedRequest(componentClient.getComponent("unknownComponent"),
					HttpConstants.STATUS_ERR_NOT_FOUND);
			assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_GET_COMPONENT);
		} else {
			FailedRequestException exception = expectFailedRequest(componentClient.getComponent("unknownComponent"),
					HttpConstants.STATUS_ERR_SERVER);
			assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_GET_COMPONENT);
		}
	}

	@Test
	public void testGetComponentEmptyBody() {
		wireMockRule.stubFor(get(anyUrl()).willReturn(aJsonResponse(HttpConstants.STATUS_OK)));

		extractException(componentClient.getComponent("bar"), IOException.class);
	}

	@Test
	public void testCreateComponent() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			SW360Component component = componentFromJson();
			wireMockRule.stubFor(post(urlPathEqualTo("/components"))
					.willReturn(aJsonResponse(HttpConstants.STATUS_CREATED).withBodyFile(FILE_COMPONENT)));
			SW360Component createdComponent = waitFor(componentClient.createComponent(component));
			assertThat(createdComponent).isEqualTo(component);
		} else {
			cleanup();
			SW360Component component = componentFromJsonForIntegrationTest();
			SW360Component createdComponent = waitFor(componentClient.createComponent(component));
			assertEquals(createdComponent.getName(), component.getName());
			assertEquals(createdComponent.getComponentType(), component.getComponentType());
			assertEquals(createdComponent.getCreatedOn(), component.getCreatedOn());
			assertEquals(createdComponent.getHomepage(), component.getHomepage());
		}
	}

	private void cleanup() throws IOException {
		PagingResult<SW360SparseComponent> allComponentsWithPaging = waitFor(
				componentClient.search(ComponentSearchParams.ALL_COMPONENTS.builder().build()));
		List<SW360SparseComponent> allComponents = allComponentsWithPaging.getResult();
		List<String> componentIds = allComponents.stream().map(x -> x.getId()).collect(Collectors.toList());
		if (!componentIds.isEmpty()) {
			waitFor(componentClient.deleteComponents(componentIds));
		}
	}

	@Test
	public void testCreateComponentError() throws IOException {
		SW360Component component = componentFromJson();
		component.setName("");
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(post(urlPathEqualTo("/components"))
					.willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));
			FailedRequestException exception = expectFailedRequest(componentClient.createComponent(component),
					HttpConstants.STATUS_ERR_BAD_REQUEST);
			assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_CREATE_COMPONENT);
		} else {
			cleanup();
			FailedRequestException exception = expectFailedRequest(componentClient.createComponent(component),
					HttpConstants.STATUS_ERR_BAD_REQUEST);
			assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_CREATE_COMPONENT);
		}
	}

	@Test
	public void testPatchComponent() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			SW360Component component = componentFromJson();
			SW360Component componentUpdated = componentFromJson();
			component.setName("toBeUpdated");
			wireMockRule.stubFor(patch(urlPathEqualTo("/components/" + component.getId()))
					.withRequestBody(equalToJson(toJson(component)))
					.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile(FILE_COMPONENT)));

			SW360Component result = waitFor(componentClient.patchComponent(component));
			assertThat(result).isEqualTo(componentUpdated);
		} else {
			cleanup();
			SW360Component component = componentFromJsonForIntegrationTest();
			component.setName("toBeUpdated");
			SW360Component createdComponent = waitFor(componentClient.createComponent(component));
			SW360Component result = waitFor(componentClient.patchComponent(createdComponent));
			assertEquals(result.getName(), "toBeUpdated");
		}
	}

	@Test
	public void testPatchComponentError() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			SW360Component component = componentFromJson();
			wireMockRule.stubFor(patch(urlPathEqualTo("/components/" + component.getId()))
					.withRequestBody(equalToJson(toJson(component)))
					.willReturn(aJsonResponse(HttpConstants.STATUS_ERR_UNAUTHORIZED)));

			FailedRequestException exception = expectFailedRequest(componentClient.patchComponent(component),
					HttpConstants.STATUS_ERR_UNAUTHORIZED);
			assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_UPDATE_COMPONENT);
		} else {
			cleanup();
			SW360Component component = componentFromJsonForIntegrationTest();
			SW360Component createdComponent = waitFor(componentClient.createComponent(component));
			createdComponent.setName("");
			FailedRequestException exception = expectFailedRequest(componentClient.patchComponent(createdComponent),
					HttpConstants.STATUS_ERR_BAD_REQUEST);
		}
	}

	@Test
	public void testDeleteComponents() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			String compId1 = "res-1";
			String compId2 = "res-2";
			wireMockRule.stubFor(delete(urlPathEqualTo("/components/" + compId1 + "," + compId2)).willReturn(
					aJsonResponse(HttpConstants.STATUS_MULTI_STATUS).withBodyFile("multi_status_success.json")));

			MultiStatusResponse multiResponse = waitFor(
					componentClient.deleteComponents(Arrays.asList(compId1, compId2)));
			assertThat(multiResponse.responseCount()).isEqualTo(2);
			assertThat(multiResponse.getStatus("res-1")).isEqualTo(200);
			assertThat(multiResponse.getStatus("res-2")).isEqualTo(200);
		} else {
			cleanup();
			List<SW360Component> components = multipleComponentsFromJson();
			components.stream().forEach(component -> {
				try {
					waitFor(componentClient.createComponent(component));
				} catch (IOException e) {
					System.err.println("Error creating test component");
				}
			});
			PagingResult<SW360SparseComponent> result = waitFor(
					componentClient.search(ComponentSearchParams.ALL_COMPONENTS));
			List<String> ids = result.getResult().stream().map(component -> component.getId())
					.collect(Collectors.toList());
			MultiStatusResponse multiResponse = waitFor(componentClient.deleteComponents(ids));
			assertThat(multiResponse.responseCount()).isEqualTo(3);
			ids.stream().forEach(id -> {
				assertThat(multiResponse.getStatus(id)).isEqualTo(200);
			});
		}
	}

	@Test
	public void testDeleteComponentsUnexpectedStatus() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(delete(anyUrl())
					.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("multi_status_success.json")));

			FailedRequestException exception = expectFailedRequest(
					componentClient.deleteComponents(Collections.singletonList("c1")), HttpConstants.STATUS_OK);
			assertThat(exception.getTag()).isEqualTo(SW360ComponentClient.TAG_DELETE_COMPONENTS);
		} else {
			cleanup();
			MultiStatusResponse response = waitFor(componentClient.deleteComponents(Collections.singletonList("c1")));
			assertEquals(response.getStatus("c1"), HttpConstants.STATUS_ERR_SERVER);
			assertEquals(response.getResponses().size(), 1);
		}
	}

	@Test
	public void testDeleteComponentsUnexpectedResponse() throws IOException {
		if (!RUN_REST_INTEGRATION_TEST) {
			wireMockRule.stubFor(delete(anyUrl())
					.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("all_components.json")));
			extractException(componentClient.deleteComponents(Collections.singletonList("cDel")), IOException.class);
		} else {
			cleanup();
			MultiStatusResponse response = waitFor(componentClient.deleteComponents(Collections.singletonList("cDel")));
			assertEquals(response.getStatus("cDel"), HttpConstants.STATUS_ERR_SERVER);
		}
	}

	@Test
	public void testDeleteComponentsEmptyResponse() {
		// skipping this for real DB as it will be duplicate as
		// `testDeleteComponentsUnexpectedResponse()`
		assumeFalse(RUN_REST_INTEGRATION_TEST);
		wireMockRule.stubFor(delete(anyUrl()).willReturn(aResponse().withStatus(HttpConstants.STATUS_MULTI_STATUS)));

		extractException(componentClient.deleteComponents(Collections.singletonList("cDel")), IOException.class);
	}
}
