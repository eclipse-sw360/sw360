/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 * Copyright Bosch Software Innovations GmbH, 2018.
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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringJUnit4ClassRunner.class)
public class ComponentTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ComponentService componentServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

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

        given(this.componentServiceMock.getComponentsForUser(any())).willReturn(componentList);

        User user = TestHelper.getTestUser();

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
        given(this.componentServiceMock.getComponentsForUser(any())).willReturn(new ArrayList<>());
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
        when(this.componentServiceMock.getComponentsForUser(any())).thenThrow(ResourceNotFoundException.class);
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
        String body = "{\n" +
                "  \"name\": \"updatedComponentName\",\n" +
                "  \"invalid_property\": \"abcde123\",\n" +
                "  \"attachmentDTOs\": [\n" +
                "    {\n" +
                "        \"attachmentContentId\": \"1231231255\",\n" +
                "        \"filename\": \"spring-mvc-4.3.4.RELEASE.jar\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        given(this.componentServiceMock.updateComponent(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.componentServiceMock.getComponentForUserById(eq(componentId), any())).willReturn(component);
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        doThrow(TException.class).when(this.componentServiceMock).getComponentForUserById(any(), any());
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
        given(this.componentServiceMock.deleteComponent(eq(componentId), any())).willReturn(RequestStatus.SUCCESS);
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
        given(this.componentServiceMock.deleteComponent(any(), any())).willReturn(RequestStatus.FAILURE);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" + invalidComponentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        TestHelper.handleBatchDeleteResourcesResponse(response, invalidComponentId, 500);
    }

    @Test
    public void should_delete_attachments_successfully() throws TException, IOException {
        final AtomicReference<Component> refUpdatedComponent = new AtomicReference<>();
        List<Attachment> attachments = TestHelper.getDummyAttachmentsListForTest();
        List<String> attachmentIds = Arrays.asList(attachments.get(0).attachmentContentId, "otherAttachmentId");
        String strIds = String.join(",", attachmentIds);
        component.setAttachments(new HashSet<>(attachments));
        given(componentServiceMock.getComponentForUserById(componentId, TestHelper.getTestUser())).willReturn(component);
        given(componentServiceMock.updateComponent(any(), eq(TestHelper.getTestUser())))
                .will(invocationOnMock -> {
                    refUpdatedComponent.set(new Component((Component) invocationOnMock.getArguments()[0]));
                    return RequestStatus.SUCCESS;
                });
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.componentId(componentId),
                component.getAttachments(), attachmentIds))
                .willReturn(Collections.singleton(attachments.get(1)));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" +
                                componentId + "/attachments/" + strIds,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode jsonResponse = new ObjectMapper().readTree(response.getBody());
        JsonNode jsonAttachments = jsonResponse.get("_embedded").get("sw360:attachments");
        assertTrue(jsonAttachments.isArray());
        Set<String> attachmentFileNames = StreamSupport.stream(jsonAttachments.spliterator(), false)
                .map(node -> node.get("filename").textValue())
                .collect(Collectors.toSet());
        assertThat(attachmentFileNames, hasSize(1));
        assertThat(attachmentFileNames, hasItem(attachments.get(0).getFilename()));

        Component updatedComponent = refUpdatedComponent.get();
        assertThat(updatedComponent, is(notNullValue()));
        assertThat(updatedComponent.getAttachments(), hasSize(1));
        assertThat(updatedComponent.getAttachments(), hasItem(attachments.get(0)));
    }

    @Test
    public void should_delete_attachments_with_failure_handling() throws TException, IOException {
        String attachmentId = TestHelper.getDummyAttachmentInfoListForTest().get(0).getAttachment()
                .getAttachmentContentId();
        Set<Attachment> attachments = new HashSet<>(TestHelper.getDummyAttachmentsListForTest());
        component.setAttachments(attachments);
        given(componentServiceMock.getComponentForUserById(componentId, TestHelper.getTestUser()))
                .willReturn(component);
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.releaseId(componentId),
                component.getAttachments(), Collections.singletonList(attachmentId)))
                .willReturn(Collections.emptySet());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" +
                                componentId + "/attachments/" + attachmentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        then(componentServiceMock)
                .should(never())
                .updateComponent(any(), any());
    }

}