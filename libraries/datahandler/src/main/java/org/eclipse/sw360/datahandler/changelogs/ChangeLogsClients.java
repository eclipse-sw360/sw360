/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.changelogs;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class ChangeLogsClients {

    private static volatile ChangeLogsClient defaultClient;

    private ChangeLogsClients() {}

    public static ChangeLogsClient get() {
        return defaultClient();
    }

    public static void set(ChangeLogsClient client) {
        synchronized (ChangeLogsClients.class) {
            defaultClient = client;
        }
    }

    public static ChangeLogsClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ChangeLogsClients.class) {
                if (defaultClient == null) {
                    defaultClient = new ChangeLogsServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
