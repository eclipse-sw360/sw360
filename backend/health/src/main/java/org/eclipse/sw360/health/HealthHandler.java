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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.services.health.HealthResponse;
import org.eclipse.sw360.health.db.HealthDatabaseHandler;
import org.springframework.stereotype.Service;
import java.net.MalformedURLException;
import java.util.Set;

@Service
public class HealthHandler {

    private final HealthDatabaseHandler handler;

    public HealthHandler(Cloudant client) throws MalformedURLException {
        handler = new HealthDatabaseHandler(client);
    }

    public HealthResponse getHealth() {
        return handler.getHealth();
    }

    public HealthResponse getHealthOfSpecificDbs(Set<String> dbsToCheck){
        return handler.getHealthOfSpecificDbs(dbsToCheck);
    }
}
