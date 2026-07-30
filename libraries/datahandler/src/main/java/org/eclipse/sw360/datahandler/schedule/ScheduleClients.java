/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.schedule;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;

public final class ScheduleClients {

    private static volatile ScheduleClient defaultClient;

    private ScheduleClients() {}

    public static ScheduleClient get() {
        return defaultClient();
    }

    public static void set(ScheduleClient client) {
        synchronized (ScheduleClients.class) {
            defaultClient = client;
        }
    }

    public static ScheduleClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ScheduleClients.class) {
                if (defaultClient == null) {
                    defaultClient = new ScheduleServiceRestClient(BackendRestClients.shared());
                }
            }
        }
        return defaultClient;
    }
}
