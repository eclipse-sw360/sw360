/*
 * Copyright Bosch.IO GmbH 2020
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseInstance;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.health.HealthService;
import org.eclipse.sw360.datahandler.thrift.health.Status;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SW360RestHealthIndicator implements HealthIndicator {
     @JsonIgnore
     private List<Throwable> throwables = new ArrayList<>();

    @Override
    public Health health() {
        RestState restState = check();
        if (!restState.isUp()) {
            Health.Builder builderWithDetails = Health.down()
                    .withDetail("Rest State", restState);
            for (Throwable throwable : throwables) {
                builderWithDetails = builderWithDetails.withException(new Exception(throwable));
            }
            return builderWithDetails
                    .build();
        }
        return Health.up().build();
    }

    private RestState check() {
        RestState restState = new RestState();
        try {
            restState.isDbReachable = isDbReachable();
        } catch (MalformedURLException e) {
            restState.isDbReachable = false;
        }
        restState.isThriftReachable = isThriftReachable();
        return restState;
    }

    private boolean isDbReachable() throws MalformedURLException {
        final DatabaseInstance databaseInstance = new DatabaseInstance(DatabaseSettings.getConfiguredHttpClient().get());
        try {
            /*
             *
             */
            return databaseInstance.checkIfDbExists(DatabaseSettings.COUCH_DB_ATTACHMENTS);
        } catch (Exception e) {
            throwables.add(e);
            return false;
        }
    }

    private boolean isThriftReachable() {
        final HealthService.Iface healthClient = new ThriftClients().makeHealthClient();
        try {
            final org.eclipse.sw360.datahandler.thrift.health.Health health = healthClient.getHealth();
            if (health.getStatus().equals(Status.UP)) {
                return true;
            } else {
                throwables.add(
                        new Exception(health.getStatus().toString(),
                                new Throwable(health.getDetails().toString())));
                return false;
            }
        } catch (TException e) {
            throwables.add(e);
            return false;
        }
    }

    class RestState {
        @JsonInclude
        public boolean isDbReachable;
        @JsonInclude
        public boolean isThriftReachable;

        boolean isUp() {
            return isDbReachable && isThriftReachable;
        }
    }
}
