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
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.eclipse.sw360.datahandler.thrift.MainlineState.MAINLINE;
import static org.eclipse.sw360.datahandler.thrift.MainlineState.OPEN;
import static org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.CONTAINED;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockBean
    private Sw360LicenseInfoService licenseInfoMockService;

    private Project project;
    private List<Project> projectList = new ArrayList<>();
    private Attachment attachment;


    @Before
    public void before() throws TException, IOException {
        Set<Attachment> attachmentList = new HashSet<>();
        List<Resource<Attachment>> attachmentResources = new ArrayList<>();
        attachment = new Attachment("1231231254", "spring-core-4.3.4.RELEASE.jar");
        attachment.setSha1("da373e491d3863477568896089ee9457bc316783");
        attachmentList.add(attachment);
        attachmentResources.add(new Resource<>(attachment));

        given(this.attachmentServiceMock.getAttachmentContent(anyObject())).willReturn(new AttachmentContent().setId("1231231254").setFilename("spring-core-4.3.4.RELEASE.jar").setContentType("binary"));
        given(this.attachmentServiceMock.getResourcesFromList(anyObject())).willReturn(new Resources<>(attachmentResources));
        given(this.attachmentServiceMock.uploadAttachment(anyObject(), anyObject(), anyObject())).willReturn(attachment);

        Map<String, ProjectReleaseRelationship> linkedReleases = new HashMap<>();
        Map<String, ProjectRelationship> linkedProjects = new HashMap<>();
        ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship(CONTAINED, MAINLINE);

        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("portal-id", "13319-XX3");
        externalIds.put("project-ext", "515432");

        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("OSPO-Comment", "Some Comment");

        List<Project> projectListByName = new ArrayList<>();
        Set<Project> usedByProjectList = new HashSet<>();
        project = new Project();
        project.setId("376576");
        project.setName("Emerald Web");
        project.setProjectType(ProjectType.PRODUCT);
        project.setVersion("1.0.2");
        project.setDescription("Emerald Web provides a suite of components for Critical Infrastructures.");
        project.setDomain("Hardware");
        project.setCreatedOn("2016-12-15");
        project.setCreatedBy("admin@sw360.org");
        project.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        project.setBusinessUnit("sw360 AR");
        project.setExternalIds(Collections.singletonMap("mainline-id-project", "515432"));
        project.setOwnerAccountingUnit("4822");
        project.setOwnerCountry("DE");
        project.setDeliveryStart("2018-05-01");
        project.setOwnerGroup("AA BB 123 GHV2-DE");
        project.setTag("project test tag 1");
        project.setPreevaluationDeadline("2018-07-17");
        project.setSystemTestStart("2017-01-01");
        project.setSystemTestEnd("2018-03-01");
        project.setObligationsText("Lorem Ipsum");
        project.setClearingSummary("Lorem Ipsum");
        project.setSpecialRisksOSS("Lorem Ipsum");
        project.setGeneralRisks3rdParty("Lorem Ipsum");
        project.setSpecialRisks3rdParty("Lorem Ipsum");
        project.setDeliveryChannels("Lorem Ipsum");
        project.setRemarksAdditionalRequirements("Lorem Ipsum");
        linkedReleases.put("3765276512", projectReleaseRelationship);
        project.setReleaseIdToUsage(linkedReleases);
        linkedProjects.put("376576", ProjectRelationship.CONTAINED);
        project.setLinkedProjects(linkedProjects);
        project.setAttachments(attachmentList);
        project.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));
        project.setProjectResponsible("projectresponsible@sw360.org");
        project.setExternalIds(externalIds);
        project.setAdditionalData(additionalData);

        projectListByName.add(project);
        projectList.add(project);
        usedByProjectList.add(project);


        Map<String, String> externalIds2 = new HashMap<>();
        externalIds2.put("project-ext", "7657");

        Project project2 = new Project();
        project2.setId("376570");
        project2.setName("Orange Web");
        project2.setVersion("2.0.1");
        project2.setProjectType(ProjectType.PRODUCT);
        project2.setDescription("Orange Web provides a suite of components for documentation.");
        project.setDomain("Hardware");
        project2.setCreatedOn("2016-12-17");
        project2.setCreatedBy("john@sw360.org");
        project2.setBusinessUnit("sw360 EX DF");
        project2.setOwnerAccountingUnit("5661");
        project2.setOwnerCountry("FR");
        project2.setDeliveryStart("2018-05-01");
        project2.setOwnerGroup("SIM-KA12");
        project2.setTag("project test tag 2");
        project2.setPreevaluationDeadline("2018-07-17");
        project2.setSystemTestStart("2017-01-01");
        project2.setSystemTestEnd("2018-03-01");
        project2.setObligationsText("Lorem Ipsum");
        project2.setClearingSummary("Lorem Ipsum");
        project2.setSpecialRisksOSS("Lorem Ipsum");
        project2.setGeneralRisks3rdParty("Lorem Ipsum");
        project2.setSpecialRisks3rdParty("Lorem Ipsum");
        project2.setDeliveryChannels("Lorem Ipsum");
        project2.setRemarksAdditionalRequirements("Lorem Ipsum");
        project2.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));
        project2.setProjectResponsible("projectresponsible@sw360.org");
        Map<String, String> projExtKeys = new HashMap();
        projExtKeys.put("mainline-id-project", "7657");
        projExtKeys.put("portal-id", "13319-XX3");
        project2.setExternalIds(projExtKeys);
        linkedReleases = new HashMap<>();
        linkedReleases.put("5578999", projectReleaseRelationship);
        project2.setReleaseIdToUsage(linkedReleases);
        project2.setExternalIds(externalIds2);

        projectList.add(project2);

        Set<String> releaseIds = new HashSet<>(Arrays.asList("3765276512"));
        Set<String> releaseIdsTransitive = new HashSet<>(Arrays.asList("3765276512", "5578999"));

        given(this.projectServiceMock.getProjectsForUser(anyObject())).willReturn(projectList);
        given(this.projectServiceMock.getProjectForUserById(eq(project.getId()), anyObject())).willReturn(project);
        given(this.projectServiceMock.searchLinkingProjects(eq(project.getId()), anyObject())).willReturn(usedByProjectList);
        given(this.projectServiceMock.searchProjectByName(eq(project.getName()), anyObject())).willReturn(projectListByName);
        given(this.projectServiceMock.getReleaseIds(eq(project.getId()), anyObject(), eq("false"))).willReturn(releaseIds);
        given(this.projectServiceMock.getReleaseIds(eq(project.getId()), anyObject(), eq("true"))).willReturn(releaseIdsTransitive);
        given(this.projectServiceMock.convertToEmbeddedWithExternalIds(eq(project))).willReturn(
                new Project("Emerald Web")
                        .setVersion("1.0.2")
                        .setId("376576")
                        .setDescription("Emerald Web provides a suite of components for Critical Infrastructures.")
                        .setExternalIds(Collections.singletonMap("mainline-id-project", "515432"))
                        .setProjectType(ProjectType.PRODUCT));
        given(this.projectServiceMock.convertToEmbeddedWithExternalIds(eq(project2))).willReturn(
                new Project("Orange Web")
                        .setVersion("2.0.1")
                        .setId("376570")
                        .setDescription("Orange Web provides a suite of components for documentation.")
                        .setExternalIds(projExtKeys)
                        .setProjectType(ProjectType.PRODUCT));
        when(this.projectServiceMock.createProject(anyObject(), anyObject())).then(invocation ->
                new Project("Test Project")
                        .setId("1234567890")
                        .setDescription("This is the description of my Test Project")
                        .setProjectType(ProjectType.PRODUCT)
                        .setVersion("1.0")
                        .setCreatedBy("admin@sw360.org")
                        .setCreatedOn(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

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

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("jane@sw360.org")).willReturn(
                new User("jane@sw360.org", "sw360").setId("209582812"));
        OutputFormatInfo outputFormatInfo = new OutputFormatInfo();
        outputFormatInfo.setFileExtension("html");
        given(this.licenseInfoMockService.getOutputFormatInfoForGeneratorClass(anyObject()))
                .willReturn(outputFormatInfo);
        LicenseInfoFile licenseInfoFile = new LicenseInfoFile();
        licenseInfoFile.setGeneratedOutput(new byte[0]);
        given(this.licenseInfoMockService.getLicenseInfoFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyObject(),anyObject())).willReturn(licenseInfoFile);
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
    public void should_document_get_usedbyresource_for_project() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/usedBy/" + project.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
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
                                fieldWithPath("domain").description("The domain, possible values are:"  + Sw360ResourceServer.DOMAIN.toString()),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                                fieldWithPath("externalIds").description("When projects are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the project"),
                                fieldWithPath("ownerGroup").description("The owner group of the project"),
                                fieldWithPath("ownerCountry").description("The owner country of the project"),
                                fieldWithPath("obligationsText").description("The obligations text of the project"),
                                fieldWithPath("clearingSummary").description("The clearing summary text of the project"),
                                fieldWithPath("specialRisksOSS").description("The special risks OSS text of the project"),
                                fieldWithPath("generalRisks3rdParty").description("The general risks 3rd party text of the project"),
                                fieldWithPath("specialRisks3rdParty").description("The special risks 3rd party text of the project"),
                                fieldWithPath("deliveryChannels").description("The sales and delivery channels text of the project"),
                                fieldWithPath("remarksAdditionalRequirements").description("The remark additional requirements text of the project"),
                                fieldWithPath("tag").description("The project tag"),
                                fieldWithPath("deliveryStart").description("The project delivery start date"),
                                fieldWithPath("preevaluationDeadline").description("The project preevaluation deadline"),
                                fieldWithPath("systemTestStart").description("Date of the project system begin phase"),
                                fieldWithPath("systemTestEnd").description("Date of the project system end phase"),
                                fieldWithPath("linkedProjects").description("The relationship between linked projects of the project"),
                                fieldWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                fieldWithPath("projectResponsible").description("A user who is responsible for the project."),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("_embedded.createdBy").description("The user who created this project"),
                                fieldWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_embedded.sw360:moderators").description("An array of all project moderators with email and link to their <<resources-user-get,User resource>>"),
                                fieldWithPath("_embedded.sw360:attachments").description("An array of all project attachments and link to their <<resources-attachment-get,Attachment resource>>")
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
    public void should_document_get_projects_by_externalIds() throws Exception {
        Map<String, Set<String>> externalIdsQuery = new HashMap<>();
        externalIdsQuery.put("portal-id", new HashSet<>(Arrays.asList("13319-XX3")));
        externalIdsQuery.put("project-ext", new HashSet<>(Arrays.asList("515432", "7657")));
        given(this.projectServiceMock.searchByExternalIds(eq(externalIdsQuery), anyObject())).willReturn((new HashSet<>(projectList)));

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/searchByExternalIds?project-ext=515432&project-ext=7657&portal-id=13319-XX3")
                .contentType(MediaTypes.HAL_JSON)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("_embedded.sw360:projects[]name").description("The name of the project"),
                                fieldWithPath("_embedded.sw360:projects[]version").description("The project version"),
                                fieldWithPath("_embedded.sw360:projects[]externalIds").description("External Ids of the project"),
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

    @Test
    public void should_document_get_project_attachment_info() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/" + project.getId() + "/attachments")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("_embedded.sw360:attachments").description("An array of <<resources-attachment, Attachments resources>>"),
                                fieldWithPath("_embedded.sw360:attachments[]filename").description("The attachment filename"),
                                fieldWithPath("_embedded.sw360:attachments[]sha1").description("The attachment sha1 value"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project_attachment() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/projects/" + project.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", "Bearer " + accessToken)
                .accept("application/*"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_create_project() throws Exception {
        Map<String, Object> project = new HashMap<>();
        project.put("name", "Test Project");
        project.put("version", "1.0");
        project.put("visibility", "PRIVATE");
        project.put("description", "This is the description of my Test Project");
        project.put("projectType", ProjectType.PRODUCT.toString());
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        releaseIdToUsage.put("3765276512", new ProjectReleaseRelationship(CONTAINED, OPEN));
        project.put("linkedReleases", releaseIdToUsage);
        Map<String, ProjectRelationship> linkedProjects = new HashMap<String, ProjectRelationship>();
        linkedProjects.put("376576", ProjectRelationship.CONTAINED);
        project.put("linkedProjects", linkedProjects);
        project.put("leadArchitect", "lead@sw360.org");
        project.put("moderators", new HashSet<>(Arrays.asList("moderator1@sw360.org", "moderator2@sw360.org")));
        project.put("contributors", new HashSet<>(Arrays.asList("contributor1@sw360.org", "contributor2@sw360.org")));

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(post("/api/projects")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(project))
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("_embedded.createdBy.email", Matchers.is("admin@sw360.org")))
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("version").description("The version of the new project"),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                fieldWithPath("linkedProjects").description("The relationship between linked projects of the project"),
                                fieldWithPath("leadArchitect").description("The lead architect of the project"),
                                fieldWithPath("contributors").description("An array of contributors to the project"),
                                fieldWithPath("moderators").description("An array of moderators")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("version").description("The project version"),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("_embedded.createdBy").description("The user who created this project")
                        )));
    }

    @Test
    public void should_document_update_project() throws Exception {
        Project updateProject = new Project();
        updateProject.setName("updated project");
        updateProject.setDescription("Project description updated");
        updateProject.setVersion("1.0");
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc
                .perform(patch("/api/projects/376576").contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateProject))
                        .header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                links(linkWithRel("self").description("The <<resources-projects,Projects resource>>")),
                requestFields(
                        fieldWithPath("name").description("The name of the project"),
                        fieldWithPath("type").description("project"),
                        fieldWithPath("version").description("The version of the new project"),
                        fieldWithPath("visibility").description("The project visibility, possible values are: "
                                + Arrays.asList(Visibility.values())),
                        fieldWithPath("description").description("The project description"),
                        fieldWithPath("projectType").description("The project type, possible values are: "
                                + Arrays.asList(ProjectType.values())),
                        fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                        fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                        fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag.")),
                responseFields(fieldWithPath("name").description("The name of the project"),
                        fieldWithPath("version").description("The project version"),
                        fieldWithPath("createdOn").description("The date the project was created"),
                        fieldWithPath("description").description("The project description"),
                        fieldWithPath("domain").description("The domain, possible values are:"  + Sw360ResourceServer.DOMAIN.toString()),
                        fieldWithPath("projectType").description("The project type, possible values are: "
                                + Arrays.asList(ProjectType.values())),
                        fieldWithPath("visibility").description("The project visibility, possible values are: "
                                + Arrays.asList(Visibility.values())),
                        fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                        fieldWithPath("externalIds").description(
                                "When projects are imported from other tools, the external ids can be stored here"),
                        fieldWithPath("additionalData").description("A place to store additional data used by external tools"),
                        fieldWithPath("ownerAccountingUnit")
                                .description("The owner accounting unit of the project"),
                        fieldWithPath("ownerGroup").description("The owner group of the project"),
                        fieldWithPath("ownerCountry").description("The owner country of the project"),
                        fieldWithPath("obligationsText").description("The obligations text of the project"),
                        fieldWithPath("clearingSummary")
                                .description("The clearing summary text of the project"),
                        fieldWithPath("specialRisksOSS")
                                .description("The special risks OSS text of the project"),
                        fieldWithPath("generalRisks3rdParty")
                                .description("The general risks 3rd party text of the project"),
                        fieldWithPath("specialRisks3rdParty")
                                .description("The special risks 3rd party text of the project"),
                        fieldWithPath("deliveryChannels")
                                .description("The sales and delivery channels text of the project"),
                        fieldWithPath("remarksAdditionalRequirements")
                                .description("The remark additional requirements text of the project"),
                        fieldWithPath("tag").description("The project tag"),
                        fieldWithPath("deliveryStart").description("The project delivery start date"),
                        fieldWithPath("preevaluationDeadline")
                                .description("The project preevaluation deadline"),
                        fieldWithPath("systemTestStart").description("Date of the project system begin phase"),
                        fieldWithPath("systemTestEnd").description("Date of the project system end phase"),
                        fieldWithPath("linkedProjects")
                                .description("The relationship between linked projects of the project"),
                        fieldWithPath("linkedReleases")
                                .description("The relationship between linked releases of the project"),
                        fieldWithPath("securityResponsibles")
                                .description("An array of users responsible for security of the project."),
                        fieldWithPath("projectResponsible")
                                .description("A user who is responsible for the project."),
                                  fieldWithPath("_links")
                                  .description("<<resources-index-links,Links>> to other resources"),
                        fieldWithPath("_embedded.createdBy").description("The user who created this project"),
                        fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                        fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                        fieldWithPath("_embedded.sw360:projects")
                                .description("An array of <<resources-projects, Projects resources>>"),
                                  fieldWithPath("_embedded.sw360:releases")
                                  .description("An array of <<resources-releases, Releases resources>>"),
                        fieldWithPath("_embedded.sw360:attachments").description(
                                "An array of all project attachments and link to their <<resources-attachment-get,Attachment resource>>"))));
    }

    @Test
    public void should_document_upload_attachment_to_project() throws Exception {
        testAttachmentUpload("/api/projects/", project.getId());
    }

    @Test
    public void should_document_link_releases() throws Exception {
        List<String> releaseIds = Arrays.asList("3765276512", "5578999", "3765276513");

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(post("/api/projects/" + project.getId() + "/releases").contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(releaseIds))
                .header("Authorization", "Bearer " + accessToken)).andExpect(status().isCreated());
    }

    @Test
    public void should_document_get_download_license_info() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(get("/api/projects/" + project.getId()+ "/licenseinfo?generatorClassName=XhtmlGenerator&variant=DISCLOSURE&externalIds=portal-id,main-project-id")
                .header("Authorization", "Bearer " + accessToken)
                .accept("application/xhtml+xml"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler
                        .document(requestParameters(
                                parameterWithName("generatorClassName")
                                        .description("All possible values for output generator class names are "
                                                + Arrays.asList("DocxGenerator", "XhtmlGenerator", "TextGenerator")),
                                parameterWithName("variant").description("All the possible values for variants are "
                                        + Arrays.asList(OutputFormatVariant.values())),
                                parameterWithName("externalIds").description("The external Ids of the project"))));
    }
}
