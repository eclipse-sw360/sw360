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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("!SECURITY_MOCK")
public class ApiTokenAuthenticationFilter implements Filter {

    private static final Logger log = LogManager.getLogger(ApiTokenAuthenticationFilter.class);
    private static final String AUTHENTICATION_TOKEN_PARAMETER = "authorization";
    private static final String OIDC_AUTHENTICATION_TOKEN_PARAMETER = "oidcauthorization";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.getContext();

        if (context.getAuthentication() != null && context.getAuthentication().isAuthenticated()) {
            log.trace("Already authenticated");
        } else {
            Map<String, String> headers = Collections.list(((HttpServletRequest) request).getHeaderNames()).stream()
                    .collect(Collectors.toMap(h -> h, ((HttpServletRequest) request)::getHeader));
            if (!headers.isEmpty() && headers.containsKey(AUTHENTICATION_TOKEN_PARAMETER)) {
                String authorization = headers.get(AUTHENTICATION_TOKEN_PARAMETER);
                String[] token = authorization.trim().split("\\s+");
                if (token.length == 2 && token[0].equalsIgnoreCase("token")) {
                    Authentication auth = new ApiTokenAuthentication(token[1]);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if(token.length == 2 && token[0].equalsIgnoreCase("Bearer")) {
                    Authentication auth = new ApiTokenAuthentication(token[1]).setType(AuthType.JWKS);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else if (Sw360ResourceServer.IS_JWKS_VALIDATION_ENABLED && !headers.isEmpty()
                    && headers.containsKey(OIDC_AUTHENTICATION_TOKEN_PARAMETER)) {
                String authorization = headers.get(OIDC_AUTHENTICATION_TOKEN_PARAMETER);
                String[] token = authorization.trim().split("\\s+");
                if (token.length == 2 && token[0].equalsIgnoreCase("Bearer")) {
                    Authentication auth = new ApiTokenAuthentication(token[1]).setType(AuthType.JWKS);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    enum AuthType {
        JWKS;
    }

    class ApiTokenAuthentication implements Authentication {
        private static final long serialVersionUID = 1L;

        private String token;

        private AuthType type;

        private ApiTokenAuthentication(String token) {
            this.token = token;
        }

        @Override
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

        public AuthType getType() {
            return type;
        }

        public ApiTokenAuthentication setType(AuthType type) {
            this.type = type;
            return this;
        }
    }
}
