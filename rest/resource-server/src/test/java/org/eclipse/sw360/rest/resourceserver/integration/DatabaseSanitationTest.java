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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.databasesanitation.Sw360DatabaseSanitationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class DatabaseSanitationTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360DatabaseSanitationService sanitationServiceMock;

    private User adminUser;

    @Before
    public void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin@sw360.org");
        adminUser.setUserGroup(UserGroup.ADMIN);
        given(userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(adminUser);
        given(userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(adminUser);
    }

    @Test
    public void should_search_duplicate_identifiers_success() throws IOException, TException {
        Map<String, Map<String, List<String>>> responseMap = new HashMap<>();
        responseMap.put("duplicateComponents", Map.of("compA", List.of("id1", "id2")));
        given(sanitationServiceMock.duplicateIdentifiers(any())).willReturn(responseMap);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/databaseSanitation/searchDuplicate",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body.contains("duplicateComponents"));
        assertTrue(body.contains("id1"));
    }

    @Test
    public void should_return_no_content_when_no_duplicates() throws IOException, TException {
        Map<String, Map<String, List<String>>> responseMap = new HashMap<>();
        given(sanitationServiceMock.duplicateIdentifiers(any())).willReturn(responseMap);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/databaseSanitation/searchDuplicate",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        // Controller returns empty map body with 204; accept either truly empty or '{}'
        String body = response.getBody();
        assertTrue(body == null || body.trim().isEmpty() || body.trim().equals("{}"));
    }

    @Test
    public void should_handle_texception_from_service() throws IOException, TException {
        given(sanitationServiceMock.duplicateIdentifiers(any())).willThrow(new TException("thrift error"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/databaseSanitation/searchDuplicate",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        JsonNode json = new ObjectMapper().readTree(response.getBody());
        assertTrue(json.has("status"));
        assertTrue(json.has("error"));
    }

    @Test
    public void should_handle_access_denied_from_service() throws IOException, TException {
        given(sanitationServiceMock.duplicateIdentifiers(any())).willThrow(new AccessDeniedException("no admin"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/databaseSanitation/searchDuplicate",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void should_expose_database_sanitation_link_in_root() throws IOException {
        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("databaseSanitation"));
    }
}
