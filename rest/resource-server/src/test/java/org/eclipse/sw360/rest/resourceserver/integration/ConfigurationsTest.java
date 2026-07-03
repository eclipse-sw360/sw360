/*
 * Copyright Rohit Borra, 2025. Part of the SW360 GSOC Project.
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;

public class ConfigurationsTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    private Map<String, String> testConfigsFromProperties;
    private Map<String, String> testConfigsFromDb;
    private Map<String, String> allTestConfigs;

    @BeforeEach
    public void before() throws Exception, InvalidPropertiesFormatException {
        // Setup test configuration data
        testConfigsFromProperties = new HashMap<>();
        testConfigsFromProperties.put("enable.flexible.project.release.relationship", "true");
        testConfigsFromProperties.put("svm.component.id", "svm_component_id");

        testConfigsFromDb = new HashMap<>();
        testConfigsFromDb.put("spdx.document.enabled", "true");
        testConfigsFromDb.put("sw360.tool.name", "SW360");

        allTestConfigs = new HashMap<>();
        allTestConfigs.putAll(testConfigsFromProperties);
        allTestConfigs.putAll(testConfigsFromDb);

        // Setup user mock
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(UserConverter.fromThrift(user));

        // Setup configuration service mocks
        given(this.sw360ConfigurationsServiceMock.getSW360ConfigFromProperties()).willReturn(testConfigsFromProperties);
        given(this.sw360ConfigurationsServiceMock.getSW360ConfigFromDb()).willReturn(testConfigsFromDb);
        given(this.sw360ConfigurationsServiceMock.getSW360Configs()).willReturn(allTestConfigs);
        given(this.sw360ConfigurationsServiceMock.updateSW360Configs(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360ConfigurationsServiceMock.updateSW360ConfigForContainer(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360ConfigurationsServiceMock.getConfigForContainer(any())).willReturn(testConfigsFromDb);
        // filterAdminOnlyKeys returns configs as-is for ADMIN users (test user is ADMIN)
        given(this.sw360ConfigurationsServiceMock.filterAdminOnlyKeys(eq(allTestConfigs), any())).willReturn(allTestConfigs);
        given(this.sw360ConfigurationsServiceMock.filterAdminOnlyKeys(eq(testConfigsFromDb), any())).willReturn(testConfigsFromDb);
        given(this.sw360ConfigurationsServiceMock.filterAdminOnlyKeys(eq(testConfigsFromProperties), any())).willReturn(testConfigsFromProperties);
    }

    // ========== GET CONFIGURATIONS TESTS ==========

    @Test
    public void should_get_all_configurations() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify response structure
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("enable.flexible.project.release.relationship"), "Response should contain flexible project configuration");
        assertTrue(responseBody.contains("svm.component.id"), "Response should contain SVM component ID");
        assertTrue(responseBody.contains("spdx.document.enabled"), "Response should contain SPDX document configuration");
        assertTrue(responseBody.contains("sw360.tool.name"), "Response should contain SW360 tool name");
    }

    @Test
    public void should_get_changeable_configurations() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations?changeable=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify only changeable configurations (from DB) are returned
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("spdx.document.enabled"), "Response should contain SPDX document configuration");
        assertTrue(responseBody.contains("sw360.tool.name"), "Response should contain SW360 tool name");
        assertFalse(responseBody.contains("enable.flexible.project.release.relationship"), "Response should not contain properties configurations");
    }

    @Test
    public void should_get_not_changeable_configurations() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations?changeable=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify only non-changeable configurations (from properties) are returned
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("enable.flexible.project.release.relationship"), "Response should contain flexible project configuration");
        assertTrue(responseBody.contains("svm.component.id"), "Response should contain SVM component ID");
        assertFalse(responseBody.contains("spdx.document.enabled"), "Response should not contain DB configurations");
    }

    // ========== UPDATE CONFIGURATIONS TESTS ==========

    @Test
    public void should_update_changeable_configurations() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create update request
        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");
        updatedConfigurations.put("sw360.tool.name", "Updated SW360");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Configurations are updated successfully"), "Response should contain success message");
    }

    @Test
    public void should_fail_update_configurations_without_admin_authority() {
        // Test without proper authorization (ADMIN authority required)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        // Without authentication headers, we get 401 UNAUTHORIZED before reaching authorization check
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Unauthorized") || responseBody.contains("unauthorized"), "Response should contain unauthorized message");
    }

    @Test
    public void should_fail_update_configurations_with_invalid_input() throws IOException {
        // Mock invalid input scenario
        given(this.sw360ConfigurationsServiceMock.updateSW360Configs(any(), any())).willReturn(RequestStatus.INVALID_INPUT);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> invalidConfigurations = new HashMap<>();
        invalidConfigurations.put("invalid.config", "invalid_value");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(invalidConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Invalid configurations") || responseBody.contains("unable to find DB container"), "Response should contain invalid input message");
    }

    @Test
    public void should_fail_update_configurations_when_in_use() throws IOException {
        // Mock in-use scenario
        given(this.sw360ConfigurationsServiceMock.updateSW360Configs(any(), any())).willReturn(RequestStatus.IN_USE);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("being updated by another administrator"), "Response should contain in-use message");
    }

    // ========== CONTAINER-SPECIFIC CONFIGURATIONS TESTS ==========

    @Test
    public void should_get_configurations_for_sw360_container() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("spdx.document.enabled"), "Response should contain container configurations");
        assertTrue(responseBody.contains("sw360.tool.name"), "Response should contain SW360 tool name");
    }

    @Test
    public void should_get_configurations_for_ui_container() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/UI_CONFIGURATION",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("spdx.document.enabled"), "Response should contain container configurations");
        assertTrue(responseBody.contains("sw360.tool.name"), "Response should contain SW360 tool name");
    }

    @Test
    public void should_update_configurations_for_sw360_container() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Configurations are updated successfully"), "Response should contain success message");
    }

    @Test
    public void should_update_with_xss_skip() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String configKey = SW360ConfigKeys.UI_CLEARING_TEAMS;
        String configValue = "[\"TEAM A\",\"TEAM B\"]";

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put(configKey, configValue);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(this.sw360ConfigurationsServiceMock)
                .updateSW360ConfigForContainer(
                        eq(ConfigFor.SW360_CONFIGURATION),
                        argThat(x -> {
                            assertTrue(x.containsKey(configKey));
                            assertEquals(configValue, x.get(configKey));
                            return true;
                        }),
                        any());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Configurations are updated successfully"), "Response should contain success message");
    }

    @Test
    public void should_update_configurations_for_ui_container() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("ui.theme", "dark");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/UI_CONFIGURATION",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Configurations are updated successfully"), "Response should contain success message");
    }

    @Test
    public void should_fail_update_container_configurations_without_admin_authority() {
        // Test without proper authorization (ADMIN authority required)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        // Without authentication headers, we get 401 UNAUTHORIZED before reaching authorization check
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Unauthorized") || responseBody.contains("unauthorized"), "Response should contain unauthorized message");
    }

    // ========== EXCEPTION COVERAGE TESTS ==========

    @Test
    public void should_handle_exception_in_get_configurations() throws IOException {
        doThrow(new RuntimeException("Test exception")).when(sw360ConfigurationsServiceMock)
                .getSW360Configs();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }

    @Test
    public void should_handle_exception_in_update_configurations() throws IOException {
        doThrow(new RuntimeException("Test exception")).when(sw360ConfigurationsServiceMock)
                .updateSW360Configs(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }

    @Test
    public void should_handle_exception_in_get_container_configurations() throws IOException {
        doThrow(new RuntimeException("Test exception")).when(sw360ConfigurationsServiceMock)
                .getConfigForContainer(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }

    @Test
    public void should_handle_exception_in_update_container_configurations() throws IOException {
        doThrow(new RuntimeException("Test exception")).when(sw360ConfigurationsServiceMock)
                .updateSW360ConfigForContainer(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("spdx.document.enabled", "false");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }

    @Test
    public void should_handle_invalid_properties_format_exception_in_update_configurations() throws Exception {
        // Mock service to throw InvalidPropertiesFormatException
        doThrow(new InvalidPropertiesFormatException("Invalid configuration format"))
                .when(sw360ConfigurationsServiceMock)
                .updateSW360Configs(any(Map.class), any(User.class));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("invalid.config", "invalid_value");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Invalid configuration format"), "Response should contain InvalidPropertiesFormatException message");
    }

    @Test
    public void should_handle_invalid_properties_format_exception_in_update_container_configurations() throws Exception {
        // Mock service to throw InvalidPropertiesFormatException
        doThrow(new InvalidPropertiesFormatException("Invalid container configuration format"))
                .when(sw360ConfigurationsServiceMock)
                .updateSW360ConfigForContainer(any(ConfigFor.class), any(Map.class), any(User.class));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> updatedConfigurations = new HashMap<>();
        updatedConfigurations.put("invalid.container.config", "invalid_value");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(updatedConfigurations, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Invalid container configuration format"), "Response should contain InvalidPropertiesFormatException message");
    }
}
