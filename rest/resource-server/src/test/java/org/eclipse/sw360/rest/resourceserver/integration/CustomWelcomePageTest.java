/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

public class CustomWelcomePageTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @Test
    public void should_return_no_content_when_custom_welcome_page_disabled() throws TException {
        given(sw360ConfigurationsServiceMock.getSW360ConfigFromDb(ConfigFor.SW360_CONFIGURATION))
                .willReturn(Map.of(SW360ConfigKeys.CUSTOM_WELCOME_PAGE, "false"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.GET,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    public void should_return_no_content_when_custom_welcome_page_flag_absent() throws TException {
        given(sw360ConfigurationsServiceMock.getSW360ConfigFromDb(ConfigFor.SW360_CONFIGURATION))
                .willReturn(Map.of());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.GET,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    public void should_require_authentication_to_read_configurations() {
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.GET,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void should_require_authentication_for_post_to_custom_welcome_page() {
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.POST,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void should_propagate_configuration_service_failure() throws TException {
        given(sw360ConfigurationsServiceMock.getSW360ConfigFromDb(ConfigFor.SW360_CONFIGURATION))
                .willThrow(new TException("Configuration service unavailable"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.GET,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }
}
