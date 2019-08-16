/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver;

import java.util.Properties;
import java.util.Set;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.common.PropertyUtils;
import org.eclipse.sw360.rest.common.Sw360CORSFilter;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.hal.DefaultCurieProvider;

@SpringBootApplication
@Import(Sw360CORSFilter.class)
public class Sw360ResourceServer extends SpringBootServletInitializer {

    @Value("${spring.data.rest.default-page-size:10}")
    private int defaultPageSize;

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String CURIE_NAMESPACE = "sw360";
    private static final String APPLICATION_ID = "rest";

    public static final String API_TOKEN_HASH_SALT;
    public static final String API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
    public static final String API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
    public static final Set<String> DOMAIN;

    static {
        Properties props = CommonUtils.loadProperties(Sw360ResourceServer.class, SW360_PROPERTIES_FILE_PATH);
        API_TOKEN_MAX_VALIDITY_READ_IN_DAYS = props.getProperty("rest.apitoken.read.validity.days", "90");
        API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS = props.getProperty("rest.apitoken.write.validity.days", "30");
        API_TOKEN_HASH_SALT = props.getProperty("rest.apitoken.hash.salt", "$2a$04$Software360RestApiSalt");
        DOMAIN = CommonUtils.splitToSet(props.getProperty("domain",
                "Application Software, Documentation, Embedded Software, Hardware, Test and Diagnostics"));
    }

    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider(CURIE_NAMESPACE, new UriTemplate("/docs/{rel}.html"));
    }

    @Bean
    public ApiTokenAuthenticationFilter authFilterBean() {
        return new ApiTokenAuthenticationFilter();
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setLimitParamName(RestControllerHelper.PAGINATION_PARAM_PAGE_ENTRIES);
            }
        };
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder
            .sources(Sw360ResourceServer.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Sw360ResourceServer.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID))
            .build()
            .run(args);
    }
}
