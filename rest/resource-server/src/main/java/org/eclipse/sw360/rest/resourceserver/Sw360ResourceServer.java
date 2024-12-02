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

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.common.PropertyUtils;
import org.eclipse.sw360.rest.common.Sw360CORSFilter;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
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

import java.util.*;

import static org.eclipse.sw360.datahandler.common.SW360Constants.REST_SERVER_PATH_URL;

@SpringBootApplication
@Import(Sw360CORSFilter.class)
public class Sw360ResourceServer extends SpringBootServletInitializer {

    public static final String REST_BASE_PATH = "/api";

    @Value("${spring.data.rest.default-page-size:10}")
    private int defaultPageSize;

    private static final String VERSION_INFO_PROPERTIES_FILE = "/restInfo.properties";
    private static final String VERSION_INFO_KEY = "sw360RestVersion";
    private static final String CURIE_NAMESPACE = "sw360";
    private static final String APPLICATION_ID = "rest";

    private static final String APPLICATION_NAME = "/resource";
    private static final Map<Object, Object> versionInfo;

    static {
        versionInfo = new HashMap<>();
        Properties properties = CommonUtils.loadProperties(Sw360ResourceServer.class, VERSION_INFO_PROPERTIES_FILE, false);
        versionInfo.putAll(properties);

        SpringDocUtils.getConfig()
                .replaceWithClass(org.springframework.data.domain.Pageable.class,
                        OpenAPIPaginationHelper.class)
                .replaceWithClass(org.springframework.data.domain.PageRequest.class,
                        OpenAPIPaginationHelper.class);
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

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl(REST_SERVER_PATH_URL + APPLICATION_NAME + REST_BASE_PATH);
        server.setDescription("Current instance.");
        Object restVersion = versionInfo.get(VERSION_INFO_KEY);
        String restVersionString = "1.0.0";
        if (restVersion != null) {
            restVersionString = restVersion.toString();
        }
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("tokenAuth",
                                new SecurityScheme().type(SecurityScheme.Type.APIKEY).name("Authorization")
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Enter the token with the `Bearer ` prefix, e.g. \"Bearer eyJhbGciOiJ.....\"."))
                        .addSecuritySchemes("basic",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).name("Basic")
                                        .scheme("basic")
                                        .description("Username & password based authentication.")))
                .info(new Info().title("SW360 API").license(new License().name("EPL-2.0")
                                .url("https://github.com/eclipse-sw360/sw360/blob/main/LICENSE"))
                        .version(restVersionString))
                .servers(List.of(server))
                .path("/health", new PathItem().get(
                        new Operation().tags(Collections.singletonList("Health"))
                                .summary("Health endpoint").operationId("health")
                                .responses(new ApiResponses().addApiResponse("200",
                                        new ApiResponse().description("OK")
                                                .content(new Content()
                                                        .addMediaType("application/json", new MediaType()
                                                                .example("""
                                                                        {
                                                                          "status": "UP",
                                                                          "components": {
                                                                            "SW360Rest": {
                                                                              "status": "UP",
                                                                              "details": {
                                                                                "Rest State": {
                                                                                  "isDbReachable": true,
                                                                                  "isThriftReachable": true
                                                                                }
                                                                              }
                                                                            },
                                                                            "ping": {
                                                                              "status": "UP"
                                                                            }
                                                                          }
                                                                        }
                                                                        """)
                                                                .schema(new Schema<Health>())
                                                ))
                                ))
                ));
    }
}
