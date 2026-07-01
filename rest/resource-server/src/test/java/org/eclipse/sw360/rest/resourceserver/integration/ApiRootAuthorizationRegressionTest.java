/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;

@Import(ApiRootAuthorizationRegressionTest.ApiRootAuthorizationTestConfig.class)
public class ApiRootAuthorizationRegressionTest extends TestIntegrationBase {

    private static final String READ_ONLY_EMAIL = "readonly@sw360.org";
    private static final String READ_ONLY_PASSWORD = "12345";

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    public void setUpReadOnlyUser() {
        User readOnlyUser = new User();
        readOnlyUser.setEmail(READ_ONLY_EMAIL);
        readOnlyUser.setUserGroup(UserGroup.USER);

        given(userServiceMock.getUserByEmailOrExternalId(READ_ONLY_EMAIL)).willReturn(UserConverter.fromThrift(readOnlyUser));
        given(userServiceMock.getUserByEmail(READ_ONLY_EMAIL)).willReturn(UserConverter.fromThrift(readOnlyUser));
        given(sw360CustomUserDetailsService.loadUserByUsername(READ_ONLY_EMAIL))
                .willReturn(new org.springframework.security.core.userdetails.User(
                        READ_ONLY_EMAIL,
                        new BCryptPasswordEncoder().encode(READ_ONLY_PASSWORD),
                        List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()))));
    }

    @Test
    public void should_allow_read_user_to_access_api_root_and_see_admin_links() throws Exception {
        assertApiRootContainsExpectedLinks(generateBasicAuthHeader(READ_ONLY_EMAIL, READ_ONLY_PASSWORD));
    }

    @Test
    public void should_allow_read_user_with_bearer_header_to_access_api_root_and_see_admin_links() throws Exception {
        assertApiRootContainsExpectedLinks("Bearer " + READ_ONLY_EMAIL);
    }

    @Test
    public void should_allow_read_user_with_token_header_to_access_api_root_and_see_admin_links() throws Exception {
        assertApiRootContainsExpectedLinks("Token " + READ_ONLY_EMAIL);
    }

    private void assertApiRootContainsExpectedLinks(String authorizationHeader) throws Exception {
        ResponseEntity<String> response = callApiRoot(authorizationHeader);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode linksNode = new ObjectMapper().readTree(response.getBody()).path("_links");
        assertTrue(linksNode.has("sw360:fossology"));
        assertTrue(linksNode.has("sw360:department"));
    }

    private ResponseEntity<String> callApiRoot(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);

        return new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private static String generateBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class ApiRootAuthorizationTestConfig {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain apiAuthorizationHeaderTestChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/api/**")
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .addFilterBefore(new ReadAuthorityHeaderAuthenticationFilter(), BasicAuthenticationFilter.class)
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    static class ReadAuthorityHeaderAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && (authorization.startsWith("Bearer ") || authorization.startsWith("Token "))) {
                String principal = authorization.substring(authorization.indexOf(' ') + 1).trim();
                if (!principal.isBlank()) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    "N/A",
                                    List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()))
                            )
                    );
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
