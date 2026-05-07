/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.obligation.Sw360ObligationService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class RestControllerHelperAuthCachingTest {

    @Mock
    private Sw360UserService userService;

    @Mock
    private Sw360VendorService vendorService;

    @Mock
    private Sw360LicenseService licenseService;

    @Mock
    private Sw360ObligationService obligationService;

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldReuseCachedSw360UserFromAuthenticationDetails() {
        RestControllerHelper<Object> helper = new RestControllerHelper<>(
                userService,
                vendorService,
                licenseService,
                obligationService,
                new SimpleModule()
        );

        User cachedUser = new User();
        cachedUser.setEmail("cached@sw360.org");

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "cached@sw360.org",
                "secret"
        );
        authentication.setDetails(cachedUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User resolvedUser = helper.getSw360UserFromAuthentication();

        assertThat(resolvedUser).isSameAs(cachedUser);
        verifyNoInteractions(userService);
    }

    @Test
    public void shouldFailFastWhenAuthenticationIsMissing() {
        RestControllerHelper<Object> helper = new RestControllerHelper<>(
                userService,
                vendorService,
                licenseService,
                obligationService,
                new SimpleModule()
        );

        SecurityContextHolder.clearContext();

        assertThatThrownBy(helper::getSw360UserFromAuthentication)
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessage("Could not load user from authentication.");
    }
}
