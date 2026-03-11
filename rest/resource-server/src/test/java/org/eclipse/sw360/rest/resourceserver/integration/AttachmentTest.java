/*
 * Copyright Siemens AG, 2017,2019. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class AttachmentTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;

    private final String shaInvalid = "56789";
    private final String attachmentId = "test-attachment-123";

    @Before
    public void before() throws TException {

        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(TestHelper.attachmentShaUsedMultipleTimes))).willReturn(TestHelper.getDummyAttachmentInfoListForTest());
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(shaInvalid))).willReturn(new ArrayList<>());

        // Setup for getAttachmentById test - use existing TestHelper data
        given(this.attachmentServiceMock.getAttachmentById(eq(attachmentId))).willReturn(TestHelper.getDummyAttachmentInfoListForTest().get(0));

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        user.setUserGroup(UserGroup.ADMIN);

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.release1Id), eq(user))).willReturn(TestHelper.getDummyReleaseListForTest().get(0));
        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.releaseId2), eq(user))).willReturn(TestHelper.getDummyReleaseListForTest().get(1));
    }

    @Test
    public void should_get_multiple_attachments_by_sha1() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments?sha1=" + TestHelper.attachmentShaUsedMultipleTimes,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "attachments", 2);
    }

    @Test
    public void should_get_empty_attachment_collection_by_sha1() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments?sha1=" + shaInvalid,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_get_attachment_by_id() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments/" + attachmentId,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the response contains attachment data at root level
        // The response structure has attachment fields directly, not nested under "attachment"
        String responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        assertTrue("Response should contain attachmentContentId", responseBody.contains("attachmentContentId"));
        assertTrue("Response should contain filename", responseBody.contains("filename"));
        assertTrue("Response should contain _links", responseBody.contains("_links"));
        assertTrue("Response should contain _embedded", responseBody.contains("_embedded"));
    }

    @Test
    public void should_create_attachment() throws IOException, TException {
        // Mock the attachment service to return a created attachment
        given(this.attachmentServiceMock.addAttachment(any(), any())).willReturn(TestHelper.getDummyAttachmentsListForTest().getFirst());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create multipart form data with file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", new ByteArrayResource("test file content".getBytes()) {
            @Override
            public String getFilename() {
                return "test-file.txt";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the response contains attachment data
        String responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        assertTrue("Response should contain attachments", responseBody.contains("attachments"));
        assertTrue("Response should contain attachmentContentId", responseBody.contains("attachmentContentId"));
        assertTrue("Response should contain filename", responseBody.contains("filename"));
    }

    @Test
    public void should_fail_create_attachment_without_files() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create multipart form data without files
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
