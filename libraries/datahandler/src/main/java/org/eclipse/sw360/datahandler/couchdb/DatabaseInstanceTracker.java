/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.couchdb;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DatabaseInstanceTracker {
    private static ConcurrentLinkedQueue<DatabaseInstance> trackedInstances = new ConcurrentLinkedQueue<>();

    public static void track(DatabaseInstance databaseInstance) {
        trackedInstances.add(databaseInstance);
    }

    public static void destroy() {
        DatabaseInstance trackedInstance;
        while ((trackedInstance = trackedInstances.poll()) != null) {
            trackedInstance.destroy();
        }
    }
}
