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
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.common.PropertyUtils;
import org.eclipse.sw360.rest.common.Sw360CORSFilter;
import org.eclipse.sw360.rest.common.Sw360XssFilter;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
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

@SpringBootApplication
@Import({Sw360CORSFilter.class, Sw360XssFilter.class})
public class Sw360ResourceServer extends SpringBootServletInitializer {

    public static final String REST_BASE_PATH = "/api";
    public static final String EXAMPLE_HEALTH_RESPONSE = """
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
            """;

    @Value("${spring.data.rest.default-page-size:10}")
    private int defaultPageSize;

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String VERSION_INFO_PROPERTIES_FILE = "/restInfo.properties";
    private static final String VERSION_INFO_KEY = "sw360RestVersion";
    private static final String BUILD_VERSION_KEY = "sw360BuildVersion";
    private static final String COMMIT_COUNT_KEY = "sw360CommitCount";
    private static final String CURIE_NAMESPACE = "sw360";
    private static final String APPLICATION_ID = "rest";

    public static final String API_TOKEN_HASH_SALT;
    public static final Boolean API_WRITE_TOKEN_GENERATOR_ENABLED;
    public static final String API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
    public static final String API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
    public static final UserGroup API_WRITE_ACCESS_USERGROUP;
    public static final Set<String> DEFAULT_DOMAINS;
    public static final String REPORT_FILENAME_MAPPING;
    public static final String JWKS_ISSUER_URL;
    public static final String JWKS_ENDPOINT_URL;
    public static final String JWT_CLAIM_AUD;
    public static final Boolean IS_JWKS_VALIDATION_ENABLED;
    public static final UserGroup CONFIG_WRITE_ACCESS_USERGROUP;
    public static final UserGroup CONFIG_ADMIN_ACCESS_USERGROUP;
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String DEFAULT_ADMIN_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String SERVER_PATH_URL;
    private static final String APPLICATION_NAME = "/resource";
    private static final Map<Object, Object> versionInfo;
    public static final String SVM_NOTIFICATION_URL;

    static {
        Properties props = CommonUtils.loadProperties(Sw360ResourceServer.class, SW360_PROPERTIES_FILE_PATH);
        API_WRITE_TOKEN_GENERATOR_ENABLED = Boolean.parseBoolean(props.getProperty("rest.apitoken.write.generator.enable", "true"));
        API_TOKEN_MAX_VALIDITY_READ_IN_DAYS = props.getProperty("rest.apitoken.read.validity.days", "90");
        API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS = props.getProperty("rest.apitoken.write.validity.days", "30");
        API_TOKEN_HASH_SALT = props.getProperty("rest.apitoken.hash.salt", "$2a$04$Software360RestApiSalt");
        API_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", UserGroup.ADMIN.name()));
        DEFAULT_DOMAINS = CommonUtils.splitToSet(
                "Application Software, Documentation, Embedded Software, Hardware, Test and Diagnostics");
        REPORT_FILENAME_MAPPING = props.getProperty("org.eclipse.sw360.licensinfo.projectclearing.templatemapping", "");
        JWKS_ISSUER_URL = props.getProperty("jwks.issuer.url", null);
        JWKS_ENDPOINT_URL = props.getProperty("jwks.endpoint.url", null);
        JWT_CLAIM_AUD = props.getProperty("jwt.claim.aud", "");
        IS_JWKS_VALIDATION_ENABLED = Boolean.parseBoolean(props.getProperty("jwks.validation.enabled", "false"));
        CONFIG_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
        CONFIG_ADMIN_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.admin.access.usergroup", DEFAULT_ADMIN_ACCESS_USERGROUP));
        SERVER_PATH_URL = props.getProperty("backend.url", "http://localhost:8080");
        SVM_NOTIFICATION_URL = props.getProperty("svm.notification.url", "");

        versionInfo = new HashMap<>();
        Properties properties = CommonUtils.loadProperties(Sw360ResourceServer.class, VERSION_INFO_PROPERTIES_FILE, false);
        versionInfo.putAll(properties);

        SpringDocUtils.getConfig()
                .replaceParameterObjectWithClass(org.springframework.data.domain.Pageable.class,
                        OpenAPIPaginationHelper.class)
                .replaceParameterObjectWithClass(org.springframework.data.domain.PageRequest.class,
                        OpenAPIPaginationHelper.class);
    }

    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider(CURIE_NAMESPACE, UriTemplate.of("/docs/{rel}.html"));
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
            .sources(Sw360ResourceServer.class, DatabaseConfig.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Sw360ResourceServer.class)
            .sources(DatabaseConfig.class)
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
        server.setUrl(SERVER_PATH_URL + APPLICATION_NAME + REST_BASE_PATH);
        server.setDescription("Current instance.");
        String restVersionString = getRestVersion();
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
                                                                .example(EXAMPLE_HEALTH_RESPONSE)
                                                                .schema(new Schema<Health>())
                                                ))
                                ))
                ));
    }

    /**
     * Generate version string for REST using git build information.
     * <ol>
     *   <li>Check if the build version and commit count exists in properties.</li>
     *   <li>If the property does not exist, return 1.0.0 as default version.</li>
     *   <li>Replace the patch version of build version with commit count.</li>
     *   <li>Return the updated version string.</li>
     * </ol>
     * @return Version string for OpenAPI docs.
     */
    private String getRestVersion() {
        Object buildVersion = versionInfo.get(BUILD_VERSION_KEY);
        Object commitCount = versionInfo.get(COMMIT_COUNT_KEY);
        String restVersionString = "1.0.0";
        if (buildVersion != null && commitCount != null) {
            String[] versionParts = buildVersion.toString().split("\\.");
            if (versionParts.length == 3) {
                restVersionString = versionParts[0] + "." + versionParts[1] + "." + commitCount;
            }
        }
        return restVersionString;
    }
}
