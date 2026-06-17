/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security;

import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenCapabilityAuthoritiesTest {

    @Test
    public void shouldReturnReadOnlyCapabilitiesForReadScope() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.fromJwtScopeClaim("READ profile email");

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .doesNotContain(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldReturnReadWriteCapabilitiesForWriteScope() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.fromJwtScopeClaim("READ WRITE profile");

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldTreatUnknownJwtScopesAsReadWriteIdentityToken() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.fromJwtScopeClaim(List.of("profile", "email"));

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldTreatMissingJwtScopeAsReadWriteIdentityToken() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.fromJwtScopeClaim(null);

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldTreatUnknownApiTokenAuthoritiesAsReadOnly() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.fromAuthorityNames(List.of("profile", "email"));

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .doesNotContain(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldMergeGroupAndCapabilityAuthorities() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.merge(
                Set.of(new SimpleGrantedAuthority("ADMIN")),
                Set.of(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ)));

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority("ADMIN"))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ));
    }

    @Test
    public void shouldKeepUserAuthoritiesForReadOnlyTokenCapabilities() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.mergeForTokenAuthentication(
                Set.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("WRITE")),
                Set.of(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ)));

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority("ADMIN"))
                .contains(new SimpleGrantedAuthority("WRITE"))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ))
                .doesNotContain(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

    @Test
    public void shouldKeepWriteAuthorityForWriteTokenCapabilities() {
        Set<GrantedAuthority> authorities = TokenCapabilityAuthorities.mergeForTokenAuthentication(
                Set.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("WRITE")),
                Set.of(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_READ),
                        new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE)));

        assertThat(authorities)
                .contains(new SimpleGrantedAuthority("ADMIN"))
                .contains(new SimpleGrantedAuthority("WRITE"))
                .contains(new SimpleGrantedAuthority(TokenCapabilityAuthorities.TOKEN_WRITE));
    }

}
