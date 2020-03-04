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
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
public class AttachmentTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    private final String shaInvalid = "56789";

    @Before
    public void before() throws TException {

        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(TestHelper.attachmentShaUsedMultipleTimes))).willReturn(TestHelper.getDummyAttachmentInfoListForTest());
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(shaInvalid))).willReturn(new ArrayList<>());

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

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
}