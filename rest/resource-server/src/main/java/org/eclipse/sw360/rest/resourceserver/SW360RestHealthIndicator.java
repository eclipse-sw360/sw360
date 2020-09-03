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
    @Override
    public Health health() {
        List<Exception> exceptions = new ArrayList<>();
        RestState restState = check(exceptions);
        final String rest_state_detail = "Rest State";
        if (!restState.isUp()) {
            Health.Builder builderWithDetails = Health.down()
                    .withDetail(rest_state_detail, restState);
            for (Exception exception : exceptions) {
                builderWithDetails = builderWithDetails.withException(exception);
            }
            return builderWithDetails
                    .build();
        }
        return Health.up()
                .withDetail(rest_state_detail, restState)
                .build();
    }

    private RestState check(List<Exception> exception) {
        RestState restState = new RestState();
        try {
            restState.isDbReachable = isDbReachable(exception);
        } catch (MalformedURLException e) {
            restState.isDbReachable = false;
            exception.add(e);
        }
        restState.isThriftReachable = isThriftReachable(exception);
        return restState;
    }

    private boolean isDbReachable(List<Exception> exception) throws MalformedURLException {
        DatabaseInstance databaseInstance = makeDatabaseInstance();
        try {
            return databaseInstance.checkIfDbExists(DatabaseSettings.COUCH_DB_ATTACHMENTS);
        } catch (Exception e) {
            exception.add(e);
            return false;
        }
    }

    private boolean isThriftReachable(List<Exception> exception) {
        HealthService.Iface healthClient = makeHealthClient();
        try {
            final org.eclipse.sw360.datahandler.thrift.health.Health health = healthClient.getHealth();
            if (health.getStatus().equals(Status.UP)) {
                return true;
            } else {
                exception.add(
                        new Exception(health.getStatus().toString(),
                                new Throwable(health.getDetails().toString())));
                return false;
            }
        } catch (TException e) {
            exception.add(e);
            return false;
        }
    }

    protected HealthService.Iface makeHealthClient() {
        return new ThriftClients().makeHealthClient();
    }

    protected DatabaseInstance makeDatabaseInstance() throws MalformedURLException {
        return new DatabaseInstance(DatabaseSettings.getConfiguredHttpClient().get());
    }

    public static class RestState {
        @JsonInclude
        public boolean isDbReachable;

        @JsonInclude
        public boolean isThriftReachable;
        boolean isUp() {
            return isDbReachable && isThriftReachable;
        }
    }
}
