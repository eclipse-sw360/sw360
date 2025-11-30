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
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.changelog.Sw360ChangeLogService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class ChangeLogTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ChangeLogService changeLogServiceMock;

    private User adminUser;

    @Before
    public void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin@sw360.org");
        adminUser.setUserGroup(UserGroup.ADMIN);
        given(userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(adminUser);
        given(userServiceMock.getUserByEmail(anyString())).willReturn(adminUser);
    }

    @Test
    public void should_get_change_logs_for_document() throws IOException, TException {
        List<ChangeLogs> changeLogs = new ArrayList<>();

        ChangeLogs changeLog = new ChangeLogs();
        changeLog.setId("1234");
        changeLog.setDocumentId("4567");
        changeLog.setUserEdited("admin@sw360.org");
        changeLog.setChangeTimestamp("2021-01-08 10:11:12");
        changeLog.setOperation(Operation.UPDATE);
        changeLog.setDocumentType("project");

        HashSet<ChangedFields> changes = new HashSet<>();
        ChangedFields changedFields = new ChangedFields();
        changedFields.setFieldName("version");
        changedFields.setFieldValueOld("\"2\"");
        changedFields.setFieldValueNew("\"25\"");
        changes.add(changedFields);
        changeLog.setChanges(changes);

        HashSet<ReferenceDocData> referenceDoc = new HashSet<>();
        ReferenceDocData referenceDocData = new ReferenceDocData();
        referenceDocData.setRefDocId("98765");
        referenceDocData.setRefDocOperation(Operation.CREATE);
        referenceDocData.setRefDocType("attachment");
        referenceDoc.add(referenceDocData);
        changeLog.setReferenceDoc(referenceDoc);

        ChangeLogs changeLog2 = new ChangeLogs();
        changeLog2.setId("2345");
        changeLog2.setDocumentId("4567");
        changeLog2.setUserEdited("admin@sw360.org");
        changeLog2.setChangeTimestamp("2021-01-08 12:13:14");
        changeLog2.setOperation(Operation.CREATE);
        changeLog2.setDocumentType("attachment");
        Map<String, String> info = new HashMap<>();
        info.put("FILENAME", "abc.xml");
        info.put("CONTENT_TYPE", "application/rdf+xml");
        info.put("PARENT_OPERATION", "PROJECT_UPDATE");
        changeLog2.setInfo(info);

        changeLogs.add(changeLog);
        changeLogs.add(changeLog2);

        given(changeLogServiceMock.getChangeLogsByDocumentId(anyString(), any())).willReturn(changeLogs);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/changelog/document/4567",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResponse(response, "changeLogs", 2);

        String body = response.getBody();
        assertTrue(body.contains("userEdited"));
        assertTrue(body.contains("operation"));
        assertTrue(body.contains("documentType"));
        assertTrue(body.contains("changeTimestamp"));
    }

    @Test
    public void should_return_empty_change_logs_for_unknown_document() throws IOException, TException {
        given(changeLogServiceMock.getChangeLogsByDocumentId(anyString(), any())).willReturn(List.of());

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/changelog/document/unknown-doc",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body == null || body.trim().isEmpty());
    }

    @Test
    public void should_handle_service_exception() throws IOException, TException {
        given(changeLogServiceMock.getChangeLogsByDocumentId(anyString(), any())).willThrow(new TException("thrift error"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/changelog/document/4567",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        JsonNode json = new ObjectMapper().readTree(response.getBody());
        assertTrue(json.has("status"));
        assertTrue(json.has("error"));
    }

    @Test
    public void should_handle_invalid_change_field_values_gracefully() throws IOException, TException {
        List<ChangeLogs> changeLogs = new ArrayList<>();

        ChangeLogs changeLog = new ChangeLogs();
        changeLog.setId("7890");
        changeLog.setDocumentId("doc-invalid");
        changeLog.setUserEdited("admin@sw360.org");
        changeLog.setChangeTimestamp("2021-01-09 01:02:03");
        changeLog.setOperation(Operation.UPDATE);
        changeLog.setDocumentType("project");

        // Craft a ChangedFields entry with an invalid JSON string to trigger JsonProcessingException
        HashSet<ChangedFields> changes = new HashSet<>();
        ChangedFields badChange = new ChangedFields();
        badChange.setFieldName("badField");
        badChange.setFieldValueOld("invalid-json"); // not a valid JSON literal
        badChange.setFieldValueNew(null);
        changes.add(badChange);
        changeLog.setChanges(changes);

        changeLogs.add(changeLog);
        given(changeLogServiceMock.getChangeLogsByDocumentId(anyString(), any())).willReturn(changeLogs);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/changelog/document/doc-invalid",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        // Even with parsing errors inside the controller, it should return 200 and a valid HAL body
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("_embedded"));
        assertTrue(body.contains("sw360:changeLogs"));
    }

    @Test
    public void should_expose_change_logs_link_in_root() throws IOException {
        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("changeLogs"));
    }
}
