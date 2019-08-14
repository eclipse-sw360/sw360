/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 * Copyright Bosch Software Innovations GmbH, 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringJUnit4ClassRunner.class)
public class ComponentTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ComponentService componentServiceMock;

    private Component component;
    private final String componentId = "123456789";

    @Before
    public void before() throws TException {
        List<Component> componentList = new ArrayList<>();
        component = new Component();
        component.setName("Component name");
        component.setHomepage("http://example-component.com");
        component.setOwnerGroup("ownerGroup1");
        component.setDescription("Component description");
        component.setId(componentId);
        component.setCreatedBy("admin@sw360.org");
        componentList.add(component);

        given(this.componentServiceMock.getComponentsForUser(anyObject())).willReturn(componentList);

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
    }

    @Test
    public void should_get_all_components() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "components", 1);
    }

    @Test
    public void should_get_all_components_empty_list() throws IOException, TException {
        given(this.componentServiceMock.getComponentsForUser(anyObject())).willReturn(new ArrayList<>());
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "components", 0);
    }

    @Test
    public void should_get_all_components_wrong_page() throws IOException, TException {
        when(this.componentServiceMock.getComponentsForUser(anyObject())).thenThrow(ResourceNotFoundException.class);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?page=5&page_entries=10",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_get_all_components_with_field() throws IOException {
        String extraField = "ownerGroup";
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?fields=" + extraField,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "components", 1, Collections.singletonList(extraField));
    }

    @Test
    public void should_update_component_valid() throws IOException, TException {
        String updatedComponentName = "updatedComponentName";
        given(this.componentServiceMock.updateComponent(anyObject(), anyObject())).willReturn(RequestStatus.SUCCESS);
        given(this.componentServiceMock.getComponentForUserById(eq(componentId), anyObject())).willReturn(component);
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedComponentName);
        body.put("invalid_property", "abcde123");
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" + componentId,
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(response.getBody());
        assertEquals(responseBodyJsonNode.get("name").textValue(), updatedComponentName);
        assertNull(responseBodyJsonNode.get("invalid_property"));

    }

    @Test
    public void should_update_component_invalid() throws IOException, TException {
        doThrow(TException.class).when(this.componentServiceMock).getComponentForUserById(anyObject(), anyObject());
        String updatedComponentName = "updatedComponentName";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedComponentName);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/someRandomId123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_delete_component_valid() throws IOException, TException {
        given(this.componentServiceMock.deleteComponent(eq(componentId), anyObject())).willReturn(RequestStatus.SUCCESS);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" + componentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        TestHelper.handleBatchDeleteResourcesResponse(response, componentId, 200);
    }

    @Test
    public void should_delete_component_invalid() throws IOException, TException {
        String invalidComponentId = "2734982743928374";
        given(this.componentServiceMock.deleteComponent(anyObject(), anyObject())).willReturn(RequestStatus.FAILURE);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" + invalidComponentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        TestHelper.handleBatchDeleteResourcesResponse(response, invalidComponentId, 500);
    }

}