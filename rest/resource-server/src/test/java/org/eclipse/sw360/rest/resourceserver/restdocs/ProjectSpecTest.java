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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestType;
import org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.thrift.ImportBomDryRunReport;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ObligationStatus;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportBean;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.thrift.MainlineState.MAINLINE;
import static org.eclipse.sw360.datahandler.thrift.MainlineState.OPEN;
import static org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.CONTAINED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private SW360PackageService packageServiceMock;

    @MockitoBean
    private Sw360ProjectService projectServiceMock;

    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;

    @MockitoBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockitoBean
    private Sw360LicenseInfoService licenseInfoMockService;

    @MockitoBean
    private Sw360VulnerabilityService vulnerabilityMockService;

    @MockitoBean
    private SW360ReportService sw360ReportServiceMock;

    private Project project;
    private Project project8;
    private Set<Project> projectList = new HashSet<>();
    private Attachment attachment;
    private Release release;

    @Before
    public void before() throws TException, IOException {
        Set<Attachment> attachmentList = new HashSet<>();
        List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        attachment = new Attachment("1231231254", "spring-core-4.3.4.RELEASE.jar");
        attachment.setSha1("da373e491d3863477568896089ee9457bc316783");
        attachmentList.add(attachment);
        attachmentResources.add(EntityModel.of(attachment));

        Set<Attachment> setOfAttachment = new HashSet<Attachment>();
        Attachment att1 = new Attachment("1234", "test.zip").setAttachmentType(AttachmentType.SOURCE)
                .setCreatedBy("user@sw360.org").setSha1("da373e491d312365483589ee9457bc316783").setCreatedOn("2021-04-27")
                .setCreatedTeam("DEPARTMENT");
        Attachment att2 = att1.deepCopy().setAttachmentType(AttachmentType.BINARY).setCreatedComment("Created Comment")
                .setCheckStatus(CheckStatus.ACCEPTED).setCheckedComment("Checked Comment").setCheckedOn("2021-04-27")
                .setCheckedBy("admin@sw360.org").setCheckedTeam("DEPARTMENT1");

        given(this.attachmentServiceMock.getAttachmentContent(any())).willReturn(new AttachmentContent().setId("1231231254").setFilename("spring-core-4.3.4.RELEASE.jar").setContentType("binary"));
        given(this.attachmentServiceMock.getResourcesFromList(any())).willReturn(CollectionModel.of(attachmentResources));
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment);
        given(this.attachmentServiceMock.updateAttachment(any(), any(), any(), any())).willReturn(att2);
        Mockito.doNothing().when(projectServiceMock).copyLinkedObligationsForClonedProject(any(), any(),
                any());

        Map<String, ProjectReleaseRelationship> linkedReleases = new HashMap<>();
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship(CONTAINED, MAINLINE)
                .setComment("Test Comment").setCreatedOn("2020-08-05").setCreatedBy("admin@sw360.org");
        ProjectReleaseRelationship projectReleaseRelationshipResponseBody = projectReleaseRelationship.deepCopy()
                .setComment("Test Comment").setMainlineState(MainlineState.SPECIFIC)
                .setReleaseRelation(ReleaseRelationship.STANDALONE);
        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("portal-id", "13319-XX3");
        externalIds.put("project-ext", "515432");
        externalIds.put("ws-project-token", "[\"490389ac-0269-4719-9cbf-fb5e299c8415\",\"3892f1db-4361-4e83-a89d-d28a262d65b9\"]");

        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("OSPO-Comment", "Some Comment");

        Map<String, ProjectPackageRelationship> linkedPckg = new HashMap<>();
        ProjectPackageRelationship projectPackageRelationship = new ProjectPackageRelationship()
                .setComment("Test Comment").setCreatedOn("2020-08-05").setCreatedBy("admin@sw360.org");

        setOfAttachment.add(att1);
        Project projectForAtt = new Project();
        projectForAtt.setAttachments(setOfAttachment);
        projectForAtt.setId("98745");
        projectForAtt.setName("Test Project");
        projectForAtt.setProjectType(ProjectType.PRODUCT);
        projectForAtt.setVersion("1");
        projectForAtt.setCreatedOn("2021-04-27");
        projectForAtt.setCreatedBy("admin@sw360.org");

        Set<String> licenseIds = new HashSet<>();
        licenseIds.add("MIT");
        licenseIds.add("GPL");

        Package package1 = new Package("angular-sanitize", "1.8.2", "pkg:npm/angular-sanitize@1.8.2", CycloneDxComponentType.FRAMEWORK)
                .setId("122357345")
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2023-01-02")
                .setVcs("git+https://github.com/angular/angular.js.git")
                .setHomepageUrl("http://angularjs.org")
                .setLicenseIds(licenseIds)
                .setReleaseId("12345678")
                .setPackageManager(PackageManager.NPM)
                .setDescription("Sanitizes an html string by stripping all potentially dangerous tokens.");

        Package package2 = new Package()
                .setId("875689754")
                .setName("applicationinsights-web")
                .setVersion("2.5.11")
                .setCreatedBy("user@sw360.org")
                .setCreatedOn("2023-02-02")
                .setPurl("pkg:npm/@microsoft/applicationinsights-web@2.5.11")
                .setPackageManager(PackageManager.NPM)
                .setPackageType(CycloneDxComponentType.LIBRARY)
                .setVcs("git+https://github.com/microsoft/ApplicationInsights-JS.git")
                .setHomepageUrl("https://github.com/microsoft/ApplicationInsights-JS#readme")
                .setDescription("Application Insights is an extension of Azure Monitor and provides application performance monitoring (APM) features");

        given(this.packageServiceMock.getPackageForUserById(eq(package1.getId()))).willReturn(package1);
        given(this.packageServiceMock.getPackageForUserById(eq(package2.getId()))).willReturn(package2);
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);

        Set<String> linkedPackages = new HashSet<>();
        linkedPackages.add(package1.getId());
        linkedPackages.add(package2.getId());

        Map<String, ProjectPackageRelationship> linkedPckg2 = new HashMap<>();
        linkedPckg2.put(package1.getId(), projectPackageRelationship);
        linkedPckg2.put(package2.getId(), projectPackageRelationship);

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
        //project.setPackageIds(new HashSet<>(Arrays.asList("123456", "54844")));
        linkedPckg.put("123456", projectPackageRelationship);
        linkedPckg.put("54844", projectPackageRelationship);
        project.setPackageIds(linkedPckg);
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
        project.setLicenseInfoHeaderText("Lorem Ipsum");
        project.setDeliveryChannels("Lorem Ipsum");
        //project.setPackageIds(new HashSet<>(Arrays.asList("pkg-001", "pkg-002", "pkg-003")));
        linkedPckg.put("pkg-001", projectPackageRelationship);
        linkedPckg.put("pkg-002", projectPackageRelationship);
        linkedPckg.put("pkg-003", projectPackageRelationship);
        project.setPackageIds(linkedPckg);
        project.setVendor(new Vendor());
        project.setRemarksAdditionalRequirements("Lorem Ipsum");
        ReleaseClearingStateSummary clearingCount = new ReleaseClearingStateSummary();
        clearingCount.newRelease = 2;
        clearingCount.sentToClearingTool = 1;
        clearingCount.underClearing = 0;
        clearingCount.reportAvailable = 0;
        clearingCount.scanAvailable = 0;
        clearingCount.internalUseScanAvailable = 1;
        clearingCount.approved = 2;
        project.setReleaseClearingStateSummary(clearingCount);
        linkedReleases.put("3765276512", projectReleaseRelationship);
        project.setReleaseIdToUsage(linkedReleases);
        linkedProjects.put("376570", new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true));
        project.setLinkedProjects(linkedProjects);
        project.setPackageIds(linkedPckg2);
        project.setAttachments(attachmentList);
        project.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));
        project.setProjectResponsible("projectresponsible@sw360.org");
        project.setExternalIds(externalIds);
        project.setAdditionalData(additionalData);
        project.setPhaseOutSince("2020-06-24");
        project.setClearingRequestId("CR-1");
        Map<String, String> externalURLs1 = new HashMap<>();
        externalURLs1.put("homepage", "http://test_wiki_url.com");
        externalURLs1.put("wiki", "http://test_wiki_url.com");
        project.setExternalUrls(externalURLs1);
        project.setReleaseRelationNetwork("[{\"comment\":\"\",\"releaseLink\":[],\"createBy\":\"admin@sw360.org\",\"createOn\":\"2022-08-15\",\"mainlineState\":\"OPEN\",\"releaseId\":\"3765276512\",\"releaseRelationship\":\"CONTAINED\"}]");

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
        linkedPckg.put("123456", projectPackageRelationship);
        linkedPckg.put("54844", projectPackageRelationship);
        project2.setPackageIds(linkedPckg);
        //project2.setPackageIds(new HashSet<>(Arrays.asList("123456", "54844")));
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
        project2.setLicenseInfoHeaderText("Lorem Ipsum");
        project2.setVendor(new Vendor());
        project2.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));
        project2.setProjectResponsible("projectresponsible@sw360.org");
        Map<String, String> projExtKeys = new HashMap<String, String>();
        projExtKeys.put("mainline-id-project", "7657");
        projExtKeys.put("portal-id", "13319-XX3");
        project2.setExternalIds(projExtKeys);
        linkedReleases = new HashMap<>();
        linkedReleases.put("5578999", projectReleaseRelationship);
        project2.setReleaseIdToUsage(linkedReleases);
        project2.setExternalIds(externalIds2);
        project2.setPackageIds(linkedPckg2);
        Map<String, String> externalURLs = new HashMap<>();
        externalURLs.put("homepage", "http://test_wiki_url.com");
        externalURLs.put("wiki", "http://test_wiki_url.com");
        project2.setExternalUrls(externalURLs);
        project2.setPhaseOutSince("2020-06-02");
        project2.setClearingTeam("Unknown");
        project2.setContributors(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        project2.setClearingRequestId("CR-2");

        projectList.add(project2);

        Map<String, ProjectReleaseRelationship> linkedReleases2 = new HashMap<>();
        Map<String, ProjectReleaseRelationship> linkedReleases3 = new HashMap<>();
        Map<String, ProjectReleaseRelationship> linkedReleases4 = new HashMap<>();
        Map<String, ProjectProjectRelationship> linkedProjects2 = new HashMap<>();
        Map<String, ProjectProjectRelationship> linkedProjects3 = new HashMap<>();
        Project project4 = new Project();
        project4.setId("12345");
        project4.setName("dummy");
        project4.setVersion("2.0.1");
        project4.setProjectType(ProjectType.PRODUCT);
        project4.setState(ProjectState.ACTIVE);
        project4.setClearingState(ProjectClearingState.OPEN);
        linkedProjects2.put("123456", new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true));
        project4.setLinkedProjects(linkedProjects2);
        project4.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));
        //project4.setPackageIds(new HashSet<>(Arrays.asList("123456", "54844")));
        linkedPckg.put("123456", projectPackageRelationship);
        linkedPckg.put("54844", projectPackageRelationship);
        project4.setPackageIds(linkedPckg);
        Project project5 = new Project();
        project5.setId("123456");
        project5.setName("dummy2");
        project5.setVersion("2.0.1");
        project5.setProjectType(ProjectType.PRODUCT);
        project5.setState(ProjectState.ACTIVE);
        project5.setClearingState(ProjectClearingState.OPEN);
        linkedReleases2.put("37652765121", projectReleaseRelationship);
        project5.setReleaseIdToUsage(linkedReleases2);
        linkedProjects3.put("1234567", new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true));
        project5.setLinkedProjects(linkedProjects3);
        project5.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));

        Project project6 = new Project();
        project6.setId("1234567");
        project6.setName("dummy3");
        project6.setVersion("3.0.1");
        project6.setProjectType(ProjectType.PRODUCT);
        project6.setState(ProjectState.ACTIVE);
        project6.setClearingState(ProjectClearingState.OPEN);
        project6.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));

        Project project7 = new Project();
        project7.setId("345678");
        project7.setName("project1");
        project7.setVersion("1");
        project7.setCreatedBy(testUserId);
        project7.setProjectType(ProjectType.PRODUCT);
        project7.setState(ProjectState.ACTIVE);
        project7.setLinkedProjects(new HashMap<String, ProjectProjectRelationship>());
        project7.setClearingState(ProjectClearingState.OPEN);
        project7.setSecurityResponsibles(new HashSet<>(Arrays.asList("securityresponsible1@sw360.org", "securityresponsible2@sw360.org")));

        Set<Attachment> attachmentSet = new HashSet<Attachment>();
        List<Obligation> obligationList = new ArrayList<>();
        Set<String> licenseIds2 = new HashSet<>();
        licenseIds2.add("MIT");

        License license = new License();
        license.setId("MIT");
        license.setFullname("The MIT License (MIT)");
        license.setShortname("MIT");

        Obligation obligation = new Obligation();
        obligation.setId("0001");
        obligation.setTitle("obligation_title");
        obligation.setText("This is text of Obligation");
        obligation.setObligationType(ObligationType.PERMISSION);
        obligationList.add(obligation);
        license.setObligations(obligationList);

        Attachment releaseAttachment = new Attachment("33312312533", "CLIXML_core-js.xml");
        releaseAttachment.setSha1("d32a6dcbf27c61230d909515e69ecd0d");
        releaseAttachment.setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML);
        releaseAttachment.setCheckStatus(CheckStatus.ACCEPTED);
        attachmentSet.add(releaseAttachment);

        Release release7 = new Release();
        release7.setId("376527651233");
        release7.setName("Angular_Obl");
        release7.setVersion("2");
        release7.setAttachments(attachmentSet);

        project8 = new Project();
        project8.setId("123456733");
        project8.setName("oblProject");
        project8.setVersion("3");
        project8.setLinkedObligationId("009");
        linkedReleases3.put("376527651233", projectReleaseRelationship);
        project8.setReleaseIdToUsage(linkedReleases3);
        List<String> title = Arrays.asList("obligation_title");

        Source ownerSrc3 = Source.releaseId("376527651233");
        Source usedBySrc3 = Source.projectId("123456733");
        LicenseInfoUsage licenseInfoUsage3 = new LicenseInfoUsage(new HashSet<>());
        licenseInfoUsage3.setProjectPath("123456733");
        licenseInfoUsage3.setExcludedLicenseIds(Sets.newHashSet());
        licenseInfoUsage3.setIncludeConcludedLicense(false);
        UsageData usageData3 = new UsageData();
        usageData3.setLicenseInfo(licenseInfoUsage3);
        usageData3.setFieldValue(UsageData._Fields.LICENSE_INFO, licenseInfoUsage3);
        AttachmentUsage attachmentUsage3 = new AttachmentUsage(ownerSrc3, "aa1122334455bb33", usedBySrc3);
        attachmentUsage3.setUsageData(usageData3);
        attachmentUsage3.setId("11223344889933");

        Set<Release> releaseSet = new HashSet<>();
        releaseSet.add(release7);
        Map<String, AttachmentUsage> licenseInfoUsages = Map.of("aa1122334455bb33", attachmentUsage3);
        Map<String, String> releaseIdToAcceptedCLI = Map.of(release7.getId(), "aa1122334455bb33");
        Map<String, Set<Release>> licensesFromAttachmentUsage = Map.of(license.getId(), releaseSet);
        ObligationStatusInfo osi = new ObligationStatusInfo();
        osi.setText(obligation.getText());
        osi.setLicenseIds(licenseIds2);
        osi.setReleaseIdToAcceptedCLI(releaseIdToAcceptedCLI);
        osi.setId(obligation.getId());
        osi.setComment("comment");
        osi.setStatus(ObligationStatus.OPEN);
        osi.setObligationType(obligation.getObligationType());
        Map<String, ObligationStatusInfo> obligationStatusMap = Map.of(obligation.getTitle(), osi);
        Map<String, ObligationStatusInfo> obligationStatusMapFromAdminSection = new HashMap<>();

        ObligationList obligationLists = new ObligationList();
        obligationLists.setProjectId(project8.getId());
        obligationLists.setId("009");
        obligationLists.setLinkedObligationStatus(obligationStatusMap);

        Release release5 = new Release();
        release5.setId("37652765121");
        release5.setName("Angular 2.3.1");
        release5.setCpeid("cpe:/a:Google:Angular:2.3.1:");
        release5.setReleaseDate("2016-12-17");
        release5.setVersion("2.3.1");
        release5.setCreatedOn("2016-12-28");
        release = release5.deepCopy();

        Project project9 = new Project();
        project9.setId("0000007");
        project9.setName("attachUsages");
        project9.setVersion("2");
        project9.setCreatedBy(testUserId);
        project9.setProjectType(ProjectType.PRODUCT);
        project9.setState(ProjectState.ACTIVE);
        project9.setClearingState(ProjectClearingState.OPEN);
        linkedReleases4.put("00000071", projectReleaseRelationship);
        project9.setReleaseIdToUsage(linkedReleases4);

        Release release9 = new Release();
        release9.setId("00000071");
        release9.setName("docker");
        release9.setCpeid("cpe:/a:Google:Angular:2.3.1:");
        release9.setReleaseDate("2024-03-17");
        release9.setVersion("2");
        release9.setCreatedOn("2024-03-28");
        release9.setAttachments(setOfAttachment);

        List<AttachmentUsage> attachmentUsageNewList = new ArrayList<>();
        List<AttachmentUsage> deselectedUsagesFromRequest = new ArrayList<>();
        List<AttachmentUsage> selectedUsagesFromRequest = new ArrayList<>();
        List<String> selectedUsages = Arrays.asList("00000071_sourcePackage_1234");

        Set<String> releaseIds2 = new HashSet<>(Collections.singletonList("00000071"));
        Set<String> releaseIds = new HashSet<>(Collections.singletonList("3765276512"));
        Set<String> releaseIdsTransitive = new HashSet<>(Arrays.asList("3765276512", "5578999"));

        Attachment SPDXAttachment = new Attachment("3331231254", "bom.spdx.rdf");
        SPDXAttachment.setSha1("df903e491d3863477568896089ee9457bc316183");
        SPDXAttachment.setAttachmentType(AttachmentType.SBOM);
        Set<Attachment> spdxSet = new HashSet<>();
        spdxSet.add(SPDXAttachment);

        Attachment CycloneDXAttachment = new Attachment("3331111231254", "sampleBOM.xml");
        CycloneDXAttachment.setSha1("df3e491d3863477568896089ee9457bc316183");
        CycloneDXAttachment.setAttachmentType(AttachmentType.SBOM);
        Set<Attachment> cyclonedxSet = new HashSet<>();
        cyclonedxSet.add(CycloneDXAttachment);

        Project SPDXProject = new Project();
        SPDXProject.setId("333655");
        SPDXProject.setName("Green Web");
        SPDXProject.setVersion("1.0.1");
        SPDXProject.setCreatedOn("2022-11-13");
        SPDXProject.setBusinessUnit("sw360 BA");
        SPDXProject.setState(ProjectState.ACTIVE);
        SPDXProject.setClearingState(ProjectClearingState.OPEN);
        SPDXProject.setProjectType(ProjectType.PRODUCT);
        SPDXProject.setCreatedBy("admin@sw360.org");
        SPDXProject.setAttachments(spdxSet);

        Project cycloneDXProject = new Project();
        cycloneDXProject.setId("3336565435");
        cycloneDXProject.setName("Azure Web");
        cycloneDXProject.setVersion("1.0.2");
        cycloneDXProject.setCreatedOn("2022-11-13");
        cycloneDXProject.setBusinessUnit("sw360 BA");
        cycloneDXProject.setState(ProjectState.ACTIVE);
        cycloneDXProject.setClearingState(ProjectClearingState.OPEN);
        cycloneDXProject.setProjectType(ProjectType.PRODUCT);
        cycloneDXProject.setCreatedBy("admin@sw360.org");
        cycloneDXProject.setAttachments(cyclonedxSet);
        cycloneDXProject.setPackageIds(linkedPckg2);

        RequestSummary requestSummaryForSPDX = new RequestSummary();
        requestSummaryForSPDX.setMessage(SPDXProject.getId());
        requestSummaryForSPDX.setRequestStatus(RequestStatus.SUCCESS);

        RequestSummary requestSummaryForCycloneDX = new RequestSummary();
        requestSummaryForCycloneDX.setMessage("{\"projectId\":\"" + cycloneDXProject.getId() + "\"}");
        ImportBomDryRunReport dryRunReport = new ImportBomDryRunReport();
        dryRunReport.setRequestStatus(RequestStatus.SUCCESS);
        dryRunReport.setNewComponents(new HashSet<>(Collections.singleton("new-component")));
        dryRunReport.setExistingComponents(new HashSet<>(Collections.singleton("existing-component")));
        dryRunReport.setLicenseConflicts(new HashSet<>());
        dryRunReport.setWarnings(new HashSet<>());

        String projectName="project_name_version_createdOn.xlsx";

        AddDocumentRequestSummary requestSummaryForCR = new AddDocumentRequestSummary();
        requestSummaryForCR.setMessage("Clearing request created successfully");
        requestSummaryForCR.setRequestStatus(AddDocumentRequestStatus.SUCCESS);
        requestSummaryForCR.setId("CR-1");

        SW360ReportBean reportBean = new SW360ReportBean();
        reportBean.setWithLinkedReleases(false);
        reportBean.setExcludeReleaseVersion(true);
        reportBean.setGeneratorClassName("DocxGenerator");
        reportBean.setVariant("REPORT");
        reportBean.setExternalIds("portal-id");
        reportBean.setWithSubProject(false);
        reportBean.setBomType(null);

        given(this.projectServiceMock.createClearingRequest(any(),any(),any(),eq(project.getId()))).willReturn(requestSummaryForCR);
        given(this.projectServiceMock.loadPreferredClearingDateLimit()).willReturn(Integer.valueOf(7));

        given(this.projectServiceMock.getLicenseInfoHeaderText()).willReturn("Default License Info Header Text");
        given(this.projectServiceMock.importSPDX(any(),any())).willReturn(requestSummaryForSPDX);
        given(this.projectServiceMock.dryRunImportSPDX(any(), any(), any(byte[].class))).willReturn(dryRunReport);
        given(this.projectServiceMock.importCycloneDX(any(),any(),any(),anyBoolean())).willReturn(requestSummaryForCycloneDX);
        given(this.sw360ReportServiceMock.getDocumentName(any(), any(), any())).willReturn(projectName);
        given(this.sw360ReportServiceMock.getProjectBuffer(any(),anyBoolean(),any())).willReturn(ByteBuffer.allocate(10000));
        given(this.sw360ReportServiceMock.getProjectReleaseSpreadSheetWithEcc(any(),any())).willReturn(ByteBuffer.allocate(10000));
        given(this.projectServiceMock.getProjectForUserById(eq(project.getId()), any())).willReturn(project);
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(projectList.size()).setDisplayStart(0).setTotalRowCount(projectList.size()),
                        projectList.stream().toList()
                )
        );
        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(projectList.size()).setDisplayStart(0).setTotalRowCount(projectList.size()),
                        projectList.stream().toList()
                )
        );
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        given(this.projectServiceMock.getProjectForUserById(eq(project4.getId()), any())).willReturn(project4);
        given(this.projectServiceMock.getProjectForUserById(eq(project5.getId()), any())).willReturn(project5);
        given(this.projectServiceMock.getProjectForUserById(eq(project6.getId()), any())).willReturn(project6);
        given(this.projectServiceMock.getProjectForUserById(eq(project7.getId()), any())).willReturn(project7);
        given(this.projectServiceMock.getProjectForUserById(eq(project8.getId()), any())).willReturn(project8);
        given(this.sw360ReportServiceMock.downloadSourceCodeBundle(any(), any(), anyBoolean())).willReturn(ByteBuffer.allocate(10000));
        given(this.sw360ReportServiceMock.getLicenseInfoBuffer(any(), any(), any())).willReturn(ByteBuffer.allocate(10000));
        given(this.sw360ReportServiceMock.getSourceCodeBundleName(any(), any())).willReturn("SourceCodeBundle-ProjectName");
        given(this.projectServiceMock.getLicenseInfoAttachmentUsage(eq(project8.getId()))).willReturn(licenseInfoUsages);
        given(this.projectServiceMock.getObligationData(eq(project8.getLinkedObligationId()), any())).willReturn(obligationLists);
        given(this.projectServiceMock.setObligationsFromAdminSection(any(), any(), any(), any())).willReturn(obligationStatusMapFromAdminSection);
        given(this.projectServiceMock.setLicenseInfoWithObligations(eq(obligationStatusMap), eq(releaseIdToAcceptedCLI), any(), any())).willReturn(obligationStatusMap);
        given(this.projectServiceMock.getLicensesFromAttachmentUsage(eq(licenseInfoUsages), any())).willReturn(licensesFromAttachmentUsage);
        given(this.projectServiceMock.getLicenseObligationData(eq(licensesFromAttachmentUsage), any())).willReturn(obligationStatusMap);
        given(this.projectServiceMock.addLinkedObligations(any(), any(), eq(obligationStatusMap))).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.compareObligationStatusMap(any(), any(), any())).willReturn(obligationStatusMap);
        given(this.projectServiceMock.patchLinkedObligations(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.getProjectForUserById(eq(project9.getId()), any())).willReturn(project9);
        given(this.projectServiceMock.getUsedAttachments(any(), any())).willReturn(attachmentUsageNewList);
        given(this.projectServiceMock.validate(any(), any(), any(), any())).willReturn(true);
        given(this.projectServiceMock.deselectedAttachmentUsagesFromRequest(any(), eq(selectedUsages), any(), any(), any())).willReturn(deselectedUsagesFromRequest);
        given(this.projectServiceMock.selectedAttachmentUsagesFromRequest(any(), eq(selectedUsages), any(), any(), any())).willReturn(selectedUsagesFromRequest);
        given(this.projectServiceMock.removeOrphanObligations(eq(obligationStatusMap), any(), eq(project8), any(), eq(obligationLists))).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.getProjectForUserById(eq(projectForAtt.getId()), any())).willReturn(projectForAtt);
        given(this.projectServiceMock.getProjectForUserById(eq(SPDXProject.getId()), any())).willReturn(SPDXProject);
        given(this.projectServiceMock.getProjectForUserById(eq(cycloneDXProject.getId()), any())).willReturn(cycloneDXProject);
        given(this.projectServiceMock.searchLinkingProjects(eq(project.getId()), any())).willReturn(usedByProjectList);
        given(this.projectServiceMock.searchProjectByTag(any(), any())).willReturn(new ArrayList<Project>(projectList));
        given(this.projectServiceMock.searchProjectByType(any(), any())).willReturn(new ArrayList<Project>(projectList));
        given(this.projectServiceMock.searchProjectByGroup(any(), any())).willReturn(new ArrayList<Project>(projectList));
        given(this.projectServiceMock.refineSearch(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(projectListByName.size()).setDisplayStart(0).setTotalRowCount(projectListByName.size()),
                        projectListByName.stream().toList()
                )
        );
        given(this.projectServiceMock.getReleaseIds(eq(project.getId()), any(), eq(false))).willReturn(releaseIds);
        given(this.projectServiceMock.getReleaseIds(eq(project9.getId()), any(), eq(true))).willReturn(releaseIds2);
        given(this.projectServiceMock.getReleaseIds(eq(project.getId()), any(), eq(true))).willReturn(releaseIdsTransitive);
        given(this.projectServiceMock.deleteProject(eq(project.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.updateProjectReleaseRelationship(any(), any(), any())).willReturn(projectReleaseRelationshipResponseBody);
        given(this.projectServiceMock.getClearingInfo(eq(project), any())).willReturn(project);
        given(this.projectServiceMock.updateProjectForAttachment(eq(project7), any(), any(), any(), eq(projectName))).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.getCyclicLinkedProjectPath(eq(project7), any())).willReturn("");
        given(this.projectServiceMock.updateProject(eq(project7), any())).willReturn(RequestStatus.SUCCESS);
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
                        .setProjectType(ProjectType.PRODUCT)
                        .setVendor((new  Vendor("Test", "Test short", "http://testvendoraddress.com").setId("987567468"))));
        when(this.projectServiceMock.createProject(any(), any())).then(invocation ->
                new Project("Test Project")
                        .setId("1234567890")
                        .setDescription("This is the description of my Test Project")
                        .setProjectType(ProjectType.PRODUCT)
                        .setVersion("1.0")
                        .setCreatedBy("admin@sw360.org")
                        .setPhaseOutSince("2020-06-25")
                        .setState(ProjectState.ACTIVE)
                        .setCreatedOn(new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                        .setVendor((new  Vendor("Test", "Test short", "http://testvendoraddress.com").setId("987567468"))));
        given(this.projectServiceMock.getMyProjects(any(), any())).willReturn(new ArrayList<>(projectList));
        given(this.projectServiceMock.getWithFilledClearingStatus(
                eq(new ArrayList<>(projectList)), eq(ImmutableMap.<String, Boolean>builder()
                        .put(ProjectClearingState.OPEN.toString(), true)
                        .put(ProjectClearingState.CLOSED.toString(), true)
                        .put(ProjectClearingState.IN_PROGRESS.toString(), true)
                        .build()))).willReturn(new ArrayList<>(projectList));

        Release release = new Release();
        release.setId("3765276512");
        release.setName("Angular 2.3.0");
        release.setCpeid("cpe:/a:Google:Angular:2.3.0:");
        release.setReleaseDate("2016-12-07");
        release.setVersion("2.3.0");
        release.setCreatedOn("2016-12-18");
        Component compo = new Component();
        compo.setComponentType(ComponentType.OSS);
        release.setComponentType(compo.getComponentType());
        EccInformation eccInformation = new EccInformation();
        eccInformation.setEccStatus(ECCStatus.APPROVED);
        release.setEccInformation(eccInformation);
        release.setCreatedBy("admin@sw360.org");
        release.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setComponentId("12356115");
        release.setClearingState(ClearingState.APPROVED);
        release.setExternalIds(Collections.singletonMap("mainline-id-component", "1432"));
        release.setPackageIds(linkedPackages);
        release.setMainlineState(MainlineState.MAINLINE);
        release.setMainLicenseIds(new HashSet<>(Arrays.asList("GPL-2.0-or-later", "Apache-2.0")));
        release.setOtherLicenseIds(new HashSet<>(Arrays.asList("LGPL-2.0")));
        release.setAttachments(setOfAttachment);

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
        release2.setComponentType(ComponentType.COTS);
        release2.setMainlineState(MainlineState.MAINLINE);
        release2.setMainLicenseIds(new HashSet<>(Arrays.asList("Apache-2.0")));
        release2.setOtherLicenseIds(new HashSet<>(Arrays.asList("GPL-2.0")));
        release2.setAttachments(setOfAttachment);

        Release rel = new Release();
        Map<String, String> releaseExternalIds = new HashMap<>();
        releaseExternalIds.put("mainline-id-component", "1432");
        releaseExternalIds.put("ws-component-id", "[\"2365\",\"5487923\"]");

        rel.setId("3765276512");
        rel.setName("Spring Core 4.3.4");
        rel.setCpeid("cpe:/a:pivotal:spring-core:4.3.4:");
        rel.setReleaseDate("2016-12-07");
        rel.setVersion("4.3.4");
        rel.setCreatedOn("2016-12-18");
        rel.setCreatedBy("admin@sw360.org");
        rel.setSourceCodeDownloadurl("http://www.google.com");
        rel.setBinaryDownloadurl("http://www.google.com/binaries");
        rel.setComponentId("17653524");
        rel.setComponentType(ComponentType.OSS);
        rel.setClearingState(ClearingState.APPROVED);
        rel.setExternalIds(releaseExternalIds);
        rel.setAdditionalData(Collections.singletonMap("Key", "Value"));
        rel.setLanguages(new HashSet<>(Arrays.asList("C++", "Java")));
        rel.setMainLicenseIds(new HashSet<>(Arrays.asList("GPL-2.0-or-later", "Apache-2.0")));
        rel.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        rel.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Java SE", ".NET")));
        rel.setMainlineState(MainlineState.MAINLINE);
        rel.setVendor(new Vendor("TV", "Test Vendor", "http://testvendor.com"));
        rel.setPackageIds(linkedPackages);

        given(this.releaseServiceMock.getReleaseForUserById(eq(release.getId()), any())).willReturn(release);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release7.getId()), any())).willReturn(release7);

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789").setUserGroup(UserGroup.ADMIN));
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("jane@sw360.org")).willReturn(
                new User("jane@sw360.org", "sw360").setId("209582812"));
        given(this.userServiceMock.getUserByEmail("clearingTeam@sw360.org")).willReturn(
                new User("clearingTeam@sw360.org", "sw360").setId("2012312"));
        OutputFormatInfo outputFormatInfo = new OutputFormatInfo();
        outputFormatInfo.setFileExtension("html");
        given(this.licenseInfoMockService.getOutputFormatInfoForGeneratorClass(any()))
                .willReturn(outputFormatInfo);
        LicenseInfoFile licenseInfoFile = new LicenseInfoFile();
        licenseInfoFile.setGeneratedOutput(new byte[0]);
        given(this.licenseInfoMockService.getLicenseInfoFile(any(), any(), any(), any(),
                any(),any(), any(), eq(false))).willReturn(licenseInfoFile);
        given(this.licenseInfoMockService.getLicenseInfoFile(any(), any(), any(), any(),
                any(),any(), any(), eq(true))).willReturn(licenseInfoFile);

        Source ownerSrc1 = Source.releaseId("9988776655");
        Source usedBySrc = Source.projectId(project.getId());
        LicenseInfoUsage licenseInfoUsage1 = new LicenseInfoUsage(new HashSet<>());
        licenseInfoUsage1.setProjectPath("11223344:22334455");
        licenseInfoUsage1.setExcludedLicenseIds(Sets.newHashSet("22334455", "9988776655"));
        licenseInfoUsage1.setIncludeConcludedLicense(false);
        UsageData usageData1 = new UsageData();
        usageData1.setLicenseInfo(licenseInfoUsage1);
        AttachmentUsage attachmentUsage1 = new AttachmentUsage(ownerSrc1, "aa1122334455bbcc", usedBySrc);
        attachmentUsage1.setUsageData(usageData1);
        attachmentUsage1.setId("11223344889977");

        Source ownerSrc2 = Source.releaseId("5566778899");
        LicenseInfoUsage licenseInfoUsage2 = new LicenseInfoUsage(new HashSet<>());
        licenseInfoUsage2.setProjectPath("11223344:22334455:445566778899");
        licenseInfoUsage2.setExcludedLicenseIds(Sets.newHashSet("44553322", "5566778899"));
        licenseInfoUsage2.setIncludeConcludedLicense(true);
        UsageData usageData2 = new UsageData();
        usageData2.setLicenseInfo(licenseInfoUsage2);
        AttachmentUsage attachmentUsage2 = new AttachmentUsage(ownerSrc2, "aa1122334455ee", usedBySrc);
        attachmentUsage2.setUsageData(usageData2);
        attachmentUsage2.setId("8899776655");

        List<AttachmentUsage> attachmentUsageList = new ArrayList<>();
        attachmentUsageList.add(attachmentUsage1);
        attachmentUsageList.add(attachmentUsage2);
        given(this.attachmentServiceMock.getAllAttachmentUsage(any())).willReturn(attachmentUsageList);
        List<VulnerabilityDTO> vulDtos = new ArrayList<VulnerabilityDTO>();
        VulnerabilityDTO vulDto = new VulnerabilityDTO();
        vulDto.setComment("Lorem Ipsum");
        vulDto.setExternalId("12345");
        vulDto.setProjectRelevance("IRRELEVANT");
        vulDto.setIntReleaseId("21055");
        vulDto.setIntReleaseName("Angular 2.3.0");
        vulDto.setAction("Update to Fixed Version");
        vulDto.setPriority("2 - major");
        vulDto.setTitle("title");
        ReleaseVulnerabilityRelation relation = new ReleaseVulnerabilityRelation();
        relation.setReleaseId("3765276512");
        relation.setVulnerabilityId("1333333333");
        relation.setMatchedBy("matchedBy");
        relation.setUsedNeedle("usedNeedle");
        vulDto.setReleaseVulnerabilityRelation(relation);
        vulDtos.add(vulDto);
        VulnerabilityDTO vulDto1 = new VulnerabilityDTO();
        vulDto1.setComment("Lorem Ipsum");
        vulDto1.setExternalId("23105");
        vulDto1.setProjectRelevance("APPLICABLE");
        vulDto1.setIntReleaseId("21055");
        vulDto1.setIntReleaseName("Angular 2.3.0");
        vulDto1.setAction("Update to Fixed Version");
        vulDto1.setPriority("1 - critical");
        vulDtos.add(vulDto1);
        given(this.vulnerabilityMockService.getVulnerabilitiesByProjectId(any(), any())).willReturn(vulDtos);
        VulnerabilityCheckStatus vulnCheckStatus = new VulnerabilityCheckStatus();
        vulnCheckStatus.setCheckedBy("admin@sw360.org");
        vulnCheckStatus.setCheckedOn(SW360Utils.getCreatedOn());
        vulnCheckStatus.setVulnerabilityRating(VulnerabilityRatingForProject.IRRELEVANT);
        vulnCheckStatus.setComment("Lorem Ipsum");
        vulnCheckStatus.setProjectAction("Lorem Ipsum");

        VulnerabilityCheckStatus vulnCheckStatus1 = new VulnerabilityCheckStatus();
        vulnCheckStatus1.setCheckedBy("admin@sw360.org");
        vulnCheckStatus1.setCheckedOn(SW360Utils.getCreatedOn());
        vulnCheckStatus1.setVulnerabilityRating(VulnerabilityRatingForProject.APPLICABLE);
        vulnCheckStatus1.setComment("Lorem Ipsum");
        vulnCheckStatus1.setProjectAction("Lorem Ipsum");

        List<VulnerabilityCheckStatus> vulCheckStatuses = new ArrayList<VulnerabilityCheckStatus>();
        vulCheckStatuses.add(vulnCheckStatus);
        vulCheckStatuses.add(vulnCheckStatus1);

        Map<String, List<VulnerabilityCheckStatus>> releaseIdToStatus = new HashMap<String, List<VulnerabilityCheckStatus>>();
        releaseIdToStatus.put("21055", vulCheckStatuses);

        ProjectVulnerabilityRating projVulnRating = new ProjectVulnerabilityRating();
        projVulnRating.setProjectId(project.getId());
        Map<String, Map<String, List<VulnerabilityCheckStatus>>> vulnerabilityIdToReleaseIdToStatus = new HashMap<String, Map<String, List<VulnerabilityCheckStatus>>>();
        vulnerabilityIdToReleaseIdToStatus.put("12345", releaseIdToStatus);
        vulnerabilityIdToReleaseIdToStatus.put("23105", releaseIdToStatus);
        projVulnRating.setVulnerabilityIdToReleaseIdToStatus(vulnerabilityIdToReleaseIdToStatus);
        List<ProjectVulnerabilityRating> projVulnRatings = new ArrayList<ProjectVulnerabilityRating>();
        projVulnRatings.add(projVulnRating);
        given(this.vulnerabilityMockService.getProjectVulnerabilityRatingByProjectId(any(), any())).willReturn(projVulnRatings);

        Map<String, VulnerabilityRatingForProject> relIdToprojVUlRating = new HashMap<String, VulnerabilityRatingForProject>();
        relIdToprojVUlRating.put("21055", VulnerabilityRatingForProject.IRRELEVANT);

        Map<String, VulnerabilityRatingForProject> relIdToprojVUlRating1 = new HashMap<String, VulnerabilityRatingForProject>();
        relIdToprojVUlRating1.put("21055", VulnerabilityRatingForProject.APPLICABLE);

        Map<String, Map<String, VulnerabilityRatingForProject>> vulIdToRelIdToRatings = new HashMap<String, Map<String, VulnerabilityRatingForProject>>();
        vulIdToRelIdToRatings.put("12345", relIdToprojVUlRating);
        vulIdToRelIdToRatings.put("23105", relIdToprojVUlRating1);

        given(this.vulnerabilityMockService.fillVulnerabilityMetadata(any(), any())).willReturn(vulIdToRelIdToRatings);
        given(this.vulnerabilityMockService.updateProjectVulnerabilityRating(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.getReleasesFromProjectIds(any(), anyBoolean(), any(), any())).willReturn(Set.of(rel));
        given(this.projectServiceMock.getLinkedReleasesOfSubProjects(any(), any())).willReturn(List.of(release, release2));
        given(this.attachmentServiceMock.getAttachmentResourcesFromList(any(), any(), any())).willReturn(CollectionModel.of(attachmentResources));
    }

    @Test
    public void should_document_get_projects() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                //subsectionWithPath("_embedded.sw360:projects.[]packageIds").description("List of package IDs associated with the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_usedbyresource_for_project() throws Exception {
        mockMvc.perform(get("/api/projects/usedBy/" + project.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]businessUnit").description("The business unit this project belongs to"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_obligations_from_license_db() throws Exception {
        mockMvc.perform(get("/api/projects/" + project8.getId() + "/licenseDbObligations")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page")
                        ),
                        responseFields(
                                subsectionWithPath("obligations.obligation_title").description("Title of license obligation"),
                                subsectionWithPath("obligations.obligation_title.text").description("Text of license obligation"),
                                subsectionWithPath("obligations.obligation_title.licenseIds[]").description("List of licenseIds"),
                                subsectionWithPath("obligations.obligation_title.id").description("Id of the obligation"),
                                subsectionWithPath("obligations.obligation_title.releaseIdToAcceptedCLI").description("Releases having accepted attachments"),
                                subsectionWithPath("obligations.obligation_title.obligationType").description("Type of the obligation"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of obligations per page"),
                                fieldWithPath("page.totalElements").description("Total number of all license obligations"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page"))));
    }

    @Test
    public void should_document_add_obligations_from_license_db() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/api/projects/" + "123456733" + "/licenseObligation");
        List<String> licenseObligationIds = Arrays.asList("0001");

        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(licenseObligationIds))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isCreated());
    }

    @Test
    public void should_document_update_license_obligations() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/projects/" + project8.getId() + "/updateLicenseObligation");
        ObligationStatusInfo obligationStatusInfo = new ObligationStatusInfo();
        obligationStatusInfo.setComment("updating comment");
        obligationStatusInfo.setStatus(ObligationStatus.ESCALATED);
        Map<String, ObligationStatusInfo> licOblMap = new HashMap<>();
        licOblMap.put("obligation_title", obligationStatusInfo);

        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(licOblMap))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isCreated());
    }

    @Test
    public void should_document_get_attachment_usage_for_project() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/attachmentUsage")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("transitive", "true")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("transitive").description("Get the transitive releases")
                        ),
                        responseFields(
                                subsectionWithPath("releaseIdToUsage").description("The relationship between linked releases of the project"),
                                subsectionWithPath("linkedProjects").description("The linked projects"),
                                subsectionWithPath("_embedded.sw360:release").description("An array of linked releases"),
                                subsectionWithPath("_embedded.sw360:attachmentUsages").description("An array of project's attachment usages")
                        )));
    }

    @Test
    public void should_document_get_projects_with_all_details() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("allDetails", "true")
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("allDetails").description("Flag to get projects with all details. Possible values are `<true|false>`"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]id").description("The id of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]createdOn").description("The date the project was created"),
                                subsectionWithPath("_embedded.sw360:projects.[]description").description("The project description"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects.[]domain").description("The domain, possible values are:"  + Sw360ResourceServer.DEFAULT_DOMAINS.toString()).optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:projects.[]businessUnit").description("The business unit this project belongs to"),
                                subsectionWithPath("_embedded.sw360:projects.[]externalIds").description("When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("_embedded.sw360:projects.[]additionalData").description("A place to store additional data used by external tools").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]ownerAccountingUnit").description("The owner accounting unit of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]ownerGroup").description("The owner group of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]ownerCountry").description("The owner country of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]obligationsText").description("The obligations text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]clearingSummary").description("The clearing summary text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]specialRisksOSS").description("The special risks OSS text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]generalRisks3rdParty").description("The general risks 3rd party text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]specialRisks3rdParty").description("The special risks 3rd party text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]deliveryChannels").description("The sales and delivery channels text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]remarksAdditionalRequirements").description("The remark additional requirements text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]tag").description("The project tag"),
                                subsectionWithPath("_embedded.sw360:projects.[]deliveryStart").description("The project delivery start date"),
                                subsectionWithPath("_embedded.sw360:projects.[]preevaluationDeadline").description("The project preevaluation deadline"),
                                subsectionWithPath("_embedded.sw360:projects.[]systemTestStart").description("Date of the project system begin phase"),
                                subsectionWithPath("_embedded.sw360:projects.[]systemTestEnd").description("Date of the project system end phase"),
                                subsectionWithPath("_embedded.sw360:projects.[]linkedProjects").description("The relationship between linked projects of the project").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]linkedReleases").description("The relationship between linked releases of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]securityResponsibles").description("An array of users responsible for security of the project."),
                                subsectionWithPath("_embedded.sw360:projects.[]projectResponsible").description("A user who is responsible for the project."),
                                subsectionWithPath("_embedded.sw360:projects.[]enableSvm").description("Security vulnerability monitoring flag"),
                                subsectionWithPath("_embedded.sw360:projects.[]enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                subsectionWithPath("_embedded.sw360:projects[]considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                subsectionWithPath("_embedded.sw360:projects.[]state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                subsectionWithPath("_embedded.sw360:projects.[]phaseOutSince").description("The project phase-out date"),
                                subsectionWithPath("_embedded.sw360:projects.[]clearingRequestId").description("Clearing Request id associated with project."),
                                subsectionWithPath("_embedded.sw360:projects.[]_links").description("Self <<resources-index-links,Links>> to Project resource"),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.createdBy").description("The user who created this project"),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.clearingTeam").type(JsonFieldType.STRING).description("The clearingTeam of the project").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.homepage").description("The homepage url of the project").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.wiki").description("The wiki url of the project").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]licenseInfoHeaderText").description("The licenseInfoHeaderText text of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]externalUrls").description("A place to store additional data used by external tools").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.sw360:moderators").description("An array of all project moderators with email").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.sw360:contributors").type(JsonFieldType.ARRAY).description("An array of all project contributors with email").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]_embedded.sw360:attachments").description("An array of all project attachments").optional(),
                                subsectionWithPath("_embedded.sw360:projects.[]vendor").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:projects[].packageIds").description("List of package IDs associated with each project."),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_project() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-projects,Projects resource>>")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the project"),
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("version").description("The project version"),
                                //fieldWithPath("packageIds").description("List of package IDs associated with the project."),
                                fieldWithPath("packageIds").description("Map of package IDs to their relationship metadata, including comment, creation date and creator"),
                                fieldWithPath("packageIds.*.comment").description("Comment about the package relationship"),
                                fieldWithPath("packageIds.*.createdOn").description("Date when the package relationship was created"),
                                fieldWithPath("packageIds.*.createdBy").description("User who created the package relationship"),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("domain").description("The domain, possible values are:"  + Sw360ResourceServer.DEFAULT_DOMAINS.toString()),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                                subsectionWithPath("externalIds").description("When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
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
                                subsectionWithPath("linkedProjects").description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                subsectionWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                fieldWithPath("projectResponsible").description("A user who is responsible for the project."),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                fieldWithPath("licenseInfoHeaderText").description("The licenseInfoHeaderText text of the project"),
                                subsectionWithPath("externalUrls").description("A place to store additional data used by external URLs"),
                                fieldWithPath("clearingRequestId").description("Clearing Request id associated with project."),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this project"),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_embedded.sw360:packages").description("An array of linked <<resources-packages, Packages resources>>"),
                                subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                subsectionWithPath("_embedded.sw360:moderators").description("An array of all project moderators with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:attachments").description("An array of all project attachments and link to their <<resources-attachment-get,Attachment resource>>")
                        )));
    }

    @Test
    public void should_document_get_projects_by_type() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("type", project.getProjectType().toString())
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("type").description("Project types = `{CUSTOMER, INTERNAL, PRODUCT, SERVICE, INNER_SOURCE}`"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects.[]visibility")
                                        .description("The visibility of the project, possible values are: "
                                                + Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:projects")
                                        .description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links")
                                        .description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_license_obligations() throws Exception {
        mockMvc.perform(get("/api/projects/" + project8.getId() + "/licenseObligations")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page")
                        ),
                        responseFields(
                                subsectionWithPath("obligations.obligation_title").description("Title of license obligation"),
                                subsectionWithPath("obligations.obligation_title.text").description("Text of license obligation"),
                                subsectionWithPath("obligations.obligation_title.releaseIdToAcceptedCLI").description("Release Ids having accepted attachments"),
                                subsectionWithPath("obligations.obligation_title.licenseIds[]").description("List of licenseIds"),
                                subsectionWithPath("obligations.obligation_title.comment").description("Comment on the obligation"),
                                subsectionWithPath("obligations.obligation_title.status").description("Status of the obligation"),
                                subsectionWithPath("obligations.obligation_title.id").description("Id of the obligation"),
                                subsectionWithPath("obligations.obligation_title.obligationType").description("Type of the obligation"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of obligations per page"),
                                fieldWithPath("page.totalElements").description("Total number of all license obligations"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_project_page_obligations() throws Exception {
        mockMvc.perform(get("/api/projects/" + project8.getId() + "/obligation")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .param("obligationLevel", "License")
                        .param("page", "0")
                        .param("page_entries", "5")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("obligationLevel").description("Possible values are: [LICENSE, PROJECT, COMPONENT or ORGANIZATION]"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page")
                        ),
                        responseFields(
                                subsectionWithPath("obligations.obligation_title").description("Title of license obligation"),
                                subsectionWithPath("obligations.obligation_title.text").description("Text of license obligation"),
                                subsectionWithPath("obligations.obligation_title.releaseIdToAcceptedCLI").description("Release Ids having accepted attachments"),
                                subsectionWithPath("obligations.obligation_title.licenseIds[]").description("List of licenseIds"),
                                subsectionWithPath("obligations.obligation_title.comment").description("Comment on the obligation"),
                                subsectionWithPath("obligations.obligation_title.status").description("Status of the obligation"),
                                subsectionWithPath("obligations.obligation_title.id").description("Id of the obligation"),
                                subsectionWithPath("obligations.obligation_title.obligationType").description("Type of the obligation"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of obligations per page"),
                                fieldWithPath("page.totalElements").description("Total number of all license obligations"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_projects_by_group() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("group", project.getBusinessUnit())
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("group").description("The project group"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects[]visibility").description("The visibility of the project, possible values are: " + Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_projects_by_tag() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("tag", project.getTag())
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("tag").description("The project tag"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects[]visibility").description("The visibility of the project, possible values are: " + Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_projects_by_name() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("name", project.getName())
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("name").description("The name of the project"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects.[]visibility").description("The visibility of the project, possible values are: "+ Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_projects_by_lucene_search() throws Exception {
        mockMvc.perform(get("/api/projects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("name", project.getName())
                .queryParam("type", project.getProjectType().name())
                .queryParam("group", project.getBusinessUnit())
                .queryParam("tag", project.getTag())
                .queryParam("luceneSearch", "true")
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("name").description("The name of the project"),
                                parameterWithName("type").description("The type of the project"),
                                parameterWithName("group").description("The group of the project"),
                                parameterWithName("tag").description("The tag of the project"),
                                parameterWithName("luceneSearch").description("List project by lucene search"),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects.[]visibility").description("The visibility of the project, possible values are: "+ Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_projects_by_externalIds() throws Exception {
        Map<String, Set<String>> externalIdsQuery = new HashMap<>();
        externalIdsQuery.put("portal-id", new HashSet<>(Collections.singletonList("13319-XX3")));
        externalIdsQuery.put("project-ext", new HashSet<>(Arrays.asList("515432", "7657")));
        given(this.projectServiceMock.searchByExternalIds(eq(externalIdsQuery), any())).willReturn((new HashSet<>(projectList)));

        mockMvc.perform(get("/api/projects/searchByExternalIds?project-ext=515432&project-ext=7657&portal-id=13319-XX3")
                .contentType(MediaTypes.HAL_JSON)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]externalIds").description("External Ids of the project. Return as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_license_clearing() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/licenseClearing")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("transitive", "true")
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("transitive").description("Get the transitive releases"),
                                parameterWithName("page").description("Page of releases"),
                                parameterWithName("page_entries").description("Amount of releases page"),
                                parameterWithName("sort").description("Defines order of the releases")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the project"),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                subsectionWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                subsectionWithPath("linkedProjects").description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                subsectionWithPath("_embedded.sw360:release").description("An array of linked releases"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_linked_projects() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/linkedProjects")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("transitive", "false")
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects page"),
                                parameterWithName("sort").description("Defines order of the projects"),
                                parameterWithName("transitive").description("Get the transitive projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_linked_projects_transitive() throws Exception {
        mockMvc.perform(get("/api/projects/" + "12345" + "/linkedProjects?transitive=true")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects page"),
                                parameterWithName("sort").description("Defines order of the projects"),
                                parameterWithName("transitive").description("Get the transitive projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_project_releases() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/releases")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("transitive", "false")
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("transitive").description("Get the transitive releases"),
                                parameterWithName("page").description("Page of releases"),
                                parameterWithName("page_entries").description("Amount of releases page"),
                                parameterWithName("sort").description("Defines order of the releases")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
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
    public void should_document_get_project_vulnerabilities() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/vulnerabilities")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("priority", "1 - critical")
                .queryParam("priority", "2 - major")
                .queryParam("projectRelevance", "IRRELEVANT")
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "externalId,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("priority").description("The priority of vulnerability. For example: `1 - critical`, `2 - major`"),
                                parameterWithName("projectRelevance").description("The relevance of project of the vulnerability, possible values are: " + Arrays.asList(VulnerabilityRatingForProject.values())),
                                parameterWithName("page").description("Page of vulnerabilities"),
                                parameterWithName("page_entries").description("Amount of vulnerability page"),
                                parameterWithName("sort").description("Defines order of the vulnerability. It can be done based on " + Arrays.asList(VulnerabilityDTO._Fields.EXTERNAL_ID.getFieldName(), VulnerabilityDTO._Fields.TITLE.getFieldName(), VulnerabilityDTO._Fields.PRIORITY.getFieldName(), VulnerabilityDTO._Fields.PROJECT_RELEVANCE.getFieldName()))
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]priority").description("The priority of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectAction").description("The action of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectRelevance").description("The relevance of project of the vulnerability, possible values are: " + Arrays.asList(VulnerabilityRatingForProject.values())),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]comment").description("Any message to added while updating project vulnerabilities"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]intReleaseId").description("The release id"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]intReleaseName").description("The release name"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]title").description("The title"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes").description("An array of <<resources-vulnerabilities, Vulnerability resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of vulnerability per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing vulnerability"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_project_vulnerability_summary() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/vulnerabilitySummary")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .param("page", "0")
                        .param("page_entries", "5")
                        .param("sort", "externalId,desc")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of vulnerabilities"),
                                parameterWithName("page_entries").description("Amount of vulnerability page"),
                                parameterWithName("sort").description("Defines order of the vulnerability on the basis of externalId")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries.[]priority").description("The priority of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries.[]action").description("The action of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries.[]projectRelevance").description("The relevance of project of the vulnerability, possible values are: " + Arrays.asList(VulnerabilityRatingForProject.values())),
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries.[]comment").description("Any message to added while updating project vulnerabilities"),
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries.[]intReleaseId").description("The release id"),
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries.[]intReleaseName").description("The release name"),
                                subsectionWithPath("_embedded.sw360:vulnerabilitySummaries").description("An array of <<resources-vulnerabilities, Vulnerability resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of vulnerability per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing vulnerability"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_project_vulnerabilities_by_externalid() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/vulnerabilities")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("releaseId", "21055")
                .queryParam("externalId", "12345")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("releaseId").description("The release Id of vulnerability."),
                                parameterWithName("externalId").description("The external Id of vulnerability.")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]priority").description("The priority of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectAction").description("The action of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectRelevance").description("The relevance of project of the vulnerability, possible values are: " + Arrays.asList(VulnerabilityRatingForProject.values())),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]comment").description("Any message to added while updating project vulnerabilities"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]intReleaseId").description("The release id"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]intReleaseName").description("The release name"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes").description("An array of <<resources-vulnerabilities, Vulnerability resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_project_vulnerabilities() throws Exception {
        Map<String, String> vulDtoMap = new HashMap<>();
        vulDtoMap.put("externalId", "12345");
        vulDtoMap.put("projectRelevance", "IRRELEVANT");
        vulDtoMap.put("comment", "Lorem Ipsum");
        vulDtoMap.put("intReleaseId", "21055");
        vulDtoMap.put("projectAction", "Lorem Ipsum");
        List<Map<String, String>> vulDtoMaps = new ArrayList<Map<String, String>>();
        vulDtoMaps.add(vulDtoMap);
        mockMvc.perform(patch("/api/projects/" + project.getId() + "/vulnerabilities")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(vulDtoMaps))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectRelevance").description("The relevance of project of the vulnerability, possible values are: " + Arrays.asList(VulnerabilityRatingForProject.values())),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]intReleaseId").description("The release id"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]comment").description("Any message to add while updating project vulnerabilities"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectAction").description("The action of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes").description("An array of <<resources-vulnerabilities, Vulnerability resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_linked_project_releases() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/linkedProjects/releases")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of releases"),
                                parameterWithName("page_entries").description("Amount of releases page"),
                                parameterWithName("sort").description("Defines order of the releases")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
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
    public void should_document_get_project_releases_transitive() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/releases?transitive=true")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project_releases_ecc_information() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/releases/ecc?transitive=false")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .queryParam("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("transitive").description("Get the transitive releases"),
                                parameterWithName("page").description("Page of releases"),
                                parameterWithName("page_entries").description("Amount of releases per page"),
                                parameterWithName("sort").description("Defines order of the releases")
                        ),
                        links(
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page"),
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_embedded.sw360:releases.[].eccInformation.eccStatus").description("The ECC information status value"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of releases per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing releases"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_project_attachment_info() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/attachments")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:attachments").description("An array of <<resources-attachment, Attachments resources>>"),
                                subsectionWithPath("_embedded.sw360:attachments.[]filename").description("The attachment filename"),
                                subsectionWithPath("_embedded.sw360:attachments.[]sha1").description("The attachment sha1 value"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_project_attachment_info() throws Exception {
        Attachment updateAttachment = new Attachment().setAttachmentType(AttachmentType.BINARY)
                .setCreatedComment("Created Comment").setCheckStatus(CheckStatus.ACCEPTED)
                .setCheckedComment("Checked Comment");
        this.mockMvc
                .perform(patch("/api/projects/98745/attachment/1234").contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateAttachment))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                requestFields(
                        fieldWithPath("attachmentType").description("The type of Attachment. Possible Values are: "+Arrays.asList(AttachmentType.values())),
                        fieldWithPath("createdComment").description("The upload Comment of Attachment"),
                        fieldWithPath("checkStatus").description("The checkStatus of Attachment. Possible Values are: "+Arrays.asList(CheckStatus.values())),
                        fieldWithPath("checkedComment").description("The checked Comment of Attachment")),
                responseFields(
                        fieldWithPath("attachmentContentId").description("The attachment content id"),
                        fieldWithPath("filename").description("The attachment filename"),
                        fieldWithPath("sha1").description("The attachment sha1 value"),
                        fieldWithPath("attachmentType").description("The type of attachment. Possible Values are: "+Arrays.asList(AttachmentType.values())),
                        fieldWithPath("createdBy").description("The email of user who uploaded the attachment"),
                        fieldWithPath("createdTeam").description("The department of user who uploaded the attachment"),
                        fieldWithPath("createdOn").description("The date when attachment was uploaded"),
                        fieldWithPath("createdComment").description("The upload Comment of attachment"),
                        fieldWithPath("checkStatus").description("The checkStatus of attachment. Possible Values are: "+Arrays.asList(CheckStatus.values())),
                        fieldWithPath("checkedComment").description("The checked comment of attachment"),
                        fieldWithPath("checkedBy").description("The email of user who checked the attachment"),
                        fieldWithPath("checkedTeam").description("The department of user who checked the attachment"),
                        fieldWithPath("checkedOn").description("The date when attachment was checked"),
                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                )));
    }

    @Test
    public void should_document_get_project_attachment() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept("application/*"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_create_clearing_request() throws Exception {
        Map<String, Object> cr = new HashMap<>();
        cr.put("clearingType", ClearingRequestType.DEEP.toString());
        cr.put("clearingTeam", "clearingTeam@sw360.org");
        LocalDate requestedClearingDate = LocalDate.now().plusDays(7);
        cr.put("requestedClearingDate", requestedClearingDate.toString());
        cr.put("requestingUserComment", "New clearing");
        cr.put("priority", "HIGH");
        this.mockMvc.perform(post("/api/projects/" + project.getId() + "/clearingRequest")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(cr))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        links(linkWithRel("self").description("The <<resources-projects,Projects resource>>")),
                        requestFields(fieldWithPath("clearingTeam").description("Email of the clearing team. This is a mandatory field."),
                                fieldWithPath("requestedClearingDate").description(
                                        "Requested clearing date of the project. It should be in the format yyyy-MM-dd and requested after the "
                                        + "configured clearing date limit. This is a mandatory field."),
                                fieldWithPath("clearingType").description("Clearing type of the project. Possible values are: "
                                        + Arrays.asList(ClearingRequestType.values()).toString() + ". This is a mandatory field."),
                                fieldWithPath("requestingUserComment").description("Requesting user comment on the clearing of the project."),
                                fieldWithPath("priority")
                                        .description("Priority of the clearing request. Possible values are: "
                                                + Arrays.asList(ClearingRequestPriority.values()).toString())
                                ),
                        responseFields(fieldWithPath("id").description("Clearing request id."),
                                fieldWithPath("projectId").description("Project id associated with clearing request."),
                                fieldWithPath("clearingState").description("Clearing state of the project."),
                                fieldWithPath("clearingTeam").description("Clearing team of the project."),
                                fieldWithPath("requestedClearingDate")
                                        .description("Requested clearing date of the project."),
                                fieldWithPath("clearingType").description("Clearing type of the project."),
                                fieldWithPath("requestingUser").description("User requesting the clearing of the project."),
                                fieldWithPath("requestingUserComment").description("Requesting user comment on the clearing of the project."),
                                fieldWithPath("priority").description("Priority of the clearing request."),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                  )));
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
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put("376576", new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true));
        project.put("linkedProjects", linkedProjects);
        project.put("leadArchitect", "lead@sw360.org");
        project.put("moderators", new HashSet<>(Arrays.asList("moderator1@sw360.org", "moderator2@sw360.org")));
        project.put("contributors", new HashSet<>(Arrays.asList("contributor1@sw360.org", "contributor2@sw360.org")));
        project.put("state", ProjectState.ACTIVE.toString());
        project.put("phaseOutSince", "2020-06-24");

        this.mockMvc.perform(post("/api/projects")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(project))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("_embedded.createdBy.email", Matchers.is("admin@sw360.org")))
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("version").description("The version of the new project"),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                subsectionWithPath("linkedProjects").description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                fieldWithPath("leadArchitect").description("The lead architect of the project"),
                                fieldWithPath("contributors").description("An array of contributors to the project"),
                                fieldWithPath("moderators").description("An array of moderators"),
                                fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                fieldWithPath("phaseOutSince").description("The project phase-out date")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The project id"),
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("version").description("The project version"),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this project")
                        )));
    }
    @Test
    public void should_document_create_duplicate_project() throws Exception {
        Map<String, Object> projectReqs = new HashMap<>();
        projectReqs.put("name", "Test Project");
        projectReqs.put("version", "1.0");
        projectReqs.put("visibility", "PRIVATE");
        projectReqs.put("description", "This is the description of my Test Project");
        projectReqs.put("projectType", ProjectType.PRODUCT.toString());
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        releaseIdToUsage.put("3765276512", new ProjectReleaseRelationship(CONTAINED, OPEN));
        projectReqs.put("linkedReleases", releaseIdToUsage);
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put("376576", new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true));
        projectReqs.put("linkedProjects", linkedProjects);
        projectReqs.put("leadArchitect", "lead@sw360.org");
        projectReqs.put("moderators", new HashSet<>(Arrays.asList("moderator1@sw360.org", "moderator2@sw360.org")));
        projectReqs.put("contributors", new HashSet<>(Arrays.asList("contributor1@sw360.org", "contributor2@sw360.org")));
        projectReqs.put("state", ProjectState.ACTIVE.toString());
        projectReqs.put("phaseOutSince", "2020-06-24");

        this.mockMvc.perform(post("/api/projects/duplicate/" + project.getId())
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(projectReqs))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("_embedded.createdBy.email", Matchers.is("admin@sw360.org")))
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("version").description("The version of the new project"),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("linkedReleases").description("The relationship between linked releases of the project"),
                                subsectionWithPath("linkedProjects").description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                fieldWithPath("leadArchitect").description("The lead architect of the project"),
                                fieldWithPath("contributors").description("An array of contributors to the project"),
                                fieldWithPath("moderators").description("An array of moderators"),
                                fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                fieldWithPath("phaseOutSince").description("The project phase-out date")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The project id"),
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("version").description("The project version"),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this project")
                        )));
    }

    @Test
    public void should_document_update_project() throws Exception {
        project.setClearingState(ProjectClearingState.OPEN);
        Project updateProject = new Project();
        updateProject.setName("updated project");
        updateProject.setDescription("Project description updated");
        updateProject.setVersion("1.0");
        updateProject.setState(ProjectState.PHASE_OUT);
        updateProject.setPhaseOutSince("2020-06-24");
        this.mockMvc
                .perform(patch("/api/projects/"+project.getId()).contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateProject))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON))
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
                        fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                        fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                        fieldWithPath("phaseOutSince").description("The project phase-out date"),
                        fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag.")),
                responseFields(
                        fieldWithPath("id").description("The project id"),
                        fieldWithPath("name").description("The name of the project"),
                        fieldWithPath("version").description("The project version"),
                        fieldWithPath("createdOn").description("The date the project was created"),
                        fieldWithPath("description").description("The project description"),
                        fieldWithPath("domain").description("The domain, possible values are:"  + Sw360ResourceServer.DEFAULT_DOMAINS.toString()),
                        fieldWithPath("projectType").description("The project type, possible values are: "
                                + Arrays.asList(ProjectType.values())),
                        fieldWithPath("visibility").description("The project visibility, possible values are: "
                                + Arrays.asList(Visibility.values())),
                        fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                        subsectionWithPath("externalIds").description(
                                "When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                        subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
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
                        fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                        fieldWithPath("phaseOutSince").description("The project phase-out date"),
                        subsectionWithPath("linkedProjects")
                                .description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                        subsectionWithPath("linkedReleases")
                                .description("The relationship between linked releases of the project"),
                        fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                        fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                        fieldWithPath("clearingRequestId").description("Clearing Request id associated with project."),
                        fieldWithPath("projectResponsible").description("A user who is responsible for the project."),
                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                        subsectionWithPath("_embedded.createdBy").description("The user who created this project"),
                        fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                        fieldWithPath("licenseInfoHeaderText").description("The licenseInfoHeaderText text of the project"),
                        subsectionWithPath("externalUrls").description("A place to store additional data used by external URLs"),
                        fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                        fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                        fieldWithPath("clearingState").description("The clearingState of the project"),
                        subsectionWithPath("packageIds").description("List of package IDs associated with the project"),
                        subsectionWithPath("_embedded.sw360:moderators").description("An array of moderators"),
                        subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                        subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                        subsectionWithPath("_embedded.sw360:packages").description("An array of linked <<resources-packages, Packages resources>>"),
                        subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                        subsectionWithPath("_embedded.sw360:attachments").description("An array of all project attachments and link to their <<resources-attachment-get,Attachment resource>>"))));
    }

    @Test
    public void should_document_link_projects() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/api/projects/" + "1234567" + "/linkProjects");
        List<String> projectIds = Arrays.asList("345678");

        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(projectIds))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("Message regarding successfully linked project(s)").description("project linked to respective project ids").optional()
                        )));
    }

    @Test
    public void should_document_save_usages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/api/projects/" + "0000007" + "/saveAttachmentUsages");
        Map<String, List<String>> usages = Map.of(
            "selected", new ArrayList<>(List.of("00000071_sourcePackage_1234")),
            "deselected", new ArrayList<>(List.of()),
            "selectedConcludedUsages", new ArrayList<>(List.of()),
            "deselectedConcludedUsages", new ArrayList<>(List.of()));

        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(usages))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated());
    }

    @Test
    public void should_document_upload_attachment_to_project() throws Exception {
        testAttachmentUploadProject("/api/projects/", project.getId());
    }


    @Test
    public void should_document_link_releases() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/api/projects/" + project.getId() + "/releases");
        add_patch_releases(requestBuilder);
    }

    @Test
    public void should_document_link_releases_with_project_release_relation() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/api/projects/" + project.getId() + "/releases");
        add_patch_releases_with_project_release_relation(requestBuilder);
    }

    @Test
    public void should_document_patch_releases() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/projects/" + project.getId() + "/releases");
        add_patch_releases(requestBuilder);
    }

    @Test
    public void should_document_patch_releases_with_project_release_relation() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/projects/" + project.getId() + "/releases");
        add_patch_releases_with_project_release_relation(requestBuilder);
    }

    @Test
    public void should_document_link_packages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/projects/" + project.getId() + "/link/packages");
        link_unlink_packages(requestBuilder);
    }

    @Test
    public void should_document_unlink_packages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/projects/" + project.getId() + "/unlink/packages");
        link_unlink_packages(requestBuilder);
    }

    @Test
    public void should_document_update_project_release_relationship() throws Exception {
        ProjectReleaseRelationship updateProjectReleaseRelationship = new ProjectReleaseRelationship()
                .setComment("Test Comment").setMainlineState(MainlineState.SPECIFIC)
                .setReleaseRelation(ReleaseRelationship.STANDALONE);
        this.mockMvc
                .perform(patch("/api/projects/376576/release/3765276512").contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateProjectReleaseRelationship))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                requestFields(
                        fieldWithPath("releaseRelation").description("The relation of linked release. Possible Values are: "+Arrays.asList(ReleaseRelationship.values())),
                        fieldWithPath("mainlineState").description("The mainlineState of linked release. Possible Values are: "+Arrays.asList(MainlineState.values())),
                        fieldWithPath("comment").description("The Comment for linked release")),
                responseFields(
                        fieldWithPath("releaseRelation").description("The relation of linked release. Possible Values are: "+Arrays.asList(ReleaseRelationship.values())),
                        fieldWithPath("mainlineState").description("The mainlineState of linked release. Possible Values are: "+Arrays.asList(MainlineState.values())),
                        fieldWithPath("comment").description("The Comment for linked release"),
                        fieldWithPath("createdOn").description("The date when release was linked to project"),
                        fieldWithPath("createdBy").description("The email of user who linked release to project")
                )));
    }

    @Test
    public void should_document_get_download_license_info() throws Exception {
        this.mockMvc.perform(get("/api/projects/" + project.getId()+ "/licenseinfo?generatorClassName=XhtmlGenerator&variant=DISCLOSURE&externalIds=portal-id,main-project-id")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .param("module", "licenseInfo")
                .param("projectId", project.getId())
                .param("generatorClassName", "XhtmlGenerator")
                .param("variant", "DISCLOSURE")
                .param("externalIds", "portal-id,main-project-id")
                .accept("application/xhtml+xml"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler
                        .document(queryParameters(
                                parameterWithName("generatorClassName").description("Projects download format. Possible values are `<DocxGenerator|XhtmlGenerator|TextGenerator>`"),
                                parameterWithName("variant").description("All the possible values for variants are `<REPORT|DISCLOSURE>`"),
                                parameterWithName("externalIds").description("The external Ids of the project")
                                )));
    }

    @Test
    public void should_document_get_download_license_info_without_release_version() throws Exception {
        this.mockMvc.perform(get("/api/projects/" + project.getId()+ "/licenseinfo?generatorClassName=XhtmlGenerator&variant=DISCLOSURE&excludeReleaseVersion=true")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept("application/xhtml+xml"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler
                        .document(queryParameters(
                                parameterWithName("generatorClassName")
                                        .description("All possible values for output generator class names are "
                                                + Arrays.asList("DocxGenerator", "XhtmlGenerator", "TextGenerator")),
                                parameterWithName("variant").description("All the possible values for variants are "
                                        + Arrays.asList(OutputFormatVariant.values())),
                                parameterWithName("excludeReleaseVersion").description("Exclude version of the components from the generated license info file. "
                                		+ "Possible values are `<true|false>`")
                                )));
    }

    @Test
    public void should_document_get_download_license_info_with_all_attachemnts() throws Exception {
        this.mockMvc.perform(get("/api/projects/" + project.getId()+ "/licenseinfo?generatorClassName=XhtmlGenerator&variant=DISCLOSURE&includeAllAttachments=true")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept("application/xhtml+xml"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler
                        .document(queryParameters(
                                parameterWithName("generatorClassName")
                                        .description("All possible values for output generator class names are "
                                                + Arrays.asList("DocxGenerator", "XhtmlGenerator", "TextGenerator")),
                                parameterWithName("variant").description("All the possible values for variants are "
                                        + Arrays.asList(OutputFormatVariant.values())),
                                parameterWithName("includeAllAttachments").description("Set this option to `true` to include all attachments from linked releases. "
                                        + "Note that only one attachment per release will be parsed for "
                                        + "license information, and if available, a CLX file will be preferred over an ISR file."))));
    }

    @Test
    public void should_document_get_projects_releases() throws Exception {
        this.mockMvc.perform(get("/api/projects/releases")
                 .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                 .queryParam("clearingState", ClearingState.APPROVED.toString())
                 .queryParam("transitive", "false")
                 .queryParam("page", "0")
                 .queryParam("page_entries", "5")
                 .queryParam("sort", "name,desc")
                 .content(this.objectMapper.writeValueAsString(List.of("376576","376570")))
                 .contentType(MediaTypes.HAL_JSON)
                 .accept(MediaTypes.HAL_JSON))
                 .andExpect(status().isOk())
                 .andDo(this.documentationHandler.document(
                             links(
                                     linkWithRel("curies").description("Curies are used for online documentation"),
                                     linkWithRel("first").description("Link to first page"),
                                     linkWithRel("last").description("Link to last page")
                                     ),
                             queryParameters(
                                     parameterWithName("transitive").description("Get the transitive releases"),
                                     parameterWithName("clearingState").description("The clearing state of the release. Possible values are: "+Arrays.asList(ClearingState.values())),
                                     parameterWithName("page").description("Page of releases"),
                                     parameterWithName("page_entries").description("Amount of releases page"),
                                     parameterWithName("sort").description("Defines order of the releases")
                                     ),
                             responseFields(
                                     subsectionWithPath("_embedded.sw360:releases.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                     subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release, optional"),
                                     subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]createdBy").description("Email of the release creator"),
                                     subsectionWithPath("_embedded.sw360:releases.[]componentId").description("The component id"),
                                     subsectionWithPath("_embedded.sw360:releases.[]packageIds").description("The component id"),
                                     subsectionWithPath("_embedded.sw360:releases.[]id").description("Id of the release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]cpeid").description("CpeId of the release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                                     subsectionWithPath("_embedded.sw360:releases.[]releaseDate").description("The date of this release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]createdOn").description("The creation date of the internal sw360 release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                                     subsectionWithPath("_embedded.sw360:releases.[]sourceCodeDownloadurl").description("the source code download url of the release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]binaryDownloadurl").description("the binary download url of the release"),
                                     subsectionWithPath("_embedded.sw360:releases.[]externalIds").description("When releases are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                     subsectionWithPath("_embedded.sw360:releases.[]additionalData").description("A place to store additional data used by external tools"),
                                     subsectionWithPath("_embedded.sw360:releases.[]languages").description("The language of the component"),
                                     subsectionWithPath("_embedded.sw360:releases.[]mainLicenseIds").description("An array of all main licenses"),
                                     subsectionWithPath("_embedded.sw360:releases.[]operatingSystems").description("The OS on which the release operates"),
                                     subsectionWithPath("_embedded.sw360:releases.[]softwarePlatforms").description("The software platforms of the component"),
                                     subsectionWithPath("_embedded.sw360:releases.[]vendor").description("The Id of the vendor"),
                                     subsectionWithPath("_embedded.sw360:releases.[]_links").description("<<resources-release-get,Release>> to release resource"),
                                     subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                     fieldWithPath("page").description("Additional paging information"),
                                     fieldWithPath("page.size").description("Number of releases per page"),
                                     fieldWithPath("page.totalElements").description("Total number of all existing releases"),
                                     fieldWithPath("page.totalPages").description("Total number of pages"),
                                     fieldWithPath("page.number").description("Number of the current page")
                                     )
                             ));
    }

    @Test
    public void should_document_delete_project() throws Exception {
        mockMvc.perform(delete("/api/projects/" + project.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_remove_orphaned_obligations() throws Exception {
        List<String> orphanedObligationTitles = Arrays.asList("obligation_title");
        mockMvc.perform(patch("/api/projects/" + project8.getId() + "/orphanObligation")
                .content(this.objectMapper.writeValueAsString(orphanedObligationTitles))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    private void add_patch_releases(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        List<String> releaseIds = Arrays.asList("3765276512", "5578999", "3765276513");

        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(releaseIds))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isCreated());
    }

    private void link_unlink_packages(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        Set<String> packageIds = new HashSet<>();

        packageIds.add("9876746589");
        packageIds.add("4444444467");
        packageIds.add("5555555576");

        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(packageIds))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isCreated());
    }

    private void add_patch_releases_with_project_release_relation(MockHttpServletRequestBuilder requestBuilder)
            throws Exception {
        ProjectReleaseRelationship projectReleaseRelationship1 = new ProjectReleaseRelationship(
                ReleaseRelationship.REFERRED, MAINLINE);
        ProjectReleaseRelationship projectReleaseRelationship2 = new ProjectReleaseRelationship(
                ReleaseRelationship.STANDALONE, MainlineState.SPECIFIC).setComment("Test Comment 2");

        ImmutableMap<String, ProjectReleaseRelationship> releaseIdToUsage = ImmutableMap
                .<String, ProjectReleaseRelationship>builder().put("12345", projectReleaseRelationship1)
                .put("54321", projectReleaseRelationship2).build();
        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(releaseIdToUsage))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))).andExpect(status().isCreated());
    }

    @Test
    public void should_document_get_my_projects() throws Exception {
        mockMvc.perform(get("/api/projects/myprojects")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("createdBy", "true")
                        .queryParam("moderator", "true")
                        .queryParam("contributor", "true")
                        .queryParam("projectOwner", "true")
                        .queryParam("leadArchitect", "true")
                        .queryParam("projectResponsible", "true")
                        .queryParam("securityResponsible", "true")
                        .queryParam("stateOpen", "true")
                        .queryParam("stateClosed", "true")
                        .queryParam("stateInProgress", "true")
                        .queryParam("allDetails", "true")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("createdBy").description("Projects with current user as creator. Possible values are `<true|false>`"),
                                parameterWithName("moderator").description("Projects with current user as moderator. Possible values are `<true|false>`"),
                                parameterWithName("contributor").description("Projects with current user as contributor. Possible values are `<true|false>`"),
                                parameterWithName("projectOwner").description("Projects with current user as owner. Possible values are `<true|false>`"),
                                parameterWithName("leadArchitect").description("Projects with current user as lead architect. Possible values are `<true|false>`"),
                                parameterWithName("projectResponsible").description("Projects with current user as project responsible. Possible values are `<true|false>`"),
                                parameterWithName("securityResponsible").description("Projects with current user as security responsible. Possible values are `<true|false>`"),
                                parameterWithName("stateOpen").description("Projects with state as open. Possible values are `<true|false>`"),
                                parameterWithName("stateClosed").description("Projects with state as closed. Possible values are `<true|false>`"),
                                parameterWithName("stateInProgress").description("Projects with state as in progress. Possible values are `<true|false>`"),
                                parameterWithName("allDetails").description("Flag to get projects with all details. Possible values are `<true|false>`")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_import_spdx() throws Exception {
        given(this.attachmentServiceMock.isValidSbomFile(any(), any())).willReturn(true);
        MockMultipartFile file = new MockMultipartFile("file","file=@/bom.spdx.rdf".getBytes());
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/api/projects/import/SBOM")
                .content(file.getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("type", "SPDX");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_import_spdx_dry_run() throws Exception {
        given(this.attachmentServiceMock.isValidSbomFile(any(), any())).willReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "bom.spdx.rdf",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, "file=@/bom.spdx.rdf".getBytes());
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/projects/import/SBOM/dry-run")
                .file(file)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("type", "SPDX");
        this.mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus", Matchers.is("SUCCESS")))
                .andExpect(jsonPath("$.newComponents[0]", Matchers.is("new-component")))
                .andExpect(jsonPath("$.existingComponents[0]", Matchers.is("existing-component")))
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_project_count() throws Exception {
        this.mockMvc.perform(get("/api/projects/projectcount")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                       responseFields(
                               fieldWithPath("status").description("status of the API. Possible values are `<success|failure>`").optional(),
                               fieldWithPath("count").description("Count of projects for a user.").optional()
                       )));
    }

    @Test
    public void should_document_get_license_info_header() throws Exception {
        this.mockMvc.perform(get("/api/projects/licenseInfoHeader")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON)
                        .contentType(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("licenseInfoHeaderText").description("default license info header text").optional()
                        )));
    }

    @Test
    public void should_document_get_license_clearing_information() throws Exception {
        this.mockMvc.perform(get("/api/projects/" + project.getId()+ "/licenseClearingCount")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)
                .contentType(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                       responseFields(
                               fieldWithPath("releaseCount").description("Total count of releases of a project including sub-projects releases"),
                               fieldWithPath("approvedCount").description("Approved license clearing state releases")
                       )));
    }

    @Test
    public void should_document_get_license_clearing_details_count() throws Exception {
        this.mockMvc.perform(get("/api/projects/" + project.getId()+ "/clearingDetailsCount")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON)
                        .contentType(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("newClearing").description("Number of new releases for clearing"),
                                fieldWithPath("underClearing").description("Number of releases under clearing"),
                                fieldWithPath("sentToClearingTool").description("Number of releases sent to clearing tool"),
                                fieldWithPath("reportAvailable").description("Number of releases with report available"),
                                fieldWithPath("approved").description("Number of approved license clearing state"),
                                fieldWithPath("totalReleases").description("Total count of releases of a project including sub-projects releases")
                        )));
    }

    @Test
    public void should_document_create_summary_administration() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId()+ "/summaryAdministration")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("id").description("The project id"),
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("version").description("The project version"),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("domain").description("The domain, possible values are:"  + Sw360ResourceServer.DEFAULT_DOMAINS.toString()),
                                fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                subsectionWithPath("externalIds").description("When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the project"),
                                fieldWithPath("ownerGroup").description("The owner group of the project"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("ownerCountry").description("The owner country of the project"),
                                fieldWithPath("obligationsText").description("The obligations text of the project"),
                                fieldWithPath("clearingSummary").description("The clearing summary text of the project"),
                                fieldWithPath("specialRisksOSS").description("The special risks OSS text of the project"),
                                fieldWithPath("generalRisks3rdParty").description("The general risks 3rd party text of the project"),
                                fieldWithPath("specialRisks3rdParty").description("The special risks 3rd party text of the project"),
                                fieldWithPath("deliveryChannels").description("The sales and delivery channels text of the project"),
                                fieldWithPath("remarksAdditionalRequirements").description("The remark additional requirements text of the project"),
                                fieldWithPath("tag").description("The project tag"),
                                fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                                fieldWithPath("deliveryStart").description("The project delivery start date"),
                                fieldWithPath("preevaluationDeadline").description("The project preevaluation deadline"),
                                fieldWithPath("systemTestStart").description("Date of the project system begin phase"),
                                fieldWithPath("systemTestEnd").description("Date of the project system end phase"),
                                fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                fieldWithPath("clearingRequestId").description("Clearing Request id associated with project."),
                                fieldWithPath("licenseInfoHeaderText").description("LicenseInfoHeaderText associated with project."),
                                subsectionWithPath("externalUrls").description("A place to store additional data used by external URLs"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this project"),
                                subsectionWithPath("_embedded.projectResponsible").description("The project responsible displayed").type(JsonFieldType.OBJECT).optional(),
                                subsectionWithPath("_embedded.securityResponsibles").description("An array of project securityResponsible will get displayed").type(JsonFieldType.ARRAY).optional(),
                                subsectionWithPath("_embedded.projectOwner").description("The project owner").type(JsonFieldType.OBJECT).optional(),
                                subsectionWithPath("_embedded.modifiedBy").description("The user who modified the project").type(JsonFieldType.OBJECT).optional(),
                                subsectionWithPath("_embedded.leadArchitect").description("The user who leadArchitect of this project").type(JsonFieldType.OBJECT).optional(),
                                subsectionWithPath("_embedded.sw360:moderators").description("An array of moderators"),
                                subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"))
                        		));
    }

    @Test
    public void should_document_get_project_report() throws Exception {
        mockMvc.perform(get("/api/reports").
                header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("withlinkedreleases", "true")
                .queryParam("module", "projects")
                .queryParam("excludeReleaseVersion", "false")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("withlinkedreleases").description("Projects with linked releases. Possible values are `<true|false>`"),
                                parameterWithName("module").description("module represent the project or component. Possible values are `<components|projects>`"),
                                parameterWithName("excludeReleaseVersion").description("Exclude version of the components from the generated license info file. "
                                        + "Possible values are `<true|false>`")
                        )
                ));
    }

    @Test
    public void should_document_get_project_licenseclearing_spreadsheet() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("withlinkedreleases", "true")
                        .queryParam("module", "projects")
                        .queryParam("projectId", project.getId())
                        .queryParam("excludeReleaseVersion", "false")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("withlinkedreleases").description("Projects with linked releases. Possible values are `<true|false>`"),
                                parameterWithName("module").description("module represent the project or component. Possible values are `<components|projects>`"),
                                parameterWithName("projectId").description("Id of a project"),
                                parameterWithName("excludeReleaseVersion").description("Exclude version of the components from the generated license info file. "
                                        + "Possible values are `<true|false>`")
                        )));
    }

    @Test
    public void should_document_get_export_project_create_clearing_request() throws Exception{
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .param("module", "exportCreateProjectClearingReport")
                        .param("projectId", project.getId())
                        .param("generatorClassName", "DocxGenerator")
                        .param("variant", "REPORT")
                        .param("externalIds", "portal-id,main-project-id")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("projectId").description("Id for the project."),
                                parameterWithName("generatorClassName").description("Projects download format. Possible values are `<DocxGenerator>`"),
                                parameterWithName("variant").description("The possible values for variants are `<REPORT>`"),
                                parameterWithName("externalIds").description("The external Ids of the project"),
                                parameterWithName("module").description("module possible values are `<exportCreateProjectClearingReport>`")
                        )));
    }

    @Test
    public void should_document_import_cyclonedx() throws Exception {
        given(this.attachmentServiceMock.isValidSbomFile(any(), any())).willReturn(true);
        MockMultipartFile file = new MockMultipartFile("file","file=@/sampleBOM.xml".getBytes());
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/api/projects/import/SBOM")
                .content(file.getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("type", "CycloneDX");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_import_cyclonedx_on_project() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file","file=@/sampleBOM.xml".getBytes());
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/api/projects/"+project.getId()+"/import/SBOM")
                .content(file.getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .queryParam("doNotReplacePackageAndRelease", "false")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword));
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_project_with_dependencies_network() throws Exception {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            mockMvc.perform(get("/api/projects/network/" + project.getId())
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isInternalServerError());
        } else {
            mockMvc.perform(get("/api/projects/network/" + project.getId())
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
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
                                    fieldWithPath("domain").description("The domain, possible values are:"  + Sw360ResourceServer.DEFAULT_DOMAINS.toString()),
                                    fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                    fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                                    subsectionWithPath("externalIds").description("When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                    subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
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
                                    subsectionWithPath("linkedProjects").description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                    fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                    fieldWithPath("projectResponsible").description("A user who is responsible for the project."),
                                    fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                    fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                    fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                    fieldWithPath("clearingRequestId").description("Clearing Request id associated with project."),
                                    fieldWithPath("licenseInfoHeaderText").description("License info header text."),
                                    subsectionWithPath("dependencyNetwork").description("Dependency network of project with release."),
                                    subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                    subsectionWithPath("_embedded.createdBy").description("The user who created this project"),
                                    subsectionWithPath("_embedded.sw360:projectDTOs").description("An array of <<resources-projects, Projects resources>>"),
                                    subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                    subsectionWithPath("_embedded.sw360:moderators").description("An array of all project moderators with email and link to their <<resources-user-get,User resource>>"),
                                    subsectionWithPath("_embedded.sw360:attachments").description("An array of all project attachments and link to their <<resources-attachment-get,Attachment resource>>")
                            )));
        }
    }

    @Test
    public void should_document_create_project_with_network() throws Exception {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        when(this.projectServiceMock.createProject(any(), any())).
                thenReturn(
                        new Project("Test Project")
                                .setId("1234567890")
                                .setDescription("This is the description of my Test Project")
                                .setProjectType(ProjectType.PRODUCT)
                                .setVersion("1.0")
                                .setCreatedBy("admin@sw360.org")
                                .setPhaseOutSince("2020-06-25")
                                .setState(ProjectState.ACTIVE)
                                .setReleaseRelationNetwork("[{\"comment\":\"Test Comment\",\"releaseLink\":[],\"createBy\":\"admin@sw360.org\",\"createOn\":\"" + currentDate + "\",\"mainlineState\":\"OPEN\",\"releaseId\":\"3765276512\",\"releaseRelationship\":\"CONTAINED\"}]")
                                .setVendor((new Vendor("Test", "Test short", "http://testvendoraddress.com").setId("987567468")))
                                .setCreatedOn(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

        Map<String, Object> project = new HashMap<>();
        project.put("name", "Test Project");
        project.put("version", "1.0");
        project.put("visibility", "PRIVATE");
        project.put("description", "This is the description of my Test Project");
        project.put("projectType", ProjectType.PRODUCT.toString());
        List<Map<String, Object>> dependencyNetwork = new ArrayList<>();
        Map<String, Object> releaseNode = new HashMap<>();
        releaseNode.put("releaseId", "3765276512");
        releaseNode.put("comment", "Test Comment");
        releaseNode.put("releaseRelationship", "CONTAINED");
        releaseNode.put("mainlineState", "MAINLINE");
        releaseNode.put("createBy", "admin@sw360.org");
        dependencyNetwork.add(releaseNode);
        project.put("dependencyNetwork", dependencyNetwork);
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put("376576", new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true));
        project.put("linkedProjects", linkedProjects);
        project.put("leadArchitect", "lead@sw360.org");
        project.put("moderators", new HashSet<>(Arrays.asList("moderator1@sw360.org", "moderator2@sw360.org")));
        project.put("contributors", new HashSet<>(Arrays.asList("contributor1@sw360.org", "contributor2@sw360.org")));
        project.put("state", ProjectState.ACTIVE.toString());
        project.put("phaseOutSince", "2020-06-24");

        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            this.mockMvc.perform(post("/api/projects/network")
                            .contentType(MediaTypes.HAL_JSON)
                            .content(this.objectMapper.writeValueAsString(project))
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                    .andExpect(status().isInternalServerError());
        } else {
            this.mockMvc.perform(post("/api/projects/network")
                            .contentType(MediaTypes.HAL_JSON)
                            .content(this.objectMapper.writeValueAsString(project))
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("_embedded.createdBy.email", Matchers.is("admin@sw360.org")))
                    .andDo(this.documentationHandler.document(
                            requestFields(
                                    fieldWithPath("name").description("The name of the project"),
                                    fieldWithPath("description").description("The project description"),
                                    fieldWithPath("version").description("The version of the new project"),
                                    fieldWithPath("visibility").description("The project visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                    fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                    subsectionWithPath("linkedProjects").description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                    fieldWithPath("leadArchitect").description("The lead architect of the project"),
                                    fieldWithPath("contributors").description("An array of contributors to the project"),
                                    fieldWithPath("moderators").description("An array of moderators"),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                    subsectionWithPath("dependencyNetwork").description("Dependency network")
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
                                    fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                    fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                    subsectionWithPath("dependencyNetwork").description("Dependency network"),
                                    subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                    subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                    subsectionWithPath("_embedded.createdBy").description("The user who created this project")
                            )));
        }
    }

    @Test
    public void should_document_update_project_with_network() throws Exception {
        Project updateProject = new Project();
        updateProject.setName("updated project");
        updateProject.setDescription("Project description updated");
        updateProject.setVersion("1.0");
        updateProject.setState(ProjectState.PHASE_OUT);
        updateProject.setPhaseOutSince("2020-06-24");
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            this.mockMvc
                    .perform(patch("/api/projects/network/376576").contentType(MediaTypes.HAL_JSON)
                            .content(this.objectMapper.writeValueAsString(updateProject))
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isInternalServerError());
        } else {
            this.mockMvc
                    .perform(patch("/api/projects/network/376576").contentType(MediaTypes.HAL_JSON)
                            .content(this.objectMapper.writeValueAsString(updateProject))
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON))
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
                                    fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                    fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag.")),
                            responseFields(fieldWithPath("name").description("The name of the project"),
                                    fieldWithPath("version").description("The project version"),
                                    fieldWithPath("createdOn").description("The date the project was created"),
                                    fieldWithPath("description").description("The project description"),
                                    fieldWithPath("domain").description("The domain, possible values are:" + Sw360ResourceServer.DEFAULT_DOMAINS.toString()),
                                    fieldWithPath("projectType").description("The project type, possible values are: "
                                            + Arrays.asList(ProjectType.values())),
                                    fieldWithPath("visibility").description("The project visibility, possible values are: "
                                            + Arrays.asList(Visibility.values())),
                                    fieldWithPath("businessUnit").description("The business unit this project belongs to"),
                                    subsectionWithPath("externalIds").description(
                                            "When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                    subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
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
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                    subsectionWithPath("linkedProjects")
                                            .description("The `linked project id` - metadata of linked projects (`enableSvm` - whether linked projects will be part of SVM, `projectRelationship` - relationship between linked project and the project. Possible values: " + Arrays.asList(ProjectRelationship.values())),
                                    fieldWithPath("securityResponsibles").description("An array of users responsible for security of the project."),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("clearingRequestId").description("Clearing Request id associated with project."),
                                    fieldWithPath("projectResponsible").description("A user who is responsible for the project."),
                                    fieldWithPath("licenseInfoHeaderText").description("License info header text."),
                                    subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                    subsectionWithPath("_embedded.createdBy").description("The user who created this project"),
                                    fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                    fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                    fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                    subsectionWithPath("dependencyNetwork").description("Dependency network"),
                                    subsectionWithPath("_embedded.sw360:moderators").description("An array of moderators"),
                                    subsectionWithPath("_embedded.sw360:projectDTOs").description("An array of <<resources-projects, Projects resources>>"),
                                    subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                    subsectionWithPath("_embedded.sw360:attachments").description("An array of all project attachments and link to their <<resources-attachment-get,Attachment resource>>"))));
        }
    }

    @Test
    public void should_document_get_list_view_of_dependencies_network() throws Exception {
        Map<String, String> subProjectView = new HashMap<>();
        subProjectView.put("isAccessible", "true");
        subProjectView.put("clearingState", "In Progress");
        subProjectView.put("type", "Product");
        subProjectView.put("relation", "Is a subproject");
        subProjectView.put("isRelease", "false");
        subProjectView.put("projectState", "Unknown");
        subProjectView.put("projectOrigin", "Project3 (v0.1)");
        subProjectView.put("id", "61f4d4781d0a40df9c92d9c79e08030a");

        Map<String, String> rootReleaseView = new HashMap<>();
        rootReleaseView.put("isAccessible", "true");
        rootReleaseView.put("clearingState", "Report available");
        rootReleaseView.put("mainLicenses", "Apache-2.0");
        rootReleaseView.put("type", "OSS");
        rootReleaseView.put("projectMainlineState", "Open");
        rootReleaseView.put("relation", "Contained");
        rootReleaseView.put("isRelease", "true");
        rootReleaseView.put("releaseMainlineState", "Open");
        rootReleaseView.put("projectOrigin", "");
        rootReleaseView.put("name", "Release1 (2.0)");
        rootReleaseView.put("releaseOrigin", "");
        rootReleaseView.put("comment", "Comment");
        rootReleaseView.put("id", "d8407b28b8c34c71913b324870e38bdc");

        Map<String, String> leafReleaseView = new HashMap<>();
        leafReleaseView.put("isAccessible", "true");
        leafReleaseView.put("clearingState", "Report available");
        leafReleaseView.put("mainLicenses", "Apache-2.0");
        leafReleaseView.put("type", "OSS");
        leafReleaseView.put("projectMainlineState", "Open");
        leafReleaseView.put("relation", "Contained");
        leafReleaseView.put("isRelease", "true");
        leafReleaseView.put("releaseMainlineState", "Open");
        leafReleaseView.put("projectOrigin", "");
        leafReleaseView.put("name", "Release2 (2.0)");
        leafReleaseView.put("releaseOrigin", "Release1 (2.0)");
        leafReleaseView.put("comment", "Comment");
        leafReleaseView.put("id", "f8407a28b8434c71913b32a870e38bdc");


        given(this.projectServiceMock.serveDependencyNetworkListView(eq(project.getId()), any()))
                .willReturn(List.of(subProjectView, rootReleaseView, leafReleaseView));
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            mockMvc.perform(get("/api/projects/network/" + project.getId() + "/listView")
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON)
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isInternalServerError());
        } else {
            mockMvc.perform(get("/api/projects/network/" + project.getId() + "/listView")
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)).accept(MediaTypes.HAL_JSON)
                            .accept(MediaTypes.HAL_JSON))
                            .andExpect(status().isOk());
        }
    }

    @Test
    public void should_document_get_linked_releases_in_dependency_network_of_project() throws Exception {
        ReleaseNode subRelease = new ReleaseNode();
        subRelease.setReleaseId("98765");
        subRelease.setReleaseName("Component2");
        subRelease.setReleaseVersion("v2");
        subRelease.setComponentId("888888");
        subRelease.setReleaseRelationship(CONTAINED.toString());
        subRelease.setMainlineState(OPEN.toString());
        subRelease.setComment("Comment");

        ReleaseNode release = new ReleaseNode();
        release.setReleaseId("12345");
        release.setReleaseName("Component1");
        release.setReleaseVersion("v1");
        release.setComponentId("777777777");
        release.setReleaseRelationship(CONTAINED.toString());
        release.setMainlineState(OPEN.toString());
        release.setComment("Comment");
        release.setReleaseLink(List.of(subRelease));

        given(this.projectServiceMock.getLinkedReleasesInDependencyNetworkOfProject(any(), any())).willReturn(List.of(release));

        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            mockMvc.perform(get("/api/projects/network/888888/linkedReleases")
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isInternalServerError());
        } else {
            mockMvc.perform(get("/api/projects/network/888888/linkedReleases")
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void should_document_get_linked_resources_of_project_in_dependency_network() throws Exception {
        ReleaseLink releaseLink = new ReleaseLink();
        releaseLink.setId("d86a45f8288243fd8395052f1f25b50c");
        releaseLink.setVendor("");
        releaseLink.setName("release1");
        releaseLink.setVersion("v1");
        releaseLink.setLongName("release1 v1");
        releaseLink.setReleaseRelationship(CONTAINED);
        releaseLink.setMainlineState(MAINLINE);
        releaseLink.setHasSubreleases(true);
        releaseLink.setClearingState(ClearingState.NEW_CLEARING);
        releaseLink.setAttachments(List.of(attachment));
        releaseLink.setComponentType(ComponentType.OSS);
        releaseLink.setComment("Comment 1");
        releaseLink.setLayer(0);
        releaseLink.setIndex(0);
        releaseLink.setProjectId(project.getId());
        releaseLink.setReleaseMainLineState(OPEN);

        ProjectLink subProject = new ProjectLink();
        subProject.setId(project.getId());
        subProject.setName(project.getName());
        subProject.setRelation(ProjectRelationship.UNKNOWN);
        subProject.setVersion(project.getVersion());
        subProject.setProjectType(project.getProjectType());
        subProject.setState(project.getState());
        subProject.setClearingState(project.getClearingState());
        subProject.setSubprojects(Collections.emptyList());
        subProject.setEnableSvm(true);

        ProjectLink requestedProject = new ProjectLink();
        requestedProject.setId("00d7fa23c80e46cc85e4194c3f78a7f2");
        requestedProject.setName("Subproject");
        requestedProject.setRelation(ProjectRelationship.CONTAINED);
        requestedProject.setVersion("v1.1");
        requestedProject.setProjectType(ProjectType.PRODUCT);
        requestedProject.setState(ProjectState.ACTIVE);
        requestedProject.setClearingState(ProjectClearingState.OPEN);
        requestedProject.setSubprojects(Collections.singletonList(subProject));
        requestedProject.setLinkedReleases(Collections.singletonList(releaseLink));
        requestedProject.setEnableSvm(true);

        given(this.projectServiceMock.serveLinkedResourcesOfProjectInDependencyNetwork(eq(project.getId()), eq(false), any())).willReturn(requestedProject);

        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            this.mockMvc
                    .perform(get("/api/projects/network/"+project.getId()+"/linkedResources")
                            .param("transitive", "false")
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                    .andExpect(status().isInternalServerError());
        } else {
            this.mockMvc
                    .perform(
                            get("/api/projects/network/"+project.getId()+ "/linkedResources")
                                    .param("transitive", "false")
                                    .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isOk())
                    .andDo(this.documentationHandler.document(
                            queryParameters(
                                    parameterWithName("transitive").description("This allow to get all transitive linked releases in network")
                            ),
                            responseFields(
                                    fieldWithPath("id").description("The id of the project"),
                                    fieldWithPath("name").description("The name of the project"),
                                    fieldWithPath("relation").description("The relationship of the project"),
                                    fieldWithPath("version").description("The version of the project"),
                                    fieldWithPath("projectType").description("The project type, possible values are: "
                                            + Arrays.asList(ProjectType.values())),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("clearingState").description("The clearing state of project, possible values are:" + Arrays.asList(ClearingState.values())),
                                    fieldWithPath("enableSvm").description("Security vulnerability monitoring flag"),
                                    subsectionWithPath("linkedReleases").description("List of linked releases in dependency network"),
                                    subsectionWithPath("subprojects").description("List of sub projects of requested project")
                            )
                    ));
        }
    }

    @Test
    public void should_document_get_linked_releases_in_dependency_network_by_index_path() throws Exception {
        ReleaseLink releaseLink = new ReleaseLink();
        releaseLink.setId("d86a45f8288243fd8395052f1f25b50c");
        releaseLink.setVendor("");
        releaseLink.setName("release1");
        releaseLink.setVersion("v1");
        releaseLink.setLongName("release1 v1");
        releaseLink.setReleaseRelationship(CONTAINED);
        releaseLink.setMainlineState(MAINLINE);
        releaseLink.setHasSubreleases(true);
        releaseLink.setClearingState(ClearingState.NEW_CLEARING);
        releaseLink.setAttachments(List.of(attachment));
        releaseLink.setComponentType(ComponentType.OSS);
        releaseLink.setComment("Comment 1");
        releaseLink.setLayer(0);
        releaseLink.setIndex(0);
        releaseLink.setProjectId(project.getId());
        releaseLink.setReleaseMainLineState(OPEN);

        given(this.projectServiceMock.serveLinkedReleasesInDependencyNetworkByIndexPath(eq(project.getId()), any(), any())).willReturn(List.of(releaseLink));


        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            this.mockMvc
                    .perform(get("/api/projects/network/" + project.getId() + "/releases?path=0->1")
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                    .andExpect(status().isInternalServerError());
        } else {
            this.mockMvc
                    .perform(
                            get("/api/projects/network/" + project.getId() + "/releases?path=0")
                                    .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                                    .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isOk())
                    .andDo(this.documentationHandler.document(
                            queryParameters(
                                    parameterWithName("path").description("Index path of to get indirect release" +
                                            " in dependency network, each index is separated by -> character, e,g: 0->2")
                            ),
                            links(
                                    linkWithRel("curies").description("Curies are used for online documentation")
                            ),
                            responseFields(
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].id").description("Release's id"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].name").description("Release's name"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].version").description("Release's version"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].longName").description("Release's long name"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].releaseRelationship").description("Release's relationship"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].mainlineState").description("Release's mainline state"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].hasSubreleases").description("Is release have release or not"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].clearingState").description("Release's clearing state"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].attachments").description("Release's attachments"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].componentType").description("Release's type"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].comment").description("Release's comment"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].accessible").description("Is release accessible"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].index").description("Index of release"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].projectId").description("Project's id that release is linked to"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].releaseMainLineState").description("Release's mainline state"),
                                    subsectionWithPath("_embedded.sw360:releaseLinks.[].vendor").description("Release's vendor"),
                                    subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                            )
                    ));
        }
    }

    @Test
    public void should_document_get_resource_source_bundle() throws Exception {
        mockMvc.perform(get("/api/reports").header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .param("module", "licenseResourceBundle").param("projectId", project.getId())
                .param("withSubProject", "true").accept(MediaTypes.HAL_JSON)).andExpect(status().isOk())
                .andDo(this.documentationHandler.document(queryParameters(
                        parameterWithName("projectId").description("Project id"),
                        parameterWithName("withSubProject").description(
                                "Use subprojects as well to download source code bundle. Possible values are `<true|false>`"),
                        parameterWithName("module").description(
                                "module represent the type oa document. Possible values are `<licenseResourceBundle>`"))));
    }

    @Test
    public void should_document_get_linked_releases_of_linked_projects() throws Exception {
        mockMvc.perform(get("/api/projects/888888/subProjects/releases")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_compare_dependency_network_with_default_releases_relationship() throws Exception {
        ReleaseNode subRelease = new ReleaseNode();
        subRelease.setReleaseId("98765");
        subRelease.setReleaseName("Component2");
        subRelease.setReleaseVersion("v2");
        subRelease.setComponentId("888888");
        subRelease.setReleaseRelationship(CONTAINED.toString());
        subRelease.setMainlineState(OPEN.toString());
        subRelease.setComment("Comment");
        subRelease.setReleaseLink(Collections.emptyList());

        ReleaseNode release = new ReleaseNode();
        release.setReleaseId("12345");
        release.setReleaseName("Component1");
        release.setReleaseVersion("v1");
        release.setComponentId("777777777");
        release.setReleaseRelationship(CONTAINED.toString());
        release.setMainlineState(OPEN.toString());
        release.setComment("Comment");
        release.setReleaseLink(List.of(subRelease));

        Map<String, Object> comparedChild = (Map<String, Object>) objectMapper.convertValue(subRelease, Map.class);
        comparedChild.put("isDiff", true);
        comparedChild.put("releaseLink", Collections.emptyList());
        Map<String, Object> comparedRoot = (Map<String, Object>) objectMapper.convertValue(release, Map.class);
        comparedRoot.put("isDiff", false);
        comparedRoot.put("releaseLink", List.of(comparedChild));

        String jsonData = this.objectMapper.writeValueAsString(List.of(release));
        given(projectServiceMock.compareWithDefaultNetwork(any(), any())).willReturn(List.of(comparedRoot));

        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            mockMvc.perform(post("/api/projects/network/compareDefaultNetwork")
                            .contentType(MediaTypes.HAL_JSON)
                            .accept(MediaTypes.HAL_JSON_VALUE)
                            .content(jsonData)
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                    .andExpect(status().isInternalServerError());
        } else {
            mockMvc.perform(post("/api/projects/network/compareDefaultNetwork")
                            .contentType(MediaTypes.HAL_JSON)
                            .accept(MediaTypes.HAL_JSON_VALUE)
                            .content(jsonData)
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void should_document_duplicate_project_with_dependency_network() throws Exception {
        ReleaseNode release = new ReleaseNode();
        release.setReleaseId("3765276512");
        release.setReleaseRelationship(CONTAINED.toString());
        release.setMainlineState(OPEN.toString());
        release.setComment("Test Comment");
        release.setReleaseLink(new ArrayList<>());
        release.setCreateBy("admin@sw360.org");
        release.setCreateOn("2024-07-04");

        Map<String, Object> newProject = new HashMap<>();
        newProject.put("name", "Test Project");
        newProject.put("description", "This is the description of my Test Project");
        newProject.put("version", "1.0");
        newProject.put("dependencyNetwork", List.of(release));

        when(this.projectServiceMock.createProject(any(), any())).
                thenReturn(
                        new Project("Test Project")
                                .setId("1234567890")
                                .setDescription("This is the description of my Test Project")
                                .setProjectType(ProjectType.PRODUCT)
                                .setVersion("1.0")
                                .setCreatedBy("admin@sw360.org")
                                .setPhaseOutSince("2020-06-25")
                                .setState(ProjectState.ACTIVE)
                                .setReleaseRelationNetwork("[{\"comment\":\"Test Comment\",\"releaseLink\":[],\"createBy\":\"admin@sw360.org\",\"createOn\":\"2024-07-04\",\"mainlineState\":\"OPEN\",\"releaseId\":\"3765276512\",\"releaseRelationship\":\"CONTAINED\"}]")
                                .setVendor((new Vendor("Test", "Test short", "http://testvendoraddress.com").setId("987567468")))
                                .setCreatedOn(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            this.mockMvc
                    .perform(post("/api/projects/network/duplicate/376576").contentType(MediaTypes.HAL_JSON)
                            .content(this.objectMapper.writeValueAsString(newProject))
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isInternalServerError());
        } else {
            this.mockMvc
                    .perform(post("/api/projects/network/duplicate/376576").contentType(MediaTypes.HAL_JSON)
                            .content(this.objectMapper.writeValueAsString(newProject))
                            .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                            .accept(MediaTypes.HAL_JSON))
                    .andExpect(status().isCreated())
                    .andDo(this.documentationHandler.document(
                            requestFields(
                                    fieldWithPath("name").description("The name of the project"),
                                    fieldWithPath("version").description("The version of new project"),
                                    fieldWithPath("description").description("The description of new project"),
                                    subsectionWithPath("dependencyNetwork").description("Dependency network")
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
                                    fieldWithPath("considerReleasesFromExternalList").description("Consider list of releases from existing external list"),
                                    fieldWithPath("enableVulnerabilitiesDisplay").description("Displaying vulnerabilities flag."),
                                    fieldWithPath("state").description("The project active status, possible values are: " + Arrays.asList(ProjectState.values())),
                                    fieldWithPath("phaseOutSince").description("The project phase-out date"),
                                    subsectionWithPath("dependencyNetwork").description("Dependency network"),
                                    subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                    subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                    subsectionWithPath("_embedded.createdBy").description("The user who created this project")
                            )));
        }
    }

    @Test
    public void should_document_get_project_release_with_ecc_spreadsheet() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .param("mimetype", "xlsx")
                        .param("module", SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO)
                        .param("projectId", project.getId())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("mimetype").description("Projects download format. Possible values are `<xls|xlsx>`"),
                                parameterWithName("module").description("module represent the project or component. Possible values are `<projectReleaseSpreadSheetWithEcc>`"),
                                parameterWithName("projectId").description("Id of a project"))
                        ));
    }

    @Test
    public void should_document_get_projects_by_advance_search() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("projectType", project.getProjectType().toString())
                        .queryParam("createdOn", project.getCreatedOn())
                        .queryParam("version", project.getVersion())
                        .queryParam("luceneSearch", "false")
                        .queryParam("page", "0")
                        .queryParam("page_entries", "5")
                        .queryParam("sort", "name,desc")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("projectType").description("Filter for type"),
                                parameterWithName("createdOn").description("Filter for project creation date"),
                                parameterWithName("version").description("Filter for version"),
                                parameterWithName("luceneSearch").description("Filter with exact match or lucene match."),
                                parameterWithName("page").description("Page of projects"),
                                parameterWithName("page_entries").description("Amount of projects per page"),
                                parameterWithName("sort").description("Defines order of the projects")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_package_by_project_id() throws Exception {
        Set<String> licenseIds = new HashSet<>();
        licenseIds.add("MIT");
        licenseIds.add("GPL");

        Package packages = new Package("angular-sanitize", "1.8.2", "pkg:npm/angular-sanitize@1.8.2",
                CycloneDxComponentType.FRAMEWORK)
                .setId("122357345")
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2023-01-02")
                .setVcs("git+https://github.com/angular/angular.js.git")
                .setHomepageUrl("http://angularjs.org")
                .setLicenseIds(licenseIds)
                .setReleaseId("12345678")
                .setPackageManager(PackageManager.NPM)
                .setDescription("Sanitizes an html string by stripping all potentially dangerous tokens.");

        given(this.packageServiceMock.getPackageForUserById(eq(packages.getId()))).willReturn(packages);

        Project sw360Project = new Project();
        sw360Project.setId(project.getId());
        //sw360Project.setPackageIds(new HashSet<>(Collections.singleton("122357345")));
        sw360Project.setPackageIds(
                Collections.singleton("122357345").stream()
                        .collect(Collectors.toMap(id -> id, id -> new ProjectPackageRelationship()))
        );

        given(this.projectServiceMock.getProjectForUserById(eq(project.getId()), any())).willReturn(sw360Project);

        mockMvc.perform(get("/api/projects/" + project.getId() + "/packages")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON)).andExpect(status().isOk());
    }

    @Test
    public void should_add_license_to_linked_releases() throws Exception {
        String projectId = project.getId();
        when(projectServiceMock.addLicenseToLinkedReleases(eq(projectId), any(User.class))).thenReturn(
                Map.of(Sw360ProjectService.ReleaseCLIInfo.UPDATED, List.of(release))
        );

        MockHttpServletRequestBuilder requestBuilder = post("/api/projects/" + projectId + "/addLinkedReleasesLicenses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword));
        this.mockMvc.perform(requestBuilder).andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath(Sw360ProjectService.ReleaseCLIInfo.UPDATED.toString()).description("Array of release IDs which are updated."),
                                fieldWithPath(Sw360ProjectService.ReleaseCLIInfo.NOT_UPDATED.toString()).description("Array of release IDs which are not updated."),
                                fieldWithPath(Sw360ProjectService.ReleaseCLIInfo.MULTIPLE_ATTACHMENTS.toString()).description("Array of release IDs with multiple attachments."),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>")
                        )));
    }
}
