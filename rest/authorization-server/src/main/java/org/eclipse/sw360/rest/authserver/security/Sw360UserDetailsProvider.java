/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.clients.users.UsersClient;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * This user details provider queries the SW360 users REST service to
 * check if a user identified by an email address or an external id exists.
 */
@Service
@RequiredArgsConstructor
public class Sw360UserDetailsProvider {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final UsersClient usersClient;

    public User provideUserDetails(String email, String extId) {
        User result = null;

        log.debug("Looking up user with email <" + email + "> and external id <" + extId + ">.");

        User user = getUserByEmailOrExternalId(email, extId);
        if (user != null) {
            log.debug("Found user with email <" + email + "> and external id <" + extId + "> in UserService.");
            result = user;
        } else {
            log.warn("No user found with email <" + email + "> and external id <" + extId + "> in UserService.");
        }

        return result;
    }

    private User getUserByEmailOrExternalId(String email, String externalId) {
        // client should be put into threadlocal some day after this pattern proofed
        // itself
        try {
            if (StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(externalId)) {
                return usersClient.getByEmailOrExternalId(email, externalId);
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    @VisibleForTesting
    public @NonNull UsersClient getUsersClient() {
        return usersClient;
    }
}
