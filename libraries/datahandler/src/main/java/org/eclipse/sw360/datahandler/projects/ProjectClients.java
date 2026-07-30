/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.projects;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

/**
 * Entry point for obtaining a {@link ProjectClient}.
 *
 * Same pattern as {@link org.eclipse.sw360.datahandler.licenses.LicenseClients}: static factory
 * for non-Spring callers, aimed at {@link org.eclipse.sw360.datahandler.thrift.ThriftClients#BACKEND_URL}.
 */
public final class ProjectClients {

    private static volatile ProjectClient defaultClient;

    private ProjectClients() {}

    public static ProjectClient get() {
        return defaultClient();
    }

    public static void set(ProjectClient client) {
        synchronized (ProjectClients.class) {
            defaultClient = client;
        }
    }

    public static ProjectClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ProjectClients.class) {
                if (defaultClient == null) {
                    defaultClient = new ProjectServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
