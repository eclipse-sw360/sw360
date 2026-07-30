/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.packages;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link PackageClient}.
 */
public final class PackageClients {

    private static volatile PackageClient defaultClient;

    private PackageClients() {}

    public static PackageClient get() {
        return defaultClient();
    }

    public static void set(PackageClient client) {
        synchronized (PackageClients.class) {
            defaultClient = client;
        }
    }

    public static PackageClient defaultClient() {
        if (defaultClient == null) {
            synchronized (PackageClients.class) {
                if (defaultClient == null) {
                    defaultClient = new PackageServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
