/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ApiSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360ProjectService projectServiceMock;

    @Test
    public void should_document_headers() throws Exception {
        this.mockMvc.perform(RestDocumentationRequestBuilders.get("/api")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .header("Accept", MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseHeaders(
                                headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/hal+json`"))));
    }

    @Test
    public void should_document_error_bad_request() throws Exception {
        this.mockMvc.perform(
                post("/api/projects")
                        .contentType(MediaTypes.HAL_JSON)
                        .content("{")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isBadRequest())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("timestamp").description("The timestamp when the error occurred"),
                                fieldWithPath("status").description("The HTTP status code, e.g. 400"),
                                fieldWithPath("error").description("The HTTP error code, e.g. Bad Request"),
                                fieldWithPath("message").description("The error message, e.g. JSON parse error: Unexpected end-of-input: expected close marker for Object"))));
    }

    @Test
    // SecurityContextHolder.getContext in test filter chain supports no mocking.
    // The authentication object is always null in the test class.
    // As result the test filter automatically authenticates the request
    // thus the unauthorized case is not testable
    public void should_document_error_unauthorized() throws Exception {
        this.mockMvc.perform(RestDocumentationRequestBuilders.get("/api")
                .header("Authorization", "Bearer " + "123456789"))
                .andExpect(status().isUnauthorized())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("timestamp").description("The timestamp when the error occurred"),
                                fieldWithPath("status").description("The HTTP status code, e.g. 400"),
                                fieldWithPath("error").description("The HTTP error code, e.g. Bad Request"),
                                fieldWithPath("message").description("The error message, e.g. JSON parse error: Unexpected end-of-input: expected close marker for Object"))));
    }

    @Test
    public void should_document_error_not_found() throws Exception {
        this.mockMvc.perform(
                post("/api/endpoint_not_found")
                        .contentType(MediaTypes.HAL_JSON)
                        .content("{}")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_document_error_method_not_allowed() throws Exception {
        this.mockMvc.perform(RestDocumentationRequestBuilders.delete("/api")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isMethodNotAllowed())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("timestamp").description("The timestamp when the error occurred"),
                                fieldWithPath("status").description("The HTTP status code, e.g. 405"),
                                fieldWithPath("error").description("The HTTP error code, e.g. Method Not Allowed"),
                                fieldWithPath("message").description("The error message, e.g. Request method 'DELETE' not supported"))));
    }

    @Test
    public void should_document_error_unsupported_media_type() throws Exception {
        this.mockMvc.perform(
                post("/api/projects")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isUnsupportedMediaType())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("timestamp").description("The timestamp when the error occurred"),
                                fieldWithPath("status").description("The HTTP status code, e.g. 415"),
                                fieldWithPath("error").description("The HTTP error code, e.g. Unsupported Media Typ"),
                                fieldWithPath("message").description("The error message, e.g. Content type 'application/text;charset=UTF-8' not supported"))));
    }

    @Test
    public void should_document_error_internal_error() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(anyString(), any())).willThrow(new RuntimeException(new TException("Internal error processing getProjectById")));
        this.mockMvc.perform(RestDocumentationRequestBuilders.get("/api/projects/12321")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isInternalServerError())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("timestamp").description("The timestamp when the error occurred"),
                                fieldWithPath("status").description("The HTTP status code, e.g. 500"),
                                fieldWithPath("error").description("The HTTP error code, e.g. Internal Server Error"),
                                fieldWithPath("message").description("The error message, e.g. Internal error processing getProjectById"))));
    }

    @Test
    public void should_document_index() throws Exception {
        this.mockMvc.perform(get("/api")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("sw360:users").description("The <<resources-users,Users resource>>"),
                                linkWithRel("sw360:reports").description("The <<resources-reports,Reports resource>>"),
                                linkWithRel("sw360:projects").description("The <<resources-projects,Projects resource>>"),
                                linkWithRel("sw360:components").description("The <<resources-components,Components resource>>"),
                                linkWithRel("sw360:releases").description("The <<resources-releases,Releases resource>>"),
                                linkWithRel("sw360:packages").description("The <<resources-packages,Packages resource>>"),
                                linkWithRel("sw360:attachments").description("The <<resources-attachments,Attachments resource>>"),
                                linkWithRel("sw360:vendors").description("The <<resources-vendors,Vendors resource>>"),
                                linkWithRel("sw360:licenses").description("The <<resources-licenses,Licenses resource>>"),
                                linkWithRel("sw360:licenseinfo").description("The <<resources-licenseinfo,Licenseinfo resource>>"),
                                linkWithRel("sw360:vulnerabilities").description("The <<resources-vulnerabilities,Vulnerabilities resource>>"),
                                linkWithRel("sw360:vulnerabilities").description("The <<resources-vulnerabilities,Vulnerabilities resource>>"),
                                linkWithRel("sw360:searchs").description("The <<resources-search,Vulnerabilities resource>>"),
                                linkWithRel("sw360:changeLogs").description("The <<resources-changelog,Changelog resource>>"),
                                linkWithRel("sw360:clearingRequests").description("The <<resources-clearingRequest,ClearingRequest resource>>"),
                                linkWithRel("sw360:obligations").description("The <<resources-obligations,Obligation resource>>"),
                                linkWithRel("sw360:databaseSanitation").description("The <<resources-databaseSanitation,DatabaseSanitation resource>>"),
                                linkWithRel("sw360:moderationRequests").description("The <<resources-moderationRequest,ModerationRequest resource>>"),
                                linkWithRel("sw360:fossology").description("The <<resources-fossology,Fossology resource>>"),
                                linkWithRel("sw360:schedule").description("The <<resources-schedule,Schedule resource>>"),
                                linkWithRel("sw360:ecc").description("The <<resources-ecc,Ecc resource>>"),
                                linkWithRel("sw360:attachmentCleanUp").description("The <<resources-attachmentCleanUp,attachmentCleanUp resource>>"),
                                linkWithRel("sw360:importExport").description("The <<resources-importExport,ImportExport resource>>"),
                                linkWithRel("sw360:department").description("The <<resources-department,Department resource>>"),
                                linkWithRel("curies").description("The Curies for documentation"),
                                linkWithRel("profile").description("The profiles of the REST resources")
                        ),
                        responseFields(
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));
    }
}
