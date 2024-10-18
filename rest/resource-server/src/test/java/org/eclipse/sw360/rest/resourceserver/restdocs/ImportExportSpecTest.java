/*
 * Copyright Siemens AG, 2023-2024. Part of the SW360 Portal Project.
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.importexport.Sw360ImportExportService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ImportExportSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360ImportExportService importExportService;

    @Before
    public void before() throws TException, IOException {
        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(sw360User);
        Mockito.doNothing().when(importExportService).getDownloadCsvComponentTemplate(any(), any());
        Mockito.doNothing().when(importExportService).getDownloadAttachmentTemplate(any(), any());
        Mockito.doNothing().when(importExportService).getDownloadAttachmentInfo(any(), any());
        Mockito.doNothing().when(importExportService).getDownloadReleaseSample(any(), any());
        Mockito.doNothing().when(importExportService).getDownloadReleaseLink(any(), any());
        Mockito.doNothing().when(importExportService).getComponentDetailedExport(any(), any());
    }

    @Test
    public void should_document_get_download_component_template() throws Exception {
        mockMvc.perform(get("/api/importExport/downloadComponentTemplate").header("Authorization",
                TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_download_attachment_template() throws Exception {
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        mockMvc.perform(get("/api/importExport/downloadAttachmentSample").header("Authorization",
                TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isOk());
    }

    @Test
    public void should_document_get_download_attachment_information() throws Exception {
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        mockMvc.perform(get("/api/importExport/downloadAttachmentInfo")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)).andExpect(status().isOk());
    }

    @Test
    public void should_document_get_download_release_sample() throws Exception {
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        mockMvc.perform(get("/api/importExport/downloadReleaseSample")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)).andExpect(status().isOk());
    }

    @Test
    public void should_document_get_download_release_link_info() throws Exception {
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        mockMvc.perform(get("/api/importExport/downloadReleaseLink")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)).andExpect(status().isOk());
    }

    @Test
    public void should_document_get_download_component_details() throws Exception {
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        mockMvc.perform(get("/api/importExport/downloadComponent")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)).andExpect(status().isOk());
    }
}