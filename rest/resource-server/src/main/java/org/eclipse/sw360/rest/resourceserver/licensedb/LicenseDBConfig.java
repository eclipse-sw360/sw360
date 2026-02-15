/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sw360.licensedb")
@Getter
@Setter
public class LicenseDBConfig {

    private boolean enabled = false;

    private String apiUrl;

    private String apiVersion = "v1";

    private OAuth oAuth = new OAuth();

    private Sync sync = new Sync();

    private Connection connection = new Connection();

    @Getter
    @Setter
    public static class OAuth {
        private String clientId;
        private String clientSecret;
    }

    @Getter
    @Setter
    public static class Sync {
        private String cron = "0 0 2 * * ?";
        private int batchSize = 100;
        private boolean onStartup = false;
    }

    @Getter
    @Setter
    public static class Connection {
        private int timeout = 30000;
        private int readTimeout = 60000;
    }

    public String getBaseUrl() {
        if (apiUrl == null || apiUrl.isEmpty()) {
            return null;
        }
        return apiUrl + "/api/" + apiVersion;
    }
}
