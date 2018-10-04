/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver;

import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.hal.DefaultCurieProvider;

@SpringBootApplication
public class Sw360ResourceServer extends SpringBootServletInitializer {
    public static String CURIE_NAMESPACE = "sw360";

    @Value("${spring.data.rest.default-page-size:10}")
    private int defaultPageSize;

    public @Bean
    CurieProvider curieProvider() {
        return new DefaultCurieProvider(CURIE_NAMESPACE, new UriTemplate("/docs/{rel}.html"));
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Sw360ResourceServer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Sw360ResourceServer.class, args);
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setLimitParamName(RestControllerHelper.PAGINATION_PARAM_PAGE_ENTRIES);
                config.setDefaultPageSize(defaultPageSize);
            }
        };
    }
}
