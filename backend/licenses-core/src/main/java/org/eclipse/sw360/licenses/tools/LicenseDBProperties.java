/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.tools;

import org.eclipse.sw360.datahandler.common.SW360Constants;

/**
 * Utility class for LicenseDB configuration properties
 */
public class LicenseDBProperties {

    private final boolean enabled;
    private final String apiUrl;
    private final String apiVersion;
    private final String oauthClientId;
    private final String oauthClientSecret;
    private final String syncCron;
    private final int syncBatchSize;
    private final int connectionTimeout;
    private final int connectionReadTimeout;

    public LicenseDBProperties() {
        this.enabled = Boolean.parseBoolean(SW360Constants.LICENSEDB_ENABLED);
        this.apiUrl = SW360Constants.LICENSEDB_API_URL;
        this.apiVersion = SW360Constants.LICENSEDB_API_VERSION;
        this.oauthClientId = SW360Constants.LICENSEDB_OAUTH_CLIENT_ID;
        this.oauthClientSecret = SW360Constants.LICENSEDB_OAUTH_CLIENT_SECRET;
        this.syncCron = SW360Constants.LICENSEDB_SYNC_CRON;
        this.syncBatchSize = Integer.parseInt(SW360Constants.LICENSEDB_SYNC_BATCH_SIZE);
        this.connectionTimeout = Integer.parseInt(SW360Constants.LICENSEDB_CONNECTION_TIMEOUT);
        this.connectionReadTimeout = Integer.parseInt(SW360Constants.LICENSEDB_CONNECTION_READ_TIMEOUT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getOAuthClientId() {
        return oauthClientId;
    }

    public String getOAuthClientSecret() {
        return oauthClientSecret;
    }

    public String getSyncCron() {
        return syncCron;
    }

    public int getSyncBatchSize() {
        return syncBatchSize;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getConnectionReadTimeout() {
        return connectionReadTimeout;
    }

    public String getFullApiUrl() {
        return apiUrl + "/api/" + apiVersion;
    }

    @Override
    public String toString() {
        return "LicenseDBProperties{" +
                "enabled=" + enabled +
                ", apiUrl='" + apiUrl + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", oauthClientId='" + oauthClientId + '\'' +
                ", syncCron='" + syncCron + '\'' +
                ", syncBatchSize=" + syncBatchSize +
                ", connectionTimeout=" + connectionTimeout +
                ", connectionReadTimeout=" + connectionReadTimeout +
                '}';
    }
}
