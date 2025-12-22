/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.fossology.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;

@Configuration
@ComponentScan({"org.eclipse.sw360.fossology"})
public class FossologyConfig {

    private static final Logger log = LogManager.getLogger(FossologyConfig.class);
    private Duration downloadTimeout = null;

    @Autowired
    ConfigContainerRepository configContainerRepository;

    /**
     * Get download timeout configuration with v2 API support
     */
    private Duration getDownloadTimeout() {
        if (downloadTimeout != null) {
            return downloadTimeout;
        }
        FossologyRestConfig fossologyRestConfig;
        try {
            fossologyRestConfig = new FossologyRestConfig(configContainerRepository());
        } catch (SW360Exception e) {
            log.error("Failed to load Fossology configuration.", e);
            return durationOf(5, TimeUnit.MINUTES); // Increased default for v2 API
        }
        long timeoutValue = 5;
        TimeUnit timeoutUnit = TimeUnit.MINUTES;

        String timeoutStr = fossologyRestConfig.getDownloadTimeout();
        String timeoutUnitStr = fossologyRestConfig.getDownloadTimeoutUnit();

        if (timeoutStr != null) {
            try {
                timeoutValue = Long.parseLong(timeoutStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid timeout value in config, using default (5 minutes).", e);
            }
        }

        if (timeoutUnitStr != null) {
            try {
                timeoutUnit = TimeUnit.valueOf(timeoutUnitStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid timeout unit in config, using default (MINUTES).", e);
            }
        }

        downloadTimeout = durationOf(timeoutValue, timeoutUnit);
        return downloadTimeout;
    }

    @Bean
    public ConfigContainerRepository configContainerRepository() {
        return configContainerRepository;
    }

    /**
     * Enhanced RestTemplate configuration for v2 API
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000); // 30 seconds
        requestFactory.setReadTimeout(300000);   // 5 minutes for large file

        restTemplate.setRequestFactory(requestFactory);

        log.info("RestTemplate configured for FOSSology v2 API with enhanced timeouts");
        return restTemplate;
    }

    /**
     * ObjectMapper configuration optimized for v2 API JSON processing
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Configure for better v2 API compatibility
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        log.info("ObjectMapper configured for FOSSology v2 API JSON processing");
        return mapper;
    }
}
