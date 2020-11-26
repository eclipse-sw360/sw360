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
package org.eclipse.sw360.antenna.sw360.client.rest;

import org.eclipse.sw360.antenna.http.RequestBuilder;
import org.eclipse.sw360.antenna.http.utils.HttpUtils;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ResourceUtils;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360LicenseList;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;

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
     * {@link org.eclipse.sw360.antenna.http.utils.FailedRequestException} with
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
}
