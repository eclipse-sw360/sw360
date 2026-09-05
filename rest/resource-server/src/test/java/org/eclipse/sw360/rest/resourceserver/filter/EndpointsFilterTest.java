/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointsFilterTest {

    @Test
    void should_allow_security_user_for_release_batch_summary() throws Exception {
        RestControllerHelper<?> restControllerHelper = Mockito.mock(RestControllerHelper.class);
        User securityUser = new User();
        securityUser.setUserGroup(UserGroup.SECURITY_USER);
        when(restControllerHelper.getSw360UserFromAuthentication()).thenReturn(securityUser);

        EndpointsFilter filter = new EndpointsFilter(restControllerHelper, "");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/releases/batch-summary");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void should_block_security_user_for_non_exempt_write_endpoint() throws Exception {
        RestControllerHelper<?> restControllerHelper = Mockito.mock(RestControllerHelper.class);
        User securityUser = new User();
        securityUser.setUserGroup(UserGroup.SECURITY_USER);
        when(restControllerHelper.getSw360UserFromAuthentication()).thenReturn(securityUser);

        EndpointsFilter filter = new EndpointsFilter(restControllerHelper, "");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/releases");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        Mockito.verifyNoInteractions(filterChain);
        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }
}
