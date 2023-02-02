/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EndpointsFilter extends OncePerRequestFilter {

    private static final String ERROR_MESSAGE = "Service is disabled";

    private static final String CREATE_USER_ENDPOINT = "/resource/api/users";

    @Value("${sw360.rest.api.createuser.disabled}")
    boolean disabledUsrCreation;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        if (disabledUsrCreation && requestURI.equalsIgnoreCase(CREATE_USER_ENDPOINT) && method.equals("POST")) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), ERROR_MESSAGE);
        } else {
            filterChain.doFilter(request, response);
        }

    }

}
