/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.vendors;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link VendorClient}.
 */
public final class VendorClients {

    private static volatile VendorClient defaultClient;

    private VendorClients() {}

    public static VendorClient get() {
        return defaultClient();
    }

    public static void set(VendorClient client) {
        synchronized (VendorClients.class) {
            defaultClient = client;
        }
    }

    public static VendorClient defaultClient() {
        if (defaultClient == null) {
            synchronized (VendorClients.class) {
                if (defaultClient == null) {
                    defaultClient = new VendorServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
