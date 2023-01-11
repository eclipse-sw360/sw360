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

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.clearingrequest.Sw360ClearingRequestService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ClearingRequestSpecTest extends TestRestDocsSpecBase {

	@Value("${sw360.test-user-id}")
	private String testUserId;

	@Value("${sw360.test-user-password}")
	private String testUserPassword;

	@MockBean
	private Sw360UserService userServiceMock;

	@MockBean
	private Sw360ProjectService projectServiceMock;

	@MockBean
	private Sw360ClearingRequestService clearingRequestServiceMock;

	@Before
	public void before() throws TException, IOException {
		ClearingRequest clearingRequest = new ClearingRequest();
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

		Set<ClearingRequest> clearingrequests = new HashSet<>();
		Set<ClearingRequest> clearingrequestsbystate = new HashSet<>();
		ClearingRequest cr1 = new ClearingRequest();
		cr1.setId("CR-2");
		cr1.setAgreedClearingDate("12-10-2020");
		cr1.setClearingState(ClearingRequestState.ACCEPTED);
		cr1.setPriority(ClearingRequestPriority.HIGH);
		cr1.setClearingTeam("clearing.team@sw60.org");
		cr1.setProjectBU("DEP");
		cr1.setProjectId("54121");
		cr1.setRequestedClearingDate("10-08-2020");
		cr1.setRequestingUser("test.user@sw60.org");

		ClearingRequest cr2 = new ClearingRequest();
		cr2.setId("CR-3");
		cr2.setAgreedClearingDate("24-10-2020");
		cr2.setClearingState(ClearingRequestState.NEW);
		cr2.setPriority(ClearingRequestPriority.LOW);
		cr2.setClearingTeam("clearing.team@sw60.org");
		cr2.setProjectBU("DEP");
		cr2.setProjectId("54181");
		cr2.setRequestedClearingDate("13-09-2020");
		cr2.setRequestingUser("test.admin@sw60.org");

		clearingrequests.add(cr1);
		clearingrequests.add(cr2);
		clearingrequestsbystate.add(cr2);

		Project project = new Project();
		project.setId(clearingRequest.getProjectId());
		project.setName("Alpha");
		project.setVersion("1.0");
		project.setProjectType(ProjectType.CUSTOMER);
		project.setVisbility(Visibility.EVERYONE);
		project.setBusinessUnit("DEPT");

		given(this.projectServiceMock.getProjectForUserById(eq(clearingRequest.getProjectId()), any()))
				.willReturn(project);
		given(this.userServiceMock.getUserByEmail(clearingRequest.getRequestingUser()))
				.willReturn(new User("test.admin@sw360.org", "DEPT").setId("12345"));
		given(this.userServiceMock.getUserByEmail(clearingRequest.getClearingTeam()))
				.willReturn(new User("clearing.team@sw60.org", "XYZ").setId("67890"));

		List<Comment> comments = new ArrayList<Comment>();
		Comment comment = new Comment();
		comment.setText("comment text 1");
		comment.setCommentedBy("test.user@sw360.org");

		comments.add(comment);
		clearingRequest.setComments(comments);

		given(this.clearingRequestServiceMock.getClearingRequestById(eq(clearingRequest.getId()), any()))
				.willReturn(clearingRequest);
		given(this.clearingRequestServiceMock.getClearingRequestByProjectId(eq(clearingRequest.getProjectId()), any()))
				.willReturn(clearingRequest);
		given(this.clearingRequestServiceMock.getMyClearingRequests(any(), eq(null))).willReturn(clearingrequests);
		given(this.clearingRequestServiceMock.getMyClearingRequests(any(), eq(ClearingRequestState.NEW)))
				.willReturn(clearingrequestsbystate);
	}

	@Test
	public void should_document_get_clearingrequest() throws Exception {
		String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
		mockMvc.perform(get("/api/clearingrequest/CR-101")
				.header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseFields(
						fieldWithPath("id").description("The id of the clearing request"),
						fieldWithPath("agreedClearingDate").description(
								"The agreed clearing date of the request, on / before which CR should be cleared"),
						fieldWithPath("clearingState")
								.description("The clearing state of request. Possible values are:  "
										+ Arrays.asList(ClearingRequestState.values())),
						fieldWithPath("clearingTeam").description("The clearing team email id."),
						fieldWithPath("projectBU").description(
								"The Business Unit / Group of the Project, for which clearing request is created"),
						fieldWithPath("projectId")
								.description("The id of the Project, for which clearing request is created"),
						fieldWithPath("requestedClearingDate").description("The requested clearing date of releases"),
						fieldWithPath("requestingUser").description("The user who created the clearing request"),
						fieldWithPath("requestingUserComment").description("The comment from requesting user"),
						fieldWithPath("priority").description("The priority of clearing request. Possible values are:  "
								+ Arrays.asList(ClearingRequestPriority.values())),
						subsectionWithPath("comments.[]").description("The clearing request comment"),
						subsectionWithPath("comments.[]text").description("The clearing request comment text"),
						subsectionWithPath("comments.[]commentedBy")
								.description("The user who added the comment on clearing request"),
						subsectionWithPath("_embedded.sw360:project")
								.description("<<resources-projects, Project>> associated with the ClearingRequest"),
						subsectionWithPath("_embedded.clearingTeam").description("clearing team user detail"),
						subsectionWithPath("_embedded.requestingUser").description("requesting user, user detail"),
						subsectionWithPath("_links")
								.description("<<resources-index-links,Links>> to other resources"))));
	}

	@Test
	public void should_document_get_clearingrequest_by_projectid() throws Exception {
		String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
		mockMvc.perform(get("/api/clearingrequest/project/543210")
				.header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseFields(
						fieldWithPath("id").description("The id of the clearing request"),
						fieldWithPath("agreedClearingDate").description(
								"The agreed clearing date of the request, on / before which CR should be cleared"),
						fieldWithPath("clearingState")
								.description("The clearing state of request. Possible values are:  "
										+ Arrays.asList(ClearingRequestState.values())),
						fieldWithPath("clearingTeam").description("The clearing team email id."),
						fieldWithPath("projectBU").description(
								"The Business Unit / Group of the Project, for which clearing request is created"),
						fieldWithPath("projectId")
								.description("The id of the Project, for which clearing request is created"),
						fieldWithPath("requestedClearingDate").description("The requested clearing date of releases"),
						fieldWithPath("requestingUser").description("The user who created the clearing request"),
						fieldWithPath("requestingUserComment").description("The comment from requesting user"),
						fieldWithPath("priority").description("The priority of clearing request. Possible values are:  "
								+ Arrays.asList(ClearingRequestPriority.values())),
						subsectionWithPath("comments.[]").description("The clearing request comment"),
						subsectionWithPath("comments.[]text").description("The clearing request comment text"),
						subsectionWithPath("comments.[]commentedBy")
								.description("The user who added the comment on clearing request"),
						subsectionWithPath("_embedded.sw360:project")
								.description("<<resources-projects, Project>> associated with the ClearingRequest"),
						subsectionWithPath("_embedded.clearingTeam").description("clearing team user detail"),
						subsectionWithPath("_embedded.requestingUser").description("requesting user, user detail"),
						subsectionWithPath("_links")
								.description("<<resources-index-links,Links>> to other resources"))));
	}

	@Test
	public void should_document_get_clearingrequests() throws Exception {
		String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
		mockMvc.perform(get("/api/clearingrequests")
				.header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseFields(
						subsectionWithPath("_embedded.sw360:clearingRequests.[]id")
								.description("The id of the clearing request"),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]agreedClearingDate")
								.description("The agreed clearing date of the request"),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingState")
								.description("The clearing state of request. Possible values are:  "
										+ Arrays.asList(ClearingRequestState.values())),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingTeam")
								.description("The clearing team email id."),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]projectBU").description(
								"The Business Unit / Group of the Project, for which clearing request is created"),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]projectId")
								.description("The id of the Project, for which clearing request is created"),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]requestedClearingDate")
								.description("The requested clearing date of releases"),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]requestingUser")
								.description("The user who created the clearing request"),
						subsectionWithPath("_embedded.sw360:clearingRequests.[]priority")
								.description("The priorityof clearing request. Possible values are:  "
										+ Arrays.asList(ClearingRequestPriority.values())),
						subsectionWithPath("_embedded.sw360:clearingRequests")
								.description("An array of <<resources-clearingRequest, ClearingRequests>>"),
						subsectionWithPath("_links")
								.description("Link to <<resources-clearingRequest, ClearingRequest resource>>")

				)));
	}

	@Test
	public void should_document_get_clearingrequests_by_state() throws Exception {
		String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
		mockMvc.perform(get("/api/clearingrequests")
				.header("Authorization", "Bearer " + accessToken).param("state", "NEW").accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(parameterWithName("state")
								.description("The clearing request state of the request. Possible values are:  "
										+ Arrays.asList(ClearingRequestState.values()))),
						responseFields(
								subsectionWithPath("_embedded.sw360:clearingRequests.[]id")
										.description("The id of the clearing request"),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]agreedClearingDate")
										.description("The agreed clearing date of the request"),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingState")
										.description("The clearing state of request. Possible values are:  "
												+ Arrays.asList(ClearingRequestState.values())),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]clearingTeam")
										.description("The clearing team email id."),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]projectBU").description(
										"The Business Unit / Group of the Project, for which clearing request is created"),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]projectId")
										.description("The id of the Project, for which clearing request is created"),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]requestedClearingDate")
										.description("The requested clearing date of releases"),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]requestingUser")
										.description("The user who created the clearing request"),
								subsectionWithPath("_embedded.sw360:clearingRequests.[]priority")
										.description("The priorityof clearing request. Possible values are:  "
												+ Arrays.asList(ClearingRequestPriority.values())),
								subsectionWithPath("_embedded.sw360:clearingRequests")
										.description("An array of <<resources-clearingRequest, ClearingRequests>>"),
								subsectionWithPath("_links")
										.description("Link to <<resources-clearingRequest, ClearingRequest resource>>")

						)));
	}

}
