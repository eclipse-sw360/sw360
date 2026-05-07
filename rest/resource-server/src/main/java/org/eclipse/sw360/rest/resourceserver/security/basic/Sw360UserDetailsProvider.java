/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Repository;

@Repository
public class Sw360UserDetailsProvider {

    private final Logger log = LogManager.getLogger(this.getClass());

    private final ThriftClients thriftClients = new ThriftClients();

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
        UserService.Iface client = thriftClients.makeUserClient();
        try {
            if (StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(externalId)) {
                return client.getByEmailOrExternalId(email, externalId);
            }
        } catch (TException e) {
            // do nothing
        }
        return null;
    }
}
