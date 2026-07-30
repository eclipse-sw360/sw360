/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.licenses;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link LicenseClient}.
 *
 * Same pattern as {@link org.eclipse.sw360.datahandler.moderation.ModerationClients}: static factory
 * for non-Spring callers, aimed at {@link ThriftClients#BACKEND_URL}.
 */
public final class LicenseClients {

    private static volatile LicenseClient defaultClient;

    private LicenseClients() {}

    public static LicenseClient get() {
        return defaultClient();
    }

    public static void set(LicenseClient client) {
        synchronized (LicenseClients.class) {
            defaultClient = client;
        }
    }

    public static LicenseClient defaultClient() {
        if (defaultClient == null) {
            synchronized (LicenseClients.class) {
                if (defaultClient == null) {
                    defaultClient = new LicenseServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
