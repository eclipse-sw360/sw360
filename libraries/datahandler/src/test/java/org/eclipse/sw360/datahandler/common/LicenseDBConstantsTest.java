/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for LicenseDB configuration constants
 * 
 * @author SW360 Team
 */
public class LicenseDBConstantsTest {

    @Test
    public void testLicenseDBEnabledConstant() {
        assertEquals("licensedb.enabled", SW360Constants.LICENSEDB_ENABLED);
    }

    @Test
    public void testLicenseDBApiUrlConstant() {
        assertEquals("licensedb.api.url", SW360Constants.LICENSEDB_API_URL);
    }

    @Test
    public void testLicenseDBApiVersionConstant() {
        assertEquals("licensedb.api.version", SW360Constants.LICENSEDB_API_VERSION);
    }

    @Test
    public void testLicenseDBOAuthClientIdConstant() {
        assertEquals("licensedb.oauth.client.id", SW360Constants.LICENSEDB_OAUTH_CLIENT_ID);
    }

    @Test
    public void testLicenseDBOAuthClientSecretConstant() {
        assertEquals("licensedb.oauth.client.secret", SW360Constants.LICENSEDB_OAUTH_CLIENT_SECRET);
    }

    @Test
    public void testLicenseDBSyncCronConstant() {
        assertEquals("licensedb.sync.cron", SW360Constants.LICENSEDB_SYNC_CRON);
    }

    @Test
    public void testLicenseDBSyncBatchSizeConstant() {
        assertEquals("licensedb.sync.batch-size", SW360Constants.LICENSEDB_SYNC_BATCH_SIZE);
    }

    @Test
    public void testLicenseDBSyncOnStartupConstant() {
        assertEquals("licensedb.sync.on-startup", SW360Constants.LICENSEDB_SYNC_ON_STARTUP);
    }

    @Test
    public void testLicenseDBConnectionTimeoutConstant() {
        assertEquals("licensedb.connection.timeout", SW360Constants.LICENSEDB_CONNECTION_TIMEOUT);
    }

    @Test
    public void testLicenseDBReadTimeoutConstant() {
        assertEquals("licensedb.connection.read-timeout", SW360Constants.LICENSEDB_READ_TIMEOUT);
    }

    @Test
    public void testAllLicenseDBConstantsNotNull() {
        assertNotNull(SW360Constants.LICENSEDB_ENABLED);
        assertNotNull(SW360Constants.LICENSEDB_API_URL);
        assertNotNull(SW360Constants.LICENSEDB_API_VERSION);
        assertNotNull(SW360Constants.LICENSEDB_OAUTH_CLIENT_ID);
        assertNotNull(SW360Constants.LICENSEDB_OAUTH_CLIENT_SECRET);
        assertNotNull(SW360Constants.LICENSEDB_SYNC_CRON);
        assertNotNull(SW360Constants.LICENSEDB_SYNC_BATCH_SIZE);
        assertNotNull(SW360Constants.LICENSEDB_SYNC_ON_STARTUP);
        assertNotNull(SW360Constants.LICENSEDB_CONNECTION_TIMEOUT);
        assertNotNull(SW360Constants.LICENSEDB_READ_TIMEOUT);
    }

    @Test
    public void testAllLicenseDBConstantsNotEmpty() {
        assertFalse(SW360Constants.LICENSEDB_ENABLED.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_API_URL.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_API_VERSION.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_OAUTH_CLIENT_ID.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_OAUTH_CLIENT_SECRET.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_SYNC_CRON.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_SYNC_BATCH_SIZE.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_SYNC_ON_STARTUP.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_CONNECTION_TIMEOUT.isEmpty());
        assertFalse(SW360Constants.LICENSEDB_READ_TIMEOUT.isEmpty());
    }

    @Test
    public void testLicenseDBConstantsHaveCorrectPrefix() {
        assertTrue(SW360Constants.LICENSEDB_ENABLED.startsWith("licensedb."));
        assertTrue(SW360Constants.LICENSEDB_API_URL.startsWith("licensedb."));
        assertTrue(SW360Constants.LICENSEDB_API_VERSION.startsWith("licensedb."));
        assertTrue(SW360Constants.LICENSEDB_OAUTH_CLIENT_ID.startsWith("licensedb.oauth."));
        assertTrue(SW360Constants.LICENSEDB_OAUTH_CLIENT_SECRET.startsWith("licensedb.oauth."));
        assertTrue(SW360Constants.LICENSEDB_SYNC_CRON.startsWith("licensedb.sync."));
        assertTrue(SW360Constants.LICENSEDB_SYNC_BATCH_SIZE.startsWith("licensedb.sync."));
        assertTrue(SW360Constants.LICENSEDB_SYNC_ON_STARTUP.startsWith("licensedb.sync."));
        assertTrue(SW360Constants.LICENSEDB_CONNECTION_TIMEOUT.startsWith("licensedb.connection."));
        assertTrue(SW360Constants.LICENSEDB_READ_TIMEOUT.startsWith("licensedb.connection."));
    }
}
