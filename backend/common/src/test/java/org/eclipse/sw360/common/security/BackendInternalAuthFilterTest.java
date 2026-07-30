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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.sw360.datahandler.rest.BackendInternalAuth;
import org.junit.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BackendInternalAuthFilterTest {

    @Test
    public void headerNameIsStable() {
        assertEquals("X-SW360-Internal-Token", BackendInternalAuth.HEADER_NAME);
    }

    @Test
    public void whenAuthDisabled_requestPassesThrough() throws Exception {
        if (BackendInternalAuth.isEnabled()) {
            return;
        }

        BackendInternalAuthFilter filter = new BackendInternalAuthFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    public void whenAuthEnabledAndHeaderMissing_returnsUnauthorized() throws Exception {
        if (!BackendInternalAuth.isEnabled()) {
            return;
        }

        BackendInternalAuthFilter filter = new BackendInternalAuthFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(BackendInternalAuth.HEADER_NAME)).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
