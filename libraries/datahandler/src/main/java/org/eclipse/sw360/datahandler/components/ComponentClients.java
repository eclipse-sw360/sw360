/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.components;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link ComponentClient}.
 * Same pattern as {@link org.eclipse.sw360.datahandler.moderation.ModerationClients}.
 */
public final class ComponentClients {

    private static volatile ComponentClient defaultClient;

    private ComponentClients() {}

    public static ComponentClient get() {
        return defaultClient();
    }

    public static void set(ComponentClient client) {
        synchronized (ComponentClients.class) {
            defaultClient = client;
        }
    }

    public static ComponentClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ComponentClients.class) {
                if (defaultClient == null) {
                    defaultClient = new ComponentServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
