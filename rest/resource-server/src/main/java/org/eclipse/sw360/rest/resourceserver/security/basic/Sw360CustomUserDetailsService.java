/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.security.TokenCapabilityAuthorities;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Sw360CustomUserDetailsService {

    private static final Logger log = LogManager.getLogger(Sw360CustomUserDetailsService.class);

    private final Sw360UserDetailsProvider sw360UserDetailsProvider;

    public @Nonnull UserDetails loadUserByUsername(@Nullable String userid) {
        log.info("Authenticating for the user with username {}", userid);
        User user = sw360UserDetailsProvider.provideUserDetails(userid, null);
        Set<GrantedAuthority> authorities = new HashSet<>(Sw360GrantedAuthoritiesCalculator.generateFromUser(user));
        authorities.addAll(TokenCapabilityAuthorities.readWrite());
        return new Sw360UserDetails(user, authorities);
    }
}
