/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.security;

import java.io.IOException;

import org.eclipse.sw360.datahandler.rest.BackendInternalAuth;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects direct access to backend WARs unless {@link BackendInternalAuth} is satisfied.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BackendInternalAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!BackendInternalAuth.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(BackendInternalAuth.HEADER_NAME);
        if (BackendInternalAuth.matches(provided)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Unauthorized: missing or invalid " + BackendInternalAuth.HEADER_NAME);
    }
}
