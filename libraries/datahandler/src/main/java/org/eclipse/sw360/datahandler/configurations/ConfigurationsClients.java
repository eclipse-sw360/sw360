/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.configurations;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link ConfigurationsClient}.
 * Same pattern as {@link org.eclipse.sw360.datahandler.moderation.ModerationClients}.
 */
public final class ConfigurationsClients {

    private static volatile ConfigurationsClient defaultClient;

    private ConfigurationsClients() {}

    public static ConfigurationsClient get() {
        return defaultClient();
    }

    public static void set(ConfigurationsClient client) {
        synchronized (ConfigurationsClients.class) {
            defaultClient = client;
        }
    }

    public static ConfigurationsClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ConfigurationsClients.class) {
                if (defaultClient == null) {
                    defaultClient = new ConfigurationsServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
