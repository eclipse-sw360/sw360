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
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360LicenseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.block;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SW360LicenseClientAdapterAsyncImplTest {
    private static final String LICENSE_NAME = "licenseName";

    private SW360LicenseClientAdapterAsync licenseClientAdapter;

    private SW360LicenseClient licenseClient;

    @Before
    public void setUp() {
        licenseClient = mock(SW360LicenseClient.class);
        licenseClientAdapter = new SW360LicenseClientAdapterAsyncImpl(licenseClient);
    }

    @Test
    public void testGetLicenses() {
        List<SW360SparseLicense> licenses = Arrays.asList(new SW360SparseLicense().setShortName("l1"),
                new SW360SparseLicense().setShortName("l2"));
        when(licenseClient.getLicenses())
                .thenReturn(CompletableFuture.completedFuture(licenses));

        List<SW360SparseLicense> result = block(licenseClientAdapter.getLicenses());
        assertThat(result).isEqualTo(licenses);
    }

    @Test
    public void testGetLicenseByName() {
        SW360License license = prepareLicenseClientGetLicenseByName();

        Optional<SW360License> sw360LicenseByAntennaLicense =
                block(licenseClientAdapter.getLicenseByName(LICENSE_NAME));

        assertThat(sw360LicenseByAntennaLicense).isPresent();
        assertThat(sw360LicenseByAntennaLicense).hasValue(license);
        verify(licenseClient, atLeastOnce()).getLicenseByName(LICENSE_NAME);
    }

    @Test
    public void testGetLicenseByNameUnresolved() {
        FailedRequestException exception =
                new FailedRequestException("get_license", HttpConstants.STATUS_ERR_NOT_FOUND);
        when(licenseClient.getLicenseByName(LICENSE_NAME))
                .thenReturn(FutureUtils.failedFuture(exception));

        Optional<SW360License> result = block(licenseClientAdapter.getLicenseByName(LICENSE_NAME));
        assertThat(result).isEmpty();
    }

    @Test
    public void testEnrichSparseLicense() {
        SW360License license = prepareLicenseClientGetLicenseByName();
        SW360SparseLicense sparseLicense = new SW360SparseLicense()
                .setShortName(LICENSE_NAME);

        SW360License licenseDetails = block(licenseClientAdapter.enrichSparseLicense(sparseLicense));
        assertThat(licenseDetails).isEqualTo(license);
    }

    @Test
    public void testCreateLicense() {
        SW360License licenseTemplate = new SW360License()
                .setFullName("testLicense");
        SW360License licenseCreated = new SW360License()
                .setShortName("createdLicense")
                .setFullName("testLicense");
        when(licenseClient.createLicense(licenseTemplate))
                .thenReturn(CompletableFuture.completedFuture(licenseCreated));

        SW360License result = block(licenseClientAdapter.createLicense(licenseTemplate));
        assertThat(result).isEqualTo(licenseCreated);
    }

    private SW360License prepareLicenseClientGetLicenseByName() {
        SW360License license = new SW360License()
                .setShortName(LICENSE_NAME);
        when(licenseClient.getLicenseByName(LICENSE_NAME))
                .thenReturn(CompletableFuture.completedFuture(license));

        return license;
    }
}