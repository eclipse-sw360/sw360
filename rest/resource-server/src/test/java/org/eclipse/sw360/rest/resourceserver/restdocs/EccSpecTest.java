/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringJUnit4ClassRunner.class)
public class EccSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360ReleaseService releaseService;

    private Release rel1;
    private Release rel2;

    @Before
    public void before() throws TException, URISyntaxException {
        List<Release> releaseList = new ArrayList<>();

        rel1 = new Release();
        rel1.setId("rel001");
        rel1.setName("testRelease");
        rel1.setVersion("1.0");

        EccInformation eccInfo1 = new EccInformation();
        eccInfo1.setEccStatus(ECCStatus.OPEN);
        eccInfo1.setAssessorContactPerson("john@siemens.com");
        eccInfo1.setAssessmentDate("24-01-2024");
        eccInfo1.setAssessorDepartment("Department");
        rel1.setEccInformation(eccInfo1);

        rel2 = new Release();
        rel2.setId("rel002");
        rel2.setName("testRelease2");
        rel2.setVersion("2.0");

        EccInformation eccInfo2 = new EccInformation();
        eccInfo2.setEccStatus(ECCStatus.APPROVED);
        eccInfo2.setAssessorContactPerson("john2@siemens.com");
        eccInfo2.setAssessmentDate("22-01-2024");
        eccInfo2.setAssessorDepartment("Department2");
        rel2.setEccInformation(eccInfo2);

        releaseList.add(rel1);
        releaseList.add(rel2);

        given(this.releaseService.getReleasesForUser(any())).willReturn(releaseList);
        given(this.releaseService.getReleaseForUserById(eq("rel001"), any())).willReturn(rel1);
        given(this.releaseService.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789").setUserGroup(UserGroup.ADMIN));
    }

    @Test
    public void should_document_get_ecc() throws Exception {
        mockMvc.perform(get("/api/ecc")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of releases"),
                                parameterWithName("page_entries").description("Amount of releases per page"),
                                parameterWithName("sort").description("Defines order of the releases"),
                                parameterWithName("eccStatus").description("(Optional) Filter by ECC status: " +
                                        "OPEN, IN_PROGRESS, APPROVED, REJECTED. Omit to return all releases.")
                                        .optional()
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]eccInformation").description("The eccInformation of the release"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of releases per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing releases"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_patch_ecc() throws Exception {
        String eccBody = "{"
                + "\"eccStatus\": \"APPROVED\","
                + "\"assessorContactPerson\": \"ecc-lead@siemens.com\","
                + "\"assessorDepartment\": \"Export Control\","
                + "\"assessmentDate\": \"2026-04-25\","
                + "\"eccn\": \"EAR99\","
                + "\"al\": \"N\""
                + "}";

        mockMvc.perform(patch("/api/ecc/{releaseId}", rel1.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eccBody)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        pathParameters(
                                parameterWithName("releaseId").description("The ID of the release to update")
                        ),
                        requestFields(
                                fieldWithPath("eccStatus").description("ECC assessment status: OPEN, IN_PROGRESS, APPROVED, REJECTED").optional(),
                                fieldWithPath("assessorContactPerson").description("Email of the ECC assessor").optional(),
                                fieldWithPath("assessorDepartment").description("Department of the ECC assessor").optional(),
                                fieldWithPath("assessmentDate").description("Date of the ECC assessment (YYYY-MM-dd)").optional(),
                                fieldWithPath("eccn").description("Export Control Classification Number").optional(),
                                fieldWithPath("al").description("German Ausfuhrliste value").optional(),
                                fieldWithPath("eccComment").description("Free-text ECC comment").optional(),
                                fieldWithPath("materialIndexNumber").description("Material index number").optional(),
                                fieldWithPath("containsCryptography").description("Whether the release contains cryptography").optional()
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the release"),
                                fieldWithPath("version").description("The version of the release"),
                                fieldWithPath("id").description("The ID of the release"),
                                subsectionWithPath("eccInformation").description("The updated ECC information for the release"),
                                subsectionWithPath("_links").description("Links to other resources")
                        )));
    }
}
