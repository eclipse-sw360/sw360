/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Profile("SECURITY_MOCK")
@Primary
@Service
@Component
public class Sw360AuthenticationProvider implements AuthenticationProvider {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    // TODO Thomas Maier 15-12-2017
    // Use Sw360GrantedAuthority from authorization server
    private final String GRANTED_AUTHORITY_READ = "READ";
    private final String GRANTED_AUTHORITY_WRITE = "WRITE";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = (String) authentication.getCredentials();
        // For the tests we mock an existing sw360 user with read and write authorities
        if (name.equals(testUserId) && password.equals(testUserPassword)) {
            List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            grantedAuthorities.add(new SimpleGrantedAuthority(GRANTED_AUTHORITY_READ));
            grantedAuthorities.add(new SimpleGrantedAuthority(GRANTED_AUTHORITY_WRITE));
            Authentication auth = new UsernamePasswordAuthenticationToken(name, password, grantedAuthorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return auth;
        } else {
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
