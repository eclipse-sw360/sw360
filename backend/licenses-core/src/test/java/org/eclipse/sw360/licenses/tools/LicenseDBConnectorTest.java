/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.tools;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

public class LicenseDBConnectorTest {

    @Test
    public void testConstructorAcceptsValidHttpUrl() {
        LicenseDBTokenManager tokenManager = Mockito.mock(LicenseDBTokenManager.class);
        LicenseDBConnector connector = new LicenseDBConnector("http://localhost:8080", tokenManager);
        assertNotNull(connector);
    }

    @Test
    public void testConstructorAcceptsValidHttpsUrl() {
        LicenseDBTokenManager tokenManager = Mockito.mock(LicenseDBTokenManager.class);
        LicenseDBConnector connector = new LicenseDBConnector("https://licensedb.example.com/", tokenManager);
        assertNotNull(connector);
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorRejectsMalformedUrl() {
        LicenseDBTokenManager tokenManager = Mockito.mock(LicenseDBTokenManager.class);
        new LicenseDBConnector("not a url", tokenManager);
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorRejectsFtpScheme() {
        LicenseDBTokenManager tokenManager = Mockito.mock(LicenseDBTokenManager.class);
        new LicenseDBConnector("ftp://licensedb.example.com", tokenManager);
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorRejectsNullBaseUrl() {
        LicenseDBTokenManager tokenManager = Mockito.mock(LicenseDBTokenManager.class);
        new LicenseDBConnector(null, tokenManager);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullTokenManager() {
        new LicenseDBConnector("http://localhost:8080", null);
    }
}
