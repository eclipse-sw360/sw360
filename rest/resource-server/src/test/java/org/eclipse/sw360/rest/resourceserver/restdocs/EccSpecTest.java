/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.mockito.ArgumentMatchers.any;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.formParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
public class EccSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ReleaseService releaseService;

    @Before
    public void before() throws TException, URISyntaxException {
        List<Release> releaseList = new ArrayList<>();
        Release rel1 = new Release();
        rel1.setName("testRealease");
        rel1.setVersion("1.0");
        
        EccInformation eccInfo1 = new EccInformation();
        eccInfo1.setAssessorContactPerson("john@siemens.com");
        eccInfo1.setAssessmentDate("24-01-2024");
        eccInfo1.setAssessorDepartment("Department");
        rel1.setEccInformation(eccInfo1);
        
        Release rel2 = new Release();
        rel2.setName("testRealease2");
        rel2.setVersion("2.0");
        EccInformation eccInfo2 = new EccInformation();
        eccInfo2.setAssessorContactPerson("john2@siemens.com");
        eccInfo2.setAssessmentDate("22-01-2024");
        eccInfo2.setAssessorDepartment("Department2");
        rel2.setEccInformation(eccInfo2);

        releaseList.add(rel1);
        releaseList.add(rel2);
        
        given(this.releaseService.getReleasesForUser(any())).willReturn(releaseList);
    }

    @Test
    public void should_document_get_ecc() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/ecc")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        formParameters(
                                parameterWithName("page").description("Page of releases"),
                                parameterWithName("page_entries").description("Amount of releases per page"),
                                parameterWithName("sort").description("Defines order of the releases")
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
    
}
