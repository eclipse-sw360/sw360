/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.jwt;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.security.TokenCapabilityAuthorities;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Sw360JWTAccessTokenConverterTest {

    @Mock
    private Sw360UserService userService;

    private Sw360JWTAccessTokenConverter converter;

    @Before
    public void setUp() {
        converter = new Sw360JWTAccessTokenConverter();
        ReflectionTestUtils.setField(converter, "userService", userService);
        ReflectionTestUtils.setField(converter, "principleAttribute", "email");
    }

    @Test
    public void shouldKeepAdminAuthorityForReadOnlyScopeToken() {
        User adminUser = new User();
        adminUser.setEmail("admin@sw360.org");
        adminUser.setUserGroup(UserGroup.ADMIN);
        when(userService.getUserByEmail("admin@sw360.org")).thenReturn(adminUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin@sw360.org")
                .claim("scope", "READ profile email")
                .claim("client_id", "kc-client")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getDetails()).isSameAs(adminUser);
        assertThat(authentication.getAuthorities())
                .contains(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .doesNotContain(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldDefaultToReadWriteCapabilitiesWhenScopeMissing() {
        User adminUser = new User();
        adminUser.setEmail("admin-noscope@sw360.org");
        adminUser.setUserGroup(UserGroup.ADMIN);
        when(userService.getUserByEmail("admin-noscope@sw360.org")).thenReturn(adminUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin-noscope@sw360.org")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }
}
