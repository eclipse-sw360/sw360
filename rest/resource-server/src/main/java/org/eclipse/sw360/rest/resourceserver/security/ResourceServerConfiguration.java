/*
 * Copyright Siemens AG, 2017-2018,2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.resourceserver.core.SimpleAuthenticationEntryPoint;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360CustomUserDetailsService;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360UserAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.jwt.JwtIssuer;
import org.eclipse.sw360.rest.resourceserver.security.jwt.Sw360JWTAccessTokenConverter;
import org.eclipse.sw360.rest.resourceserver.security.jwt.Sw360JwtIssuerProperties;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Profile("!SECURITY_MOCK")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(Sw360JwtIssuerProperties.class)
@RequiredArgsConstructor
public class ResourceServerConfiguration {

    private static final String[] PUBLIC_SWAGGER_ENDPOINTS = {"/v3/api-docs/**", "/swagger-ui/**", "/index.html",
            "/docs/**", "/mkdocs/**"};

    private static final String[] PUBLIC_API_GET_ENDPOINTS = {"/api/health", "/api/version", "/api"};

    private final SimpleAuthenticationEntryPoint saep;
    private final Sw360JWTAccessTokenConverter sw360JWTAccessTokenConverter;
    private final Sw360JwtIssuerProperties jwtIssuerProperties;
    private final Sw360UserService userService;
    private final Sw360CustomUserDetailsService userDetailsService;

    @Value("${springdoc.swagger-ui.require-authentication:true}")
    private boolean swaggerRequireAuthentication;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String fallbackIssuerUri;

    /**
     * Allow HTTP Basic authentication to be disabled for production environments.
     * Basic auth is useful for development/testing but should be disabled in production
     * when all clients authenticate via JWT or API token.
     * Set {@code sw360.security.http-basic.enabled=false} in {@code application-prod.yml}.
     */
    @Value("${sw360.security.http-basic.enabled:true}")
    private boolean basicAuthEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
            AuthenticationManagerResolver<HttpServletRequest> jwtAuthenticationManagerResolver) {
        ApiTokenAuthenticationFilter apiTokenAuthenticationFilter = new ApiTokenAuthenticationFilter(authenticationManager, saep);
        http.authenticationManager(authenticationManager);

        http
                .addFilterBefore(apiTokenAuthenticationFilter, BasicAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(jwtAuthenticationManagerResolver)
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
        ApiTokenAuthenticationProvider apiTokenAuthenticationProvider = new ApiTokenAuthenticationProvider(userService);
        Sw360UserAuthenticationProvider sw360UserAuthenticationProvider =
                new Sw360UserAuthenticationProvider(passwordEncoder(), userDetailsService);
        return new ProviderManager(List.of(apiTokenAuthenticationProvider, sw360UserAuthenticationProvider));
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> jwtAuthenticationManagerResolver() {
        Map<String, JwtIssuer> trustedIssuers = trustedIssuers();
        ConcurrentMap<String, AuthenticationManager> managers = new ConcurrentHashMap<>();

        return new JwtIssuerAuthenticationManagerResolver(issuer -> {
            JwtIssuer entry = trustedIssuers.get(issuer);
            if (entry == null) {
                throw new InvalidBearerTokenException("Invalid issuer");
            }
            return managers.computeIfAbsent(issuer, key -> jwtAuthenticationManagerForIssuer(entry));
        });
    }

    @Contract("_ -> new")
    private @NonNull AuthenticationManager jwtAuthenticationManagerForIssuer(JwtIssuer issuer) {
        JwtDecoder jwtDecoder = buildJwtDecoder(issuer);
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        jwtAuthenticationProvider.setJwtAuthenticationConverter(sw360JWTAccessTokenConverter);
        return new ProviderManager(jwtAuthenticationProvider);
    }

    /**
     * Build the {@link JwtDecoder} for a trusted issuer.
     *
     * <p>When {@link JwtIssuer#getJwkSetUri()} is provided, the decoder is built directly
     * against the JWKS endpoint and the issuer claim is validated against
     * {@link JwtIssuer#getIssuerUri()}. This skips OpenID Connect discovery.</p>
     *
     * <p>When {@link JwtIssuer#getJwkSetUri()} is absent, the decoder is built via
     * discovery against {@link JwtIssuer#getIssuerUri()}, which requires the issuer URL
     * to be reachable and TLS-trusted by the JVM.</p>
     */
    private JwtDecoder buildJwtDecoder(@NonNull JwtIssuer issuer) {
        if (issuer.getJwkSetUri() != null && !issuer.getJwkSetUri().isBlank()) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer.getJwkSetUri()).build();
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer.getIssuerUri());
            decoder.setJwtValidator(validator);
            return decoder;
        }
        return JwtDecoders.fromIssuerLocation(issuer.getIssuerUri());
    }

    private @NonNull @Unmodifiable Map<String, JwtIssuer> trustedIssuers() {
        Map<String, JwtIssuer> issuers = new LinkedHashMap<>(jwtIssuerProperties.getEffectiveIssuers());
        if (issuers.isEmpty() && CommonUtils.isNotNullEmptyOrWhitespace(fallbackIssuerUri)) {
            JwtIssuer fallback = new JwtIssuer();
            fallback.setIssuerUri(fallbackIssuerUri.trim());
            issuers.put(fallback.getIssuerUri(), fallback);
        }
        if (issuers.isEmpty()) {
            throw new IllegalStateException("No trusted JWT issuer configured for the SW360 resource server.");
        }
        return Map.copyOf(issuers);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
