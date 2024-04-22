/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-FileCopyrightText: 2023, Siemens AG. Part of the SW360 Portal Project.
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.ModerationPatch;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationRequestSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    @MockBean
    private Sw360ModerationRequestService moderationRequestServiceMock;

    @Before
    public void before() throws TException, IOException {
        Set<String> moderatorList = new HashSet<>();
        moderatorList.add("admin@sw360.org");
        moderatorList.add("admin2@sw360.org");

        Release releaseAdditions = new Release();
        releaseAdditions.setId("R-101");
        releaseAdditions.setName("New Release");
        releaseAdditions.setVersion("V2");
        releaseAdditions.setComponentId("C-101");
        releaseAdditions.setEccInformation(new EccInformation().setEccStatus(ECCStatus.OPEN));

        Map<String, ProjectProjectRelationship> linkedProject = new HashMap<>();
        linkedProject.put("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED));

        Project project1Additions = new Project();
        project1Additions.setName("Project 1");
        project1Additions.setState(ProjectState.ACTIVE);
        project1Additions.setVendorId("V-101");
        project1Additions.setProjectType(ProjectType.CUSTOMER);
        project1Additions.setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS);
        project1Additions.setLinkedProjects(linkedProject);

        Project project1Deletions = new Project();
        project1Deletions.setName("Project 1");
        project1Deletions.setState(ProjectState.ACTIVE);
        project1Deletions.setVendorId("V-101");
        project1Deletions.setProjectType(ProjectType.CUSTOMER);
        project1Deletions.setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS);

        Project project2Additions = new Project();
        project2Additions.setName("Project 2");
        project2Additions.setVersion("3");
        project2Additions.setState(ProjectState.ACTIVE);
        project2Additions.setProjectType(ProjectType.CUSTOMER);
        project2Additions.setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS);

        Project project2Deletions = new Project();
        project2Deletions.setName("Project 2");
        project2Deletions.setVersion("2");
        project2Deletions.setState(ProjectState.ACTIVE);
        project2Deletions.setProjectType(ProjectType.CUSTOMER);
        project2Deletions.setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS);

        ModerationRequest moderationRequest = new ModerationRequest();
        moderationRequest.setId("MR-101");
        moderationRequest.setTimestamp(System.currentTimeMillis() / 1000L - 172800);
        moderationRequest.setDocumentId("R-101");
        moderationRequest.setDocumentType(DocumentType.RELEASE);
        moderationRequest.setRequestingUser("test.admin@sw360.org");
        moderationRequest.setModerators(moderatorList);
        moderationRequest.setDocumentName("Release 1");
        moderationRequest.setModerationState(ModerationState.INPROGRESS);
        moderationRequest.setRequestingUserDepartment("DEPT");
        moderationRequest.setComponentType(ComponentType.OSS);
        moderationRequest.setCommentRequestingUser("Fixing the release information");
        moderationRequest.setReleaseAdditions(releaseAdditions);
        moderationRequest.setReleaseDeletions(releaseAdditions);

        Set<ModerationRequest> moderationRequests = new HashSet<>();
        Set<ModerationRequest> moderationRequestsByState = new HashSet<>();
        ModerationRequest mr1 = new ModerationRequest();
        mr1.setId("MR-102");
        mr1.setRevision("3");
        mr1.setTimestamp(System.currentTimeMillis() / 1000L - 172800);
        mr1.setTimestampOfDecision(System.currentTimeMillis() / 1000L - 155000);
        mr1.setDocumentId("P-101");
        mr1.setDocumentType(DocumentType.PROJECT);
        mr1.setRequestingUser("test.admin@sw360.org");
        mr1.setModerators(moderatorList);
        mr1.setDocumentName("Project 1");
        mr1.setModerationState(ModerationState.APPROVED);
        mr1.setReviewer("admin@sw360.org");
        mr1.setRequestingUserDepartment("DEPT");
        mr1.setComponentType(ComponentType.OSS);
        mr1.setCommentRequestingUser("Add linked project");
        mr1.setProjectAdditions(project1Additions);
        mr1.setProjectDeletions(project1Deletions);

        ModerationRequest mr2 = new ModerationRequest();
        mr2.setId("MR-103");
        mr2.setRevision("1");
        mr2.setTimestamp(System.currentTimeMillis() / 1000L - 172800);
        mr2.setTimestampOfDecision(System.currentTimeMillis() / 1000L - 155000);
        mr2.setDocumentId("P-102");
        mr2.setDocumentType(DocumentType.PROJECT);
        mr2.setRequestingUser("test.admin@sw360.org");
        mr2.setModerators(moderatorList);
        mr2.setDocumentName("Project 2");
        mr2.setModerationState(ModerationState.REJECTED);
        mr2.setReviewer("admin@sw360.org");
        mr2.setRequestingUserDepartment("DEPT");
        mr2.setComponentType(ComponentType.OSS);
        mr2.setCommentRequestingUser("Update project version");
        mr2.setProjectAdditions(project2Additions);
        mr2.setProjectDeletions(project2Deletions);

        moderationRequests.add(mr1);
        moderationRequests.add(mr2);
        moderationRequestsByState.add(mr1);

        Map<PaginationData, List<ModerationRequest>> requestsByState = new HashMap<>();
        requestsByState.put(new PaginationData().setTotalRowCount(moderationRequestsByState.size()), new ArrayList<>(moderationRequestsByState));

        User user = new User();
        user.setId("123456789");
        user.setEmail(testUserId);
        user.setFullname("John Doe");

        given(this.releaseServiceMock.getReleaseForUserById(eq(moderationRequest.getDocumentId()), any())).willReturn(releaseAdditions);
        given(this.userServiceMock.getUserByEmail(moderationRequest.getRequestingUser())).willReturn(new User("test.admin@sw360.org", "DEPT").setId("12345"));
        given(this.userServiceMock.getUserByEmailOrExternalId(testUserId)).willReturn(user);
        given(this.moderationRequestServiceMock.getRequestsByModerator(any(), any())).willReturn(new ArrayList<>(moderationRequests));
        given(this.moderationRequestServiceMock.getTotalCountOfRequests(any())).willReturn((long) moderationRequests.size());
        given(this.moderationRequestServiceMock.getModerationRequestById(eq(moderationRequest.getId()))).willReturn(moderationRequest);
        given(this.moderationRequestServiceMock.getRequestsByState(any(), any(), eq(false), anyBoolean())).willReturn(requestsByState);
        given(this.moderationRequestServiceMock.acceptRequest(eq(moderationRequest), eq("Changes looks good."), any())).willReturn(ModerationState.APPROVED);
        given(this.moderationRequestServiceMock.assignRequest(eq(moderationRequest), any())).willReturn(ModerationState.INPROGRESS);
        given(this.moderationRequestServiceMock.getRequestsByRequestingUser(any(), any())).willReturn(requestsByState);
    }

    @Test
    public void should_document_get_moderationrequests() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/moderationrequest")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("page_entries", "5")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("page").description("Page of moderation requests"),
                                parameterWithName("page_entries").description("Amount of requests per page")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:moderationRequests").description("An array of <<resources-moderationRequest, ModerationRequest>>."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]id").description("The id of the moderation request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestamp").description("Timestamp (in unix epoch) when the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestampOfDecision").description("Timestamp (in unix epoch) when the decision on the request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentId").description("The ID of the document for which the moderation request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentType").description("Type of the document. Possible values are: " + Arrays.asList(DocumentType.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUser").description("The user who created the moderation request."),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderators.[]").description("List of users who are marked as moderators for the request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentName").description("Name of the document for which the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]moderationState").description("The state of the moderation request. Possible values are: " + Arrays.asList(ModerationState.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUserDepartment").description("The Business Unit / Group of the Project, for which clearing request is created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]componentType").description("Type of the component for which the moderation request is created. Possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderatorsSize").description("Number of users in moderators list."),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing moderation requests"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_moderationrequests_alldetails() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/moderationrequest")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("allDetails", "true")
                        .param("page", "0")
                        .param("page_entries", "5")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("page").description("Page of moderation requests"),
                                parameterWithName("page_entries").description("Amount of requests per page"),
                                parameterWithName("allDetails").description("Set `true` to get all details for the <<resources-moderationRequest, ModerationRequests>>")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:moderationRequests").description("An array of <<resources-moderationRequest, ModerationRequest>>."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]id").description("The id of the moderation request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestamp").description("Timestamp (in unix epoch) when the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestampOfDecision").description("Timestamp (in unix epoch) when the decision on the request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentId").description("The ID of the document for which the moderation request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentType").description("Type of the document. Possible values are: " + Arrays.asList(DocumentType.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUser").description("The user who created the moderation request."),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderators.[]").description("List of users who are marked as moderators for the request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentName").description("Name of the document for which the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]moderationState").description("The state of the moderation request. Possible values are: " + Arrays.asList(ModerationState.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]reviewer").description("User who is currently assigned as the reviewer of the request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestDocumentDelete").description(""),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUserDepartment").description("The Business Unit / Group of the Project, for which clearing request is created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]componentType").description("Type of the component for which the moderation request is created. Possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]commentRequestingUser").description("The comment from requesting user."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]commentDecisionModerator").description("The comment from decision making user."),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]componentAdditions").description("Information which user wants to add for the component (if `documentType` is `COMPONENT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]releaseAdditions").description("Information which user wants to add for the release (if `documentType` is `RELEASE`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]projectAdditions").description("Information which user wants to add for the project (if `documentType` is `PROJECT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]licenseAdditions").description("Information which user wants to add for the license (if `documentType` is `LICENSE`).").optional().type(JsonFieldType.OBJECT),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]user").description(""),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]componentDeletions").description("Information which user wants to remove for the component (if `documentType` is `COMPONENT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]releaseDeletions").description("Information which user wants to remove for the release (if `documentType` is `RELEASE`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]projectDeletions").description("Information which user wants to remove for the project (if `documentType` is `PROJECT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]licenseDeletions").description("Information which user wants to remove for the license (if `documentType` is `LICENSE`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderatorsSize").description("Number of users in moderators list."),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing moderation requests"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_moderationrequest() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/moderationrequest/MR-101")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("Link to current Moderation Request.")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the moderation request."),
                                fieldWithPath("timestamp").description("Timestamp (in unix epoch) when the request was created."),
                                fieldWithPath("timestampOfDecision").description("Timestamp (in unix epoch) when the decision on the request was made."),
                                fieldWithPath("documentId").description("The ID of the document for which the moderation request was made."),
                                fieldWithPath("documentType").description("Type of the document. Possible values are: " + Arrays.asList(DocumentType.values())),
                                fieldWithPath("requestingUser").description("The user who created the moderation request."),
                                subsectionWithPath("moderators.[]").description("List of users who are marked as moderators for the request."),
                                fieldWithPath("documentName").description("Name of the document for which the request was created."),
                                fieldWithPath("moderationState").description("The state of the moderation request. Possible values are: " + Arrays.asList(ModerationState.values())),
                                fieldWithPath("reviewer").description("User who is currently assigned as the reviewer of the request."),
                                fieldWithPath("requestDocumentDelete").description(""),
                                fieldWithPath("requestingUserDepartment").description("The Business Unit / Group of the Project, for which clearing request is created."),
                                fieldWithPath("componentType").description("Type of the component for which the moderation request is created. Possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("commentRequestingUser").description("The comment from requesting user."),
                                fieldWithPath("commentDecisionModerator").description("The comment from decision making user."),
                                subsectionWithPath("componentAdditions").description("Information which user wants to add for the component (if `documentType` is `COMPONENT`).").optional().type(JsonFieldType.VARIES),
                                subsectionWithPath("releaseAdditions").description("Information which user wants to add for the release (if `documentType` is `RELEASE`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("projectAdditions").description("Information which user wants to add for the project (if `documentType` is `PROJECT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("licenseAdditions").description("Information which user wants to add for the license (if `documentType` is `LICENSE`).").optional().type(JsonFieldType.OBJECT),
                                fieldWithPath("user").description(""),
                                subsectionWithPath("componentDeletions").description("Information which user wants to remove for the component (if `documentType` is `COMPONENT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("releaseDeletions").description("Information which user wants to remove for the release (if `documentType` is `RELEASE`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("projectDeletions").description("Information which user wants to remove for the project (if `documentType` is `PROJECT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("licenseDeletions").description("Information which user wants to remove for the license (if `documentType` is `LICENSE`).").optional().type(JsonFieldType.OBJECT),
                                fieldWithPath("moderatorsSize").description("Number of users in moderators list."),
                                subsectionWithPath("_embedded.requestingUser").description("<<resources-users, User>> who created the ModerationRequest."),
                                subsectionWithPath("_embedded.sw360:project").description("<<resources-projects, Project>> for which the ModerationRequest was created (if `documentType` is `PROJECT`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:releases").description("<<resources-releases, Release>> for which the ModerationRequest was created (if `documentType` is `RELEASE`).").optional().type(JsonFieldType.ARRAY),
                                subsectionWithPath("_embedded.sw360:components").description("<<resources-components, Component>> for which the ModerationRequest was created (if `documentType` is `COMPONENT`).").optional().type(JsonFieldType.ARRAY),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_moderationrequests_by_state() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/moderationrequest/byState")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("state", "closed")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("state").description("The moderation request state of the request. Possible values are: <open|closed>"),
                                parameterWithName("page").description("Page of moderation requests").optional(),
                                parameterWithName("page_entries").description("Amount of requests per page").optional(),
                                parameterWithName("allDetails").description("Get all details for the <<resources-moderationRequest, ModerationRequests>>").optional()
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:moderationRequests").description("An array of <<resources-moderationRequest, ModerationRequest>>."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]id").description("The id of the moderation request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]revision").description("The revision of the moderation request (available with `allDetails`).").optional().type(JsonFieldType.STRING),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestamp").description("Timestamp (in unix epoch) when the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestampOfDecision").description("Timestamp (in unix epoch) when the decision on the request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentId").description("The ID of the document for which the moderation request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentType").description("Type of the document. Possible values are: " + Arrays.asList(DocumentType.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUser").description("The user who created the moderation request."),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderators.[]").description("List of users who are marked as moderators for the request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentName").description("Name of the document for which the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]moderationState").description("The state of the moderation request. Possible values are: " + Arrays.asList(ModerationState.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]reviewer").description("User who is currently assigned as the reviewer of the request (available with `allDetails`).").optional().type(JsonFieldType.STRING),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestDocumentDelete").description("Request document deletion (available with `allDetails`).").optional().type(JsonFieldType.BOOLEAN),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUserDepartment").description("The Business Unit / Group of the Project, for which clearing request is created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]componentType").description("Type of the component for which the moderation request is created. Possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]commentRequestingUser").description("The comment from requesting user (available with `allDetails`).").optional().type(JsonFieldType.STRING),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]commentDecisionModerator").description("The comment from decision making user (available with `allDetails`).").optional().type(JsonFieldType.STRING),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]componentAdditions").description("Information which user wants to add for the component (if `documentType` is `COMPONENT`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]releaseAdditions").description("Information which user wants to add for the release (if `documentType` is `RELEASE`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]projectAdditions").description("Information which user wants to add for the project (if `documentType` is `PROJECT`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]licenseAdditions").description("Information which user wants to add for the license (if `documentType` is `LICENSE`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]user").description("").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]componentDeletions").description("Information which user wants to remove for the component (if `documentType` is `COMPONENT`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]releaseDeletions").description("Information which user wants to remove for the release (if `documentType` is `RELEASE`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]projectDeletions").description("Information which user wants to remove for the project (if `documentType` is `PROJECT`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]licenseDeletions").description("Information which user wants to remove for the license (if `documentType` is `LICENSE`) (available with `allDetails`).").optional().type(JsonFieldType.OBJECT),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderatorsSize").description("Number of users in moderators list."),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing moderation requests"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page"),
                                subsectionWithPath("_links").description("Link to <<resources-moderationRequest, ModerationRequest resource>>")
                        )));
    }

    @Test
    public void should_document_get_moderationrequests_accept() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        ModerationPatch patch = new ModerationPatch();
        patch.setAction(ModerationPatch.ModerationAction.ACCEPT);
        patch.setComment("Changes looks good.");

        mockMvc.perform(patch("/api/moderationrequest/MR-101")
                        .header("Authorization", "Bearer " + accessToken)
                        .content(this.objectMapper.writeValueAsString(patch))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("action").description("Action to perform on the moderation request. Possible values are: `" + List.of(ModerationPatch.ModerationAction.ACCEPT, ModerationPatch.ModerationAction.REJECT) + '`'),
                                fieldWithPath("comment").description("Comment on the action from reviewer.")
                        ),
                        responseFields(
                                fieldWithPath("status").description("New status of the moderation request. Possible values are: `" + List.of(ModerationState.APPROVED, ModerationState.REJECTED) + '`'),
                                subsectionWithPath("_links").description("Link to current <<resources-moderationRequest, ModerationRequest resource>>")
                        )));
    }

    @Test
    public void should_document_get_moderationrequests_assign() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        ModerationPatch patch = new ModerationPatch();
        patch.setAction(ModerationPatch.ModerationAction.ASSIGN);

        mockMvc.perform(patch("/api/moderationrequest/MR-101")
                        .header("Authorization", "Bearer " + accessToken)
                        .content(this.objectMapper.writeValueAsString(patch))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isAccepted())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("action").description("Action to perform on the moderation request. Possible values are: `" + List.of(ModerationPatch.ModerationAction.ASSIGN, ModerationPatch.ModerationAction.UNASSIGN) + '`')
                        ),
                        responseFields(
                                fieldWithPath("status").description("`" + ModerationState.PENDING + "` if unassigned, `" + ModerationState.INPROGRESS + "` if assigned. Exception thrown in case of errors."),
                                subsectionWithPath("_links").description("Link to current <<resources-moderationRequest, ModerationRequest resource>>")
                        )));
    }

    @Test
    public void should_document_get_moderationrequests_submission() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/moderationrequest/mySubmissions")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("page_entries", "5")
                        .param("sort", "documentName,asc")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("page").description("Page of moderation requests"),
                                parameterWithName("page_entries").description("Amount of requests per page"),
                                parameterWithName("sort").description("Sort the result by the given field and order. " +
                                        "Possible values are: `documentName`, `timestamp` and `moderationState`.")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:moderationRequests").description("An array of <<resources-moderationRequest, ModerationRequest>>."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]id").description("The id of the moderation request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestamp").description("Timestamp (in unix epoch) when the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]timestampOfDecision").description("Timestamp (in unix epoch) when the decision on the request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentId").description("The ID of the document for which the moderation request was made."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentType").description("Type of the document. Possible values are: " + Arrays.asList(DocumentType.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUser").description("The user who created the moderation request."),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderators.[]").description("List of users who are marked as moderators for the request."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]documentName").description("Name of the document for which the request was created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]moderationState").description("The state of the moderation request. Possible values are: " + Arrays.asList(ModerationState.values())),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]requestingUserDepartment").description("The Business Unit / Group of the Project, for which clearing request is created."),
                                fieldWithPath("_embedded.sw360:moderationRequests.[]componentType").description("Type of the component for which the moderation request is created. Possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:moderationRequests.[]moderatorsSize").description("Number of users in moderators list."),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing moderation requests"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
