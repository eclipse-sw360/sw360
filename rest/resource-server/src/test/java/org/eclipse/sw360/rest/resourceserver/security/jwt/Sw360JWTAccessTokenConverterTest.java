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

import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.rest.common.security.TokenCapabilityAuthorities;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Sw360JWTAccessTokenConverterTest {

    @Mock
    private Sw360UserService userService;

    private Sw360JWTAccessTokenConverter converter;


    @BeforeEach
    public void setUp() {
        converter = new Sw360JWTAccessTokenConverter();
        ReflectionTestUtils.setField(converter, "userService", userService);
        ReflectionTestUtils.setField(converter, "principleAttribute", "email");
    }

    @Test
    public void shouldKeepAdminAuthorityButNoTokenWriteForReadOnlyScopeToken() {
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
                .contains(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .doesNotContain(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldKeepUserAuthoritiesWhenJwtHasNoSw360Scope() {
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
                .contains(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))
                .contains(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldKeepUserAuthoritiesWhenJwtHasOnlyIdentityProviderScopes() {
        User adminUser = new User();
        adminUser.setEmail("admin-keycloak@sw360.org");
        adminUser.setUserGroup(UserGroup.ADMIN);
        when(userService.getUserByEmail("admin-keycloak@sw360.org")).thenReturn(adminUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "admin-keycloak@sw360.org")
                .claim("scope", "openid profile email")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .contains(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))
                .contains(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }


    @Test
    public void shouldResolveUserByClientIdWhenEmailClaimIsMissing() {
        User clientMappedUser = new User();
        clientMappedUser.setEmail("oidc-client-user@sw360.org");
        clientMappedUser.setUserGroup(UserGroup.USER);
        when(userService.getUserFromClientId("trusted-sw360-client")).thenReturn(clientMappedUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("client_id", "trusted-sw360-client")
                .claim("scope", "READ")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getDetails()).isSameAs(clientMappedUser);
        verify(userService).getUserFromClientId("trusted-sw360-client");
        verify(userService, never()).getUserByEmail("trusted-sw360-client");
    }

    @Test
    public void shouldResolveUserByFirstClientIdWhenClaimIsArray() {
        User clientMappedUser = new User();
        clientMappedUser.setEmail("oidc-client-user@sw360.org");
        clientMappedUser.setUserGroup(UserGroup.USER);
        when(userService.getUserFromClientId("trusted-sw360-client")).thenReturn(clientMappedUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("client_id", List.of("trusted-sw360-client", "fallback-client"))
                .claim("scope", "READ")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getDetails()).isSameAs(clientMappedUser);
        verify(userService).getUserFromClientId("trusted-sw360-client");
    }

    @Test
    public void shouldFailWhenJwtCannotBeMappedToSw360User() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("client_id", "unmapped-client")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessage(Sw360JWTAccessTokenConverter.USER_IS_DEACTIVATED_OR_NOT_AVAILABLE);
    }
}
