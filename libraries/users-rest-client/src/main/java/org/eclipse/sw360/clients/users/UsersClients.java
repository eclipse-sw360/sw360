/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.users;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.web.client.RestClient;

/**
 * Static access to a shared {@link UsersClient} for modules that are not Spring-managed
 * or cannot inject a {@link RestClient} bean.
 */
public final class UsersClients {

    private static volatile UsersClient defaultClient;

    private UsersClients() {}

    public static UsersClient defaultClient() {
        if (defaultClient == null) {
            synchronized (UsersClients.class) {
                if (defaultClient == null) {
                    RestClient restClient = RestClient.builder().baseUrl(ThriftClients.BACKEND_URL).build();
                    defaultClient = new UsersClient(restClient);
                }
            }
        }
        return defaultClient;
    }
}
