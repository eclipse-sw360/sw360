/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.DatabaseSettings.COUCH_DB_ATTACHMENTS;
import static org.eclipse.sw360.datahandler.common.DatabaseSettings.COUCH_DB_CONFIG;
import static org.eclipse.sw360.datahandler.common.DatabaseSettings.getConfiguredClient;
import static org.eclipse.sw360.datahandler.common.Duration.durationOf;

@Configuration
@ComponentScan({"org.eclipse.sw360.fossology"})
public class FossologyConfig {
    // TODO get from a config class
    private final Duration downloadTimeout = durationOf(2, TimeUnit.MINUTES);

    @Bean
    public ConfigContainerRepository configContainerRepository() throws MalformedURLException {
        DatabaseConnectorCloudant configContainerDatabaseConnector = new DatabaseConnectorCloudant(getConfiguredClient(),
                COUCH_DB_CONFIG);
        return new ConfigContainerRepository(configContainerDatabaseConnector);
    }

    @Bean
    public AttachmentConnector attachmentConnector() throws MalformedURLException {
        return new AttachmentConnector(getConfiguredClient(), COUCH_DB_ATTACHMENTS, downloadTimeout);
    }

    @Bean
    public ThriftClients thriftClients() {
        return new ThriftClients();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
