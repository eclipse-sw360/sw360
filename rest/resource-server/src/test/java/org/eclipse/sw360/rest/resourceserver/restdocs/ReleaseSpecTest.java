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

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.ExternalTool;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStatus;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStep;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelationDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityState;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReleaseSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private SW360PackageService packageServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    @MockBean
    private Sw360VulnerabilityService vulnerabilityServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockBean
    private Sw360VendorService sw360VendorService;

    @MockBean
    private Sw360LicenseService licenseServiceMock;

    @MockBean
    private Sw360LicenseInfoService licenseInfoMockService;

    private Release release, release3, releaseTest;
    private Attachment attachment;
    Component component;
    private Project project;
    private Map<String, ReleaseRelationship> releaseIdToRelationship1;

    private final String releaseId = "3765276512";
    private final String attachmentSha1 = "da373e491d3863477568896089ee9457bc316783";
    private final String attachmentId = "11112222";

    @Before
    public void before() throws TException, IOException {
        Set<Attachment> attachments = new HashSet<>();
        Set<Component> usedByComponent = new HashSet<>();
        List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        attachment = new Attachment("1231231254", "spring-core-4.3.4.RELEASE.jar");
        attachment.setSha1(attachmentSha1);
        attachments.add(attachment);
        attachmentResources.add(EntityModel.of(attachment));

        attachment.setSha1(attachmentSha1);
        attachment.setAttachmentType(AttachmentType.BINARY_SELF);
        attachment.setCreatedTeam("Clearing Team 1");
        attachment.setCreatedComment("please check asap");
        attachment.setCreatedOn("2016-12-18");
        attachment.setCreatedBy("admin@sw360.org");
        attachment.setCheckedTeam("Clearing Team 2");
        attachment.setCheckedComment("everything looks good");
        attachment.setCheckedOn("2016-12-18");
        attachment.setCheckStatus(CheckStatus.ACCEPTED);
        attachments.add(attachment);
        AttachmentInfo attachmentInfo = new AttachmentInfo(attachment);
        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        attachmentInfos.add(attachmentInfo);

        Set<Attachment> setOfAttachment = new HashSet<Attachment>();
        Attachment att1 = new Attachment("1234", "test.zip").setAttachmentType(AttachmentType.SOURCE)
                .setCreatedBy("user@sw360.org").setSha1("da373e491d312365483589ee9457bc316783").setCreatedOn("2021-04-27")
                .setCreatedTeam("DEPARTMENT");
        Attachment att2 = att1.deepCopy().setAttachmentType(AttachmentType.BINARY).setCreatedComment("Created Comment")
                .setCheckStatus(CheckStatus.ACCEPTED).setCheckedComment("Checked Comment").setCheckedOn("2021-04-27")
                .setCheckedBy("admin@sw360.org").setCheckedTeam("DEPARTMENT1");

        Source owner = new Source();
        attachmentInfo.setOwner(owner);

        given(this.attachmentServiceMock.getAttachmentById(eq(attachment.getAttachmentContentId())))
                .willReturn(attachmentInfo);
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(attachment.getSha1()))).willReturn(attachmentInfos);
        given(this.attachmentServiceMock.getAttachmentContent(any())).willReturn(new AttachmentContent().setId("1231231254").setFilename("spring-core-4.3.4.RELEASE.jar").setContentType("binary"));
        given(this.attachmentServiceMock.getResourcesFromList(any())).willReturn(CollectionModel.of(attachmentResources));
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment);
        given(this.attachmentServiceMock.filterAttachmentsToRemove(any(), any(), any())).willReturn(Collections.singleton(attachment));
        given(this.attachmentServiceMock.updateAttachment(any(), any(), any(), any())).willReturn(att2);
        given(this.sw360VendorService.getVendorById(any())).willReturn(new Vendor("TV", "Test Vendor", "http://testvendor.com"));
        given(this.attachmentServiceMock.isAttachmentExist(eq("1231231254"))).willReturn(true);

        Map<String, Set<String>> externalIds = new HashMap<>();
        externalIds.put("mainline-id-component", new HashSet<>(Arrays.asList("1432", "4876")));
        COTSDetails cotsDetails = new COTSDetails().setClearingDeadline("2016-12-18").setContainsOSS(true)
                .setCotsResponsible("admin").setLicenseClearingReportURL("http://licenseclearingreporturl.com")
                .setOssInformationURL("http://ossinformationurl.com").setUsedLicense("MIT");
        Map<String, ReleaseRelationship> releaseIdToRelationship = ImmutableMap.of("90876",
                ReleaseRelationship.DYNAMICALLY_LINKED);
        ClearingInformation clearingInfo = new ClearingInformation().setComment("Approved").setEvaluated("yes");

        component = new Component();
        component.setId("17653524");
        component.setName("Angular");
        component.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        component.setCreatedOn("2016-12-15");
        component.setCreatedBy("admin@sw360.org");
        component.setComponentType(ComponentType.COTS);
        component.setVendorNames(new HashSet<>(Collections.singletonList("Google")));
        component.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "john@sw360.org")));
        usedByComponent.add(component);

        Release testRelease = new Release().setAttachments(setOfAttachment).setId("98745").setName("Test Release")
                .setVersion("2").setComponentId("17653524").setCreatedOn("2021-04-27").setCreatedBy("admin@sw360.org");

        List<Release> releaseList = new ArrayList<>();
        release = new Release();
        Map<String, String> releaseExternalIds = new HashMap<>();
        releaseExternalIds.put("mainline-id-component", "1432");
        releaseExternalIds.put("ws-component-id", "[\"2365\",\"5487923\"]");

        EccInformation eccInformation = new EccInformation();
        eccInformation.setAl("AL");
        eccInformation.setEccn("ECCN");
        eccInformation.setAssessorContactPerson("admin@sw360.org");
        eccInformation.setAssessorDepartment("DEPARTMENT");
        eccInformation.setEccComment("Set ECC");
        eccInformation.setMaterialIndexNumber("12");
        eccInformation.setAssessmentDate("2023-06-27");
        eccInformation.setEccStatus(ECCStatus.OPEN);

        ClearingInformation clearingInformation = new ClearingInformation();
        clearingInformation.setComment("Comment");
        clearingInformation.setEvaluated("Evaluated");
        clearingInformation.setProcStart("Proc Start");
        clearingInformation.setRequestID("REQ-111");
        clearingInformation.setScanned("Scanned");
        clearingInformation.setClearingStandard("Clearing Standard");
        clearingInformation.setCountOfSecurityVn(2);
        clearingInformation.setComponentClearingReport(true);
        clearingInformation.setComponentClearingReportIsSet(false);
        clearingInformation.setExternalUrl("https://external.url");

        COTSDetails cotsDetails1 = new COTSDetails().setClearingDeadline("2016-12-18").setContainsOSS(true)
                .setCotsResponsible("admin@sw360.org").setLicenseClearingReportURL("http://licenseclearingreporturl.com")
                .setOssInformationURL("http://ossinformationurl.com").setUsedLicense("MIT");

        release.setId(releaseId);
        owner.setReleaseId(release.getId());
        release.setName("Spring Core 4.3.4");
        release.setCpeid("cpe:/a:pivotal:spring-core:4.3.4:");
        release.setReleaseDate("2016-12-07");
        release.setVersion("4.3.4");
        release.setCreatedOn("2016-12-18");
        release.setCreatedBy("admin@sw360.org");
        release.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setSubscribers(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setContributors(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setCreatedBy("admin@sw360.org");
        release.setModifiedBy("admin@sw360.org");
        release.setSourceCodeDownloadurl("http://www.google.com");
        release.setBinaryDownloadurl("http://www.google.com/binaries");
        release.setComponentId(component.getId());
        release.setClearingState(ClearingState.APPROVED);
        release.setMainlineState(MainlineState.SPECIFIC);
        release.setExternalIds(releaseExternalIds);
        release.setComponentType(ComponentType.OSS);
        release.setAdditionalData(Collections.singletonMap("Key", "Value"));
        release.setAttachments(attachments);
        release.setLanguages(new HashSet<>(Arrays.asList("C++", "Java")));
        release.setMainLicenseIds(new HashSet<>(Arrays.asList("GPL-2.0-or-later", "Apache-2.0")));
        release.setOtherLicenseIds(new HashSet<>(Arrays.asList("MIT", "BSD-3-Clause")));
        release.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        release.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Java SE", ".NET")));
        release.setEccInformation(eccInformation);
        release.setClearingInformation(clearingInformation);
        release.setCotsDetails(cotsDetails1);
        release.setComponentType(ComponentType.COTS);

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
                .setRelease(release)
                .setPackageManager(PackageManager.NPM)
                .setDescription("Sanitizes an html string by stripping all potentially dangerous tokens.");

        given(this.packageServiceMock.getPackageForUserById(eq(package1.getId()))).willReturn(package1);

        Set<String> linkedPackages = new HashSet<>();
        linkedPackages.add(package1.getId());

        release.setPackageIds(linkedPackages);
        releaseList.add(release);

        Release release2 = new Release();
        Map<String, String> release2ExternalIds = new HashMap<>();
        release2ExternalIds.put("mainline-id-component", "4876");
        release2ExternalIds.put("ws-component-id", "[\"589211\",\"987135\"]");
        release2.setId("6868686868");
        release2.setName("Angular");
        release2.setCpeid("cpe:/a:Google:Angular:2.3.1:");
        release2.setReleaseDate("2016-12-15");
        release2.setVersion("2.3.1");
        release2.setCreatedOn("2016-12-18");
        release2.setCreatedBy("admin@sw360.org");
        release2.setSourceCodeDownloadurl("http://www.google.com");
        release2.setBinaryDownloadurl("http://www.google.com/binaries");
        release2.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release2.setComponentId(component.getId());
        release2.setComponentType(ComponentType.OSS);
        release2.setClearingState(ClearingState.APPROVED);
        release2.setMainlineState(MainlineState.MAINLINE);
        release2.setExternalIds(release2ExternalIds);
        release2.setLanguages(new HashSet<>(Arrays.asList("C++", "Java")));
        release2.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        release2.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Java SE", ".NET")));
        release2.setContributors(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release2.setVendor(new Vendor("TV", "Test Vendor", "http://testvendor.com"));
        release2.setReleaseIdToRelationship(releaseIdToRelationship);
        release2.setClearingInformation(clearingInfo);
        release2.setCotsDetails(cotsDetails);
        release2.setEccInformation(eccInformation);
        releaseList.add(release2);

        Package package2 = new Package()
                .setId("875689754")
                .setName("applicationinsights-web")
                .setVersion("2.5.11")
                .setCreatedBy("user@sw360.org")
                .setCreatedOn("2023-02-02")
                .setPurl("pkg:npm/@microsoft/applicationinsights-web@2.5.11")
                .setRelease(release2)
                .setPackageManager(PackageManager.NPM)
                .setPackageType(CycloneDxComponentType.LIBRARY)
                .setVcs("git+https://github.com/microsoft/ApplicationInsights-JS.git")
                .setHomepageUrl("https://github.com/microsoft/ApplicationInsights-JS#readme")
                .setDescription("Application Insights is an extension of Azure Monitor and provides application performance monitoring (APM) features");

        given(this.packageServiceMock.getPackageForUserById(eq(package2.getId()))).willReturn(package2);

        Set<String> linkedPackages2 = new HashSet<>();
        linkedPackages2.add(package2.getId());

        release2.setPackageIds(linkedPackages2);
        releaseList.add(release2);
        release3 = new Release();
        release3.setId("987456");
        release3.setName("Angular");
        release3.setVersion("2.3.1");
        release3.setCreatedOn("2016-12-18");
        release3.setCreatedBy("admin@sw360.org");
        release3.setComponentId("1234");
        Attachment attachment3 = new Attachment(attachment);
        attachment3.setAttachmentContentId("34535345");
        attachment3.setAttachmentType(AttachmentType.SOURCE);
        release3.setAttachments(ImmutableSet.of(attachment3));

        AttachmentDTO attachmentDTO = new AttachmentDTO();
        attachmentDTO.setAttachmentContentId(attachment3.getAttachmentContentId());
        attachmentDTO.setFilename(attachment3.getFilename());
        attachmentDTO.setSha1(attachment3.getSha1());
        attachmentDTO.setAttachmentType(attachment3.getAttachmentType());
        attachmentDTO.setCreatedBy(attachment3.getCreatedBy());
        attachmentDTO.setCreatedTeam(attachment3.getCreatedTeam());
        attachmentDTO.setCreatedComment(attachment3.getCreatedComment());
        attachmentDTO.setCreatedOn(attachment3.getCreatedOn());
        attachmentDTO.setCheckedBy(attachment3.getCheckedBy());
        attachmentDTO.setCheckedTeam(attachment3.getCheckedTeam());
        attachmentDTO.setCheckedComment(attachment3.getCheckedComment());
        attachmentDTO.setCheckedOn(attachment3.getCheckedOn());
        attachmentDTO.setCheckStatus(attachment3.getCheckStatus());

        UsageAttachment usageAttachment = new UsageAttachment();
        usageAttachment.setVisible(1);
        usageAttachment.setRestricted(0);

        ProjectUsage projectUsage = new ProjectUsage();
        projectUsage.setProjectId("376576");
        projectUsage.setProjectName("Emerald Web");

        usageAttachment.setProjectUsages(new HashSet<>(Arrays.asList(projectUsage)));

        attachmentDTO.setUsageAttachment(usageAttachment);
        List<EntityModel<AttachmentDTO>> atEntityModels = new ArrayList<>();
        atEntityModels.add(EntityModel.of(attachmentDTO));
        given(this.attachmentServiceMock.getAttachmentDTOResourcesFromList(any(), any(), any())).willReturn(CollectionModel.of(atEntityModels));

        Set<Project> projectList = new HashSet<>();
        project = new Project();
        project.setId("376576");
        project.setName("Emerald Web");
        project.setProjectType(ProjectType.PRODUCT);
        project.setVersion("1.0.2");
        projectList.add(project);

        given(this.releaseServiceMock.getReleasesForUser(any())).willReturn(releaseList);
        given(this.releaseServiceMock.getRecentReleases(any())).willReturn(releaseList);
        given(this.releaseServiceMock.getReleaseSubscriptions(any())).willReturn(releaseList);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release.getId()), any())).willReturn(release);
        given(this.releaseServiceMock.getReleaseForUserById(eq(testRelease.getId()), any())).willReturn(testRelease);
        given(this.releaseServiceMock.getProjectsByRelease(eq(release.getId()), any())).willReturn(projectList);
        given(this.releaseServiceMock.getUsingComponentsForRelease(eq(release.getId()), any())).willReturn(usedByComponent);
        given(this.releaseServiceMock.deleteRelease(eq(release.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.searchByExternalIds(eq(externalIds), any())).willReturn((new HashSet<>(releaseList)));
        given(this.releaseServiceMock.convertToEmbeddedWithExternalIds(eq(release))).willReturn(
                new Release("Angular", "2.3.0", component.getId())
                        .setId(releaseId)
                        .setExternalIds(Collections.singletonMap("mainline-id-component", "1432")));
        given(this.releaseServiceMock.convertToEmbeddedWithExternalIds(eq(release2))).willReturn(
                new Release("Angular", "2.3.1", component.getId())
                        .setId("3765276512")
                        .setExternalIds(Collections.singletonMap("mainline-id-component", "4876")));
        when(this.releaseServiceMock.createRelease(any(), any())).then(invocation ->
                new Release("Test Release", "1.0", component.getId())
                        .setId("1234567890"));
        given(this.releaseServiceMock.countProjectsByReleaseId(eq(release.getId()))).willReturn(2);

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("jane@sw360.org")).willReturn(
                new User("jane@sw360.org", "sw360").setId("209582812"));
        given(this.licenseServiceMock.getLicenseById("Apache-2.0")).willReturn(
                new License("Apache 2.0 License")
                        .setText("Dummy License Text")
                        .setShortname("Apache-2.0")
                        .setId(UUID.randomUUID().toString()));
        given(this.licenseServiceMock.getLicenseById("GPL-2.0-or-later")).willReturn(
                new License("GNU General Public License 2.0").setText("GNU General Public License 2.0 Text")
                        .setShortname("GPL-2.0-or-later").setId(UUID.randomUUID().toString()));
        given(this.licenseServiceMock.getLicenseById("ML1")).willReturn(
                new License("Main license 1 name").setText("Main license 1 Text")
                        .setShortname("ML1").setId("ML1"));
        given(this.licenseServiceMock.getLicenseById("ML2")).willReturn(
                new License("Main license 2 name").setText("Main license 2 Text")
                        .setShortname("ML2").setId("ML2"));
        ExternalToolProcess fossologyProcess = new ExternalToolProcess();
        fossologyProcess.setAttachmentId("5345ab789");
        fossologyProcess.setAttachmentHash("535434657567");
        fossologyProcess.setExternalTool(ExternalTool.FOSSOLOGY);
        fossologyProcess.setProcessStatus(ExternalToolProcessStatus.DONE);
        List<ExternalToolProcessStep> processSteps = new ArrayList<>();
        ExternalToolProcessStep uploadStep = new ExternalToolProcessStep();
        uploadStep.setStepName("01_upload");
        uploadStep.setProcessStepIdInTool("2");
        uploadStep.setResult("12");
        uploadStep.setStartedBy("abc@sw360.org");
        uploadStep.setStartedByGroup("DEPARTMENT");
        uploadStep.setStartedOn("2020-02-27T08:18:51.393Z");
        uploadStep.setFinishedOn("2020-02-27T08:18:55.696Z");
        uploadStep.setStepStatus(ExternalToolProcessStatus.DONE);

        ExternalToolProcessStep scanStep = uploadStep.deepCopy();
        scanStep.setStepName("02_scan");
        scanStep.setProcessStepIdInTool("3");
        scanStep.setResult("14");
        scanStep.setStartedOn("2020-02-27T08:41:26.882Z");
        scanStep.setFinishedOn("2020-02-27T08:42:29.445Z");

        ExternalToolProcessStep reportStep = uploadStep.deepCopy();
        reportStep.setStepName("03_report");
        reportStep.setProcessStepIdInTool("4");
        reportStep.setResult(attachment.getAttachmentContentId());
        reportStep.setStartedOn("2020-02-27T08:46:50.155Z");
        reportStep.setFinishedOn("2020-02-27T08:47:15.708Z");

        processSteps.add(uploadStep);
        processSteps.add(scanStep);
        processSteps.add(reportStep);
        fossologyProcess.setProcessSteps(processSteps);
        release3.setExternalToolProcesses(ImmutableSet.of(fossologyProcess));
        when(releaseServiceMock.getReleaseForUserById(eq(release3.getId()), any())).thenReturn(release3);
        when(releaseServiceMock.getExternalToolProcess(release3)).thenReturn(fossologyProcess);

        releaseIdToRelationship1 = ImmutableMap.of(release2.getId(), ReleaseRelationship.DYNAMICALLY_LINKED, release3.getId(), ReleaseRelationship.CONTAINED);

        List<VulnerabilityDTO> vulDtos = new ArrayList<VulnerabilityDTO>();
        List<VulnerabilityDTO> vulUpdates = new ArrayList<VulnerabilityDTO>();

        VerificationStateInfo verificationStateInfo = new VerificationStateInfo();
        verificationStateInfo.setCheckedBy("admin@sw360.org");
        verificationStateInfo.setCheckedOn("2018-12-18");
        verificationStateInfo.setComment("Change status checked");
        verificationStateInfo.setVerificationState(VerificationState.CHECKED);

        List<VerificationStateInfo> verificationStateInfos = new ArrayList<>();
        verificationStateInfos.add(verificationStateInfo);

        ReleaseVulnerabilityRelation relation = new ReleaseVulnerabilityRelation();
        relation.setReleaseId("3765276512");
        relation.setVulnerabilityId("1333333333");
        relation.setVerificationStateInfo(verificationStateInfos);
        relation.setMatchedBy("matchedBy1");
        relation.setUsedNeedle("usedNeedle1");

        VulnerabilityDTO vulDto = new VulnerabilityDTO();
        vulDto.setComment("Lorem Ipsum");
        vulDto.setExternalId("12345");
        vulDto.setProjectRelevance("IRRELEVANT");
        vulDto.setIntReleaseId("3765276512");
        vulDto.setIntReleaseName("Angular 2.3.0");
        vulDto.setAction("Update to Fixed Version");
        vulDto.setPriority("2 - major");
        vulDto.setTitle("1_Title");
        vulDto.setReleaseVulnerabilityRelation(relation);
        vulUpdates.add(vulDto);
        vulDtos.add(vulDto);

        VerificationStateInfo verificationStateInfo1 = new VerificationStateInfo();
        verificationStateInfo1.setCheckedBy("admin@sw360.org");
        verificationStateInfo1.setCheckedOn("2016-12-12");
        verificationStateInfo1.setComment("Change status checked");
        verificationStateInfo1.setVerificationState(VerificationState.CHECKED);

        List<VerificationStateInfo> verificationStateInfos1 = new ArrayList<>();
        verificationStateInfos1.add(verificationStateInfo1);

        ReleaseVulnerabilityRelation relation1 = new ReleaseVulnerabilityRelation();
        relation1.setReleaseId("3765276512");
        relation1.setVulnerabilityId("122222222");
        relation1.setVerificationStateInfo(verificationStateInfos1);
        relation1.setMatchedBy("matchedBy2");
        relation1.setUsedNeedle("usedNeedle2");

        VulnerabilityDTO vulDto1 = new VulnerabilityDTO();
        vulDto1.setComment("Lorem Ipsum 1");
        vulDto1.setExternalId("23105");
        vulDto1.setProjectRelevance("APPLICABLE");
        vulDto1.setIntReleaseId("3765276512");
        vulDto1.setIntReleaseName("ReactJs 2.3.0");
        vulDto1.setAction("Update to Version");
        vulDto1.setPriority("1 - critical");
        vulDto1.setTitle("2_Title");
        vulDto1.setReleaseVulnerabilityRelation(relation1);
        vulDtos.add(vulDto1);

        given(this.vulnerabilityServiceMock.getVulnerabilityDTOByExternalId(any(), any())).willReturn(vulUpdates);
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByReleaseId(any(), any())).willReturn(vulDtos);
        given(this.vulnerabilityServiceMock.updateReleaseVulnerabilityRelation(any(), any())).willReturn(RequestStatus.SUCCESS);

        releaseTest = new Release();
        releaseTest.setId("12121212");
        releaseTest.setName("Test Load SPDX");
        releaseTest.setVersion("1.0");
    }
    @Test
    public void should_document_get_releases() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("page").description("Page of release"),
                                parameterWithName("page_entries").description("Amount of releases per page"),
                                parameterWithName("sort").description("Defines order of the releases")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of projects per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing projects"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_release_all_details() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases?allDetails=true")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("The curies for documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]componentType").description("The componentType of the release, possible values are " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]createdBy").description("Email of the release creator"),
                                subsectionWithPath("_embedded.sw360:releases.[]cpeid").description("CpeId of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                                subsectionWithPath("_embedded.sw360:releases.[]releaseDate").description("The date of this release"),
                                subsectionWithPath("_embedded.sw360:releases.[]createdOn").description("The creation date of the internal sw360 release"),
                                subsectionWithPath("_embedded.sw360:releases.[]mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                                subsectionWithPath("_embedded.sw360:releases.[]contributors").description("the contributors of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]moderators").description("the moderators of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]sourceCodeDownloadurl").description("the source code download url of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]binaryDownloadurl").description("the binary download url of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]externalIds").description("When releases are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("_embedded.sw360:releases.[]eccInformation").description("The eccInformation of this release"),
                                subsectionWithPath("_embedded.sw360:releases.[]additionalData").description("A place to store additional data used by external tools").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]releaseIdToRelationship").description("Release Id To Relationship of Release").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]languages").description("The language of the component"),
                                subsectionWithPath("_embedded.sw360:releases.[]mainLicenseIds").description("An array of all main licenses").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]otherLicenseIds").description("An array of all other licenses associated with the release").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]operatingSystems").description("The OS on which the release operates"),
                                subsectionWithPath("_embedded.sw360:releases.[]softwarePlatforms").description("The software platforms of the component"),
                                subsectionWithPath("_embedded.sw360:releases.[]vendor").description("The Id of the vendor").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]clearingInformation").description("Clearing information of release").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]_embedded.sw360:moderators").description("An array of all release moderators with email"),
                                subsectionWithPath("_embedded.sw360:releases.[]_embedded.sw360:attachments").description("An array of all release attachments").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]_embedded.sw360:cotsDetails").description("An cotsDetails of the release").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]_embedded.sw360:releaseIdToRelationship").description("An linked release Id with relation").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]_embedded.sw360:clearingInformation").description("An Clearing Information of the release").optional(),
                                subsectionWithPath("_embedded.sw360:releases.[]_links").description("Self <<resources-index-links,Links>> to Release resource"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_releases_with_fields() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases?fields=cpeId,releaseDate")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]cpeid").description("The cpeId of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases.[]releaseDate").description("The releaseDate of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_recent_releases() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/recentReleases")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_releases_by_name() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases?name=" + release.getName())
                .header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(linkWithRel("curies").description("Curies are used for online documentation")),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")))
                        );
    }

    @Test
    public void should_document_get_release() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-releases,Releases resource>>"),
                                linkWithRel("sw360:component").description("The link to the corresponding component"),
                                linkWithRel("curies").description("The curies for documentation")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the release, optional"),
                                fieldWithPath("version").description("The version of the release"),
                                fieldWithPath("createdBy").description("Email of the release creator"),
                                fieldWithPath("cpeid").description("CpeId of the release"),
                                fieldWithPath("clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                                fieldWithPath("releaseDate").description("The date of this release"),
                                fieldWithPath("createdOn").description("The creation date of the internal sw360 release"),
                                fieldWithPath("componentType").description("The componentType of the release, possible values are " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                                subsectionWithPath("eccInformation").description("The eccInformation of this release"),
                                fieldWithPath("sourceCodeDownloadurl").description("the source code download url of the release"),
                                fieldWithPath("binaryDownloadurl").description("the binary download url of the release"),
                                fieldWithPath("otherLicenseIds").description("An array of all other licenses associated with the release"),
                                subsectionWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("languages").description("The language of the component"),
                                subsectionWithPath("_embedded.sw360:licenses").description("An array of all main licenses with their fullName and link to their <<resources-license-get,License resource>>"),
                                subsectionWithPath("_embedded.sw360:packages").description("An array of all the linked packages and link to their <<resources-package-get,Package resource>>"),
                                fieldWithPath("operatingSystems").description("The OS on which the release operates"),
                                fieldWithPath("softwarePlatforms").description("The software platforms of the component"),
                                subsectionWithPath("clearingInformation").description("Clearing information of release"),
                                subsectionWithPath("_embedded.sw360:moderators").description("An array of all release moderators with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:subscribers").description("An array of all release subscribers with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:contributors").description("An array of all release contributors with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:modifiedBy").description("A release modifiedBy with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:createdBy").description("A release createdBy with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:attachments").description("An array of all release attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                                subsectionWithPath("_embedded.sw360:cotsDetails").description("Cots details information of release has component type = COTS "),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_delete_releases() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(delete("/api/releases/" + release.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isMultiStatus())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("[].resourceId").description("id of the deleted resource"),
                                fieldWithPath("[].status").description("status of the delete operation")
                        )
                ));
    }

    @Test
    public void should_document_get_usedbyresource_for_release() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/usedBy/" + release.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:projects.[]name").description("The name of the project"),
                                subsectionWithPath("_embedded.sw360:projects.[]version").description("The project version"),
                                subsectionWithPath("_embedded.sw360:projects.[]projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                subsectionWithPath("_embedded.sw360:projects").description("An array of <<resources-projects, Projects resources>>"),
                                subsectionWithPath("_embedded.sw360:restrictedResources.[]projects").description("Number of restricted projects"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }


    @Test
    public void should_document_update_release() throws Exception {
        Map<String, Object> updateRelease = new HashMap<>();
        updateRelease.put("name", "Updated release");
        updateRelease.put("componentType", ComponentType.OSS.toString());

        Map<String, String> attachmentData = new HashMap<>();
        attachmentData.put("sha1", "da373e491d3863477568896089ee9457bc316783");
        attachmentData.put("attachmentType",AttachmentType.BINARY_SELF.toString());
        attachmentData.put("attachmentContentId", "1231231254");
        attachmentData.put("createdTeam", "Clearing Team 1");
        attachmentData.put("createdComment", "please check asap");
        attachmentData.put("createdOn", "2022-08-19");
        attachmentData.put("createdBy", "admin@sw360.org");
        attachmentData.put("checkedComment", "everything looks good");
        attachmentData.put("checkedTeam", "Clearing Team 2");
        attachmentData.put("checkedOn", "2016-12-18");
        updateRelease.put("attachments", Collections.singletonList(attachmentData));

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/releases/" + releaseId)
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(updateRelease))
                .header("Authorization", "Bearer" + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(documentReleaseProperties());
    }

    @Test
    public void should_document_get_release_attachment_info() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release3.getId() + "/attachments")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:attachmentDTOes").description("An array of <<resources-attachment, Attachments resources>>"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]attachmentContentId").description("The attachment attachmentContentId"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]filename").description("The attachment filename"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]sha1").description("The attachment sha1"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]attachmentType").description("The attachment attachmentType"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]createdBy").description("The attachment createdBy"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]createdTeam").description("The attachment createdTeam"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]createdComment").description("The attachment createdComment"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]createdOn").description("The attachment createdOn"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]checkedComment").description("The attachment checkedComment"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]checkStatus").description("The attachment checkStatus"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]usageAttachment").description("The usages in project"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]usageAttachment.visible").description("The visible usages in project"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]usageAttachment.restricted").description("The restricted usages in project"),
                                subsectionWithPath("_embedded.sw360:attachmentDTOes.[]usageAttachment.projectUsages").description("The name of project usages"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_release_attachment_info() throws Exception {
        Attachment updateAttachment = new Attachment().setAttachmentType(AttachmentType.BINARY)
                .setCreatedComment("Created Comment").setCheckStatus(CheckStatus.ACCEPTED)
                .setCheckedComment("Checked Comment");
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc
                .perform(patch("/api/releases/98745/attachment/1234").contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateAttachment))
                        .header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                requestFields(
                        fieldWithPath("attachmentType").description("The type of Attachment. Possible Values are: "+Arrays.asList(AttachmentType.values())),
                        fieldWithPath("createdComment").description("The upload Comment of Attachment"),
                        fieldWithPath("checkStatus").description("The checkStatus of Attachment. Possible Values are: "+Arrays.asList(CheckStatus.values())),
                        fieldWithPath("checkedComment").description("The checked Comment of Attachment")),
                responseFields(
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
    public void should_document_get_release_attachment() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", "Bearer " + accessToken)
                .accept("application/*"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_release_attachment_bundle() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release.getId() + "/attachments/download")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept("application/*"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_releases_by_externalIds() throws Exception {
        MultiValueMap<String, String> externalIds = new LinkedMultiValueMap<>();
        externalIds.put("mainline-id-component", List.of("1432","4876"));
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/searchByExternalIds?mainline-id-component=1432&mainline-id-component=4876")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(externalIds))
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release, optional"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]externalIds").description("External Ids of the release. Return as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_trigger_fossology_process() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(
                get("/api/releases/" + release3.getId() + "/triggerFossologyProcess?uploadDescription=uploadDescription&markFossologyProcessOutdated=false")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(responseFields(
//                        fieldWithPath("content.message").description(
//                                "Message indicating whether FOSSology Process for Release triggered or not"),
                        fieldWithPath("message").description(
                                "Message indicating whether FOSSology Process for Release triggered or not"),
                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));
    }

    @Test
    public void should_document_check_fossology_process_status() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release3.getId() + "/checkFossologyProcessStatus").header("Authorization",
                "Bearer " + accessToken)).andExpect(status().isOk())
                .andDo(this.documentationHandler.document(responseFields(
                        fieldWithPath("status").description("The status of triggered FOSSology, possible values are: " + Arrays.asList(RequestStatus.SUCCESS, RequestStatus.FAILURE, RequestStatus.PROCESSING)),
                        fieldWithPath("fossologyProcessInfo")
                                .description("The information about triggered FOSSology process."),
                        fieldWithPath("fossologyProcessInfo.externalTool")
                                .description("The name of external tool ,possible values are: " + Arrays.asList(ExternalTool.values())),
                        fieldWithPath("fossologyProcessInfo.processStatus")
                                .description("The status of process, possible values are: " + Arrays.asList(ExternalToolProcessStatus.values())),
                        fieldWithPath("fossologyProcessInfo.attachmentId")
                                .description("The attachement Id of the source."),
                        fieldWithPath("fossologyProcessInfo.attachmentHash")
                                .description("The attachement hash of the source."),
                        fieldWithPath("fossologyProcessInfo.processSteps")
                                .description("An array of ExternalToolProcessStep"),
                        fieldWithPath("fossologyProcessInfo.processSteps[]stepName")
                                .description("The name of step in FOSSology process, possible values are: " + Arrays.asList("01_upload", "02_scan", "03_report")),
                        fieldWithPath("fossologyProcessInfo.processSteps[]stepStatus")
                                .description("The status of step, possible values are: " + Arrays.asList(ExternalToolProcessStatus.DONE, ExternalToolProcessStatus.IN_WORK, ExternalToolProcessStatus.NEW)),
                        fieldWithPath("fossologyProcessInfo.processSteps[]startedBy")
                                .description("The email of user ,triggering the step."),
                        fieldWithPath("fossologyProcessInfo.processSteps[]startedByGroup")
                                .description("The group of user ,triggering the step."),
                        fieldWithPath("fossologyProcessInfo.processSteps[]startedOn")
                                .description("The start time of step."),
                        fieldWithPath("fossologyProcessInfo.processSteps[]processStepIdInTool")
                                .description("The upload id, scan id or report id in FOSSology."),
                        fieldWithPath("fossologyProcessInfo.processSteps[]finishedOn")
                                .description("The finished time of step."),
                        fieldWithPath("fossologyProcessInfo.processSteps[]result")
                                .description("The result of step , -1 or null indicates failure."))));
    }

    @Test
    public void should_document_create_release() throws Exception {
        Map<String, String> release = new HashMap<>();
        release.put("version", "1.0");
        release.put("componentId", component.getId());
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(post("/api/releases")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(release))
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("version").description("The version of the new release"),
                                fieldWithPath("componentId").description("The componentId of the origin component")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the release, optional"),
                                fieldWithPath("version").description("The version of the release"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_upload_attachment_to_release() throws Exception {
        testAttachmentUpload("/api/releases/", releaseId);
    }

    @Test
    public void should_document_get_releases_by_sha1() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases?sha1=" + attachmentSha1)
                        .header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(linkWithRel("curies").description("Curies are used for online documentation")),
                        responseFields(
                                subsectionWithPath("_links")
                                        .description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:releases").description(
                                        "The collection of <<resources-releases,Releases resources>>. In most cases the result should contain either one element or an empty response. If the same binary file is uploaded and attached to multiple sw360 resources, the collection will contain all the releases that have attachments with matching sha1 hash."))))
                .andReturn();
    }

    @Test
    public void should_document_delete_release_attachment() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(delete("/api/releases/" + release.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(documentReleaseProperties());
    }

    @Test
    public void should_document_link_releases_to_release() throws Exception {

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(post("/api/releases/" + release.getId() + "/releases")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(releaseIdToRelationship1))
                .header("Authorization", "Bearer" + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    public void should_document_link_packages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/releases/" + release.getId() + "/link/packages");
        link_unlink_packages(requestBuilder);
    }

    @Test
    public void should_document_unlink_packages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch("/api/releases/" + release.getId() + "/unlink/packages");
        link_unlink_packages(requestBuilder);
    }

    private void link_unlink_packages(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        Set<String> packageIds = new HashSet<>();

        packageIds.add("9876746589");
        packageIds.add("4444444467");
        packageIds.add("5555555576");

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(requestBuilder.contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(packageIds))
                .header("Authorization", "Bearer " + accessToken)).andExpect(status().isCreated());
    }

    private RestDocumentationResultHandler documentReleaseProperties() {
        return this.documentationHandler.document(
                links(
                        linkWithRel("self").description("The <<resources-release,Release resource>>"),
                        linkWithRel("sw360:component").description("The link to the corresponding component"),
                        linkWithRel("curies").description("The curies for documentation")
                ),
                responseFields(
                        fieldWithPath("name").description("The name of the release, optional"),
                        fieldWithPath("version").description("The version of the release"),
                        fieldWithPath("createdBy").description("Email of the release creator"),
                        fieldWithPath("cpeid").description("CpeId of the release"),
                        fieldWithPath("clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                        fieldWithPath("releaseDate").description("The date of this release"),
                        fieldWithPath("componentType").description("The componentType of the release, possible values are " + Arrays.asList(ComponentType.values())),
                        fieldWithPath("createdOn").description("The creation date of the internal sw360 release"),
                        fieldWithPath("mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                        fieldWithPath("contributors").description("the contributors of the release"),
                        fieldWithPath("sourceCodeDownloadurl").description("the source code download url of the release"),
                        subsectionWithPath("eccInformation").description("The eccInformation of this release"),
                        fieldWithPath("binaryDownloadurl").description("the binary download url of the release"),
                        fieldWithPath("otherLicenseIds").description("An array of all other licenses associated with the release"),
                        subsectionWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                        subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                        subsectionWithPath("clearingInformation").description("Clearing information of release"),
                        fieldWithPath("languages").description("The language of the component"),
                        subsectionWithPath("_embedded.sw360:licenses").description("An array of all main licenses with their fullName and link to their <<resources-license-get,License resource>>"),
                        subsectionWithPath("_embedded.sw360:packages").description("An array of all the linked packages and link to their <<resources-package-get,Package resource>>"),
                        fieldWithPath("operatingSystems").description("The OS on which the release operates"),
                        fieldWithPath("softwarePlatforms").description("The software platforms of the component"),
                        subsectionWithPath("_embedded.sw360:moderators").description("An array of all release moderators with email and link to their <<resources-user-get,User resource>>"),
                        subsectionWithPath("_embedded.sw360:attachments").description("An array of all release attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                )
        );
    }

    @Test
    public void should_document_get_release_vulnerabilities() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release.getId()+ "/vulnerabilities")
                        .contentType(MediaTypes.HAL_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]externalId").description("The external Id of the vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes").description("An array of <<resources-vulnerabilities, Vulnerabilities resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_release_subscription() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/mySubscriptions")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_release_vulnerabilities() throws Exception {

        VulnerabilityState vulnerabilityState = new VulnerabilityState();
        Set<ReleaseVulnerabilityRelationDTO> releaseVulnerabilityRelationDTOS = new HashSet<>();
        ReleaseVulnerabilityRelationDTO releaseVulnerabilityRelationDTO = new ReleaseVulnerabilityRelationDTO();
        releaseVulnerabilityRelationDTO.setExternalId("12345");
        releaseVulnerabilityRelationDTOS.add(releaseVulnerabilityRelationDTO);
        vulnerabilityState.setReleaseVulnerabilityRelationDTOs(releaseVulnerabilityRelationDTOS);
        vulnerabilityState.setComment("Change status NOT_CHECKED");
        vulnerabilityState.setVerificationState(VerificationState.NOT_CHECKED);

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/releases/" + releaseId + "/vulnerabilities")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(vulnerabilityState))
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]externalId").description("The externalId of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]comment").description("Any message to add while updating releases vulnerabilities"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectAction").description("The action of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]title").description("The title of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]priority").description("The priority of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation").description("The releaseVulnerabilityRelation of vulnerability"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation.vulnerabilityId").description("The vulnerabilityId of releaseVulnerabilityRelation"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation.releaseId").description("The releaseId of releaseVulnerabilityRelation"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation.verificationStateInfo.[]checkedOn").description("The checkedOn of verificationStateInfo"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation.verificationStateInfo.[]checkedBy").description("The checkedBy of verificationStateInfo"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation.verificationStateInfo.[]comment").description("The comment of verificationStateInfo"),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]releaseVulnerabilityRelation.verificationStateInfo.[]verificationState").description("The verificationState of verificationStateInfo " +  Arrays.asList(VerificationState.values())),
                                subsectionWithPath("_embedded.sw360:vulnerabilityDTOes").description("An array of <<resources-vulnerabilities, Vulnerability resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_trigger_reload_fossology_report() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(
                        get("/api/releases/" + release3.getId() + "/reloadFossologyReport")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(responseFields(
                        fieldWithPath("message").description(
                                "Message indicating whether re-generate FOSSology's report Process for Release triggered or not"),
                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));
    }

    @Test
    public void should_document_write_spdx_licenses_info_into_release() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);

        Map<String, List<String>> spdxLicenses= new HashMap<>();
        spdxLicenses.put("mainLicenseIds", List.of("ML1", "ML2"));
        spdxLicenses.put("otherLicenseIds", List.of("OL1", "OL2"));

        this.mockMvc.perform(post("/api/releases/" + release.getId() + "/spdxLicenses")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(spdxLicenses))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("mainLicenseIds").description("The main license ids need to write into release"),
                                fieldWithPath("otherLicenseIds").description("The other license ids need to write into release")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the release, optional"),
                                fieldWithPath("version").description("The version of the release"),
                                fieldWithPath("createdBy").description("Email of the release creator"),
                                fieldWithPath("cpeid").description("CpeId of the release"),
                                fieldWithPath("clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                                fieldWithPath("releaseDate").description("The date of this release"),
                                fieldWithPath("componentType").description("The componentType of the release, possible values are " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("createdOn").description("The creation date of the internal sw360 release"),
                                fieldWithPath("mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                                fieldWithPath("contributors").description("the contributors of the release"),
                                fieldWithPath("sourceCodeDownloadurl").description("the source code download url of the release"),
                                subsectionWithPath("eccInformation").description("The eccInformation of this release"),
                                fieldWithPath("binaryDownloadurl").description("the binary download url of the release"),
                                fieldWithPath("otherLicenseIds").description("An array of all other licenses associated with the release"),
                                subsectionWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                subsectionWithPath("clearingInformation").description("Clearing information of release"),
                                fieldWithPath("languages").description("The language of the component"),
                                subsectionWithPath("_embedded.sw360:licenses").description("An array of all main licenses with their fullName and link to their <<resources-license-get,License resource>>"),
                                subsectionWithPath("_embedded.sw360:packages").description("An array of all the linked packages and link to their <<resources-package-get,Package resource>>"),
                                fieldWithPath("operatingSystems").description("The OS on which the release operates"),
                                fieldWithPath("softwarePlatforms").description("The software platforms of the component"),
                                subsectionWithPath("_embedded.sw360:moderators").description("An array of all release moderators with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:attachments").description("An array of all release attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_load_spdx_licenses_info_from_isr() throws Exception {
        mockLicensesInfo(AttachmentType.INITIAL_SCAN_REPORT);
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + releaseTest.getId() + "/spdxLicensesInfo?attachmentId=" + attachmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_load_spdx_licenses_info_from_clx_or_cli() throws Exception {
        mockLicensesInfo(AttachmentType.COMPONENT_LICENSE_INFO_COMBINED);
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + releaseTest.getId() + "/spdxLicensesInfo?attachmentId=" + attachmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    public void mockLicensesInfo(AttachmentType attachmentType) throws TException{
        Set<Attachment> listAttachment = new HashSet<>();
        Attachment attachmentTest = new Attachment();

        attachmentTest.setSha1(attachmentSha1);
        attachmentTest.setAttachmentType(attachmentType);
        attachmentTest.setCreatedOn("2016-12-18");
        attachmentTest.setCreatedBy("admin@sw360.org");
        attachmentTest.setFilename("SPDX_filename.rdf");
        attachmentTest.setAttachmentContentId(attachmentId);
        listAttachment.add(attachmentTest);
        listAttachment.add(attachment);
        releaseTest.setAttachments(listAttachment);

        List<LicenseInfoParsingResult> licenseInfoResults = new ArrayList<>();
        LicenseInfoParsingResult licenseInfoParsingResult = new LicenseInfoParsingResult();
        licenseInfoParsingResult.setStatus(LicenseInfoRequestStatus.SUCCESS);
        licenseInfoParsingResult.setRelease(releaseTest);
        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setFilenames(Collections.singletonList("SPDX_filename.rdf"));
        LicenseNameWithText license1 = new LicenseNameWithText();
        license1.setLicenseName("MIT");
        license1.setLicenseText("MIT Text");
        license1.setLicenseSpdxId("MIT");

        LicenseNameWithText license2 = new LicenseNameWithText();
        license2.setLicenseName("RSA-Security");
        license2.setLicenseText("License by Nomos.");
        license2.setLicenseSpdxId("RSA-Security");

        if (AttachmentType.INITIAL_SCAN_REPORT.equals(attachmentType)) {
            license1.setSourceFiles(Sets.newHashSet(
                    "test-3.2.tar.gz/test-3.2/sample",
                    "test-3.2.tar.gz/test-3.2/support/sys/cron",
                    "test-3.2.tar.gz/test-3.2/support/README")
            );
            license2.setSourceFiles(Sets.newHashSet(
                    "test-3.2.tar.gz/test-3.2/support/md5.h")
            );
        } else {
            licenseInfo.setConcludedLicenseIds(Sets.newHashSet("GPL", "BSD-3-Clause"));
        }

        licenseInfo.setLicenseNamesWithTexts(Sets.newHashSet(license1, license2));

        licenseInfoParsingResult.setLicenseInfo(licenseInfo);
        licenseInfoResults.add(licenseInfoParsingResult);
        boolean includeConcludedLicense = AttachmentType.INITIAL_SCAN_REPORT.equals(attachmentType);
        given(this.releaseServiceMock.getReleaseForUserById(eq(releaseTest.getId()), any())).willReturn(releaseTest);
        given(this.licenseInfoMockService.getLicenseInfoForAttachment(any(), any(), any(), eq(includeConcludedLicense))).willReturn(licenseInfoResults);
    }
}
