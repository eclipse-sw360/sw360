/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * Put security headers in the API responses.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class Sw360SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain
    ) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        setSecurityHeaders(httpServletRequest, httpServletResponse);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void setSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "0");

        String path = request.getRequestURI();
        if (path != null && (path.contains("/swagger-ui") || path.contains("/v3/api-docs"))) {
            // Relaxed CSP for Swagger UI to function
            response.setHeader("Content-Security-Policy", "default-src 'self'; object-src 'none'; base-uri 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'; upgrade-insecure-requests;");
        } else {
            // Strict CSP for API responses
            response.setHeader("Content-Security-Policy", "default-src 'none'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; upgrade-insecure-requests;");
        }
    }
}
