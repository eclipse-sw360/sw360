/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Profile("!SECURITY_MOCK")
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LogManager.getLogger(Sw360CustomUserDetailsService.class);

    @Autowired
    Sw360UserDetailsProvider sw360UserDetailsProvider;

    @Override
    public UserDetails loadUserByUsername(String userid) {
        log.info("Authenticating for the user with username {}", userid);
        User user = sw360UserDetailsProvider.provideUserDetails(userid, null);
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),
                Sw360GrantedAuthoritiesCalculator.generateFromUser(user));
    }
}
