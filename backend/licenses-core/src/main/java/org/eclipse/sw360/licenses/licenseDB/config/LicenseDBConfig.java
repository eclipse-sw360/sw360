/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 * Copyright Mahmoud Elsheemy<mahmoudalshemy.3@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.licenses.licenseDB.config;


import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.eclipse.sw360.datahandler.common.DatabaseSettings.COUCH_DB_CONFIG;
import static org.eclipse.sw360.datahandler.common.DatabaseSettings.getConfiguredClient;

@Configuration
public class LicenseDBConfig {
    private static final Logger log = LoggerFactory.getLogger(LicenseDBConfig.class);

    /**
     * RestTemplate configuration for LicenseDB API
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(300000);

        /* Extra needed Configurations will be added later based on future requirements */

        restTemplate.setRequestFactory(requestFactory);

        log.info("RestTemplate configured for LicenseDB API");
        return restTemplate;
    }


    /**
     *
     * Defining the configuration repository bean for LicenseDB, which will be used to fetch configuration settings from CouchDB.
     */
    @Bean
    public ConfigContainerRepository configContainerRepository() {
        DatabaseConnectorCloudant configContainerDatabaseConnector = new DatabaseConnectorCloudant(getConfiguredClient(),
                COUCH_DB_CONFIG);
        return new ConfigContainerRepository(configContainerDatabaseConnector);
    }

}
