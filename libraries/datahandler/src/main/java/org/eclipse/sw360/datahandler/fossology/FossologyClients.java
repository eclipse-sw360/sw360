/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.fossology;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class FossologyClients {

    private static volatile FossologyClient defaultClient;

    private FossologyClients() {}

    public static FossologyClient get() {
        return defaultClient();
    }

    public static void set(FossologyClient client) {
        synchronized (FossologyClients.class) {
            defaultClient = client;
        }
    }

    public static FossologyClient defaultClient() {
        if (defaultClient == null) {
            synchronized (FossologyClients.class) {
                if (defaultClient == null) {
                    defaultClient = new FossologyServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
