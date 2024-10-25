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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;

import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.schedule.Sw360ScheduleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ScheduleSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360ScheduleService scheduleServiceMock;
    private RequestSummary requestSummary = new RequestSummary();

    @Before
    public void before() throws TException {

        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(sw360User);
        given(this.scheduleServiceMock.cancelAllServices(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.scheduleCveSearch(any())).willReturn(requestSummary);
        given(this.scheduleServiceMock.cancelCveSearch(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.deleteAttachmentService(any())).willReturn(requestSummary);
        given(this.scheduleServiceMock.cancelDeleteAttachment(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.cancelAttachmentDeletionLocalFS(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.triggerCveSearch(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.cancelSvmSync(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.cancelSvmReverseMatch(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.scheduleSvmReverseMatch(any())).willReturn(requestSummary);
        given(this.scheduleServiceMock.svmReleaseTrackingFeedback(any())).willReturn(requestSummary);
        given(this.scheduleServiceMock.svmMonitoringListUpdate(any())).willReturn(requestSummary);
        given(this.scheduleServiceMock.triggerSrcUpload(any())).willReturn(requestSummary);
        given(this.scheduleServiceMock.cancelSvmMonitoringListUpdate(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.unscheduleSrcUpload(any())).willReturn(RequestStatus.SUCCESS);

    }

    @Test
    public void should_document_cancel_all_schedule() throws Exception {
        mockMvc.perform(post("/api/schedule/unscheduleAllServices")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_schedule_cve_service() throws Exception {
        mockMvc.perform(post("/api/schedule/cveService")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_schedule_svm_sync() throws Exception {
        mockMvc.perform(post("/api/schedule/scheduleSvmSync")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_cancel_schedule_svm_sync() throws Exception {
        mockMvc.perform(delete("/api/schedule/unscheduleSvmSync")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_reverse_svm_match() throws Exception {
        mockMvc.perform(post("/api/schedule/svmReverseMatch")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_cancel_reverse_match() throws Exception {
        mockMvc.perform(delete("/api/schedule/unscheduleSvmReverseMatch")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_track_feedback() throws Exception {
        mockMvc.perform(post("/api/schedule/trackingFeedback")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_svm_list_update() throws Exception {
        mockMvc.perform(post("/api/schedule/monitoringListUpdate")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_cancel_monitoring_svm_list() throws Exception {
        mockMvc.perform(delete("/api/schedule/cancelMonitoringListUpdate")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_src_upload() throws Exception {
        mockMvc.perform(post("/api/schedule/srcUpload")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_cancel_monitoring_cancel_svm_list() throws Exception {
        mockMvc.perform(delete("/api/schedule/cancelSrcUpload")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_unschedule_cve_search() throws Exception {
        mockMvc.perform(post("/api/schedule/unscheduleCve")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_schedule_service_from_local() throws Exception {
        mockMvc.perform(post("/api/schedule/deleteAttachment")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_schedule_cve_search() throws Exception {
        mockMvc.perform(post("/api/schedule/cveSearch")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_cancel_schedule_attachment() throws Exception {
        mockMvc.perform(post("/api/schedule/unScheduleDeleteAttachment")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void should_document_delete_old_attachment_from_local() throws Exception {
        mockMvc.perform(post("/api/schedule/cancelAttachmentDeletion")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void schedule_source_upload() throws Exception {
        mockMvc.perform(post("/api/schedule/scheduleSourceUploadForReleaseComponents")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted());
    }

}
