/*
 * Copyright Rohit Borra, 2025. Part of the SW360 GSOC Project.
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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.configuration.SW360ConfigurationsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class ConfigurationsTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private SW360ConfigurationsService sw360ConfigurationsService;

    private Map<String, String> testConfigsFromProperties;
    private Map<String, String> testConfigsFromDb;
    private Map<String, String> allTestConfigs;

    @Before
    public void before() throws TException, InvalidPropertiesFormatException {
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
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        // Setup configuration service mocks
        given(this.sw360ConfigurationsService.getSW360ConfigFromProperties()).willReturn(testConfigsFromProperties);
        given(this.sw360ConfigurationsService.getSW360ConfigFromDb()).willReturn(testConfigsFromDb);
        given(this.sw360ConfigurationsService.getSW360Configs()).willReturn(allTestConfigs);
        given(this.sw360ConfigurationsService.updateSW360Configs(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360ConfigurationsService.updateSW360ConfigForContainer(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360ConfigurationsService.getConfigForContainer(any())).willReturn(testConfigsFromDb);
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
        assertTrue("Response should contain flexible project configuration",
                responseBody.contains("enable.flexible.project.release.relationship"));
        assertTrue("Response should contain SVM component ID",
                responseBody.contains("svm.component.id"));
        assertTrue("Response should contain SPDX document configuration",
                responseBody.contains("spdx.document.enabled"));
        assertTrue("Response should contain SW360 tool name",
                responseBody.contains("sw360.tool.name"));
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
        assertTrue("Response should contain SPDX document configuration",
                responseBody.contains("spdx.document.enabled"));
        assertTrue("Response should contain SW360 tool name",
                responseBody.contains("sw360.tool.name"));
        assertFalse("Response should not contain properties configurations",
                responseBody.contains("enable.flexible.project.release.relationship"));
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
        assertTrue("Response should contain flexible project configuration",
                responseBody.contains("enable.flexible.project.release.relationship"));
        assertTrue("Response should contain SVM component ID",
                responseBody.contains("svm.component.id"));
        assertFalse("Response should not contain DB configurations",
                responseBody.contains("spdx.document.enabled"));
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
        assertTrue("Response should contain success message",
                responseBody.contains("Configurations are updated successfully"));
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
        assertTrue("Response should contain unauthorized message",
                responseBody.contains("Unauthorized") || responseBody.contains("unauthorized"));
    }

    @Test
    public void should_fail_update_configurations_with_invalid_input() throws IOException, TException {
        // Mock invalid input scenario
        given(this.sw360ConfigurationsService.updateSW360Configs(any(), any())).willReturn(RequestStatus.INVALID_INPUT);

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
        assertTrue("Response should contain invalid input message",
                responseBody.contains("Invalid configurations") || responseBody.contains("unable to find DB container"));
    }

    @Test
    public void should_fail_update_configurations_when_in_use() throws IOException, TException {
        // Mock in-use scenario
        given(this.sw360ConfigurationsService.updateSW360Configs(any(), any())).willReturn(RequestStatus.IN_USE);

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
        assertTrue("Response should contain in-use message",
                responseBody.contains("being updated by another administrator"));
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
        assertTrue("Response should contain container configurations",
                responseBody.contains("spdx.document.enabled"));
        assertTrue("Response should contain SW360 tool name",
                responseBody.contains("sw360.tool.name"));
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
        assertTrue("Response should contain container configurations",
                responseBody.contains("spdx.document.enabled"));
        assertTrue("Response should contain SW360 tool name",
                responseBody.contains("sw360.tool.name"));
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
        assertTrue("Response should contain success message",
                responseBody.contains("Configurations are updated successfully"));
    }

    @Test
    public void should_update_with_xss_skip() throws IOException, TException {
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

        verify(this.sw360ConfigurationsService)
                .updateSW360ConfigForContainer(
                        eq(ConfigFor.SW360_CONFIGURATION),
                        argThat(x -> {
                            assertTrue(x.containsKey(configKey));
                            assertEquals(configValue, x.get(configKey));
                            return true;
                        }),
                        any());

        String responseBody = response.getBody();
        assertTrue("Response should contain success message",
                responseBody.contains("Configurations are updated successfully"));
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
        assertTrue("Response should contain success message",
                responseBody.contains("Configurations are updated successfully"));
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
        assertTrue("Response should contain unauthorized message",
                responseBody.contains("Unauthorized") || responseBody.contains("unauthorized"));
    }

    // ========== EXCEPTION COVERAGE TESTS ==========

    @Test
    public void should_handle_exception_in_get_configurations() throws IOException, TException {
        // Mock TException in getSW360Configs
        doThrow(new TException("Test TException")).when(sw360ConfigurationsService)
                .getSW360Configs();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }

    @Test
    public void should_handle_exception_in_update_configurations() throws IOException, TException {
        // Mock TException in updateSW360Configs
        doThrow(new TException("Test TException")).when(sw360ConfigurationsService)
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

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }

    @Test
    public void should_handle_exception_in_get_container_configurations() throws IOException, TException {
        // Mock TException in getConfigForContainer
        doThrow(new TException("Test TException")).when(sw360ConfigurationsService)
                .getConfigForContainer(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/configurations/container/SW360_CONFIGURATION",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }

    @Test
    public void should_handle_exception_in_update_container_configurations() throws IOException, TException {
        // Mock TException in updateSW360ConfigForContainer
        doThrow(new TException("Test TException")).when(sw360ConfigurationsService)
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

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }

    @Test
    public void should_handle_invalid_properties_format_exception_in_update_configurations() throws Exception {
        // Mock service to throw InvalidPropertiesFormatException
        doThrow(new InvalidPropertiesFormatException("Invalid configuration format"))
                .when(sw360ConfigurationsService)
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
        assertTrue("Response should contain InvalidPropertiesFormatException message",
                responseBody.contains("Invalid configuration format"));
    }

    @Test
    public void should_handle_invalid_properties_format_exception_in_update_container_configurations() throws Exception {
        // Mock service to throw InvalidPropertiesFormatException
        doThrow(new InvalidPropertiesFormatException("Invalid container configuration format"))
                .when(sw360ConfigurationsService)
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
        assertTrue("Response should contain InvalidPropertiesFormatException message",
                responseBody.contains("Invalid container configuration format"));
    }
}
