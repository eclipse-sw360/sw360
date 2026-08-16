/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.users.UsersClients;

/**
 * Small client for smoke-testing the users HTTP API (POJO / service-api).
 *
 * @author cedric.bodet@tngtech.com
 */
public class TestUserClient {
    private static final Logger log = LogManager.getLogger(TestUserClient.class);

    public static void main(String[] args) {
        User user = UsersClients.get().getByEmail("cedric.bodet@tngtech.com");
        log.info("{}", user);
    }
}
