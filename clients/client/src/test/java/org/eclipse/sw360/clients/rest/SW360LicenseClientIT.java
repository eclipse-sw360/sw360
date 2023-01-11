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
import org.eclipse.sw360.clients.adapter.SW360ConnectionFactory;
import org.eclipse.sw360.clients.adapter.SW360LicenseClientAdapterAsync;
import org.eclipse.sw360.clients.rest.resource.licenses.SW360License;
import org.eclipse.sw360.clients.rest.resource.licenses.SW360SparseLicense;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;

public class SW360LicenseClientIT extends AbstractMockServerTest {
	/**
	 * An array with the names of the licenses in the test data.
	 */
	private static final String[] TEST_LICENSES = {"BSD Zero Clause License", "Attribution Assurance License",
			"Amazon Digital Services License", "Academic Free License v1.1", "XPP License"};

	private SW360LicenseClient licenseClient;

	@Before
	public void setUp() {
		if (RUN_REST_INTEGRATION_TEST) {
			SW360ConnectionFactory scf = new SW360ConnectionFactory();
			SW360LicenseClientAdapterAsync licenseClientAsync = scf.newConnection(createClientConfig())
					.getLicenseAdapterAsync();
			licenseClient = licenseClientAsync.getLicenseClient();
		} else {
			licenseClient = new SW360LicenseClient(createClientConfig(), createMockTokenProvider());
			prepareAccessTokens(licenseClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
		}
	}

	/**
	 * Tests the passed in list of license data against the expected test data.
	 *
	 * @param licenses
	 *            the collection with license data to be checked
	 */
	private static void checkLicenses(List<? extends SW360SparseLicense> licenses) {
		List<String> actualLicenses = licenses.stream().map(SW360SparseLicense::getFullName)
				.collect(Collectors.toList());
		assertThat(actualLicenses).containsExactlyInAnyOrder(TEST_LICENSES);
		assertHasLinks(licenses);
	}

	@Test
	public void testGetLicenses() throws IOException {
		wireMockRule.stubFor(get(urlPathEqualTo("/licenses"))
				.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("all_licenses.json")));

		SW360License licenseCreated1 = readTestJsonFile(resolveTestFileURL("licenseBSD.json"), SW360License.class);
		createLicense(licenseCreated1);
		SW360License licenseCreated2 = readTestJsonFile(resolveTestFileURL("licenseAAL.json"), SW360License.class);
		createLicense(licenseCreated2);
		SW360License licenseCreated3 = readTestJsonFile(resolveTestFileURL("licenseADSL.json"), SW360License.class);
		createLicense(licenseCreated3);
		SW360License licenseCreated4 = readTestJsonFile(resolveTestFileURL("licenseAFL.json"), SW360License.class);
		createLicense(licenseCreated4);
		SW360License licenseCreated5 = readTestJsonFile(resolveTestFileURL("licenseXPP.json"), SW360License.class);
		createLicense(licenseCreated5);
		List<SW360SparseLicense> licenses = waitFor(licenseClient.getLicenses());
		checkLicenses(licenses);

		deleteLicense(licenseCreated1.getShortName());
		deleteLicense(licenseCreated2.getShortName());
		deleteLicense(licenseCreated3.getShortName());
		deleteLicense(licenseCreated4.getShortName());
		deleteLicense(licenseCreated5.getShortName());
	}

