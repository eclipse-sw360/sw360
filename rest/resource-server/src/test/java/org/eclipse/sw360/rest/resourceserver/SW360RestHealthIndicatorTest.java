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

import org.eclipse.sw360.datahandler.couchdb.DatabaseInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Sw360ResourceServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SW360RestHealthIndicatorTest {

    @LocalServerPort
    private int port;

    @SpyBean
    private SW360RestHealthIndicator restHealthIndicatorMock;

    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * Makes a request to localhost with the default server port and returns
     * the response as a response entity with type Map
     * @param endpoint endpoint that will be called
     * @return response of request
     */
    private ResponseEntity<Map> getMapResponseEntityForHealthEndpointRequest(String endpoint) {
        return this.testRestTemplate.getForEntity(
                "http://localhost:" + this.port + endpoint, Map.class);
    }

    @Test
    public void info_should_return_200() {
        ResponseEntity<Map> entity = getMapResponseEntityForHealthEndpointRequest("/info");

        then(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void health_should_return_503_with_missing_db() {
        ResponseEntity<Map> entity = getMapResponseEntityForHealthEndpointRequest("/health");

        then(entity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        final LinkedHashMap sw360Rest = (LinkedHashMap<String, Object>) entity.getBody().get("SW360Rest");
        final LinkedHashMap restState = (LinkedHashMap<String, Object>) sw360Rest.get("Rest State");
        assertFalse((boolean) restState.get("isDbReachable"));
    }

    @Test
    public void health_should_return_503_with_throwable() {
        final DatabaseInstance databaseInstanceMock = mock(DatabaseInstance.class);
        when(databaseInstanceMock.checkIfDbExists(anyString()))
                .thenThrow(new RuntimeException());

        restHealthIndicatorMock.setDatabaseInstance(databaseInstanceMock);

        ResponseEntity<Map> entity = getMapResponseEntityForHealthEndpointRequest("/health");

        then(entity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        final LinkedHashMap sw360Rest = (LinkedHashMap<String, Object>) entity.getBody().get("SW360Rest");
        final LinkedHashMap restState = (LinkedHashMap<String, Object>) sw360Rest.get("Rest State");
        assertFalse((boolean) restState.get("isDbReachable"));
        assertTrue(sw360Rest.get("error") != null);
    }

    @Test
    public void health_should_return_200_when_healthy() {
        final DatabaseInstance databaseInstanceMock = mock(DatabaseInstance.class);
        when(databaseInstanceMock.checkIfDbExists(anyString()))
                .thenReturn(true);

        restHealthIndicatorMock.setDatabaseInstance(databaseInstanceMock);

        ResponseEntity<Map> entity = getMapResponseEntityForHealthEndpointRequest("/health");

        then(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        final LinkedHashMap sw360Rest = (LinkedHashMap<String, Object>) entity.getBody().get("SW360Rest");
        final LinkedHashMap restState = (LinkedHashMap<String, Object>) sw360Rest.get("Rest State");
        assertTrue((boolean) restState.get("isDbReachable"));
    }
}
