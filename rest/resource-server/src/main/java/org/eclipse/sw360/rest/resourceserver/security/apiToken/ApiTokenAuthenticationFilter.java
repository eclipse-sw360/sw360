/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.apiToken;

import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;

@Profile("!SECURITY_MOCK")
public class ApiTokenAuthenticationFilter implements Filter {

    private static final Logger log = LogManager.getLogger(ApiTokenAuthenticationFilter.class);
    private static final String AUTHENTICATION_TOKEN_PARAMETER = "authorization";

    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ApiTokenAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationManager = authenticationManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.getContext();

        if (context.getAuthentication() != null && context.getAuthentication().isAuthenticated()) {
            log.trace("Already authenticated");
        } else {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            try {
                String authorization = httpRequest.getHeader(AUTHENTICATION_TOKEN_PARAMETER);
                if (authorization != null && !authorization.isBlank()) {
                    String[] token = authorization.trim().split("\\s+");
                    if (token.length == 2 && token[0].equalsIgnoreCase("token")) {
                        Authentication auth = authenticationManager.authenticate(new ApiTokenAuthentication(token[1]));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (AuthenticationException e) {
                log.error("Authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(httpRequest, httpResponse, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    static class ApiTokenAuthentication implements Authentication {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String token;

        private ApiTokenAuthentication(String token) {
            this.token = token;
        }

        @Override
        @NonNull
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return new ArrayList<>();
        }

        @Override
        public Object getCredentials() {
            return token;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return null;
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        }

        @Override
        public String getName() {
            return null;
        }
    }
}