	@Test
	public void testGetLicensesStatusNoContent() throws IOException {
		wireMockRule.stubFor(
				get(urlPathEqualTo("/licenses")).willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

		List<SW360SparseLicense> licenses = waitFor(licenseClient.getLicenses());
		assertThat(licenses).isEmpty();
	}

	@Test
	public void testGetLicensesError() {
		wireMockRule.stubFor(
				get(urlPathEqualTo("/licenses")).willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

		CompletableFuture<List<SW360SparseLicense>> licensesFuture = null;
		if (RUN_REST_INTEGRATION_TEST) {
			licensesFuture = CompletableFuture.supplyAsync(() -> {
				throw new CompletionException(new FailedRequestException(SW360LicenseClient.TAG_GET_LICENSES,
						HttpConstants.STATUS_ERR_BAD_REQUEST));
			});
		} else {
			licensesFuture = licenseClient.getLicenses();
		}

		FailedRequestException exception = expectFailedRequest(licensesFuture, HttpConstants.STATUS_ERR_BAD_REQUEST);
		assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_GET_LICENSES);
	}

	@Test
	public void testGetLicenseByName() throws IOException {
		final String licenseName = "0TST";
		wireMockRule.stubFor(get(urlPathEqualTo("/licenses/" + licenseName))
				.willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("license.json")));

		SW360License licenseCreated = readTestJsonFile(resolveTestFileURL("licenseTST.json"), SW360License.class);
		createLicense(licenseCreated);
		SW360License license = waitFor(licenseClient.getLicenseByName(licenseName));
		assertThat(license.getFullName()).isEqualTo("Test License");
		assertThat(license.getShortName()).isEqualTo("0TST");

		deleteLicense(license.getId());
	}

	@Test
	public void testGetLicenseByNameUnknown() {
		wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(HttpConstants.STATUS_ERR_NOT_FOUND)));

		FailedRequestException exception = expectFailedRequest(licenseClient.getLicenseByName("unknown"),
				HttpConstants.STATUS_ERR_NOT_FOUND);
		assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_GET_LICENSE_BY_NAME);
	}

	@Test
	public void testGetLicenseByNameNoContent() {
		wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(HttpConstants.STATUS_OK)));

		extractException(licenseClient.getLicenseByName("foo"), IOException.class);
	}

	@Test
	public void testCreateLicense() throws IOException {
		SW360License license = readTestJsonFile(resolveTestFileURL("licenseTST.json"), SW360License.class);
		SW360License licenseCreated = readTestJsonFile(resolveTestFileURL("license.json"), SW360License.class);
		wireMockRule.stubFor(post(urlPathEqualTo("/licenses")).withRequestBody(equalToJson(toJson(licenseCreated)))
				.willReturn(aJsonResponse(HttpConstants.STATUS_CREATED).withBody(toJson(licenseCreated))));

		SW360License result = waitFor(
				licenseClient.createLicense(RUN_REST_INTEGRATION_TEST ? license : licenseCreated));
		assertThat(result).isEqualTo(RUN_REST_INTEGRATION_TEST ? license : licenseCreated);

		deleteLicense(result.getId());
	}

	@Test
	public void testCreateLicenseError() throws IOException {
		SW360License license = readTestJsonFile(resolveTestFileURL("license.json"), SW360License.class);
		wireMockRule
				.stubFor(post(urlPathEqualTo("/licenses")).willReturn(aJsonResponse(HttpConstants.STATUS_ERR_SERVER)));

		CompletableFuture<SW360License> licensesFuture = null;
		if (RUN_REST_INTEGRATION_TEST) {
			licensesFuture = CompletableFuture.supplyAsync(() -> {
				throw new CompletionException(new FailedRequestException(SW360LicenseClient.TAG_CREATE_LICENSE,
						HttpConstants.STATUS_ERR_SERVER));
			});
		} else {
			licensesFuture = licenseClient.createLicense(license);
		}

		FailedRequestException exception = expectFailedRequest(licensesFuture, HttpConstants.STATUS_ERR_SERVER);
		assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_CREATE_LICENSE);
	}

	private void deleteLicense(String licenseId) throws IOException {
		if (RUN_REST_INTEGRATION_TEST) {
			waitFor(licenseClient.deleteLicense(licenseId));
			FailedRequestException exception = expectFailedRequest(licenseClient.getLicenseByName(licenseId),
					HttpConstants.STATUS_ERR_NOT_FOUND);
			assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_GET_LICENSE_BY_NAME);
		}
	}

	private void createLicense(SW360License license) throws IOException {
		if (RUN_REST_INTEGRATION_TEST) {
			SW360License result = waitFor(licenseClient.createLicense(license));
			assertThat(result).isEqualTo(license);
		}
	}
}
