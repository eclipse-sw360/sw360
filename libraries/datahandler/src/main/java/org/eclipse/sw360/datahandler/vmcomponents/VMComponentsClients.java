/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.vmcomponents;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link VMComponentsClient}.
 */
public final class VMComponentsClients {

    private static volatile VMComponentsClient defaultClient;

    private VMComponentsClients() {}

    public static VMComponentsClient get() {
        return defaultClient();
    }

    public static void set(VMComponentsClient client) {
        synchronized (VMComponentsClients.class) {
            defaultClient = client;
        }
    }

    public static VMComponentsClient defaultClient() {
        if (defaultClient == null) {
            synchronized (VMComponentsClients.class) {
                if (defaultClient == null) {
                    defaultClient = new VMComponentsServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
