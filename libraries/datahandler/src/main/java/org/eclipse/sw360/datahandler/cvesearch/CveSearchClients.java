/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cvesearch;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class CveSearchClients {

    private static volatile CveSearchClient defaultClient;

    private CveSearchClients() {}

    public static CveSearchClient get() {
        return defaultClient();
    }

    public static void set(CveSearchClient client) {
        synchronized (CveSearchClients.class) {
            defaultClient = client;
        }
    }

    public static CveSearchClient defaultClient() {
        if (defaultClient == null) {
            synchronized (CveSearchClients.class) {
                if (defaultClient == null) {
                    defaultClient = new CveSearchServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
