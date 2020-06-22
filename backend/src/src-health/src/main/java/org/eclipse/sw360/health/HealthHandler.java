/*
 * Copyright Bosch.IO 2020.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.health;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.HealthService;
import org.eclipse.sw360.health.db.HealthDatabaseHandler;

import java.net.MalformedURLException;

/**
 * Implementation of the thrift service "Health"
 */
public class HealthHandler implements HealthService.Iface {

    private final HealthDatabaseHandler handler;

    HealthHandler() throws MalformedURLException {
        handler = new HealthDatabaseHandler(DatabaseSettings.getConfiguredHttpClient());
    }

    @Override
    public Health getHealth() {
        return handler.getHealth();
    }
}
