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
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.services.health.HealthResponse;
import org.eclipse.sw360.datahandler.services.health.HealthStatus;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class SW360RestHealthIndicator implements HealthIndicator {

    private final String HEALTH_URI = "/health/api/health";
    private final RestClient restClient;

    public SW360RestHealthIndicator(RestClient restClient){
        this.restClient = restClient;
    }

    @Override
    public Health health() {
        List<Exception> exceptions = new ArrayList<>();
        RestState restState = check(exceptions);
        final String rest_state_detail = "Rest State";
        final String thrift_pool_detail = "Thrift Connection Pool";
        if (!restState.isUp()) {
            Health.Builder builderWithDetails = Health.down()
                    .withDetail(rest_state_detail, restState)
                    .withDetail(thrift_pool_detail, ThriftClients.getThriftConnectionPoolStats());
            for (Exception exception : exceptions) {
                builderWithDetails = builderWithDetails.withException(exception);
            }
            return builderWithDetails
                    .build();
        }
        return Health.up()
                .withDetail(rest_state_detail, restState)
                .withDetail(thrift_pool_detail, ThriftClients.getThriftConnectionPoolStats())
                .build();
    }

    private RestState check(List<Exception> exception) {
        RestState restState = new RestState();
        restState.isDbReachable = isDbReachable(exception);
        restState.isHealthServiceReachable = isHealthServiceReachable(exception);
        return restState;
    }

    private boolean isDbReachable(List<Exception> exception) {
        DatabaseInstanceCloudant databaseInstance = makeDatabaseInstance();
        try {
            return databaseInstance.checkIfDbExists(DatabaseSettings.COUCH_DB_ATTACHMENTS);
        } catch (Exception e) {
            exception.add(e);
            return false;
        }
    }

    private boolean isHealthServiceReachable(List<Exception> exception) {
        try {
            HealthResponse health = restClient.get().uri(HEALTH_URI).retrieve().body(HealthResponse.class);
            if (health == null) {
                exception.add(new Exception("Health service is not reachable"));
                return false;
            }

            if( health.getStatus().equals(HealthStatus.UP)) {
                return true;
            } else {
                String details = health.getDetails() != null ? health.getDetails().toString() : "No details available";
                String message = health.getStatus().toString() + " - " + details;
                exception.add(new Exception(message));
                return false;
            }
        } catch (Exception e) {
            exception.add(e);
            return false;
        }
    }

    protected DatabaseInstanceCloudant makeDatabaseInstance() {
        return new DatabaseInstanceCloudant(DatabaseSettings.getConfiguredClient());
    }

    public static class RestState {

        @JsonInclude
        public boolean isDbReachable;

        @JsonInclude
        public boolean isHealthServiceReachable;

        boolean isUp() {
            return isDbReachable && isHealthServiceReachable;
        }
    }
}
