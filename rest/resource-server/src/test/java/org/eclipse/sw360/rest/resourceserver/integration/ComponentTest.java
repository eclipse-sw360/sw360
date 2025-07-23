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
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportService;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyBoolean;

@RunWith(SpringRunner.class)
public class ComponentTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private Sw360ComponentService componentServiceMock;

    @MockitoBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockitoBean
    private SW360ReportService sw360ReportServiceMock;

    private Component component;
    private final String componentId = "123456789";

    private MockMvc mockMvc;

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
        component.setComponentType(org.eclipse.sw360.datahandler.thrift.components.ComponentType.OSS);
        componentList.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(componentList.size()),
                componentList
        );

        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .getRecentComponentsSummaryWithPagination(any(), any());

        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .searchComponentByExactValues(any(), any(), any());

        User user = TestHelper.getTestUser();

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);

        Mockito.doReturn(RequestStatus.SUCCESS).when(componentServiceMock)
                .splitComponents(any(), any(), any());

        Mockito.doReturn(ByteBuffer.allocate(10000)).when(sw360ReportServiceMock)
                .getComponentBuffer(any(), anyBoolean());
    }

    @Test
    public void should_get_all_components() throws Exception {
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
    public void should_download_attachment_form_component() throws Exception {
        String componentId  = "abc";
        String attachmentId = "def";

        AttachmentContent attachmentContent = TestHelper.getDummyAttachmentContent();

        Mockito.doReturn(component).when(this.componentServiceMock)
                .getComponentForUserById(eq(componentId), any());
        given(this.attachmentServiceMock.getAttachmentContent(attachmentId))
                .willReturn(attachmentContent);

        InputStream mockInputStream = mock(InputStream.class);
        given(this.attachmentServiceMock.getStreamToAttachments(any(), any(), any()))
                .willReturn(mockInputStream);

        doCallRealMethod().when(attachmentServiceMock)
                .downloadAttachmentWithContext(any(), any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.add("Accept", "application/octet-stream");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/components/" + componentId + "/attachments/" + attachmentId,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename=\"dummy.txt\"", response.getHeaders().get("Content-Disposition").get(0));
    }

    @Test
    public void should_add_attachment_to_component() throws Exception{
        String componentId = "abc";

        Mockito.doReturn(component).when(componentServiceMock)
                .getComponentForUserById(eq(componentId), any());
        given(attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(TestHelper.getDummyAttachmentsListForTest().getFirst())
        ;
        Mockito.doReturn(RequestStatus.SUCCESS).when(componentServiceMock)
                .updateComponent(any(), any());
        Resource fileResource = new ByteArrayResource("Dummy file content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };

        Attachment attachment = TestHelper.getDummyAttachmentsListForTest().getFirst();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("attachment", attachment);
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/components/" + componentId + "/attachments" ,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        System.out.println("Response is" + response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @Test
    public void should_get_all_components_empty_list() throws IOException, TException {
        Mockito.doReturn(Collections.singletonMap(
                        new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(0),
                        new ArrayList<>()
                ))
                .when(componentServiceMock)
                .getRecentComponentsSummaryWithPagination(any(), any());
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
        Mockito.doReturn(Collections.singletonMap(
                        new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(0),
                        new ArrayList<>()
                ))
                .when(componentServiceMock)
                .getRecentComponentsSummaryWithPagination(any(), any());
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

        Mockito.doReturn(RequestStatus.SUCCESS).when(this.componentServiceMock)
                .updateComponent(any(), any());
        Mockito.doReturn(component).when(this.componentServiceMock)
                .getComponentForUserById(eq(componentId), any());
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
        Mockito.doThrow(TException.class).when(this.componentServiceMock)
                .getComponentForUserById(any(), any());
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
        Mockito.doReturn(RequestStatus.SUCCESS).when(this.componentServiceMock)
                .deleteComponent(eq(componentId), any());
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
        Mockito.doReturn(RequestStatus.FAILURE).when(this.componentServiceMock)
                .deleteComponent(any(), any());
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
        Mockito.doReturn(component).when(componentServiceMock)
                .getComponentForUserById(eq(componentId), eq(TestHelper.getTestUser()));
        Mockito.doAnswer(invocationOnMock -> {
            refUpdatedComponent.set(new Component((Component) invocationOnMock.getArguments()[0]));
            return RequestStatus.SUCCESS;
        }).when(componentServiceMock).updateComponent(any(), eq(TestHelper.getTestUser()));
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
        assertEquals(1, attachmentFileNames.size());
        assertTrue(attachmentFileNames.contains(attachments.get(0).getFilename()));

        Component updatedComponent = refUpdatedComponent.get();
        assertNotNull(updatedComponent);
        assertEquals(1, updatedComponent.getAttachments().size());
        assertTrue(updatedComponent.getAttachments().contains(attachments.get(0)));
    }

    @Test
    public void should_delete_attachments_with_failure_handling() throws TException, IOException {
        String attachmentId = TestHelper.getDummyAttachmentInfoListForTest().get(0).getAttachment()
                .getAttachmentContentId();
        Set<Attachment> attachments = new HashSet<>(TestHelper.getDummyAttachmentsListForTest());
        component.setAttachments(attachments);
        Mockito.doReturn(component).when(componentServiceMock)
                .getComponentForUserById(eq(componentId), eq(TestHelper.getTestUser()));
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.releaseId(componentId),
                component.getAttachments(), Collections.singletonList(attachmentId)))
                .willReturn(Collections.emptySet());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" +
                                componentId + "/attachments/" + attachmentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Mockito.verify(componentServiceMock, Mockito.never())
                .updateComponent(any(), any());
    }

    @Test
    public void should_merge_component_with_failure_handling() throws IOException {
        Component mergeSelection = new Component();
        mergeSelection.setCategories(Set.of("category1", "category2"));
        mergeSelection.setName("Component name");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/mergecomponents?" +
                                "mergeTargetId={targetId}&mergeSourceId={sourceId}",
                        HttpMethod.PATCH,
                        new HttpEntity<>(mergeSelection, getHeaders(port)),
                        String.class,
                        "targetId", "sourceId"
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_create_component() throws Exception {
        Map<String, String> newComponent = new HashMap<>();
        newComponent.put("name", "Component name");
        newComponent.put("description", "Component description");
        newComponent.put("componentType", "OSS");
        newComponent.put("homepage", "https://spring.io");

        Mockito.doReturn(component).when(componentServiceMock)
                .createComponent(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components",
                        HttpMethod.POST,
                        new HttpEntity<>(newComponent, headers),
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        assertEquals(component.getName(), responseNode.get("name").textValue());
        assertEquals(component.getId(), responseNode.get("id").textValue());
        assertEquals(component.getCreatedBy(), responseNode.get("_embedded").get("createdBy").get("email").textValue());
    }

    @Test
    public void should_get_component_by_id() throws Exception {
        component.setHomepage("https://angular.io");

        Mockito.doReturn(component).when(componentServiceMock)
                .getComponentForUserById(eq(componentId), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/" + componentId,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        assertEquals(component.getName(), responseNode.get("name").textValue());
        assertEquals(componentId, responseNode.get("id").textValue());
        assertEquals(component.getDescription(),
                responseNode.get("description").textValue());
        assertEquals(component.getHomepage(), responseNode.get("homepage").textValue());
    }

    @Test
    public void should_get_components_by_type() throws Exception {
        String componentType = "OSS";
        List<Component> filteredComponents = new ArrayList<>();
        filteredComponents.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(filteredComponents.size()),
                filteredComponents
        );

        // Mock the correct method that gets called for type filtering (searchComponentByExactValues)
        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .searchComponentByExactValues(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?type=" + componentType + "&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
        assertEquals(component.getComponentType().toString(), components.get(0).get("componentType").textValue());
    }

    @Test
    public void should_get_components_by_name() throws Exception {
        List<Component> filteredComponents = new ArrayList<>();
        filteredComponents.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(filteredComponents.size()),
                filteredComponents
        );

        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .searchComponentByExactValues(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?name=" + component.getName() + "&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
        assertEquals(component.getComponentType().toString(), components.get(0).get("componentType").textValue());
    }

    @Test
    public void should_get_components_by_lucene_search() throws Exception {
        List<Component> searchResults = new ArrayList<>();
        searchResults.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(searchResults.size()),
                searchResults
        );

        // Mock the refineSearch method that gets called for lucene search
        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .refineSearch(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?name=" + component.getName() + "&luceneSearch=true&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
    }

    @Test
    public void should_get_components_by_type_and_created_on() throws Exception {
        String componentType = "OSS";
        String createdOn = "2016-12-15";
        String categories = "javascript,sql";

        List<Component> filteredComponents = new ArrayList<>();
        component.setCreatedOn(createdOn);
        component.setCategories(Set.of("javascript", "sql"));
        filteredComponents.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(filteredComponents.size()),
                filteredComponents
        );

        // Mock the searchComponentByExactValues method for multiple filter criteria
        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .searchComponentByExactValues(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?componentType=" + componentType + "&createdOn=" + createdOn + "&categories=" + categories + "&luceneSearch=false&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
    }

    @Test
    public void should_get_components_by_external_ids() throws Exception {
        Set<Component> componentsWithExternalIds = new HashSet<>();
        Component component1 = new Component();
        component1.setId("component-1");
        component1.setName("Component 1");
        component1.setComponentType(org.eclipse.sw360.datahandler.thrift.components.ComponentType.OSS);
        Map<String, String> externalIds1 = new HashMap<>();
        externalIds1.put("component-id-key", "1831A3");
        component1.setExternalIds(externalIds1);
        componentsWithExternalIds.add(component1);

        Component component2 = new Component();
        component2.setId("component-2");
        component2.setName("Component 2");
        component2.setComponentType(org.eclipse.sw360.datahandler.thrift.components.ComponentType.OSS);
        Map<String, String> externalIds2 = new HashMap<>();
        externalIds2.put("component-id-key", "c77321");
        component2.setExternalIds(externalIds2);
        componentsWithExternalIds.add(component2);

        Mockito.doReturn(componentsWithExternalIds).when(componentServiceMock)
                .searchByExternalIds(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/searchByExternalIds?component-id-key=1831A3&component-id-key=c77321",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(2, components.size());
    }

    @Test
    public void should_get_components_with_all_details() throws Exception {
        List<Component> detailedComponents = new ArrayList<>();
        component.setCreatedOn("2016-12-15");
        component.setComponentOwner("John Doe");
        component.setOwnerAccountingUnit("4822");
        component.setOwnerGroup("AA BB 123 GHV2-DE");
        component.setOwnerCountry("DE");
        detailedComponents.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(10).setDisplayStart(0).setTotalRowCount(detailedComponents.size()),
                detailedComponents
        );

        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .getRecentComponentsSummaryWithPagination(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components?allDetails=true&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
        // Assert fields that are only returned with allDetails=true (not in convertToEmbeddedComponent)
        assertEquals(component.getOwnerAccountingUnit(), components.get(0).get("ownerAccountingUnit").textValue());
        assertEquals(component.getOwnerGroup(), components.get(0).get("ownerGroup").textValue());
        assertEquals(component.getOwnerCountry(), components.get(0).get("ownerCountry").textValue());
    }

    @Test
    public void should_get_components_no_paging_params() throws Exception {
        List<Component> componentList = new ArrayList<>();
        componentList.add(component);

        Map<PaginationData, List<Component>> paginationMap = Collections.singletonMap(
                new PaginationData().setRowsPerPage(20).setDisplayStart(0).setTotalRowCount(componentList.size()),
                componentList
        );

        Mockito.doReturn(paginationMap).when(componentServiceMock)
                .getRecentComponentsSummaryWithPagination(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());

        // Verify pagination info is present
        JsonNode page = responseNode.get("page");
        assertNotNull(page);
        assertTrue(page.has("size"));
        assertTrue(page.has("totalElements"));
        assertTrue(page.has("totalPages"));
        assertTrue(page.has("number"));
    }

    @Test
    public void should_get_my_components() throws Exception {
        List<Component> myComponents = new ArrayList<>();
        myComponents.add(component);

        Mockito.doReturn(myComponents).when(componentServiceMock)
                .getMyComponentsForUser(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/mycomponents",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
    }

    @Test
    public void should_get_my_subscriptions_components() throws Exception {
        List<Component> subscribedComponents = new ArrayList<>();
        component.setSubscribers(Set.of("admin@sw360.org"));
        subscribedComponents.add(component);

        Mockito.doReturn(subscribedComponents).when(componentServiceMock)
                .getComponentSubscriptions(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/mySubscriptions",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
    }

    @Test
    public void should_get_recent_components() throws Exception {
        List<Component> recentComponents = new ArrayList<>();
        recentComponents.add(component);

        Mockito.doReturn(recentComponents).when(componentServiceMock)
                .getRecentComponents(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/recentComponents",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode components = responseNode.get("_embedded").get("sw360:components");
        assertTrue(components.isArray());
        assertEquals(1, components.size());
        assertEquals(component.getName(), components.get(0).get("name").textValue());
    }

    @Test
    public void should_get_releases_by_component() throws Exception {
        List<Release> releases = new ArrayList<>();
        Release release1 = new Release();
        release1.setId("release-1");
        release1.setName("Angular");
        release1.setVersion("2.3.0");
        release1.setMainlineState(MainlineState.OPEN);
        release1.setClearingState(ClearingState.APPROVED);
        releases.add(release1);

        Release release2 = new Release();
        release2.setId("release-2");
        release2.setName("Angular");
        release2.setVersion("2.4.0");
        release2.setMainlineState(MainlineState.MAINLINE);
        release2.setClearingState(ClearingState.UNDER_CLEARING);
        releases.add(release2);

        Mockito.doReturn(releases).when(componentServiceMock)
                .getReleasesByComponentId(eq("component-1"), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/component-1/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode releaseLinks = responseNode.get("_embedded").get("sw360:releaseLinks");
        assertTrue(releaseLinks.isArray());
        assertEquals(2, releaseLinks.size());
        assertEquals("Angular", releaseLinks.get(0).get("name").textValue());
        assertEquals("2.3.0", releaseLinks.get(0).get("version").textValue());
        assertEquals("OPEN", releaseLinks.get(0).get("mainlineState").textValue());
        assertEquals("APPROVED", releaseLinks.get(0).get("clearingState").textValue());
    }

    @Test
    public void should_get_component_vulnerabilities() throws Exception {
        List<VulnerabilityDTO> vulnerabilities = new ArrayList<>();
        VulnerabilityDTO vulnerability1 = new VulnerabilityDTO();
        vulnerability1.setExternalId("CVE-2023-1234");
        vulnerability1.setTitle("Test Vulnerability");
        vulnerability1.setDescription("Test vulnerability description");
        vulnerabilities.add(vulnerability1);

        VulnerabilityDTO vulnerability2 = new VulnerabilityDTO();
        vulnerability2.setExternalId("CVE-2023-5678");
        vulnerability2.setTitle("Another Test Vulnerability");
        vulnerability2.setDescription("Another test vulnerability description");
        vulnerabilities.add(vulnerability2);

        Mockito.doReturn(vulnerabilities).when(componentServiceMock)
                .getVulnerabilitiesByComponent(eq("component-1"), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/component-1/vulnerabilities",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        JsonNode vulnerabilitiesNode = responseNode.get("_embedded").get("sw360:vulnerabilityDTOes");
        assertTrue(vulnerabilitiesNode.isArray());
        assertEquals(2, vulnerabilitiesNode.size());
        assertEquals("CVE-2023-1234", vulnerabilitiesNode.get(0).get("externalId").textValue());
        assertEquals("Test Vulnerability", vulnerabilitiesNode.get(0).get("title").textValue());
    }

    @Test
    public void should_split_components() throws Exception {
        Component srcComponent = new Component();
        srcComponent.setId("17653524");
        srcComponent.setName("Angular");
        srcComponent.setComponentOwner("John");
        srcComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");

        Component targetComponent = new Component();
        targetComponent.setId("87654321");
        targetComponent.setName("Angular");
        targetComponent.setComponentOwner("John");
        targetComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");

        Map<String, Object> componentsMap = new HashMap<>();
        componentsMap.put("srcComponent", srcComponent);
        componentsMap.put("targetComponent", targetComponent);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/components/splitComponents",
                        HttpMethod.PATCH,
                        new HttpEntity<>(componentsMap, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void should_get_component_report() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/reports?withlinkedreleases=true&module=components&excludeReleaseVersion=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
