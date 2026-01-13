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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.security.JwtBlacklistService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
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
import java.util.*;
import java.util.stream.Collectors;

@Profile("!SECURITY_MOCK")
public class ApiTokenAuthenticationFilter implements Filter {

    private static final Logger log = LogManager.getLogger(ApiTokenAuthenticationFilter.class);
    private static final String AUTHENTICATION_TOKEN_PARAMETER = "authorization";
    private static final String OIDC_AUTHENTICATION_TOKEN_PARAMETER = "oidcauthorization";
    private static final String ERROR_TOKEN_REVOKED = "Token revoked";
    private static final String ERROR_INVALID_TOKEN_FORMAT = "Invalid token format";

    private JwtBlacklistService jwtBlacklistService;

    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ApiTokenAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint) {
        this(authenticationManager, authenticationEntryPoint, null);
    }

    public ApiTokenAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint, JwtBlacklistService jwtBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.jwtBlacklistService = jwtBlacklistService;
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
            try {
                Map<String, String> headers = Collections.list(((HttpServletRequest) request).getHeaderNames()).stream()
                        .collect(Collectors.toMap(h -> h.toLowerCase(), ((HttpServletRequest) request)::getHeader));

                if (!headers.isEmpty() && headers.containsKey(AUTHENTICATION_TOKEN_PARAMETER)) {
                    String authorization = headers.get(AUTHENTICATION_TOKEN_PARAMETER);
                    String[] token = authorization.trim().split("\\s+");
                    if (token.length != 2) {
                        log.warn("Invalid token format in Authorization header: {}", authorization);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, ERROR_INVALID_TOKEN_FORMAT);
                        return;
                    }
                    if (token[0].equalsIgnoreCase("token")) {
                        Authentication auth = authenticationManager.authenticate(new ApiTokenAuthentication(token[1]));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else if (token[0].equalsIgnoreCase("Bearer")) {
                        if (!authenticateJwtToken(token[1], (HttpServletResponse) response, "Authorization")) {
                            return;
                        }
                    }
                } else if (Sw360ResourceServer.IS_JWKS_VALIDATION_ENABLED && !headers.isEmpty()
                        && headers.containsKey(OIDC_AUTHENTICATION_TOKEN_PARAMETER)) {
                    String authorization = headers.get(OIDC_AUTHENTICATION_TOKEN_PARAMETER);
                    String[] token = authorization.trim().split("\\s+");
                    if (token.length != 2) {
                        log.warn("Invalid token format in OIDC Authorization header: {}", authorization);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, ERROR_INVALID_TOKEN_FORMAT);
                        return;
                    }
                    if (token[0].equalsIgnoreCase("Bearer")) {
                        if (!authenticateJwtToken(token[1], (HttpServletResponse) response, "OIDC")) {
                            return;
                        }
                    }
                }
            } catch (AuthenticationException e) {
                log.error("Authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence((HttpServletRequest) request, (HttpServletResponse) response, e);
            } catch (Exception e) {
                log.error("Unexpected error in authentication filter", e);
                SecurityContextHolder.clearContext();
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticateJwtToken(String token, HttpServletResponse response, String logPrefix) throws IOException {
        if (jwtBlacklistService != null) {
            log.debug("{} Checking JWT blacklist for token", logPrefix);
            if (jwtBlacklistService.isTokenBlacklisted(token)) {
                log.warn("{} JWT token is blacklisted - Access denied", logPrefix);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, String> error = new HashMap<>();
                error.put("error", ERROR_TOKEN_REVOKED);
                error.put("message", "Token has been revoked. Please login again.");

                new ObjectMapper().writeValue(response.getWriter(), error);
                return false;
            }
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new ApiTokenAuthentication(token).setType(AuthType.JWKS)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        } catch (AuthenticationException e) {
            log.error("{} Authentication failed for JWT token", logPrefix, e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return false;
        }
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
