/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
