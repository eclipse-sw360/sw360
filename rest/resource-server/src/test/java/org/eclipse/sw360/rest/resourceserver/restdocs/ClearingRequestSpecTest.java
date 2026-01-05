/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.eclipse.sw360.datahandler.thrift.MainlineState.MAINLINE;
import static org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.CONTAINED;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.*;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.clearingrequest.Sw360ClearingRequestService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ClearingRequestSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360ProjectService projectServiceMock;

    @MockitoBean
    private Sw360ClearingRequestService clearingRequestServiceMock;

    ClearingRequest clearingRequest = new ClearingRequest();
    ClearingRequest cr1 = new ClearingRequest();
    ClearingRequest cr2 = new ClearingRequest();
    List<Comment> comments = new ArrayList<Comment>();

    @Before
    public void before() throws TException, IOException {

        clearingRequest.setId("CR-101");
        clearingRequest.setAgreedClearingDate("12-07-2020");
        clearingRequest.setClearingState(ClearingRequestState.ACCEPTED);
        clearingRequest.setPriority(ClearingRequestPriority.MEDIUM);
        clearingRequest.setClearingTeam("clearing.team@sw60.org");
        clearingRequest.setProjectBU("DEPT");
        clearingRequest.setProjectId("543210");
        clearingRequest.setRequestedClearingDate("10-07-2020");
        clearingRequest.setRequestingUser("test.admin@sw60.org");
        clearingRequest.setRequestingUserComment("testing comment");
        clearingRequest.setClearingType(ClearingRequestType.DEEP);
        clearingRequest.setTimestamp(1599285578);
        clearingRequest.setModifiedOn(1599285580);

        Set<ClearingRequest> clearingrequests = new HashSet<>();
        Set<ClearingRequest> clearingrequestsbystate = new HashSet<>();

        cr1.setId("CR-2");
        cr1.setAgreedClearingDate("12-10-2020");
        cr1.setClearingState(ClearingRequestState.ACCEPTED);
        cr1.setPriority(ClearingRequestPriority.HIGH);
        cr1.setClearingTeam("clearing.team@sw60.org");
        cr1.setProjectBU("DEP");
        cr1.setProjectId("54121");
        cr1.setRequestedClearingDate("10-08-2020");
        cr1.setRequestingUser("test.user@sw60.org");
        cr1.setClearingType(ClearingRequestType.HIGH);
        cr1.setTimestamp(1599285573);
        cr1.setModifiedOn(1599285590);

        cr2.setId("CR-3");
        cr2.setAgreedClearingDate("24-10-2020");
        cr2.setClearingState(ClearingRequestState.NEW);
        cr2.setPriority(ClearingRequestPriority.LOW);
        cr2.setClearingTeam("clearing.team@sw60.org");
        cr2.setProjectBU("DEP");
        cr2.setProjectId("54181");
        cr2.setRequestedClearingDate("13-09-2020");
        cr2.setRequestingUser("test.admin@sw60.org");
        cr2.setClearingType(ClearingRequestType.DEEP);
        cr2.setTimestamp(1599285571);
        cr2.setModifiedOn(1599285588);

        clearingrequests.add(cr1);
        clearingrequests.add(cr2);
        clearingrequestsbystate.add(cr2);

        Map<String, ProjectReleaseRelationship> linkedReleases = new HashMap<>();
        ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship(CONTAINED, MAINLINE)
                .setComment("Test Comment").setCreatedOn("2020-08-05").setCreatedBy("admin@sw360.org");

        Project project = new Project();
        project.setId(clearingRequest.getProjectId());
        project.setName("Alpha");
        project.setVersion("1.0");
        project.setProjectType(ProjectType.CUSTOMER);
        project.setVisbility(Visibility.EVERYONE);
        project.setBusinessUnit("DEPT");

        Project project1 = new Project();
        project.setId(cr1.getProjectId());
        project.setName("Beta");
        project.setVersion("1.0");
        project.setProjectType(ProjectType.CUSTOMER);
        project.setVisbility(Visibility.EVERYONE);
        project.setBusinessUnit("DEPT");

        Project project2 = new Project();
        project.setId(cr2.getProjectId());
        project.setName("Delta");
        project.setVersion("1.0");
        project.setProjectType(ProjectType.CUSTOMER);
        project.setVisbility(Visibility.EVERYONE);
        project.setBusinessUnit("DEPT");

        ReleaseClearingStateSummary clearingCount = new ReleaseClearingStateSummary();
        clearingCount.newRelease = 2;
        clearingCount.sentToClearingTool = 1;
        clearingCount.underClearing = 0;
        clearingCount.reportAvailable = 0;
        clearingCount.scanAvailable = 0;
        clearingCount.internalUseScanAvailable = 1;
        clearingCount.approved = 2;

        ReleaseClearingStateSummary clearingCount1 = new ReleaseClearingStateSummary();
        clearingCount1.newRelease = 3;
        clearingCount1.sentToClearingTool = 1;
        clearingCount1.underClearing = 0;
        clearingCount1.reportAvailable = 0;
        clearingCount1.scanAvailable = 0;
        clearingCount1.internalUseScanAvailable = 1;
        clearingCount1.approved = 2;

        project.setReleaseClearingStateSummary(clearingCount);

        project1.setReleaseClearingStateSummary(clearingCount1);
        project2.setReleaseClearingStateSummary(clearingCount);

        linkedReleases.put("3765276512", projectReleaseRelationship);
        linkedReleases.put("3765276513", projectReleaseRelationship);
        linkedReleases.put("3765276514", projectReleaseRelationship);

        project.setReleaseIdToUsage(linkedReleases);
        project1.setReleaseIdToUsage(linkedReleases);
        project2.setReleaseIdToUsage(linkedReleases);

        given(this.projectServiceMock.getProjectForUserById(eq(clearingRequest.getProjectId()), any())).willReturn(project);
        given(this.userServiceMock.getUserByEmail(clearingRequest.getRequestingUser())).willReturn(new User("test.admin@sw360.org", "DEPT").setId("12345"));
        given(this.userServiceMock.getUserByEmail(clearingRequest.getClearingTeam())).willReturn(new User("clearing.team@sw60.org", "XYZ").setId("67890"));
        given(this.projectServiceMock.getClearingInfo(eq(project), any())).willReturn(project);

        given(this.projectServiceMock.getProjectForUserById(eq(cr1.getProjectId()), any())).willReturn(project1);
        given(this.userServiceMock.getUserByEmail(cr1.getRequestingUser())).willReturn(new User("test.admin@sw360.org", "DEPT").setId("12345"));
        given(this.userServiceMock.getUserByEmail(cr1.getClearingTeam())).willReturn(new User("clearing.team@sw60.org", "XYZ").setId("67890"));
        given(this.projectServiceMock.getClearingInfo(eq(project1), any())).willReturn(project1);
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789").setUserGroup(UserGroup.ADMIN));

        given(this.projectServiceMock.getProjectForUserById(eq(cr2.getProjectId()), any())).willReturn(project2);
        given(this.userServiceMock.getUserByEmail(cr2.getRequestingUser())).willReturn(new User("test.admin@sw360.org", "DEPT").setId("12345"));
        given(this.userServiceMock.getUserByEmail(cr2.getClearingTeam())).willReturn(new User("clearing.team@sw60.org", "XYZ").setId("67890"));
        given(this.projectServiceMock.getClearingInfo(eq(project2), any())).willReturn(project2);

        Comment comment = new Comment();
        comment.setText("comment text 1");
        comment.setCommentedBy("test.admin@sw60.org");

        Comment comment1 = new Comment();
        comment1.setText("comment text 2");
        comment1.setCommentedBy("test.admin@sw60.org");

        Comment comment2 = new Comment();
        comment2.setText("comment text 3");
        comment2.setCommentedBy("test.admin@sw60.org");

        comments.add(comment);
        clearingRequest.setComments(comments);
        given(this.clearingRequestServiceMock.addCommentToClearingRequest(eq(clearingRequest.getId()), any(), any())).willReturn(clearingRequest);

        given(this.clearingRequestServiceMock.getClearingRequestById(eq(clearingRequest.getId()), any())).willReturn(clearingRequest);
        given(this.clearingRequestServiceMock.getClearingRequestByProjectId(eq(clearingRequest.getProjectId()), any())).willReturn(clearingRequest);
        given(this.clearingRequestServiceMock.getRecentClearingRequestsWithPagination(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(clearingrequests.size()).setDisplayStart(0).setTotalRowCount(clearingrequests.size()),
                        new ArrayList<>(clearingrequests)
                )
        );
        given(this.clearingRequestServiceMock.searchClearingRequestsByFilters(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(clearingrequestsbystate.size()).setDisplayStart(0).setTotalRowCount(clearingrequestsbystate.size()),
                        new ArrayList<>(clearingrequestsbystate)
                )
        );
    }

    @Test
    public void should_document_get_clearingrequest() throws Exception {
        mockMvc.perform(get("/api/clearingrequest/CR-101")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("id").description("The id of the clearing request"),
                                fieldWithPath("agreedClearingDate").description("The agreed clearing date of the request, on / before which CR should be cleared"),
                                fieldWithPath("clearingState").description("The clearing state of request. Possible values are:  " + Arrays.asList(ClearingRequestState.values())),
                                fieldWithPath("clearingTeam").description("The clearing team email id."),
                                fieldWithPath("projectBU").description("The Business Unit / Group of the Project, for which clearing request is created"),
                                fieldWithPath("projectId").description("The id of the Project, for which clearing request is created"),
                                fieldWithPath("requestedClearingDate").description("The requested clearing date of releases"),
                                fieldWithPath("clearingType").description("The clearing type of the request, e.g., DEEP."),
                                fieldWithPath("requestingUser").description("The user who created the clearing request"),
                                fieldWithPath("requestingUserComment").description("The comment from requesting user"),
                                fieldWithPath("priority").description("The priority of clearing request. Possible values are:  " + Arrays.asList(ClearingRequestPriority.values())),
                                subsectionWithPath("comments.[]").description("The clearing request comment"),
                                subsectionWithPath("comments.[]text").description("The clearing request comment text"),
                                subsectionWithPath("comments.[]commentedBy").description("The user who added the comment on clearing request"),
                                subsectionWithPath("comments.[].commentedOn").description("The timestamp when the comment was added."),
                                subsectionWithPath("comments.[].autoGenerated").description("Indicates if the comment was generated automatically."),
                                subsectionWithPath("_embedded.sw360:projectDTOs").description("An array of <<resources-projects, Projects>> associated with the ClearingRequest"),
                                subsectionWithPath("_embedded.clearingTeam").description("clearing team user detail"),
                                subsectionWithPath("_embedded.requestingUser").description("requesting user, user detail"),
                                subsectionWithPath("_embedded.totalRelease").description("The total number of releases for the project."),
                                subsectionWithPath("_embedded.lastUpdatedOn").description("The last updated date of the clearing request."),
                                subsectionWithPath("_embedded.createdOn").description("The creation date of the clearing request."),
                                subsectionWithPath("_embedded.openRelease").description("requesting user, user detail"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_clearingrequest_by_projectid() throws Exception {
        mockMvc.perform(get("/api/clearingrequest/project/543210")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("id").description("The id of the clearing request"),
                                fieldWithPath("agreedClearingDate").description("The agreed clearing date of the request, on / before which CR should be cleared"),
                                fieldWithPath("clearingState").description("The clearing state of request. Possible values are:  " + Arrays.asList(ClearingRequestState.values())),
                                fieldWithPath("clearingTeam").description("The clearing team email id."),
                                fieldWithPath("projectBU").description("The Business Unit / Group of the Project, for which clearing request is created"),
                                fieldWithPath("projectId").description("The id of the Project, for which clearing request is created"),
                                fieldWithPath("requestedClearingDate").description("The requested clearing date of releases"),
                                fieldWithPath("clearingType").description("The clearing type of the request, e.g., DEEP."),
                                fieldWithPath("requestingUser").description("The user who created the clearing request"),
                                fieldWithPath("requestingUserComment").description("The comment from requesting user"),
                                fieldWithPath("priority").description("The priority of clearing request. Possible values are:  " + Arrays.asList(ClearingRequestPriority.values())),
                                subsectionWithPath("comments.[]").description("The clearing request comment"),
                                subsectionWithPath("comments.[]text").description("The clearing request comment text"),
                                subsectionWithPath("comments.[]commentedBy").description("The user who added the comment on clearing request"),
                                subsectionWithPath("comments.[].commentedOn").description("The timestamp when the comment was added."),
                                subsectionWithPath("comments.[].autoGenerated").description("Indicates if the comment was generated automatically."),
                                subsectionWithPath("_embedded.sw360:projectDTOs").description("An array of <<resources-projects, Projects>> associated with the ClearingRequest"),
                                subsectionWithPath("_embedded.clearingTeam").description("clearing team user detail"),
                                subsectionWithPath("_embedded.requestingUser").description("requesting user, user detail"),
                                subsectionWithPath("_embedded.totalRelease").description("The total number of releases for the project."),
                                subsectionWithPath("_embedded.lastUpdatedOn").description("The last updated date of the clearing request."),
                                subsectionWithPath("_embedded.createdOn").description("The creation date of the clearing request."),
                                subsectionWithPath("_embedded.openRelease").description("requesting user, user detail"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_clearingrequests() throws Exception {
        mockMvc.perform(get("/api/clearingrequests")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]id").description("The id of the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]agreedClearingDate").description("The agreed clearing date of the request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingState").description("The clearing state of request. Possible values are:  " + Arrays.asList(ClearingRequestState.values())),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingTeam").description("The clearing team email id."),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]projectBU").description("The Business Unit / Group of the Project, for which clearing request is created"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]projectId").description("The id of the Project, for which clearing request is created"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]requestedClearingDate").description("The requested clearing date of releases"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]requestingUser").description("The user who created the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]priority").description("The priority of clearing request. Possible values are:  " + Arrays.asList(ClearingRequestPriority.values())),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.totalRelease").description("Total number of releases associated with the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.openRelease").description("Number of open releases associated with the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.createdOn").description("The date when the clearing request was created"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.requestingUser").description("The user who created the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests").description("An array of <<resources-clearingRequest, ClearingRequests>>"),
                                subsectionWithPath("_links").description("Link to <<resources-clearingRequest, ClearingRequest resource>>"),
                                fieldWithPath("page").description("Additional paging information for the clearing requests."),
                                fieldWithPath("page.size").description("Number of Clearing requests per page."),
                                fieldWithPath("page.totalElements").description("Total number of clearing requests available."),
                                fieldWithPath("page.totalPages").description("Total number of pages available."),
                                fieldWithPath("page.number").description("Current page number.")

                        )));
    }

    @Test
    public void should_document_get_clearingrequests_by_status() throws Exception {
        mockMvc.perform(get("/api/clearingrequests")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("status", "NEW")
                .queryParam("page", "0")
                .queryParam("page_entries", "2")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("status").description("The clearing request status to filter. Possible values are:  " + Arrays.asList(ClearingRequestState.values())),
                                parameterWithName("page").description("The page number for pagination."),
                                parameterWithName("page_entries").description("The number of clearing requests per page.")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]id").description("The id of the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]agreedClearingDate").description("The agreed clearing date of the request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingState").description("The clearing state of request. Possible values are:  " + Arrays.asList(ClearingRequestState.values())),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingTeam").description("The clearing team email id."),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]projectBU").description("The Business Unit / Group of the Project, for which clearing request is created"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]projectId").description("The id of the Project, for which clearing request is created"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]requestedClearingDate").description("The requested clearing date of releases"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]requestingUser").description("The user who created the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]priority").description("The priority of clearing request. Possible values are:  " + Arrays.asList(ClearingRequestPriority.values())),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.totalRelease").description("Total number of releases associated with the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.openRelease").description("Number of open releases associated with the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.createdOn").description("The date when the clearing request was created"),
                                subsectionWithPath("_embedded.sw360:clearingRequests.[]_embedded.requestingUser").description("The user who created the clearing request"),
                                subsectionWithPath("_embedded.sw360:clearingRequests").description("An array of <<resources-clearingRequest, ClearingRequests>>"),
                                subsectionWithPath("_links").description("Link to <<resources-clearingRequest, ClearingRequest resource>>"),
                                fieldWithPath("page").description("Additional paging information for the clearing requests."),
                                fieldWithPath("page.size").description("Number of Clearing requests per page."),
                                fieldWithPath("page.totalElements").description("Total number of clearing requests available."),
                                fieldWithPath("page.totalPages").description("Total number of pages available."),
                                fieldWithPath("page.number").description("Current page number.")
                        )));
    }

    @Test
    public void should_document_get_comments_by_clearing_request_id() throws Exception {
        mockMvc.perform(get("/api/clearingrequest/" + clearingRequest.getId() +"/comments")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("page", "0")
                        .queryParam("page_entries", "5")
                        .queryParam("sort", "commentedOn,desc")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page number of the comments list."),
                                parameterWithName("page_entries").description("Number of comments per page."),
                                parameterWithName("sort").description("Defines the order of the comments, e.g., 'commentedOn,desc' for sorting by commentedOn in descending order.")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:comments.[].text").description("The text of the comment."),
                                subsectionWithPath("_embedded.sw360:comments.[].commentedBy").description("The user who added the comment."),
                                subsectionWithPath("_embedded.sw360:comments.[].commentedOn").description("The timestamp when the comment was added."),
                                subsectionWithPath("_embedded.sw360:comments.[].autoGenerated").description("Indicates if the comment was generated automatically."),
                                subsectionWithPath("_embedded.sw360:comments[]._embedded.commentingUser").description("Details of the user who made the comment"),
                                subsectionWithPath("_embedded.sw360:comments").description("An array of comments related to the clearing request."),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information for the comments."),
                                fieldWithPath("page.size").description("Number of comments per page."),
                                fieldWithPath("page.totalElements").description("Total number of comments available."),
                                fieldWithPath("page.totalPages").description("Total number of pages available."),
                                fieldWithPath("page.number").description("Current page number.")
                        )));
    }

    public void should_add_comment_to_clearing_request() throws Exception {
        Map<String, Object> comment = new LinkedHashMap<>();
        comment.put("text", "comment text 1");
        String accessToken = TestHelper.generateAuthHeader(testUserId, testUserPassword);

        this.mockMvc.perform(
                        post("/api/clearingrequest/" + clearingRequest.getId() + "/comments")
                                .contentType(MediaTypes.HAL_JSON)
                                .content(this.objectMapper.writeValueAsString(comment))
                                .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("text").description("The text of the comment")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:comments[].text").description("The text of the comment"),
                                fieldWithPath("_embedded.sw360:comments[].commentedBy").description("The user who made the comment"),
                                fieldWithPath("_embedded.sw360:comments[].commentedOn").description("Timestamp when the comment was made"),
                                subsectionWithPath("_embedded.sw360:comments[]._embedded.commentingUser").description("Details of the user who made the comment"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )
                ));
    }

    @Test
    public void should_document_patch_clearingrequest () throws Exception {
        ClearingRequest updateClearingRequest = new ClearingRequest()
                .setClearingTeam("clearing.team@sw60.org")
                .setClearingState(ClearingRequestState.SANITY_CHECK);

        mockMvc.perform(patch("/api/clearingrequest/" + clearingRequest.getId())
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateClearingRequest))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("clearingTeam").description("The clearing team email id."),
                                fieldWithPath("clearingState").description("The clearing state of request")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the clearing request"),
                                fieldWithPath("agreedClearingDate").description("The agreed clearing date of the request, on / before which CR should be cleared"),
                                fieldWithPath("clearingState").description("The clearing state of the request. Possible values are: " + Arrays.asList(ClearingRequestState.values())),
                                fieldWithPath("clearingTeam").description("The clearing team email id."),
                                fieldWithPath("projectBU").description("The Business Unit / Group of the Project, for which the clearing request is created"),
                                fieldWithPath("projectId").description("The id of the Project, for which the clearing request is created"),
                                fieldWithPath("requestedClearingDate").description("The requested clearing date of releases"),
                                fieldWithPath("requestingUser").description("The user who created the clearing request"),
                                fieldWithPath("requestingUserComment").description("The comment from the requesting user"),
                                fieldWithPath("priority").description("The priority of the clearing request. Possible values are: " + Arrays.asList(ClearingRequestPriority.values())),
                                subsectionWithPath("comments").description("The clearing request comments"),
                                subsectionWithPath("comments[].text").description("The clearing request comment text"),
                                subsectionWithPath("comments[].commentedBy").description("The user who added the comment on the clearing request"),
                                subsectionWithPath("_embedded.sw360:projectDTOs").description("An array of <<resources-projects, Projects>> associated with the ClearingRequest"),
                                subsectionWithPath("_embedded.clearingTeam").description("Clearing team user detail"),
                                subsectionWithPath("_embedded.requestingUser").description("Requesting user detail"),
                                subsectionWithPath("_links").description("Links to other resources"),
                                fieldWithPath("clearingType").description("The type of clearing, e.g., DEEP"),
                                fieldWithPath("_embedded.totalRelease").description("Total number of releases"),
                                fieldWithPath("_embedded.openRelease").description("Number of open releases"),
                                fieldWithPath("_embedded.lastUpdatedOn").description("Last updated date for the clearing request"),
                                fieldWithPath("_embedded.createdOn").description("Creation date for the clearing request")

                        )));
    }
}
