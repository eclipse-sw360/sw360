/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import org.eclipse.sw360.rest.resourceserver.admin.licensedb.LicenseDBSyncStatus;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LicenseDBAdminSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @BeforeEach
    public void before() throws TException, IOException {
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));

        LicenseDBSyncStatus syncStatus = new LicenseDBSyncStatus();
        syncStatus.setEnabled(true);
        syncStatus.setImportRunning(false);
        syncStatus.setLastSyncTimestamp("2026-07-18T10:00:00Z");

        LicenseDBSyncStatus health = new LicenseDBSyncStatus();
        health.setEnabled(true);
        health.setConnected(true);
        health.setImportRunning(false);
        health.setLastSyncTimestamp("2026-07-18T10:00:00Z");

        RequestSummary fullSyncSummary = new RequestSummary()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setTotalAffectedElements(42)
                .setMessage("{\"licensesCreated\":30,\"licensesUpdated\":12}");

        RequestSummary incrementalSyncSummary = new RequestSummary()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setTotalAffectedElements(5)
                .setMessage("{\"licensesCreated\":2,\"licensesUpdated\":3}");

        given(this.licenseDBAdminService.getHealth(any())).willReturn(health);
        given(this.licenseDBAdminService.getSyncStatus(any())).willReturn(syncStatus);
        given(this.licenseDBAdminService.triggerFullSync(any())).willReturn(fullSyncSummary);
        given(this.licenseDBAdminService.triggerIncrementalSync(any())).willReturn(incrementalSyncSummary);
    }

    @Test
    public void should_document_get_licensedb_health() throws Exception {
        mockMvc.perform(get("/api/licenseDB/health")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.importRunning").value(false));
    }

    @Test
    public void should_document_get_licensedb_sync_status() throws Exception {
        mockMvc.perform(get("/api/licenseDB/syncStatus")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.importRunning").value(false));
    }

    @Test
    public void should_document_trigger_full_sync() throws Exception {
        mockMvc.perform(post("/api/licenseDB/sync")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("SUCCESS"));
    }

    @Test
    public void should_document_trigger_incremental_sync() throws Exception {
        mockMvc.perform(post("/api/licenseDB/sync/incremental")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("SUCCESS"));
    }
}
