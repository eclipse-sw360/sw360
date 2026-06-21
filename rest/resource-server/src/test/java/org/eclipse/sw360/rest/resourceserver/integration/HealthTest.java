/*
 * Copyright Rohit Borra 2025.
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.eclipse.sw360.rest.resourceserver.SW360RestHealthIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

public class HealthTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    public void before() throws Exception {
        // Setup default healthy state
        SW360RestHealthIndicator.RestState restState = new SW360RestHealthIndicator.RestState();
        restState.isHealthServiceReachable = true;
        restState.isDbReachable = true;

        Health springHealth = Health.up()
                .withDetail("Rest State", restState)
                .build();
        given(this.restHealthIndicatorMock.health()).willReturn(springHealth);
    }

    @Test
    public void should_get_health_status() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/health",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the response contains health data
        String responseBody = response.getBody();
        assert responseBody != null;
        assertTrue(responseBody.contains("status"), "Response should contain status");
        assertTrue(responseBody.contains("components"), "Response should contain components");
        assertTrue(responseBody.contains("diskSpace"), "Response should contain diskSpace");
        assertTrue(responseBody.contains("ping"), "Response should contain ping");
        assertTrue(responseBody.contains("total"), "Response should contain total");
        assertTrue(responseBody.contains("free"), "Response should contain free");
        assertTrue(responseBody.contains("threshold"), "Response should contain threshold");
        assertTrue(responseBody.contains("path"), "Response should contain path");
        assertTrue(responseBody.contains("exists"), "Response should contain exists");
    }

    @Test
    public void should_get_health_status_unhealthy() throws IOException {
        // Setup unhealthy state
        SW360RestHealthIndicator.RestState restState = new SW360RestHealthIndicator.RestState();
        restState.isHealthServiceReachable = false;
        restState.isDbReachable = true;

        Health springHealthDown = Health.down()
                .withDetail("Rest State", restState)
                .withException(new Exception("Fake"))
                .build();
        given(this.restHealthIndicatorMock.health()).willReturn(springHealthDown);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/health",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Health endpoint should still return 200 even when unhealthy (Spring Boot behavior)
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the response contains health data
        String responseBody = response.getBody();
        assert responseBody != null;
        assertTrue(responseBody.contains("status"), "Response should contain status");
        assertTrue(responseBody.contains("components"), "Response should contain components");
        assertTrue(responseBody.contains("diskSpace"), "Response should contain diskSpace");
        assertTrue(responseBody.contains("ping"), "Response should contain ping");
        assertTrue(responseBody.contains("total"), "Response should contain total");
        assertTrue(responseBody.contains("free"), "Response should contain free");
        assertTrue(responseBody.contains("threshold"), "Response should contain threshold");
        assertTrue(responseBody.contains("path"), "Response should contain path");
        assertTrue(responseBody.contains("exists"), "Response should contain exists");
    }

    @Test
    public void should_get_health_status_without_authentication() throws IOException {
        // Health endpoint should be accessible without authentication
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/health",
                        HttpMethod.GET,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the response contains health data
        String responseBody = response.getBody();
        assert responseBody != null;
        assertTrue(responseBody.contains("status"), "Response should contain status");
        assertTrue(responseBody.contains("components"), "Response should contain components");
    }
}
