/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.rest.resourceserver.core.SimpleAuthenticationEntryPoint;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360UserAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.jwt.Sw360JWTAccessTokenConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import java.util.List;

@Profile("!SECURITY_MOCK")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ResourceServerConfiguration {

    private static final String[] PUBLIC_SWAGGER_ENDPOINTS = {"/v3/api-docs/**", "/swagger-ui/**", "/index.html",
            "/docs/**", "/mkdocs/**"};

    private static final String[] PUBLIC_API_GET_ENDPOINTS = {"/api/health", "/api/version", "/api",
            "/api/reports/download"};

    private final SimpleAuthenticationEntryPoint saep;
    private final Sw360JWTAccessTokenConverter sw360JWTAccessTokenConverter;
    private final ApiTokenAuthenticationProvider authProvider;
    private final Sw360UserAuthenticationProvider sw360UserAuthenticationProvider;
    private final JwtDecoder jwtDecoder;

    @Value("${springdoc.swagger-ui.require-authentication:true}")
    private boolean swaggerRequireAuthentication;

    /**
     * Allow HTTP Basic authentication to be disabled for production environments.
     * Basic auth is useful for development/testing but should be disabled in production
     * when all clients authenticate via JWT or API token.
     * Set {@code sw360.security.http-basic.enabled=false} in {@code application-prod.yml}.
     */
    @Value("${sw360.security.http-basic.enabled:true}")
    private boolean basicAuthEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) {
        ApiTokenAuthenticationFilter apiTokenAuthenticationFilter = new ApiTokenAuthenticationFilter(authenticationManager, saep);
        http.authenticationManager(authenticationManager);

        http
                .addFilterBefore(apiTokenAuthenticationFilter, BasicAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(sw360JWTAccessTokenConverter))
                        .authenticationEntryPoint(saep))
                .authorizeHttpRequests(auth -> {
                    if (!swaggerRequireAuthentication) {
                        auth.requestMatchers(HttpMethod.GET, PUBLIC_SWAGGER_ENDPOINTS).permitAll();
                    }
                    auth.requestMatchers(HttpMethod.GET, PUBLIC_API_GET_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/info").hasAuthority(TokenCapabilityAuthorities.TOKEN_WRITE);
                    auth.requestMatchers(HttpMethod.GET, "/api/**").hasAuthority(TokenCapabilityAuthorities.TOKEN_READ);
                    auth.requestMatchers(HttpMethod.POST, "/api/**").hasAuthority(TokenCapabilityAuthorities.TOKEN_WRITE);
                    auth.requestMatchers(HttpMethod.PUT, "/api/**").hasAuthority(TokenCapabilityAuthorities.TOKEN_WRITE);
                    auth.requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority(TokenCapabilityAuthorities.TOKEN_WRITE);
                    auth.requestMatchers(HttpMethod.PATCH, "/api/**").hasAuthority(TokenCapabilityAuthorities.TOKEN_WRITE);
                })
                .exceptionHandling(x -> x.authenticationEntryPoint(saep))
                .headers(headers -> headers.xssProtection(xXssConfig -> xXssConfig.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                        .contentSecurityPolicy(cps -> cps.policyDirectives("script-src 'self'")))
                // Keep CSRF enabled by default and ignore stateless API endpoints using Authorization headers.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        if (basicAuthEnabled) {
            http.httpBasic(basic -> basic.authenticationEntryPoint(saep));
        } else {
            http.httpBasic(AbstractHttpConfigurer::disable);
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        jwtAuthenticationProvider.setJwtAuthenticationConverter(sw360JWTAccessTokenConverter);
        return new ProviderManager(List.of(jwtAuthenticationProvider, authProvider, sw360UserAuthenticationProvider));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
