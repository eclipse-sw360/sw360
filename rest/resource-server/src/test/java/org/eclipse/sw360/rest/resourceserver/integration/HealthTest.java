/*
 * Copyright Rohit Borra 2025.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.rest.resourceserver.SW360RestHealthIndicator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class HealthTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private SW360RestHealthIndicator restHealthIndicatorMock;

    @Before
    public void before() throws TException {
        // Setup default healthy state
        SW360RestHealthIndicator.RestState restState = new SW360RestHealthIndicator.RestState();
        restState.isThriftReachable = true;
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
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain components", responseBody.contains("components"));
        assertTrue("Response should contain diskSpace", responseBody.contains("diskSpace"));
        assertTrue("Response should contain ping", responseBody.contains("ping"));
        assertTrue("Response should contain total", responseBody.contains("total"));
        assertTrue("Response should contain free", responseBody.contains("free"));
        assertTrue("Response should contain threshold", responseBody.contains("threshold"));
        assertTrue("Response should contain path", responseBody.contains("path"));
        assertTrue("Response should contain exists", responseBody.contains("exists"));
    }

    @Test
    public void should_get_health_status_unhealthy() throws IOException {
        // Setup unhealthy state
        SW360RestHealthIndicator.RestState restState = new SW360RestHealthIndicator.RestState();
        restState.isThriftReachable = false;
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
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain components", responseBody.contains("components"));
        assertTrue("Response should contain diskSpace", responseBody.contains("diskSpace"));
        assertTrue("Response should contain ping", responseBody.contains("ping"));
        assertTrue("Response should contain total", responseBody.contains("total"));
        assertTrue("Response should contain free", responseBody.contains("free"));
        assertTrue("Response should contain threshold", responseBody.contains("threshold"));
        assertTrue("Response should contain path", responseBody.contains("path"));
        assertTrue("Response should contain exists", responseBody.contains("exists"));
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
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain components", responseBody.contains("components"));
    }
}
