/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.Quadratic;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class LicenseSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360LicenseService licenseServiceMock;

    @MockBean
    private SW360ReportService sw360ReportServiceMock;

    private License license, license2, license3;
    private Obligation obligation1, obligation2;
    private RequestSummary requestSummary = new RequestSummary();

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws TException, IOException {
        license = new License();
        license.setId("Apache-2.0");
        license.setFullname("Apache License 2.0");
        license.setShortname("Apache 2.0");
        license.setText("placeholder for the Apache 2.0 license text");
        Map<String,String> externalIds = new HashMap<>();
        externalIds.put("SPDX", "Apache-2.0");
        externalIds.put("Trove", "License :: OSI Approved :: Apache Software License");
        license.setExternalIds(externalIds);
        license.setAdditionalData(Collections.singletonMap("Key", "Value"));
        license.setNote("License's Note");
        license.setExternalLicenseLink("https://spdx.org/licenses/Apache-2.0.html");

        license2 = new License();
        license2.setId("MIT");
        license2.setFullname("The MIT License (MIT)");
        license2.setShortname("MIT");
        license2.setText("placeholder for the MIT license text");
        license2.setNote("License2's Note");
        license2.setExternalLicenseLink("https://spdx.org/licenses/MIT.html");

        List<License> licenseList = new ArrayList<>();
        licenseList.add(license);
        licenseList.add(license2);

        license3 = new License();
        license3.setId("Apache-3.0");
        license3.setShortname("Apache 3.0");
        license3.setFullname("Apache License 3.0");
        
        requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        LicenseType licensetype = new LicenseType();
        licensetype.setId("1234");
        licensetype.setLicenseType("wer");
        licensetype.setLicenseTypeId(123);
        licensetype.setType("xyz");

        given(this.licenseServiceMock.getLicenses()).willReturn(licenseList);
        given(this.licenseServiceMock.getLicenseById(eq(license.getId()))).willReturn(license);
        given(this.licenseServiceMock.createLicense(any(), any())).willReturn(license3);
        given(this.licenseServiceMock.updateLicense(any(),any())).willReturn(RequestStatus.SUCCESS);
        given(this.licenseServiceMock.updateWhitelist(any(),any(),any())).willReturn(RequestStatus.SUCCESS);
        Mockito.doNothing().when(licenseServiceMock).deleteLicenseById(any(), any());
        Mockito.doNothing().when(licenseServiceMock).deleteAllLicenseInfo(any());
        Mockito.doNothing().when(licenseServiceMock).importSpdxInformation(any());
        Mockito.doNothing().when(licenseServiceMock).getDownloadLicenseArchive(any(), any(), any());
        Mockito.doNothing().when(licenseServiceMock).uploadLicense(any(), any(), anyBoolean(), anyBoolean());
        given(this.licenseServiceMock.importOsadlInformation(any())).willReturn(requestSummary);
        given(this.licenseServiceMock.addLicenseType(any(),any() , any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360ReportServiceMock.getLicenseBuffer()).willReturn(ByteBuffer.allocate(10000));
        obligation1 = new Obligation();
        obligation1.setId("0001");
        obligation1.setTitle("Obligation 1");
        obligation1.setText("This is text of Obligation 1");
        obligation1.setWhitelist(Collections.singleton("Department"));
        obligation1.setObligationType(ObligationType.PERMISSION);
        obligation1.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);

        obligation2 = new Obligation();
        obligation2.setId("0002");
        obligation2.setTitle("Obligation 2");
        obligation2.setText("This is text of Obligation 2");
        obligation2.setWhitelist(Collections.singleton("Department2"));
        obligation2.setObligationType(ObligationType.OBLIGATION);
        obligation2.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);

        List<Obligation> obligations = Arrays.asList(obligation1, obligation2);
        Set<String> obligationIds = new HashSet<>(Arrays.asList(obligation1.getId(), obligation2.getId()));
        license2.setObligationDatabaseIds(obligationIds);
        license2.setObligations(obligations);
        given(this.licenseServiceMock.getObligationsByLicenseId(any())).willReturn(obligations);
    }

    @Test
    public void should_document_get_licenses() throws Exception {
        mockMvc.perform(get("/api/licenses")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:licenses").description("An array of <<resources-licenses, Licenses resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_whitelist_license() throws Exception {
        List<Obligation> obligationList = new ArrayList<>();
        obligationList.add(obligation1);
        obligationList.add(obligation2);
        license.setObligations(obligationList);

        Set<String> obligationIds = new HashSet<String>();
        obligationIds.add(obligation1.getId());
        obligationIds.add(obligation2.getId());
        license.setObligationDatabaseIds(obligationIds);

        Map<String, Boolean> requestBody = new HashMap<>();
        requestBody.put("0001",true);
        requestBody.put("0002",true);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/licenses/" + license.getId() +"/whitelist")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(requestBody))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("fullName").description("The full name of the license"),
                                fieldWithPath("shortName").description("The short name of the license, optional"),
                                subsectionWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("externalLicenseLink").description("The external license link of the license"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                subsectionWithPath("obligations").description("The obligations license link of the license"),
                                subsectionWithPath("obligationDatabaseIds").description("The obligationDatabaseIds license link of the license"),
                                fieldWithPath("text").description("The license's original text"),
                                fieldWithPath("checked").description("The information, whether the license is already checked, optional and defaults to true"),
                                subsectionWithPath("OSIApproved").description("The OSI aprroved information, possible values are: " + Arrays.asList(Quadratic.values())),
                                fieldWithPath("FSFLibre").description("The FSF libre information, possible values are: " + Arrays.asList(Quadratic.values())),
                                subsectionWithPath("_embedded.sw360:obligations").description("An array of <<resources-obligations, Obligations obligations>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("note").description("The license's note")
                        )));
    }

    @Test
    public void should_document_get_license() throws Exception {
        List<Obligation> obligationList = new ArrayList<>();
        obligationList.add(obligation1);
        obligationList.add(obligation2);
        license.setObligations(obligationList);

        Set<String> obligationIds = new HashSet<String>();
        obligationIds.add(obligation1.getId());
        obligationIds.add(obligation2.getId());
        license.setObligationDatabaseIds(obligationIds);

        mockMvc.perform(get("/api/licenses/" + license.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-licenses,Licenses resource>>")
                        ),
                        responseFields(
                                fieldWithPath("fullName").description("The full name of the license"),
                                fieldWithPath("shortName").description("The short name of the license, optional"),
                                subsectionWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("externalLicenseLink").description("The external license link of the license"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                subsectionWithPath("obligations").description("The obligations license link of the license"),
                                subsectionWithPath("obligationDatabaseIds").description("The obligationDatabaseIds license link of the license"),
                                fieldWithPath("text").description("The license's original text"),
                                fieldWithPath("checked").description("The information, whether the license is already checked, optional and defaults to true"),
                                subsectionWithPath("OSIApproved").description("The OSI aprroved information, possible values are: " + Arrays.asList(Quadratic.values())),
                                fieldWithPath("FSFLibre").description("The FSF libre information, possible values are: " + Arrays.asList(Quadratic.values())),
                                subsectionWithPath("_embedded.sw360:obligations").description("An array of <<resources-obligations, Obligations obligations>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("note").description("The license's note")
                        )));
    }

    @Test
    public void should_document_get_obligations_by_license() throws Exception {
        mockMvc.perform(get("/api/licenses/"+  license2.getId()+ "/obligations")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:obligations[]title").description("The title of the obligation"),
                                subsectionWithPath("_embedded.sw360:obligations[]obligationType").description("The type of the obligation"),
                                subsectionWithPath("_embedded.sw360:obligations").description("An array of <<resources-obligations, Obligations resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_create_license() throws Exception {
        Map<String, String> licenseRequestBody = new HashMap<>();
        licenseRequestBody.put("fullName", "Apache 3.0");
        licenseRequestBody.put("shortName", "Apache License 3.0");
        this.mockMvc.perform(post("/api/licenses")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(licenseRequestBody))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("fullName").description("The fullName of the new license"),
                                fieldWithPath("shortName").description("The shortname of the origin license")
                        ),
                        responseFields(
                                fieldWithPath("fullName").description("The fullName of the license"),
                                fieldWithPath("shortName").description("The shortname of the license"),
                                fieldWithPath("checked").description("The information, whether the license is already checked, optional and defaults to true"),
                                subsectionWithPath("OSIApproved").description("The OSI aprroved information, possible values are: " + Arrays.asList(Quadratic.values())),
                                fieldWithPath("FSFLibre").description("The FSF libre information, possible values are: " + Arrays.asList(Quadratic.values())),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_license() throws Exception {
        Map<String, String> licenseRequestBody = new HashMap<>();
        licenseRequestBody.put("fullName", "Apache License 4.0");
        licenseRequestBody.put("note", "Apache License");

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/licenses/" + license.getId())
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(licenseRequestBody))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("fullName").description("The full name of the license"),
                                fieldWithPath("shortName").description("The short name of the license, optional"),
                                subsectionWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("externalLicenseLink").description("The external license link of the license"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("text").description("The license's original text"),
                                fieldWithPath("checked").description("The information, whether the license is already checked, optional and defaults to true"),
                                subsectionWithPath("OSIApproved").description("The OSI aprroved information, possible values are: " + Arrays.asList(Quadratic.values())),
                                fieldWithPath("FSFLibre").description("The FSF libre information, possible values are: " + Arrays.asList(Quadratic.values())),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("note").description("The license's note")
                        )));
    }

    @Test
    public void should_document_delete_license() throws Exception {
        mockMvc.perform(delete("/api/licenses/" + license.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_link_obligation() throws Exception {
        Set<String> obligationIds = new HashSet<String>();
        obligationIds.add(obligation1.getId());
        obligationIds.add(obligation2.getId());

        mockMvc.perform(post("/api/licenses/" + license.getId() + "/obligations")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(obligationIds))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated());
    }

    @Test
    public void should_document_unlink_obligation() throws Exception {
        List<Obligation> obligationList = new ArrayList<>();
        obligationList.add(obligation1);
        obligationList.add(obligation2);
        license.setObligations(obligationList);

        Set<String> obligationLicenseIds = new HashSet<String>();
        obligationLicenseIds.add(obligation1.getId());
        obligationLicenseIds.add(obligation2.getId());
        license.setObligationDatabaseIds(obligationLicenseIds);

        Set<String> obligationIds = new HashSet<String>();
        obligationIds.add(obligation2.getId());

        this.mockMvc.perform(patch("/api/licenses/" + license.getId() + "/obligations")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(obligationIds))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_delete_all_license_info() throws Exception {
        mockMvc.perform(delete("/api/licenses/" + "/delete")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }
    @Test
    public void should_document_get_download_license_archive() throws Exception {
        mockMvc.perform(get("/api/licenses" + "/downloadLicenses")
         .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
         .accept("application/zip"))
         .andExpect(status().isOk())
         .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_import_spdx_info() throws Exception {
        mockMvc.perform(post("/api/licenses/" + "/import/SPDX")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_upload_license() throws Exception {
        MockMultipartFile file = new MockMultipartFile("licenseFile","file=@/bom.spdx.rdf".getBytes());
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/licenses/upload")
                .file(file)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("licenseFile", "Must need to attach file.");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test   		
    public void should_document_import_osadl_info() throws Exception {
        mockMvc.perform(post("/api/licenses/import/OSADL")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_create_license_type() throws Exception {
        mockMvc.perform(post("/api/licenses/" + "/addLicenseType")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("licenseType", "wer")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_license_report() throws Exception{
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("module", "licenses")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                    queryParameters(
                            parameterWithName("module").description("module represent the licenses. Possible values are `<licenses>`")
                    )));
    }
}