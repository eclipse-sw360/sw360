/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.admin.attachment.Sw360AttachmentCleanUpService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class CleanUpAttachmentSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360AttachmentCleanUpService cleanUpService;
    private RequestSummary requestSummary = new RequestSummary();

    ComponentService.Iface componentClient;
    @MockitoBean
    AttachmentService.Iface attachmentClient;

    @Before
    public void before() throws TException, IOException {
        componentClient = mock(ComponentService.Iface.class);
        attachmentClient = mock(AttachmentService.Iface.class);
        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        requestSummary.setMessage("SUCCESS");
        requestSummary.setTotalElements(10);
        requestSummary.setTotalAffectedElements(10);
        requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        Set<String> usedAttachmentIds = new HashSet<>(Arrays.asList("123", "234"));
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.cleanUpService.cleanUpAttachments(any())).willReturn(requestSummary);
        when(componentClient.getUsedAttachmentContentIds()).thenReturn(usedAttachmentIds);
        given(this.attachmentClient.vacuumAttachmentDB(any(),any())).willReturn(requestSummary);
    }

    @Test
    public void should_document_cleanup_all_attachment() throws Exception {
        mockMvc.perform(delete("/api/attachmentCleanUp/deleteAll")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk());
    }
}
