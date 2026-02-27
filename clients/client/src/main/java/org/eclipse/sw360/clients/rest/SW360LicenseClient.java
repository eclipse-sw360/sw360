/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Verifa Oy 2019.
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

import org.eclipse.sw360.http.RequestBuilder;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.eclipse.sw360.http.utils.HttpUtils;
import org.eclipse.sw360.clients.config.SW360ClientConfig;
import org.eclipse.sw360.clients.auth.AccessTokenProvider;
import org.eclipse.sw360.clients.utils.SW360ResourceUtils;
import org.eclipse.sw360.clients.rest.resource.licenses.SW360License;
import org.eclipse.sw360.clients.rest.resource.licenses.SW360LicenseList;
import org.eclipse.sw360.clients.rest.resource.licenses.SW360SparseLicense;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * An SW360 REST client implementation providing basic functionality related to
 * the {@code /licenses} endpoint.
 * </p>
 */
public class SW360LicenseClient extends SW360Client {
    /**
     * Tag for the request to query all licenses.
     */
    static final String TAG_GET_LICENSES = "get_licenses";

    /**
     * Tag for the request that queries the details for a specific license.
     */
    static final String TAG_GET_LICENSE_BY_NAME = "get_license_by_name";

    /**
     * Tag for the request to create a new license.
     */
    static final String TAG_CREATE_LICENSE = "post_create_license";

    /**
     * Tag for the request to delete a license.
     */
    static final String TAG_DELETE_LICENSE = "delete_license";

    /**
     * Tag for the request to import from LicenseDB.
     */
    static final String TAG_IMPORT_LICENSE_DB = "import_license_db";

    /**
     * Tag for the request to get LicenseDB sync status.
     */
    static final String TAG_GET_LICENSE_DB_STATUS = "get_license_db_status";

    /**
     * Tag for the request to test LicenseDB connection.
     */
    static final String TAG_TEST_LICENSE_DB_CONNECTION = "test_license_db_connection";

    private static final String LICENSES_ENDPOINT = "licenses";

    /**
     * Creates a new instance of {@code SW360LicenseClient} with the passed in
     * dependencies.
     *
     * @param config   the configuration of the client
     * @param provider the provider for access tokens
     */
    public SW360LicenseClient(SW360ClientConfig config, AccessTokenProvider provider) {
        super(config, provider);
    }

    /**
     * Returns a future with a list of sparse license information for all the
     * licenses available in SW360.
     *
     * @return a future with the list of licenses
     */
    public CompletableFuture<List<SW360SparseLicense>> getLicenses() {
        return executeJsonRequestWithDefault(HttpUtils.get(resourceUrl(LICENSES_ENDPOINT)), SW360LicenseList.class,
                TAG_GET_LICENSES, SW360LicenseList::new)
                .thenApply(SW360ResourceUtils::getSw360SparseLicenses);
    }

    /**
     * Returns a future with detail information of a license selected by its
     * name. If the name provided cannot be resolved, the future fails with a
     * {@link org.eclipse.sw360.http.utils.FailedRequestException} with
     * status code 404.
     *
     * @param name the name of the license in question
     * @return a future with the details of this license
     */
    public CompletableFuture<SW360License> getLicenseByName(String name) {
        return executeJsonRequest(HttpUtils.get(resourceUrl(LICENSES_ENDPOINT, name)), SW360License.class,
                TAG_GET_LICENSE_BY_NAME);
    }

    /**
     * Creates a new license based on the properties of the data object passed
     * in.
     *
     * @param license the data object for the license to be created
     * @return a future with the newly created license
     */
    public CompletableFuture<SW360License> createLicense(SW360License license) {
        return executeJsonRequest(builder ->
                        builder.uri(resourceUrl(LICENSES_ENDPOINT))
                                .method(RequestBuilder.Method.POST)
                                .body(bodyBuilder -> bodyBuilder.json(license)),
                SW360License.class, TAG_CREATE_LICENSE);
    }

    /**
     * Triggers a DELETE operation for the License identified by the given
     * IDs.
     *
     * @param licenseId of the License to delete
     * @return a future with the status code {@code Integer} returned by the
     * server
     */
    public CompletableFuture<Integer> deleteLicense(String licenseId) {
        String url = resourceUrl(LICENSES_ENDPOINT, licenseId);
        return executeRequest(builder -> builder.uri(url).method(RequestBuilder.Method.DELETE),
                HttpUtils.checkResponse(response -> response.statusCode(), HttpUtils.hasStatus(HttpConstants.STATUS_OK), TAG_DELETE_LICENSE),
                TAG_DELETE_LICENSE);
    }

    /**
     * Triggers import of licenses from LicenseDB.
     *
     * @return a future with the status code returned by the server
     */
    public CompletableFuture<Integer> importFromLicenseDB() {
        String url = resourceUrl(LICENSES_ENDPOINT, "import/LicenseDB");
        return executeRequest(builder -> builder.uri(url).method(RequestBuilder.Method.POST),
                HttpUtils.checkResponse(response -> response.statusCode(), HttpUtils.hasStatus(HttpConstants.STATUS_OK), TAG_IMPORT_LICENSE_DB),
                TAG_IMPORT_LICENSE_DB);
    }

    /**
     * Gets the LicenseDB sync status.
     *
     * @return a future with the sync status map
     */
    public CompletableFuture<Object> getLicenseDBSyncStatus() {
        String url = resourceUrl(LICENSES_ENDPOINT, "sync/LicenseDB/status");
        return executeJsonRequest(HttpUtils.get(url), Object.class, TAG_GET_LICENSE_DB_STATUS);
    }

    /**
     * Tests the connection to LicenseDB.
     *
     * @return a future with the test result
     */
    public CompletableFuture<Object> testLicenseDBConnection() {
        String url = resourceUrl(LICENSES_ENDPOINT, "sync/LicenseDB/test");
        return executeJsonRequest(HttpUtils.get(url), Object.class, TAG_TEST_LICENSE_DB_CONNECTION);
    }

}
