/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseInstanceTrackerCloudant {
    private static ConcurrentLinkedQueue<DatabaseInstanceCloudant> trackedInstances = new ConcurrentLinkedQueue<>();

    public static void track(DatabaseInstanceCloudant databaseInstance) {
        trackedInstances.add(databaseInstance);
    }

    public static void destroy() {
        DatabaseInstanceCloudant trackedInstance;
        while ((trackedInstance = trackedInstances.poll()) != null) {
            trackedInstance.destroy();
        }
    }
}
