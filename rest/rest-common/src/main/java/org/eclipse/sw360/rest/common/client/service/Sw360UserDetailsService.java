/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.common.client.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.common.security.Sw360UserDetailsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class Sw360UserDetailsService implements UserDetailsService {

    private final Logger log = LogManager.getLogger(this.getClass());

    private final Sw360UserDetailsProvider userProvider;


    private final Sw360GrantedAuthoritiesCalculator authoritiesCalculator;

    Sw360UserDetailsService(Sw360UserDetailsProvider sw360UserDetailsProvider, Sw360GrantedAuthoritiesCalculator sw360GrantedAuthoritiesCalculator) {
        this.userProvider = sw360UserDetailsProvider;
        this.authoritiesCalculator = sw360GrantedAuthoritiesCalculator;
    }

    /**
     * @param username the username identifying the user whose data is required.
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    @Nonnull
    public UserDetails loadUserByUsername(@Nullable String username) throws UsernameNotFoundException {
        User user = userProvider.provideUserDetails(username, null);
        if (user == null) {
            throw new UsernameNotFoundException("User not found in the database with email: " + username);
        }
        log.debug("Sw360UserDetailsProvider returned user: {}", user.toString());
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authoritiesCalculator.generateFromUser(user));
    }
}
