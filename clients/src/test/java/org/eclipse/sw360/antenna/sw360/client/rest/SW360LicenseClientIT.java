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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.antenna.http.utils.HttpUtils.waitFor;

public class SW360LicenseClientIT extends AbstractMockServerTest {
    /**
     * An array with the names of the licenses in the test data.
     */
    private static final String[] TEST_LICENSES = {
            "BSD Zero Clause License", "Attribution Assurance License", "Amazon Digital Services License",
            "Academic Free License v1.1", "XPP License"
    };

    private SW360LicenseClient licenseClient;

    @Before
    public void setUp() {
        licenseClient = new SW360LicenseClient(createClientConfig(), createMockTokenProvider());
        prepareAccessTokens(licenseClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
    }

    /**
     * Tests the passed in list of license data against the expected test data.
     *
     * @param licenses the collection with license data to be checked
     */
    private static void checkLicenses(List<? extends SW360SparseLicense> licenses) {
        List<String> actualLicenses = licenses.stream()
                .map(SW360SparseLicense::getFullName)
                .collect(Collectors.toList());
        assertThat(actualLicenses).containsExactlyInAnyOrder(TEST_LICENSES);
        assertHasLinks(licenses);
    }

    @Test
    public void testGetLicenses() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/licenses"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_licenses.json")));

        List<SW360SparseLicense> licenses = waitFor(licenseClient.getLicenses());
        checkLicenses(licenses);
    }

    @Test
    public void testGetLicensesStatusNoContent() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/licenses"))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));

        List<SW360SparseLicense> licenses = waitFor(licenseClient.getLicenses());
        assertThat(licenses).isEmpty();
    }

    @Test
    public void testGetLicensesError() {
        wireMockRule.stubFor(get(urlPathEqualTo("/licenses"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception =
                expectFailedRequest(licenseClient.getLicenses(), HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_GET_LICENSES);
    }

    @Test
    public void testGetLicenseByName() throws IOException {
        final String licenseName = "tst";
        wireMockRule.stubFor(get(urlPathEqualTo("/licenses/" + licenseName))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("license.json")));

        SW360License license = waitFor(licenseClient.getLicenseByName(licenseName));
        assertThat(license.getFullName()).isEqualTo("Test License");
        assertThat(license.getText()).contains("Bosch.IO GmbH");
        assertThat(license.getShortName()).isEqualTo("0TST");
    }

    @Test
    public void testGetLicenseByNameUnknown() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_ERR_NOT_FOUND)));

        FailedRequestException exception =
                expectFailedRequest(licenseClient.getLicenseByName("unknown"), HttpConstants.STATUS_ERR_NOT_FOUND);
        assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_GET_LICENSE_BY_NAME);
    }

    @Test
    public void testGetLicenseByNameNoContent() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_OK)));

        extractException(licenseClient.getLicenseByName("foo"), IOException.class);
    }

    @Test
    public void testCreateLicense() throws IOException {
        SW360License license = readTestJsonFile(resolveTestFileURL("license.json"), SW360License.class);
        SW360License licenseCreated = readTestJsonFile(resolveTestFileURL("license.json"), SW360License.class);
        licenseCreated.setText(license.getText() + "_updated");
        wireMockRule.stubFor(post(urlPathEqualTo("/licenses"))
                .withRequestBody(equalToJson(toJson(license)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_CREATED)
                        .withBody(toJson(licenseCreated))));

        SW360License result = waitFor(licenseClient.createLicense(license));
        assertThat(result).isEqualTo(licenseCreated);
    }

    @Test
    public void testCreateLicenseError() throws IOException {
        SW360License license = readTestJsonFile(resolveTestFileURL("license.json"), SW360License.class);
        wireMockRule.stubFor(post(urlPathEqualTo("/licenses"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_SERVER)));

        FailedRequestException exception =
                expectFailedRequest(licenseClient.createLicense(license), HttpConstants.STATUS_ERR_SERVER);
        assertThat(exception.getTag()).isEqualTo(SW360LicenseClient.TAG_CREATE_LICENSE);
    }
}
