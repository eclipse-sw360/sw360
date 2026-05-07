/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.basic;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.security.TokenCapabilityAuthorities;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Sw360CustomUserDetailsServiceTest {

    @Mock
    private Sw360UserDetailsProvider sw360UserDetailsProvider;

    @InjectMocks
    private Sw360CustomUserDetailsService service;

    private User user;

    @Before
    public void setUp() {
        user = new User();
        user.setEmail("admin@sw360.org");
        user.setPassword("$2a$10$examplehash");
        user.setUserGroup(UserGroup.ADMIN);
    }

    @Test
    public void shouldIncludeTokenCapabilitiesForBasicAuthUserDetails() {
        when(sw360UserDetailsProvider.provideUserDetails("admin@sw360.org", null)).thenReturn(user);

        UserDetails details = service.loadUserByUsername("admin@sw360.org");

        assertThat(details).isInstanceOf(Sw360UserDetails.class);
        assertThat(details.getAuthorities())
                .extracting("authority")
                .contains(TokenCapabilityAuthorities.TOKEN_READ, TokenCapabilityAuthorities.TOKEN_WRITE)
                .contains("READ", "WRITE", "ADMIN");
        assertThat(((Sw360UserDetails) details).getSw360User()).isSameAs(user);
    }
}
