/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.licenseinfo;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link LicenseInfoClient}.
 *
 * Same pattern as {@code ModerationClients}: static factory for non-Spring callers,
 * aimed at {@link ThriftClients#BACKEND_URL}.
 */
public final class LicenseInfoClients {
    private static volatile LicenseInfoClient defaultClient;

    private LicenseInfoClients() {}

    public static LicenseInfoClient get() {
        return defaultClient();
    }

    public static void set(LicenseInfoClient client) {
        synchronized (LicenseInfoClients.class) {
            defaultClient = client;
        }
    }

    public static LicenseInfoClient defaultClient() {
        if (defaultClient == null) {
            synchronized (LicenseInfoClients.class) {
                if (defaultClient == null) {
                    defaultClient = new LicenseInfoServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
