/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReleaseTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    @MockBean
    private Sw360LicenseService licenseServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    public static String attachmentShaInvalid = "999";

    @Before
    public void before() throws TException {
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(TestHelper.attachmentShaUsedMultipleTimes)))
                .willReturn(TestHelper.getDummyAttachmentInfoListForTest());

        List<AttachmentInfo> emptyAttachmentInfos = new ArrayList<>();
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(attachmentShaInvalid)))
                .willReturn(emptyAttachmentInfos);

        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.getDummyReleaseListForTest().get(0).getId()),eq(user))).willReturn(TestHelper.getDummyReleaseListForTest().get(0));
        given(this.releaseServiceMock.getReleasesForUser(anyObject())).willReturn(TestHelper.getDummyReleaseListForTest());
        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.getDummyReleaseListForTest().get(1).getId()),eq(user))).willReturn(TestHelper.getDummyReleaseListForTest().get(1));

        given(this.licenseServiceMock.getLicenseById("Apache-2.0")).willReturn(
                new License("Apache 2.0 License")
                        .setText("Dummy License Text")
                        .setShortname("Apache-2.0")
                        .setId(UUID.randomUUID().toString()));
        given(this.licenseServiceMock.getLicenseById("GPL-2.0-or-later")).willReturn(
                new License("GNU General Public License 2.0")
                        .setText("GNU General Public License 2.0 Text")
                        .setShortname("GPL-2.0-or-later")
                        .setId(UUID.randomUUID().toString()));
    }

    @Test
    public void should_update_release_valid() throws IOException, TException {
        String updatedReleaseName = "updatedReleaseName";
        given(this.releaseServiceMock.updateRelease(anyObject(), anyObject())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.release1Id), anyObject())).willReturn(TestHelper.getDummyReleaseListForTest().get(0));
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        body.put("mainLicenseIds", new String[] { "Apache-2.0" });
        body.put("wrong_prop", "abc123");
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id,
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(responseBody.get("name").textValue(), updatedReleaseName);
        JsonNode licenses = responseBody.get("_embedded").get("sw360:licenses");
        assertTrue(licenses.isArray());
        List<String> mainLicenseIds = new ArrayList<>();
        for (JsonNode license : licenses) {
            mainLicenseIds.add(license.get("fullName").textValue());
        }
        assertEquals(1, mainLicenseIds.size());
        assertEquals("Apache 2.0 License", mainLicenseIds.get(0));
        assertNull(responseBody.get("wrong_prop"));
    }

    @Test
    public void should_update_release_invalid() throws IOException, TException {
        doThrow(TException.class).when(this.releaseServiceMock).getReleaseForUserById(anyObject(), anyObject());
        String updatedReleaseName = "updatedReleaseName";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/unknownId123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_all_releases_with_fields() throws IOException {
        String extraField = "cpeId";
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?fields=" + extraField,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2, Collections.singletonList(extraField));
    }

    @Test
    public void should_delete_releases() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(eq(TestHelper.release1Id), anyObject())).willReturn(RequestStatus.SUCCESS);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "," + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(TestHelper.release1Id, HttpStatus.OK));
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_delete_release() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(anyObject(), anyObject())).willReturn(RequestStatus.FAILURE);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_get_empty_collection_for_invalid_sha() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/releases?sha1=" + attachmentShaInvalid, HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_get_collection_for_duplicated_shas() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/releases?sha1=" + TestHelper.attachmentShaUsedMultipleTimes
                        + "&fields=mainlineState,clearingState",
                HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_delete_attachments_successfully() throws TException, IOException {
        Release release = new Release(TestHelper.getDummyReleaseListForTest().get(0));
        final AtomicReference<Release> refUpdatedRelease = new AtomicReference<>();
        List<Attachment> attachments = TestHelper.getDummyAttachmentsListForTest();
        List<String> attachmentIds = Arrays.asList(attachments.get(0).attachmentContentId, "otherAttachmentId");
        String strIds = String.join(",", attachmentIds);
        release.setAttachments(new HashSet<>(attachments));
        given(releaseServiceMock.getReleaseForUserById(release.id, TestHelper.getTestUser())).willReturn(release);
        given(releaseServiceMock.updateRelease(any(), eq(TestHelper.getTestUser())))
                .will(invocationOnMock -> {
                    refUpdatedRelease.set(new Release((Release) invocationOnMock.getArguments()[0]));
                    return RequestStatus.SUCCESS;
                });
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.releaseId(release.getId()),
                release.getAttachments(), attachmentIds))
                .willReturn(Collections.singleton(attachments.get(1)));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" +
                                TestHelper.release1Id + "/attachments/" + strIds,
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

        Release updatedRelease = refUpdatedRelease.get();
        assertThat(updatedRelease, is(notNullValue()));
        assertThat(updatedRelease.getAttachments(), hasSize(1));
        assertThat(updatedRelease.getAttachments(), hasItem(attachments.get(0)));
    }

    @Test
    public void should_delete_attachments_with_failure_handling() throws TException, IOException {
        Release release = new Release(TestHelper.getDummyReleaseListForTest().get(0));
        String attachmentId = TestHelper.getDummyAttachmentInfoListForTest().get(0).getAttachment()
                .getAttachmentContentId();
        Set<Attachment> attachments = new HashSet<>(TestHelper.getDummyAttachmentsListForTest());
        release.setAttachments(attachments);
        given(releaseServiceMock.getReleaseForUserById(release.id, TestHelper.getTestUser())).willReturn(release);
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.releaseId(release.getId()),
                release.getAttachments(), Collections.singletonList(attachmentId)))
                .willReturn(Collections.emptySet());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" +
                                TestHelper.release1Id + "/attachments/" + attachmentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        then(releaseServiceMock)
                .should(never())
                .updateRelease(any(), any());
    }
}