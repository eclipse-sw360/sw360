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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This user details provider is able to query the sw360 user thrift service to
 * check if a user identified by an email address or an external id exists.
 */
@Service
public class Sw360UserDetailsProvider {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private ThriftClients thriftClients;

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
