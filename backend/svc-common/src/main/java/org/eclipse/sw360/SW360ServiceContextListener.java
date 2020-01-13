/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360;

import org.eclipse.sw360.datahandler.couchdb.DatabaseInstanceTracker;
import org.ektorp.http.IdleConnectionMonitor;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author daniele.fognini@tngtech.com
 */
public class SW360ServiceContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DatabaseInstanceTracker.destroy();
        IdleConnectionMonitor.shutdown();
    }
}
