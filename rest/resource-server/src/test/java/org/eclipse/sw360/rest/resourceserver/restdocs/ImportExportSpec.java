/*
 * Copyright Siemens AG, 2024-2025.
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.importexport.Sw360ImportExportService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.servlet.ServletException;

@RunWith(SpringJUnit4ClassRunner.class)
public class ImportExportSpec extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360ImportExportService importExportService;

    private RequestSummary requestSummary = new RequestSummary();

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws TException, IOException, ServletException {
        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");

        requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        LicenseType licensetype = new LicenseType();
        licensetype.setId("1234");
        licensetype.setLicenseType("wer");
        licensetype.setLicenseTypeId(123);
        licensetype.setType("xyz");

        given(this.importExportService.uploadComponent(any(), any(), any(),any())).willReturn(requestSummary);
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(sw360User);
        given(this.importExportService.uploadReleaseLink(any(), any(), any())).willReturn(requestSummary);
        given(this.importExportService.uploadComponentAttachment(any(), any(), any())).willReturn(requestSummary);

    }

    @Test
    public void should_document_upload_component_file() throws Exception {
        MockMultipartFile file = new MockMultipartFile("componentFile","file=@/bom.spdx.rdf".getBytes());
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/importExport/uploadComponent")
                .file(file)
                .header("Authorization", accessToken)
                .queryParam("componentFile", "Must need to attach file.");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_upload_release_link_file() throws Exception {
        MockMultipartFile file = new MockMultipartFile("releaseFile","file=@/bom.spdx.rdf".getBytes());
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/importExport/uploadRelease")
                .file(file)
                .header("Authorization", accessToken)
                .queryParam("releaseFile", "Must need to attach file.");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_upload_component_attachment_file() throws Exception {
        MockMultipartFile file = new MockMultipartFile("attachmentFile","file=@/bom.spdx.rdf".getBytes());
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/importExport/componentAttachment")
                .file(file)
                .header("Authorization", accessToken)
                .queryParam("attachmentFile", "Must need to attach file.");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }
}
