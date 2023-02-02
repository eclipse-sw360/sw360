/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver;

import java.util.Properties;
import java.util.Set;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.common.PropertyUtils;
import org.eclipse.sw360.rest.common.Sw360CORSFilter;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@SpringBootApplication
@Import(Sw360CORSFilter.class)
public class Sw360ResourceServer extends SpringBootServletInitializer {

    private static final String REST_BASE_PATH = "/api";

    @Value("${spring.data.rest.default-page-size:10}")
    private int defaultPageSize;

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String CURIE_NAMESPACE = "sw360";
    private static final String APPLICATION_ID = "rest";

    public static final String API_TOKEN_HASH_SALT;
    public static final String API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
    public static final String API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
    public static final Set<String> DOMAIN;
    public static final String REPORT_FILENAME_MAPPING;
    public static final String JWKS_ISSUER_URL;
    public static final String JWKS_ENDPOINT_URL;
    public static final Boolean IS_JWKS_VALIDATION_ENABLED;
    public static final Boolean IS_FORCE_UPDATE_ENABLED;
    public static final UserGroup CONFIG_WRITE_ACCESS_USERGROUP;
    public static final UserGroup CONFIG_ADMIN_ACCESS_USERGROUP;
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String DEFAULT_ADMIN_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();

    static {
        Properties props = CommonUtils.loadProperties(Sw360ResourceServer.class, SW360_PROPERTIES_FILE_PATH);
        API_TOKEN_MAX_VALIDITY_READ_IN_DAYS = props.getProperty("rest.apitoken.read.validity.days", "90");
        API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS = props.getProperty("rest.apitoken.write.validity.days", "30");
        API_TOKEN_HASH_SALT = props.getProperty("rest.apitoken.hash.salt", "$2a$04$Software360RestApiSalt");
        DOMAIN = CommonUtils.splitToSet(props.getProperty("domain",
                "Application Software, Documentation, Embedded Software, Hardware, Test and Diagnostics"));
        REPORT_FILENAME_MAPPING = props.getProperty("org.eclipse.sw360.licensinfo.projectclearing.templatemapping", "");
        JWKS_ISSUER_URL = props.getProperty("jwks.issuer.url", null);
        JWKS_ENDPOINT_URL = props.getProperty("jwks.endpoint.url", null);
        IS_JWKS_VALIDATION_ENABLED = Boolean.parseBoolean(props.getProperty("jwks.validation.enabled", "false"));
        IS_FORCE_UPDATE_ENABLED = Boolean.parseBoolean(
                System.getProperty("RunRestForceUpdateTest", props.getProperty("rest.force.update.enabled", "false")));
        CONFIG_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
        CONFIG_ADMIN_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.admin.access.usergroup", DEFAULT_ADMIN_ACCESS_USERGROUP));
    }

    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider(CURIE_NAMESPACE, UriTemplate.of("/docs/{rel}.html"));
    }

    @Bean
    public ApiTokenAuthenticationFilter authFilterBean() {
        return new ApiTokenAuthenticationFilter();
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
        return new RepositoryRestConfigurer() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config,  CorsRegistry cors) {
                config.setLimitParamName(RestControllerHelper.PAGINATION_PARAM_PAGE_ENTRIES);
                config.setBasePath(REST_BASE_PATH);
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

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ForwardedHeaderFilter());
        return bean;
    }
}
