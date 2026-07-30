/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.users;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link UsersClient}.
 *
 * Same pattern as other datahandler {@code *Clients} factories: static access for
 * non-Spring callers, backed by {@link BackendRestClients#shared()}.
 */
public final class UsersClients {

    private static volatile UsersClient defaultClient;

    private UsersClients() {}

    public static UsersClient get() {
        return defaultClient();
    }

    public static void set(UsersClient client) {
        synchronized (UsersClients.class) {
            defaultClient = client;
        }
    }

    public static UsersClient defaultClient() {
        if (defaultClient == null) {
            synchronized (UsersClients.class) {
                if (defaultClient == null) {
                    defaultClient = new UsersServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
