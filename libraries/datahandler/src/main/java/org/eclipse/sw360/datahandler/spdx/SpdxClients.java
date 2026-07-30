/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.spdx;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class SpdxClients {

    private static volatile SpdxClient defaultClient;

    private SpdxClients() {}

    public static SpdxClient get() {
        return defaultClient();
    }

    public static void set(SpdxClient client) {
        synchronized (SpdxClients.class) {
            defaultClient = client;
        }
    }

    public static SpdxClient defaultClient() {
        if (defaultClient == null) {
            synchronized (SpdxClients.class) {
                if (defaultClient == null) {
                    defaultClient = new SpdxServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
