/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.search;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class SearchClients {

    private static volatile SearchClient defaultClient;

    private SearchClients() {}

    public static SearchClient get() {
        return defaultClient();
    }

    public static void set(SearchClient client) {
        synchronized (SearchClients.class) {
            defaultClient = client;
        }
    }

    public static SearchClient defaultClient() {
        if (defaultClient == null) {
            synchronized (SearchClients.class) {
                if (defaultClient == null) {
                    defaultClient = new SearchServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
