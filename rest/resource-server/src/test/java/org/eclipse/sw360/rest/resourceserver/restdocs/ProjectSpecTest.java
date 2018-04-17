/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.eclipse.sw360.datahandler.thrift.MainlineState.MAINLINE;
import static org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.CONTAINED;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ProjectService projectServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    private Project project;

    @Before
    public void before() throws TException {

        Map<String, ProjectReleaseRelationship> linkedReleases = new HashMap<>();
        Map<String, ProjectRelationship> linkedProjects = new HashMap<>();
        ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship(CONTAINED, MAINLINE);

        List<Project> projectList = new ArrayList<>();
        List<Project> projectListByName = new ArrayList<>();
        project = new Project();
        project.setId("376576");
        project.setName("Emerald Web");
        project.setProjectType(ProjectType.PRODUCT);
        project.setVersion("1.0.2");
        project.setDescription("Emerald Web provides a suite of components for Critical Infrastructures.");
        project.setCreatedOn("2016-12-15");
        project.setCreatedBy("admin@sw360.org");
        project.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        project.setBusinessUnit("sw360 AR");
        project.setExternalIds(Collections.singletonMap("mainline-id-project", "515432"));
        project.setOwnerAccountingUnit("4822");
        project.setOwnerCountry("DE");
        project.setOwnerGroup("AA BB 123 GHV2-DE");
        project.setTag("project test tag 1");
        project.setPreevaluationDeadline("2018-07-17");
        project.setSystemTestStart("2017-01-01");
        project.setSystemTestEnd("2018-03-01");
        linkedReleases.put("3765276512", projectReleaseRelationship);
        project.setReleaseIdToUsage(linkedReleases);
        linkedProjects.put("376576", ProjectRelationship.CONTAINED);
        project.setLinkedProjects(linkedProjects);
        projectList.add(project);
        projectListByName.add(project);

        Project project2 = new Project();
        project2.setId("376570");
        project2.setName("Orange Web");
        project2.setVersion("2.0.1");
        project2.setProjectType(ProjectType.PRODUCT);
        project2.setDescription("Orange Web provides a suite of components for documentation.");
        project2.setCreatedOn("2016-12-17");
        project2.setCreatedBy("john@sw360.org");
        project2.setBusinessUnit("sw360 EX DF");
        project2.setOwnerAccountingUnit("5661");
        project2.setOwnerCountry("FR");
        project2.setOwnerGroup("SIM-KA12");
        project2.setTag("project test tag 2");
        project2.setPreevaluationDeadline("2018-07-17");
        project2.setSystemTestStart("2017-01-01");
        project2.setSystemTestEnd("2018-03-01");
        project2.setExternalIds(Collections.singletonMap("mainline-id-project", "7657"));
        linkedReleases = new HashMap<>();
        linkedReleases.put("5578999", projectReleaseRelationship);
        project2.setReleaseIdToUsage(linkedReleases);
        projectList.add(project2);

        Set<String> releaseIds = new HashSet<>(Arrays.asList("3765276512"));
        Set<String> releaseIdsTransitive = new HashSet<>(Arrays.asList("3765276512", "5578999"));

        given(this.projectServiceMock.getProjectsForUser(anyObject())).willReturn(projectList);
        given(this.projectServiceMock.getProjectForUserById(eq(project.getId()), anyObject())).willReturn(project);
        given(this.projectServiceMock.searchProjectByName(eq(project.getName()), anyObject())).willReturn(projectListByName);
        given(this.projectServiceMock.getReleaseIds(eq(project.getId()), anyObject(), eq("false"))).willReturn(releaseIds);
        given(this.projectServiceMock.getReleaseIds(eq(project.getId()), anyObject(), eq("true"))).willReturn(releaseIdsTransitive);

        Release release = new Release();
        release.setId("3765276512");
        release.setName("Angular 2.3.0");
        release.setCpeid("cpe:/a:Google:Angular:2.3.0:");
        release.setReleaseDate("2016-12-07");
        release.setVersion("2.3.0");
        release.setCreatedOn("2016-12-18");
        EccInformation eccInformation = new EccInformation();
        eccInformation.setEccStatus(ECCStatus.APPROVED);
        release.setEccInformation(eccInformation);
        release.setCreatedBy("admin@sw360.org");
        release.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setComponentId("12356115");
        release.setClearingState(ClearingState.APPROVED);
        release.setExternalIds(Collections.singletonMap("mainline-id-component", "1432"));

        Release release2 = new Release();
        release2.setId("5578999");
        release2.setName("Spring 1.4.0");
        release2.setCpeid("cpe:/a:Spring:1.4.0:");
        release2.setReleaseDate("2017-05-06");
        release2.setVersion("1.4.0");
        release2.setCreatedOn("2017-11-19");
        eccInformation.setEccStatus(ECCStatus.APPROVED);
        release2.setEccInformation(eccInformation);
        release2.setCreatedBy("admin@sw360.org");
        release2.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release2.setComponentId("12356115");
        release2.setClearingState(ClearingState.APPROVED);
        release2.setExternalIds(Collections.singletonMap("mainline-id-component", "1771"));

        given(this.releaseServiceMock.getReleaseForUserById(eq(release.getId()), anyObject())).willReturn(release);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), anyObject())).willReturn(release2);

        User user = new User();
        user.setId("admin@sw360.org");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        user.setDepartment("sw360");

        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
    }

    @Test
    public void should_document_get_projects() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:projects[]name").description("The name of the project"),
                                fieldWithPath("_embedded.sw360:projects[]version").description("The project version"),
                                fieldWithPath("_embedded.sw360:projects[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/" + project.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-projects,Projects resource>>")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("version").description("The project version"),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                                fieldWithPath("externalIds").description("When projects are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the project"),
                                fieldWithPath("ownerGroup").description("The owner group of the project"),
                                fieldWithPath("ownerCountry").description("The owner country of the project"),
                                fieldWithPath("tag").description("The project tag"),
                                fieldWithPath("preevaluationDeadline").description("The project preevaluation deadline"),
                                fieldWithPath("systemTestStart").description("Date of the project system begin phase"),
                                fieldWithPath("systemTestEnd").description("Date of the project system end phase"),
                                fieldWithPath("linkedProjects").description("The relationship between linked projects of the project"),
                                fieldWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("_embedded.createdBy").description("The user who created this project"),
                                fieldWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_embedded.sw360:moderators").description("An array of all project moderators with email and link to their <<resources-user-get,User resource>>")
                        )));
    }

    @Test
    public void should_document_get_projects_by_type() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects?type=" + project.getProjectType())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:projects[]name").description("The name of the project"),
                                fieldWithPath("_embedded.sw360:projects[]version").description("The project version"),
                                fieldWithPath("_embedded.sw360:projects[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_projects_by_name() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects?name=" + project.getName())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:projects[]name").description("The name of the project"),
                                fieldWithPath("_embedded.sw360:projects[]version").description("The project version"),
                                fieldWithPath("_embedded.sw360:projects[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project_releases() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/" + project.getId() + "/releases?transitive=false")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project_releases_transitive() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/" + project.getId() + "/releases?transitive=true")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project_releases_ecc_information() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/" + project.getId() + "/releases/ecc?transitive=false")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_embedded.sw360:releases[].eccInformation.eccStatus").description("The ECC information status value"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
