/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.users.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Spring {@code @Configuration} for the Users backend service.
 *
 * <p>Creates the {@link UserHandler} as a managed bean.  The factory-method
 * pattern is used here instead of {@code @Service} on {@link UserHandler}
 * itself because the constructor may throw {@link IOException} when initialising
 * the CouchDB connection.  Wrapping the checked exception here keeps
 * {@link UserHandler} free of Spring annotations, making it easier to test.
 */
@Configuration
public class UserServiceConfig {

    private static final Logger log = LogManager.getLogger(UserServiceConfig.class);

    @Bean
    public UserHandler userHandler() {
        try {
            return new UserHandler();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialise UserHandler — " +
                    "check CouchDB connectivity and database settings.", e);
        }
    }

    /**
     * Shared {@link ObjectMapper} configured to tolerate unknown properties from
     * Thrift-generated classes during JSON serialization / deserialization.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * {@link RestTemplate} for any outbound HTTP calls from within the users service
     * (e.g., future inter-service calls once all backends migrate away from Thrift).
     */
    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        restTemplate.setRequestFactory(factory);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        restTemplate.getMessageConverters().removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }
}
