/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.archival;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Lazily creates and holds the default {@link ArchivalClient}, backed by the
 * shared backend RestClient. Mirrors the other services' *Clients factories so
 * callers use {@code ArchivalClients.get()}.
 */
public final class ArchivalClients {

    private static volatile ArchivalClient defaultClient;

    private ArchivalClients() {}

    public static ArchivalClient get() {
        return defaultClient();
    }

    public static void set(ArchivalClient client) {
        synchronized (ArchivalClients.class) {
            defaultClient = client;
        }
    }

    public static ArchivalClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ArchivalClients.class) {
                if (defaultClient == null) {
                    defaultClient = new ArchivalServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
