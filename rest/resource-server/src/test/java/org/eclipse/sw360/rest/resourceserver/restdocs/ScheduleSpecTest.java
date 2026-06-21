/*
 * Copyright Siemens AG, 2023-2024,2026.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;

import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.schedule.Sw360ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.hateoas.MediaTypes;

import java.util.Map;

public class ScheduleSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360ScheduleService scheduleServiceMock;
    private RequestSummary requestSummary = new RequestSummary().setRequestStatus(RequestStatus.SUCCESS);

    @BeforeEach
    public void before() throws TException {

        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(sw360User);
        given(this.scheduleServiceMock.cancelAllServices(any())).willReturn(RequestStatus.SUCCESS);

        // New unified endpoints
        given(this.scheduleServiceMock.scheduleService(any(), anyString())).willReturn(requestSummary);
        given(this.scheduleServiceMock.unscheduleService(any(), anyString())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.triggerManualService(any(), anyString())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.isServiceScheduled(anyString(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.isAnyServiceScheduled(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.getServiceDetails(anyString(), any()))
                .willReturn(Map.of("isScheduled", true, "firstOffsetSeconds", 0,
                        "intervalSeconds", 86400, "nextSynchronization", "2026-05-13T00:00:00"));
        given(this.scheduleServiceMock.getAllServicesDetails(any()))
                .willReturn(java.util.Map.of(
                        "cvesearchService", Map.of("isScheduled", true, "firstOffsetSeconds", 0,
                                "intervalSeconds", 86400, "nextSynchronization", "2026-05-13T00:00:00"),
                        "svmsyncService", Map.of("isScheduled", false, "firstOffsetSeconds", 3600,
                                "intervalSeconds", 86400, "nextSynchronization", "N/A")
                ));

    }

    @Test
    public void should_document_cancel_all_schedule() throws Exception {
        mockMvc.perform(post("/api/schedule/unscheduleAllServices")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    // ========== NEW UNIFIED ENDPOINT SPEC TESTS ==========

    @Test
    public void should_document_schedule_service_by_name() throws Exception {
        mockMvc.perform(post("/api/schedule/scheduleService")
                .param("serviceName", "cvesearchService")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_unschedule_service_by_name() throws Exception {
        mockMvc.perform(delete("/api/schedule/unscheduleService")
                .param("serviceName", "cvesearchService")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_trigger_service_by_name() throws Exception {
        mockMvc.perform(post("/api/schedule/triggerService")
                .param("serviceName", "cvesearchService")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_service_details_for_specific_service() throws Exception {
        mockMvc.perform(get("/api/schedule/serviceDetails")
                .param("serviceName", "cvesearchService")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvesearchService.isScheduled").value(true))
                .andExpect(jsonPath("$.cvesearchService.firstOffsetSeconds").value(0))
                .andExpect(jsonPath("$.cvesearchService.intervalSeconds").value(86400))
                .andExpect(jsonPath("$.cvesearchService.nextSynchronization").value("2026-05-13T00:00:00"));
    }

    @Test
    public void should_document_get_all_service_details() throws Exception {
        mockMvc.perform(get("/api/schedule/serviceDetails")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvesearchService").exists())
                .andExpect(jsonPath("$.svmsyncService").exists())
                .andExpect(jsonPath("$.cvesearchService.isScheduled").value(true))
                .andExpect(jsonPath("$.svmsyncService.isScheduled").value(false));
    }

    @Test
    public void should_document_is_any_service_scheduled_true() throws Exception {
        mockMvc.perform(get("/api/schedule/isAnyServiceScheduled")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void should_document_check_service_status() throws Exception {
        mockMvc.perform(get("/api/schedule/status")
                .param("serviceName", "cvesearchService")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("cvesearchService"))
                .andExpect(jsonPath("$.isScheduled").value(true));
    }

}
