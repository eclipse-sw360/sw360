/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.clients.users.UsersClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class Sw360UserDetailsProvider {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final UsersClient usersClient;

    public User provideUserDetails(String email, String extId) {
        log.debug("Looking up user with email <{}> and external id <{}>.", email, extId);

        User user = getUserByEmailOrExternalId(email, extId);
        if (user != null) {
            log.debug("Found user with email <{}> and external id <{}> in UserService.", email, extId);
            return user;
        } else {
            log.warn("No user found with email <{}> and external id <{}> in UserService.", email, extId);
            throw new BadCredentialsException("Bad credentials");
        }
    }

    private User getUserByEmailOrExternalId(String email, String externalId) {
        try {
            if (StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(externalId)) {
                return UserConverter.toThrift(usersClient.getByEmailOrExternalId(email, externalId));
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }
}
