/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.health;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class HealthClients {

    private static volatile HealthClient defaultClient;

    private HealthClients() {}

    public static HealthClient get() {
        return defaultClient();
    }

    public static void set(HealthClient client) {
        synchronized (HealthClients.class) {
            defaultClient = client;
        }
    }

    public static HealthClient defaultClient() {
        if (defaultClient == null) {
            synchronized (HealthClients.class) {
                if (defaultClient == null) {
                    defaultClient = new HealthServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
