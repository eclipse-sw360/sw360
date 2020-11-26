/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.SW360LicenseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.optionalFuture;

/**
 * Adapter implementation for the SW360 licenses endpoint.
 */
class SW360LicenseClientAdapterAsyncImpl implements SW360LicenseClientAdapterAsync {
    private final SW360LicenseClient licenseClient;

    public SW360LicenseClientAdapterAsyncImpl(SW360LicenseClient client) {
        licenseClient = client;
    }

    @Override
    public SW360LicenseClient getLicenseClient() {
        return licenseClient;
    }

    @Override
    public CompletableFuture<List<SW360SparseLicense>> getLicenses() {
        return getLicenseClient().getLicenses();
    }

    @Override
    public CompletableFuture<Optional<SW360License>> getLicenseByName(String license) {
        return optionalFuture(getLicenseClient().getLicenseByName(license));
    }

    @Override
    public CompletableFuture<SW360License> enrichSparseLicense(SW360SparseLicense sparseLicense) {
        return getLicenseClient().getLicenseByName(sparseLicense.getShortName());
    }

    @Override
    public CompletableFuture<SW360License> createLicense(SW360License license) {
        return getLicenseClient().createLicense(license);
    }
}
