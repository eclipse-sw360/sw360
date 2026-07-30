/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.attachments;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining an {@link AttachmentClient}.
 *
 * Same pattern as {@link org.eclipse.sw360.datahandler.configurations.ConfigurationsClients}:
 * static factory for non-Spring callers, aimed at {@link ThriftClients#BACKEND_URL}.
 */
public final class AttachmentClients {

    private static volatile AttachmentClient defaultClient;

    private AttachmentClients() {}

    public static AttachmentClient get() {
        return defaultClient();
    }

    public static void set(AttachmentClient client) {
        synchronized (AttachmentClients.class) {
            defaultClient = client;
        }
    }

    public static AttachmentClient defaultClient() {
        if (defaultClient == null) {
            synchronized (AttachmentClients.class) {
                if (defaultClient == null) {
                    defaultClient = new AttachmentServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
