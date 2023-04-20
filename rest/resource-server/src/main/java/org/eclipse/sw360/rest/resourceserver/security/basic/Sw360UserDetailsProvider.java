/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.springframework.stereotype.Repository;

@Repository
public class Sw360UserDetailsProvider {

    private final Logger log = LogManager.getLogger(this.getClass());

    private ThriftClients thriftClients = new ThriftClients();

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
