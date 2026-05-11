/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.apiToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiTokenAuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private AuthenticationEntryPoint authenticationEntryPoint;
    private FilterChain filterChain;

    @Before
    public void setUp() {
        SecurityContextHolder.clearContext();
        authenticationManager = mock(AuthenticationManager.class);
        authenticationEntryPoint = mock(AuthenticationEntryPoint.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    public void shouldAuthenticateTokenPrefix() throws IOException, ServletException {
        ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(authenticationManager, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("authorization", "Token some-api-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(ArgumentMatchers.any())).thenReturn(new TestingAuthenticationToken("u", "c", "READ"));

        filter.doFilter(request, response, filterChain);

        verify(authenticationManager, times(1)).authenticate(ArgumentMatchers.any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void shouldNotAuthenticateBearerTokenInFilter() throws IOException, ServletException {
        ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(authenticationManager, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Bearer token should not be handled by this filter - it goes to Spring resource-server
        verify(authenticationManager, never()).authenticate(ArgumentMatchers.any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void shouldNotAuthenticateOidcAuthorizationHeader() throws IOException, ServletException {
        ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(authenticationManager, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("oidcauthorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // oidcauthorization header should not be handled - it goes to Spring resource-server
        verify(authenticationManager, never()).authenticate(ArgumentMatchers.any());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
