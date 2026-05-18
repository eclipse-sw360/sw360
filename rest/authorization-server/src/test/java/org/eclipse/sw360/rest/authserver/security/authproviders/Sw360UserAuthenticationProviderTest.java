/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security.authproviders;

import org.eclipse.sw360.rest.authserver.client.service.Sw360UserDetailsService;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sw360UserAuthenticationProviderTest {

    @Test
    public void shouldAddPasswordFactorAuthority_onSuccessfulAuthentication() {
        Sw360UserAuthenticationProvider provider = new Sw360UserAuthenticationProvider();

        Sw360UserDetailsService userDetailsService = mock(Sw360UserDetailsService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ReflectionTestUtils.setField(provider, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(provider, "passwordEncoder", passwordEncoder);

        User user = new User(
                "admin@sw360.org",
                "$2a$10$hashed",
                List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("READ"))
        );

        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches("secret", user.getPassword())).thenReturn(true);

        Authentication authentication = provider.authenticate(
                new UsernamePasswordAuthenticationToken("admin@sw360.org", "secret")
        );

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ADMIN", "READ", FactorGrantedAuthority.PASSWORD_AUTHORITY);
    }
}
