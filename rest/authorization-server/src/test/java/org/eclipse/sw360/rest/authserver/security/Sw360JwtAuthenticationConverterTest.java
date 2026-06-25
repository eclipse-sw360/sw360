/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.eclipse.sw360.rest.common.security.Sw360UserDetailsProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class Sw360JwtAuthenticationConverterTest {

    private final Sw360UserDetailsProvider userDetailsProvider = mock(Sw360UserDetailsProvider.class);
    private final Sw360JwtAuthenticationConverter converter =
            new Sw360JwtAuthenticationConverter(userDetailsProvider);

    @Test
    public void shouldKeepScopeAuthorities_forSw360IssuedTokens() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("user_name", "admin@sw360.org")
                .claim("scope", List.of("READ", "ADMIN"))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) converter.convert(jwt);
        assertNotNull(authentication);

        assertThat(authentication.getName()).isEqualTo("admin@sw360.org");
        assertThat(authorityNames(authentication)).contains("READ", "ADMIN");
    }

    @Test
    public void shouldMapKeycloakUserGroupClaim_toAdminAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "admin@sw360.org")
                .claim("email", "admin@sw360.org")
                .claim("userGroup", List.of("/SW360_ADMIN"))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) converter.convert(jwt);
        assertNotNull(authentication);

        assertThat(authentication.getName()).isEqualTo("admin@sw360.org");
        assertThat(authorityNames(authentication)).contains("READ", "WRITE", "ADMIN");
    }

    @Test
    public void shouldMapRealmRoles_andIgnoreUnknownGroupNames() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("email", "user@example.com")
                .claim("realm_access", Map.of("roles", List.of("ignored-role", "SW360_ADMIN")))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) converter.convert(jwt);
        assertNotNull(authentication);

        assertThat(authorityNames(authentication)).contains("READ", "WRITE", "ADMIN");
    }

    private static Set<String> authorityNames(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
