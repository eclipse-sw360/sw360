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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.resourceserver.core.SimpleAuthenticationEntryPoint;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360UserAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.jwt.Sw360JWTAccessTokenConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Profile("!SECURITY_MOCK")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfiguration {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private ApiTokenAuthenticationFilter filter;

    @Autowired
    Sw360JWTAccessTokenConverter sw360JWTAccessTokenConverter;

    @Autowired
    private ApiTokenAuthenticationProvider authProvider;

    @Autowired
    Sw360UserAuthenticationProvider sw360UserAuthenticationProvider;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    String issuerUri;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/", "/*/*.html", "/*/*.css", "/*/*.js", "/*.js", "/*.json",
                "/*/*.json", "/*/*.png", "/*/*.gif", "/*/*.ico", "/*/*.woff/*", "/*/*.ttf", "/*/*.html", "/*/*/*.html",
                "/*/*.yaml", "/v3/api-docs/**", "/api/health", "/api/info");
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChainRS1(HttpSecurity http) throws Exception {
        SimpleAuthenticationEntryPoint saep = new SimpleAuthenticationEntryPoint();
        return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(sw360JWTAccessTokenConverter)
                        .jwkSetUri(issuerUri)))
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(x -> x.authenticationEntryPoint(saep))
                .headers(headers -> headers.xssProtection(xXssConfig -> xXssConfig.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(cps -> cps.policyDirectives("script-src 'self'")))
                .csrf(csrf -> csrf.disable()).build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SimpleAuthenticationEntryPoint saep = new SimpleAuthenticationEntryPoint();
        return http.addFilterBefore(filter, BasicAuthenticationFilter.class).authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.GET, "/api/health").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/info").hasAuthority("WRITE");
            auth.requestMatchers(HttpMethod.GET, "/api").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/reports/download").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("READ");
            auth.requestMatchers(HttpMethod.POST, "/api/**").hasAuthority("WRITE");
            auth.requestMatchers(HttpMethod.PUT, "/api/**").hasAuthority("WRITE");
            auth.requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority("WRITE");
            auth.requestMatchers(HttpMethod.PATCH, "/api/**").hasAuthority("WRITE");
            auth.requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll();
        }).csrf(csrf -> csrf.disable()).exceptionHandling(x -> x.authenticationEntryPoint(saep)).httpBasic(Customizer.withDefaults()).build();

    }

    @Autowired
    public void authenticationManagerBuilder(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.authenticationProvider(authProvider).authenticationProvider(sw360UserAuthenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
