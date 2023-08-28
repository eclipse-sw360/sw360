/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 * Copyright Bosch Software Innovations GmbH, 2018.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import com.google.common.collect.ImmutableSet;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.VerificationState;
import org.eclipse.sw360.datahandler.thrift.VerificationStateInfo;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelationDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityState;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ComponentSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ComponentService componentServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockBean
    private Sw360VulnerabilityService vulnerabilityServiceMock;

    @MockBean
    private Sw360VendorService vendorServiceMock;
    
    @MockBean
    private SW360ReportService sw360ReportServiceMock;

    private Component angularComponent;

    private Component angularTargetComponent;

    private Attachment attachment;

    private Project project;

    private Component sBOMComponent;
    private Attachment sBOMAttachment;
    private RequestSummary requestSummary = new RequestSummary();

    private Release release;
    private Release release2;

    @Before
    public void before() throws TException, IOException {
        Set<String> licenseIds = new HashSet<>();
        licenseIds.add("MIT");
        licenseIds.add("Apache-2.0");
        Vendor vendor = new Vendor();
        vendor.setId("vendorId");
        vendor.setFullname("vendorFullName");
        vendor.setShortname("vendorShortName");
        vendor.setUrl("https://vendor.com");
        Set<Attachment> attachmentList = new HashSet<>();
        List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        attachment = new Attachment("1231231254", "spring-core-4.3.4.RELEASE.jar");
        attachment.setSha1("da373e491d3863477568896089ee9457bc316783");
        attachmentList.add(attachment);
        attachmentResources.add(EntityModel.of(attachment));
        Attachment attachment2 = new Attachment("1231231255", "spring-mvc-4.3.4.RELEASE.jar");
        attachment2.setSha1("da373e491d3863477568896089ee9457bc316784");
        attachmentList.add(attachment2);
        attachmentResources.add(EntityModel.of(attachment2));

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
        given(this.attachmentServiceMock.filterAttachmentsToRemove(any(), any(), any())).willReturn(Collections.singleton(attachment));
        given(this.attachmentServiceMock.updateAttachment(any(), any(), any(), any())).willReturn(att2);

        Map<String, Set<String>> externalIds = new HashMap<>();
        externalIds.put("component-id-key", ImmutableSet.of("1831A3", "c77321"));

        Component testComponent = new Component().setAttachments(setOfAttachment).setId("98745")
                .setName("Test Component").setComponentType(ComponentType.CODE_SNIPPET)
                .setCreatedOn("2021-04-27").setCreatedBy("admin@sw360.org")
                .setCategories(ImmutableSet.of("java", "javascript", "sql"));

        List<Component> componentList = new ArrayList<>();
        Set<Component> usedByComponent = new HashSet<>();
        List<Component> componentListByName = new ArrayList<>();
        Map<String, String> angularComponentExternalIds = new HashMap<>();
        angularComponentExternalIds.put("component-id-key", "1831A3");
        angularComponentExternalIds.put("ws-component-id", "[\"123\",\"598752\"]");
        Map<String, String> angularTargetComponentExternalIds = new HashMap<>();
        angularComponentExternalIds.put("component-id-key", "1831A4");
        angularComponentExternalIds.put("ws-component-id", "[\"123\",\"598753\"]");
        angularComponent = new Component();
        angularComponent.setId("17653524");
        angularComponent.setName("Angular");
        angularComponent.setComponentOwner("John");
        angularComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        angularComponent.setCreatedOn("2016-12-15");
        angularComponent.setCreatedBy("admin@sw360.org");
        angularComponent.setModifiedBy("admin1@sw360.org");
        angularComponent.setModifiedOn("2016-12-30");
        angularComponent.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Linux")));
        angularComponent.setMainLicenseIds(new HashSet<>(Arrays.asList("123")));
        angularComponent.setSubscribers(new HashSet<>(Arrays.asList("Mari")));
        angularComponent.setWiki("http://wiki.ubuntu.com/");
        angularComponent.setBlog("http://www.javaworld.com/");
        angularComponent.setComponentType(ComponentType.OSS);
        angularComponent.setVendorNames(new HashSet<>(Collections.singletonList("Google")));
        angularComponent.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "john@sw360.org")));
        angularComponent.setOwnerAccountingUnit("4822");
        angularComponent.setOwnerCountry("DE");
        angularComponent.setOwnerGroup("AA BB 123 GHV2-DE");
        angularComponent.setCategories(ImmutableSet.of("java", "javascript", "sql"));
        angularComponent.setLanguages(ImmutableSet.of("EN", "DE"));
        angularComponent.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        angularComponent.setAttachments(attachmentList);
        angularComponent.setExternalIds(angularComponentExternalIds);
        angularComponent.setMailinglist("test@liferay.com");
        angularComponent.setAdditionalData(Collections.singletonMap("Key", "Value"));
        angularComponent.setHomepage("https://angular.io");
        angularComponent.setMainLicenseIds(licenseIds);
        angularComponent.setDefaultVendorId("vendorId");


        angularTargetComponent = new Component();
        angularTargetComponent.setId("87654321");
        angularTargetComponent.setName("Angular");
        angularTargetComponent.setComponentOwner("John");
        angularTargetComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        angularTargetComponent.setCreatedOn("2016-12-15");
        angularTargetComponent.setCreatedBy("admin@sw360.org");
        angularTargetComponent.setModifiedBy("admin1@sw360.org");
        angularTargetComponent.setModifiedOn("2016-12-30");
        angularTargetComponent.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Linux")));
        angularTargetComponent.setMainLicenseIds(new HashSet<>(Arrays.asList("123")));
        angularTargetComponent.setSubscribers(new HashSet<>(Arrays.asList("Mari")));
        angularTargetComponent.setWiki("http://wiki.ubuntu.com/");
        angularTargetComponent.setBlog("http://www.javaworld.com/");
        angularTargetComponent.setComponentType(ComponentType.OSS);
        angularTargetComponent.setVendorNames(new HashSet<>(Collections.singletonList("Google")));
        angularTargetComponent.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "john@sw360.org")));
        angularTargetComponent.setOwnerAccountingUnit("4822");
        angularTargetComponent.setOwnerCountry("DE");
        angularTargetComponent.setOwnerGroup("AA BB 123 GHV2-DE");
        angularTargetComponent.setCategories(ImmutableSet.of("java", "javascript", "sql"));
        angularTargetComponent.setLanguages(ImmutableSet.of("EN", "DE"));
        angularTargetComponent.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        angularTargetComponent.setAttachments(attachmentList);
        angularTargetComponent.setExternalIds(angularTargetComponentExternalIds);
        angularTargetComponent.setMailinglist("test@liferay.com");
        angularTargetComponent.setAdditionalData(Collections.singletonMap("Key", "Value"));
        angularTargetComponent.setHomepage("https://angular.io");
        angularTargetComponent.setMainLicenseIds(licenseIds);
        angularTargetComponent.setDefaultVendorId("vendorId");

        componentList.add(angularComponent);
        componentList.add(angularTargetComponent);
        componentListByName.add(angularComponent);

        AttachmentDTO attachmentDTO = new AttachmentDTO();
        attachmentDTO.setAttachmentContentId("");
        attachmentDTO.setFilename(attachment.getFilename());
        attachmentDTO.setSha1(attachment.getSha1());
        attachmentDTO.setAttachmentType(AttachmentType.BINARY_SELF);
        attachmentDTO.setCreatedBy("admin@sw360.org");
        attachmentDTO.setCreatedTeam("Clearing Team 1");
        attachmentDTO.setCreatedComment("please check asap");
        attachmentDTO.setCreatedOn("2016-12-18");
        attachmentDTO.setCheckedTeam("Clearing Team 2");
        attachmentDTO.setCheckedComment("everything looks good");
        attachmentDTO.setCheckedOn("2016-12-18");
        attachmentDTO.setCheckStatus(CheckStatus.ACCEPTED);

        UsageAttachment usageAttachment = new UsageAttachment();
        usageAttachment.setVisible(0);
        usageAttachment.setRestricted(0);

        attachmentDTO.setUsageAttachment(usageAttachment);
        List<EntityModel<AttachmentDTO>> atEntityModels = new ArrayList<>();
        atEntityModels.add(EntityModel.of(attachmentDTO));
        given(this.attachmentServiceMock.getAttachmentDTOResourcesFromList(any(), any(), any())).willReturn(CollectionModel.of(atEntityModels));

        Component springComponent = new Component();
        Map<String, String> springComponentExternalIds = new HashMap<>();
        springComponentExternalIds.put("component-id-key", "c77321");
        springComponentExternalIds.put("ws-component-id", "[\"125\",\"698452\"]");

        springComponent.setId("678dstzd8");
        springComponent.setName("Spring Framework");
        springComponent.setComponentOwner("Jane");
        springComponent.setDescription("The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.");
        springComponent.setCreatedOn("2016-12-18");
        springComponent.setCreatedBy("jane@sw360.org");
        springComponent.setModifiedBy("User@sw360.org");
        springComponent.setModifiedOn("2016-12-25");
        springComponent.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Windows")));
        springComponent.setMainLicenseIds(new HashSet<>(Arrays.asList("222")));
        springComponent.setSubscribers(new HashSet<>(Arrays.asList("Natan")));
        springComponent.setWiki("http://wiki.ubuntu.com/");
        springComponent.setBlog("http://www.javaworld.com/");
        springComponent.setComponentType(ComponentType.OSS);
        springComponent.setVendorNames(new HashSet<>(Collections.singletonList("Pivotal")));
        springComponent.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        springComponent.setOwnerAccountingUnit("5661");
        springComponent.setOwnerCountry("FR");
        springComponent.setOwnerGroup("SIM-KA12");
        springComponent.setCategories(ImmutableSet.of("jdbc", "java"));
        springComponent.setLanguages(ImmutableSet.of("EN", "DE"));
        springComponent.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        springComponent.setExternalIds(springComponentExternalIds);
        springComponent.setMailinglist("test@liferay.com");
        springComponent.setMainLicenseIds(licenseIds);
        springComponent.setDefaultVendorId("vendorId");
        componentList.add(springComponent);
        usedByComponent.add(springComponent);

        Set<Project> projectList = new HashSet<>();
        project = new Project();
        project.setId("376576");
        project.setName("Emerald Web");
        project.setProjectType(ProjectType.PRODUCT);
        project.setVersion("1.0.2");
        projectList.add(project);

        when(this.componentServiceMock.createComponent(any(), any())).then(invocation ->
                new Component("Spring Framework")
                        .setDescription("The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.")
                        .setComponentType(ComponentType.OSS)
                        .setId("1234567890")
                        .setCreatedBy("admin@sw360.org")
                        .setCreatedOn(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

        given(this.componentServiceMock.getComponentsForUser(any())).willReturn(componentList);
        given(this.sw360ReportServiceMock.getComponentBuffer(any(),anyBoolean())).willReturn(ByteBuffer.allocate(10000));
        given(this.componentServiceMock.getRecentComponents(any())).willReturn(componentList);
        given(this.componentServiceMock.getComponentSubscriptions(any())).willReturn(componentList);
        given(this.componentServiceMock.getMyComponentsForUser(any())).willReturn(componentList);
        given(this.componentServiceMock.getComponentForUserById(eq("17653524"), any())).willReturn(angularComponent);
        given(this.componentServiceMock.getComponentForUserById(eq("98745"), any())).willReturn(testComponent);
        given(this.componentServiceMock.getProjectsByComponentId(eq("17653524"), any())).willReturn(projectList);
        given(this.componentServiceMock.getUsingComponentsForComponent(eq("17653524"), any())).willReturn(usedByComponent);
        given(this.componentServiceMock.searchComponentByName(eq(angularComponent.getName()))).willReturn(componentListByName);
        given(this.componentServiceMock.deleteComponent(eq(angularComponent.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.componentServiceMock.searchByExternalIds(eq(externalIds), any())).willReturn((new HashSet<>(componentList)));
        given(this.componentServiceMock.convertToEmbeddedWithExternalIds(eq(angularComponent))).willReturn(
                new Component("Angular")
                        .setId("17653524")
                        .setComponentType(ComponentType.OSS)
                        .setExternalIds(Collections.singletonMap("component-id-key", "1831A3"))
        );
        given(this.componentServiceMock.convertToEmbeddedWithExternalIds(eq(angularTargetComponent))).willReturn(
                new Component("Angular")
                        .setId("87654321")
                        .setComponentType(ComponentType.OSS)
                        .setExternalIds(Collections.singletonMap("component-id-key", "1831A4"))
        );
        given(this.componentServiceMock.convertToEmbeddedWithExternalIds(eq(springComponent))).willReturn(
                new Component("Spring Framework")
                        .setId("678dstzd8")
                        .setComponentType(ComponentType.OSS)
                        .setExternalIds(Collections.singletonMap("component-id-key", "c77321"))
        );
        given(this.componentServiceMock.countProjectsByComponentId(eq("17653524"), any())).willReturn(2);

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("john@sw360.org")).willReturn(
                new User("john@sw360.org", "sw360").setId("74427996"));

        given(this.vendorServiceMock.getVendorById("vendorId")).willReturn(vendor);

        List<Release> releaseList = new ArrayList<>();
        release = new Release();
        release.setId("3765276512");
        release.setName("Angular 2.3.0");
        release.setCpeid("cpe:/a:Google:Angular:2.3.0:");
        release.setReleaseDate("2016-12-07");
        release.setVersion("2.3.0");
        release.setCreatedOn("2016-12-18");
        release.setCreatedBy("admin@sw360.org");
        release.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setComponentId(springComponent.getId());
        releaseList.add(release);

        release2 = new Release();
        release2.setId("3765276512");
        release2.setName("Angular 2.3.1");
        release2.setCpeid("cpe:/a:Google:Angular:2.3.1:");
        release2.setReleaseDate("2016-12-15");
        release2.setVersion("2.3.1");
        release2.setCreatedOn("2016-12-18");
        release2.setCreatedBy("admin@sw360.org");
        release2.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release2.setComponentId(springComponent.getId());
        releaseList.add(release2);

        List<VulnerabilityDTO> vulDtos = new ArrayList<VulnerabilityDTO>();
        List<VulnerabilityDTO> vulDtosUpdated = new ArrayList<VulnerabilityDTO>();
        VerificationStateInfo verificationStateInfo = new VerificationStateInfo();
        verificationStateInfo.setCheckedBy("admin@sw360.org");
        verificationStateInfo.setCheckedOn("2018-12-18");
        verificationStateInfo.setComment("Change status not checked");
        verificationStateInfo.setVerificationState(VerificationState.NOT_CHECKED);

        List<VerificationStateInfo> verificationStateInfos = new ArrayList<>();
        verificationStateInfos.add(verificationStateInfo);

        ReleaseVulnerabilityRelation relation = new ReleaseVulnerabilityRelation();
        relation.setReleaseId("3765276512");
        relation.setVulnerabilityId("1333333333");
        relation.setVerificationStateInfo(verificationStateInfos);
        relation.setMatchedBy("matchedBy");
        relation.setUsedNeedle("usedNeedle");
        relation.setVerificationStateInfo(verificationStateInfos);

        VulnerabilityDTO vulDto = new VulnerabilityDTO();
        vulDto.setTitle("12345_Title");
        vulDto.setComment("Lorem Ipsum");
        vulDto.setExternalId("12345");
        vulDto.setProjectRelevance("IRRELEVANT");
        vulDto.setIntReleaseId("3765276512");
        vulDto.setIntReleaseName("Angular 2.3.0");
        vulDto.setAction("Update to Fixed Version");
        vulDto.setPriority("2 - major");
        vulDto.setReleaseVulnerabilityRelation(relation);
        vulDtosUpdated.add(vulDto);
        vulDtos.add(vulDto);

        List<VerificationStateInfo> verificationStateInfos1 = new ArrayList<>();
        VerificationStateInfo verificationStateInfo1 = new VerificationStateInfo();
        verificationStateInfo1.setCheckedBy("user@sw360.org");
        verificationStateInfo1.setCheckedOn("2016-12-18");
        verificationStateInfo1.setComment("Change status checked");
        verificationStateInfo1.setVerificationState(VerificationState.CHECKED);
        verificationStateInfos1.add(verificationStateInfo1);

        ReleaseVulnerabilityRelation relation1 = new ReleaseVulnerabilityRelation();
        relation1.setReleaseId("3765276512");
        relation1.setVulnerabilityId("122222222");
        relation1.setVerificationStateInfo(verificationStateInfos1);

        VulnerabilityDTO vulDto1 = new VulnerabilityDTO();
        vulDto1.setTitle("23105_Title");
        vulDto1.setComment("Lorem Ipsum");
        vulDto1.setExternalId("23105");
        vulDto1.setProjectRelevance("APPLICABLE");
        vulDto1.setIntReleaseId("3765276512");
        vulDto1.setIntReleaseName("Angular 2.3.0");
        vulDto1.setAction("Update to Fixed Version");
        vulDto1.setPriority("1 - critical");
        vulDto1.setReleaseVulnerabilityRelation(relation1);
        vulDtos.add(vulDto1);

        given(this.componentServiceMock.getVulnerabilitiesByComponent(any(), any())).willReturn(vulDtos);

        List<ReleaseLink> releaseLinks = new ArrayList<>();
        Set<Attachment> attachmentList1 = new HashSet<>();
        Attachment attachment1 = new Attachment("1231231254", "spring-core-4.3.4.RELEASE.jar");
        attachment1.setCheckedComment("1111");
        attachment1.setAttachmentType(AttachmentType.CLEARING_REPORT);
        attachment1.setCheckedBy("admin@sw360.org");
        attachment1.setCheckStatus(CheckStatus.ACCEPTED);
        attachmentList1.add(attachment1);

        ClearingReport clearingReport = new ClearingReport();
        clearingReport.setClearingReportStatus(ClearingReportStatus.DOWNLOAD);
        clearingReport.setAttachments(attachmentList1);

        ReleaseLink releaseLink = new ReleaseLink();

        releaseLink.setId("376527651211");
        releaseLink.setName("ReactJs 1.1.0");
        releaseLink.setVersion("1.1.0");
        releaseLink.setMainlineState(MainlineState.OPEN);
        releaseLink.setClearingReport(clearingReport);
        releaseLink.setClearingState(ClearingState.APPROVED);
        releaseLinks.add(releaseLink);

        ReleaseLink releaseLink2 = new ReleaseLink();
        ClearingReport clearingReport1 = new ClearingReport();
        clearingReport1.setClearingReportStatus(ClearingReportStatus.NO_REPORT);

        releaseLink2.setId("3765276512");
        releaseLink2.setName("Angular 2.3.1");
        releaseLink2.setVersion("2.3.1");
        releaseLink2.setMainlineState(MainlineState.OPEN);
        releaseLink2.setClearingReport(clearingReport1);
        releaseLink2.setClearingState(ClearingState.NEW_CLEARING);
        releaseLinks.add(releaseLink2);

        given(this.componentServiceMock.convertReleaseToReleaseLink(any(),any())).willReturn(releaseLinks);

        List<String> releaseIds = releaseList.stream().map(Release::getId).collect(Collectors.toList());
        given(this.vulnerabilityServiceMock.getVulnerabilityDTOByExternalId(any(), any())).willReturn(vulDtosUpdated);
        given(this.componentServiceMock.getReleaseIdsFromComponentId(any(), any())).willReturn(releaseIds);
        given(this.vulnerabilityServiceMock.updateReleaseVulnerabilityRelation(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByReleaseId(any(), any())).willReturn(vulDtos);
        angularComponent.setReleases(releaseList);

        sBOMAttachment = new Attachment("3331231254", "bom.spdx.rdf");
        sBOMAttachment.setSha1("df90312312312534543544375345345383");
        sBOMAttachment.setAttachmentType(AttachmentType.SBOM);
        Set<Attachment> attachments = new HashSet<>();
        attachments.add(sBOMAttachment);

        sBOMComponent = new Component();
        sBOMComponent.setId("2222222");
        sBOMComponent.setName("Maven");
        sBOMComponent.setCreatedOn("2023-04-30");
        sBOMComponent.setBusinessUnit("sw360 BA");
        sBOMComponent.setComponentType(ComponentType.SERVICE);
        sBOMComponent.setCreatedBy("admin@sw360.org");
        sBOMComponent.setAttachments(attachments);

        Release release1 = new Release();
        release1.setId("3333333");
        release1.setComponentId("2222222");
        release1.setName("Green Web");
        release1.setVersion("1.0.0");
        release1.setCreatedOn("2023-04-30");
        release1.setComponentType(ComponentType.SERVICE);
        release1.setCreatedBy("admin@sw360.org");

        requestSummary.setMessage(sBOMComponent.getId());
        requestSummary.setRequestStatus(RequestStatus.SUCCESS);

        ImportBomRequestPreparation importBomRequestPreparation = new ImportBomRequestPreparation();
        importBomRequestPreparation.setComponentsName(sBOMComponent.getName());
        StringBuilder relesaeName = new StringBuilder();
        relesaeName.append(release1.getName());
        relesaeName.append(" ");
        relesaeName.append(release1.getVersion());
        importBomRequestPreparation.setReleasesName(relesaeName.toString());

        given(this.componentServiceMock.prepareImportSBOM(any(),any())).willReturn(importBomRequestPreparation);
        given(this.componentServiceMock.importSBOM(any(),any())).willReturn(requestSummary);
        given(this.componentServiceMock.getReleaseById(any(),any())).willReturn(release1);
        given(this.componentServiceMock.getComponentForUserById(eq(sBOMComponent.getId()), any())).willReturn(sBOMComponent);
    }

    @Test
    public void should_document_get_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("page").description("Page of components"),
                                parameterWithName("page_entries").description("Amount of components per page"),
                                parameterWithName("sort").description("Defines order of the components")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of components per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing components"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_components_with_all_details() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("allDetails", "true")
                        .param("page", "0")
                        .param("page_entries", "5")
                        .param("sort", "name,desc")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("allDetails").description("Flag to get components with all details. Possible values are `<true|false>`"),
                                parameterWithName("page").description("Page of components"),
                                parameterWithName("page_entries").description("Amount of components per page"),
                                parameterWithName("sort").description("Defines order of the components")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]id").description("The id of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]description").description("The component description"),
                                subsectionWithPath("_embedded.sw360:components.[]createdOn").description("The date the component was created"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components.[]componentOwner").description("The owner of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]ownerAccountingUnit").description("The owner accounting unit of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]ownerGroup").description("The owner group of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]ownerCountry").description("The owner country of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]visbility").description("The component visibility, possible values are: " + Arrays.asList(Visibility.values())),
                                subsectionWithPath("_embedded.sw360:components.[]externalIds").description("When components are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("_embedded.sw360:components.[]additionalData").description("A place to store additional data used by external tools").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]mainLicenseIds").description("Main license ids of component"),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.defaultVendor").description("Default vendor of component"),
                                subsectionWithPath("_embedded.sw360:components.[]defaultVendorId").description("Default vendor of component"),

                                subsectionWithPath("_embedded.sw360:components.[]subscribers").description("The subscribers of component"),
                                subsectionWithPath("_embedded.sw360:components.[]mainLicenseIds").description("The Main License Ids of component"),
                                subsectionWithPath("_embedded.sw360:components.[]softwarePlatforms").description("The Software Platforms of component"),
                                subsectionWithPath("_embedded.sw360:components.[]wiki").description("The wiki of component"),
                                subsectionWithPath("_embedded.sw360:components.[]blog").description("The blog of component"),
                                subsectionWithPath("_embedded.sw360:components.[]modifiedOn").description("The date the component was modified"),

                                subsectionWithPath("_embedded.sw360:components.[]categories").description("The component categories"),
                                subsectionWithPath("_embedded.sw360:components.[]moderators").description("The component moderators"),
                                subsectionWithPath("_embedded.sw360:components.[]languages").description("The language of the component"),

                                subsectionWithPath("_embedded.sw360:components.[]operatingSystems").description("The OS on which the component operates"),
                                subsectionWithPath("_embedded.sw360:components.[]mailinglist").description("Component mailing lists"),
                                subsectionWithPath("_embedded.sw360:components.[]setVisbility").description("The visibility of the component"),

                                subsectionWithPath("_embedded.sw360:components.[]setBusinessUnit").description("The business unit this component belongs to"),
                                subsectionWithPath("_embedded.sw360:components.[]_links").description("Self <<resources-index-links,Links>> to Component resource").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.sw360:moderators").description("An array of all component moderators with email").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.sw360:releases").description("An array of all releases").optional(),

                                subsectionWithPath("_embedded.sw360:components.[]homepage").description("The homepage url of the component").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.createdBy.email").description("The email of user who created this Component").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.createdBy.deactivated").description("The user is activated or deactivated").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.createdBy._links").description("Self <<resources-index-links,Links>> to Component resource").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.sw360:attachments.[]filename").description("Attached file name").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.sw360:attachments.[]sha1").description("The attachment sha1 value").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.sw360:attachments.[]_links").description("Self <<resources-index-links,Links>> to Component resource").optional(),
                                subsectionWithPath("_embedded.sw360:components.[]_embedded.sw360:vendors").description("The vendors list").optional(),

                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of components per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing components"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")

                        )));
    }

    @Test
    public void should_document_get_mysubscriptions_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/mySubscriptions")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
    @Test
    public void should_document_get_recent_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/recentComponents")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_usedbyresource_for_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/usedBy/17653524")
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
    public void should_document_get_components_no_paging_params() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_components_with_fields() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components")
                .header("Authorization", "Bearer " + accessToken)
                .param("fields", "ownerGroup,ownerCountry")
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("fields").description("Properties which should be present for each component in the result"),
                                parameterWithName("page").description("Page of components"),
                                parameterWithName("page_entries").description("Amount of components per page"),
                                parameterWithName("sort").description("Defines order of the components")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]ownerGroup").description("The ownerGroup of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]ownerCountry").description("The ownerCountry of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of components per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing components"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_component() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/17653524")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-components,Component resource>>")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the component"),
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("createdOn").description("The date the component was created"),
                                fieldWithPath("modifiedOn").description("The date the component was modified"),
                                fieldWithPath("componentOwner").description("The owner name of the component"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the component"),
                                fieldWithPath("ownerGroup").description("The owner group of the component"),
                                fieldWithPath("ownerCountry").description("The owner country of the component"),
                                fieldWithPath("categories").description("The component categories"),
                                fieldWithPath("moderators").description("The component moderators"),
                                fieldWithPath("languages").description("The language of the component"),
                                subsectionWithPath("externalIds").description("When components are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("operatingSystems").description("The OS on which the component operates"),
                                fieldWithPath("softwarePlatforms").description("The Software Platforms of component"),
                                fieldWithPath("subscribers").description("The subscribers of component"),
                                fieldWithPath("mainLicenseIds").description("The Main License Ids of component"),
                                fieldWithPath("wiki").description("The wiki of component"),
                                fieldWithPath("blog").description("The blog of component"),
                                fieldWithPath("mailinglist").description("Component mailing lists"),
                                fieldWithPath("homepage").description("The homepage url of the component"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this component"),
                                subsectionWithPath("_embedded.sw360:releases").description("An array of all component releases with version and link to their <<resources-releases,Releases resource>>"),
                                subsectionWithPath("_embedded.sw360:moderators").description("An array of all component moderators with email and link to their <<resources-user-get,User resource>>"),
                                subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with full name and link to their <<resources-vendor-get,Vendor resource>>"),
                                subsectionWithPath("_embedded.sw360:attachments").description("An array of all component attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                                fieldWithPath("visbility").description("The visibility type of the component"),
                                fieldWithPath("setVisbility").description("The visibility of the component"),
                                fieldWithPath("setBusinessUnit").description("Whether or not a business unit is set for the component"),
                                fieldWithPath("mainLicenseIds").description("Main license ids of component"),
                                subsectionWithPath("_embedded.defaultVendor").description("Default vendor of component"),
                                fieldWithPath("defaultVendorId").description("Default vendor id of component")
                        )));
    }

    @Test
    public void should_document_create_component() throws Exception {
        Map<String, String> component = new HashMap<>();
        component.put("name", "Spring Framework");
        component.put("description", "The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.");
        component.put("componentType", ComponentType.OSS.toString());
        component.put("homepage", "https://angular.io");

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(
                post("/api/components")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(component))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("_embedded.createdBy.email", Matchers.is("admin@sw360.org")))
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("homepage").description("The homepage url of the component")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the component"),
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("createdOn").description("The date the component was created"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this component"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("visbility").description("The visibility type of the component"),
                                fieldWithPath("setVisbility").description("The visibility of the component"),
                                fieldWithPath("setBusinessUnit").description("Whether or not a business unit is set for the component")
                        )));
    }

    @Test
    public void should_document_get_components_by_type() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components?type=" + angularComponent.getComponentType())
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("type").description("Filter for type"),
                                parameterWithName("page").description("Page of components"),
                                parameterWithName("page_entries").description("Amount of components per page"),
                                parameterWithName("sort").description("Defines order of the components")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of components per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing components"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_components_by_name() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components?name=" + angularComponent.getName())
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("name").description("Filter for name"),
                                parameterWithName("page").description("Page of components"),
                                parameterWithName("page_entries").description("Amount of components per page"),
                                parameterWithName("sort").description("Defines order of the components")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of components per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing components"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_update_component() throws Exception {
        ComponentDTO updateComponent = new ComponentDTO();
        AttachmentDTO attachmentDTO = new AttachmentDTO("1231231255", "spring-mvc-4.3.4.RELEASE.jar");
        Set<AttachmentDTO> attachmentDTOS = new HashSet<>();
        attachmentDTOS.add(attachmentDTO);
        updateComponent.setName("Updated Component");
        updateComponent.setAttachmentDTOs(attachmentDTOS);

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/components/17653524")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(updateComponent))
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(documentComponentProperties());
    }

    @Test
    public void should_document_merge_components() throws Exception {


        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/components/mergecomponents")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(angularTargetComponent))
                .header("Authorization", "Bearer " + accessToken)
                .param("mergeTargetId", "87654321")
                .param("mergeSourceId", "17653524")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("mergeSourceId").description("Id of a source component to merge"),
                                parameterWithName("mergeTargetId").description("Id of a target component to merge")
                        ),
                        requestFields(
                                fieldWithPath("id").description("The Id of the component"),
                                fieldWithPath("subscribers").description("The subscribers of component"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the component"),
                                subsectionWithPath("externalIds").description("When components are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("mainLicenseIds").description("The Main License Ids of component"),
                                fieldWithPath("languages").description("The language of the component"),
                                fieldWithPath("softwarePlatforms").description("The Software Platforms of component"),
                                fieldWithPath("operatingSystems").description("The OS on which the component operates"),
                                fieldWithPath("wiki").description("The wiki of component"),
                                fieldWithPath("blog").description("The blog of component"),
                                fieldWithPath("homepage").description("The homepage url of the component"),
                                fieldWithPath("modifiedOn").description("The date the component was modified"),

                                fieldWithPath("moderators").description("The component moderators"),

                                fieldWithPath("name").description("The updated name of the component"),
                                fieldWithPath("type").description("The updated name of the component"),
                                fieldWithPath("createdOn").description("The date the component was created"),
                                fieldWithPath("componentOwner").description("The owner name of the component"),
                                fieldWithPath("ownerGroup").description("The owner group of the component"),
                                fieldWithPath("ownerCountry").description("The owner country of the component"),
                                fieldWithPath("visbility").description("The visibility type of the component"),
                                fieldWithPath("defaultVendorId").description("Default vendor id of component"),
                                fieldWithPath("categories").description("The component categories"),
                                fieldWithPath("mailinglist").description("Component mailing lists"),
                                fieldWithPath("setVisbility").description("The visibility of the component"),
                                fieldWithPath("setBusinessUnit").description("Whether or not a business unit is set for the component"),
                                fieldWithPath("vendors").description("The vendors list"),
                                fieldWithPath("description").description("The updated component description"),
                                fieldWithPath("componentType").description("The updated  component type, possible values are: " + Arrays.asList(ComponentType.values()))
                        )));
    }

    @Test
    public void should_document_split_components() throws Exception {


        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        Component srcComponent = new Component();
        srcComponent.setId("17653524");
        srcComponent.setName("Angular");
        srcComponent.setComponentOwner("John");
        srcComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        List<Release> releaseList = new ArrayList<>();
        releaseList.add(release);
        Release release2 = new Release();
        releaseList.add(release2);

        srcComponent.setReleases(releaseList);
        Component targetComponent = new Component();
        targetComponent.setId("87654321");
        targetComponent.setName("Angular");
        targetComponent.setComponentOwner("John");
        targetComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        targetComponent.setReleases(releaseList);
        Map<String, Object> componentsMap = new HashMap<>();
        componentsMap.put("srcComponent", srcComponent);
        componentsMap.put("targetComponent", targetComponent);

                mockMvc.perform(patch("/api/components/splitComponents")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(componentsMap))
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("srcComponent.id").description("The ID of the source component"),
                                fieldWithPath("srcComponent.name").description("The name of the source component"),
                                fieldWithPath("srcComponent.description").description("The description of the source component"),
                                fieldWithPath("srcComponent.type").description("The type of the source component"),
                                fieldWithPath("srcComponent.componentOwner").description("The owner of the source component"),
                                fieldWithPath("srcComponent.visbility").description("The visibility of the source component"),
                                fieldWithPath("srcComponent.setVisbility").description("Flag indicating if the visibility is set"),
                                fieldWithPath("srcComponent.setBusinessUnit").description("Flag indicating if the business unit is set"),
                                fieldWithPath("targetComponent.id").description("The ID of the target component"),
                                fieldWithPath("targetComponent.name").description("The name of the target component"),
                                fieldWithPath("targetComponent.description").description("The description of the target component"),
                                fieldWithPath("targetComponent.type").description("The type of the target component"),
                                fieldWithPath("targetComponent.componentOwner").description("The owner of the target component"),
                                fieldWithPath("targetComponent.visbility").description("The visibility of the target component"),
                                fieldWithPath("targetComponent.setVisbility").description("Flag indicating if the visibility is set"),
                                fieldWithPath("targetComponent.setBusinessUnit").description("Flag indicating if the business unit is set")

                        )));
    }

    @Test
    public void should_document_delete_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(delete("/api/components/" + angularComponent.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isMultiStatus())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("[].resourceId").description("id of the deleted resource"),
                                fieldWithPath("[].status").description("status of the delete operation")
                        )));
    }

    @Test
    public void should_document_get_component_attachment_info() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/" + angularComponent.getId() + "/attachments")
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
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_component_attachment_info() throws Exception {
        Attachment updateAttachment = new Attachment().setAttachmentType(AttachmentType.BINARY)
                .setCreatedComment("Created Comment").setCheckStatus(CheckStatus.ACCEPTED)
                .setCheckedComment("Checked Comment");
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc
                .perform(patch("/api/components/98745/attachment/1234").contentType(MediaTypes.HAL_JSON)
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
    public void should_document_get_component_attachment() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/" + angularComponent.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", "Bearer " + accessToken)
                .accept("application/*"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_component_attachment_bundle() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/" + angularComponent.getId() + "/attachments/download")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept("application/zip"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_components_by_externalIds() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/searchByExternalIds?component-id-key=1831A3&component-id-key=c77321")
                .contentType(MediaTypes.HAL_JSON)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components.[]name").description("The name of the component, optional"),
                                subsectionWithPath("_embedded.sw360:components.[]externalIds").description("External Ids of the component. Return as 'Single String' when single value, or 'Array of String' when multi-values"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_upload_attachment_to_component() throws Exception {
        testAttachmentUpload("/api/components/", angularComponent.getId());
    }

    @Test
    public void should_document_delete_component_attachment() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(delete("/api/components/" + angularComponent.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(documentComponentProperties());
    }

    private RestDocumentationResultHandler documentComponentProperties() {
        return this.documentationHandler.document(
                links(
                        linkWithRel("self").description("The <<resources-components,Component resource>>")
                ),
                responseFields(
                        fieldWithPath("id").description("The id of the component"),
                        fieldWithPath("name").description("The name of the component"),
                        fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                        fieldWithPath("description").description("The component description"),
                        fieldWithPath("createdOn").description("The date the component was created"),
                        fieldWithPath("componentOwner").description("The owner name of the component"),
                        fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the component"),
                        fieldWithPath("ownerGroup").description("The owner group of the component"),
                        fieldWithPath("ownerCountry").description("The owner country of the component"),
                        subsectionWithPath("externalIds").description("When projects are imported from other tools, the external ids can be stored here. Store as 'Single String' when single value, or 'Array of String' when multi-values"),
                        subsectionWithPath("additionalData").description("A place to store additional data used by external tools"),
                        fieldWithPath("modifiedOn").description("The date the component was modified"),
                        fieldWithPath("softwarePlatforms").description("The Software Platforms of component"),
                        fieldWithPath("subscribers").description("The subscribers of component"),
                        fieldWithPath("mainLicenseIds").description("The Main License Ids of component"),
                        fieldWithPath("wiki").description("The wiki of component"),
                        fieldWithPath("blog").description("The blog of component"),
                        fieldWithPath("categories").description("The component categories"),
                        fieldWithPath("moderators").description("The component moderators"),
                        fieldWithPath("languages").description("The language of the component"),
                        fieldWithPath("mailinglist").description("Component mailing lists"),
                        fieldWithPath("operatingSystems").description("The OS on which the component operates"),
                        fieldWithPath("homepage").description("The homepage url of the component"),
                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                        subsectionWithPath("_embedded.createdBy").description("The user who created this component"),
                        subsectionWithPath("_embedded.sw360:releases").description("An array of all component releases with version and link to their <<resources-releases,Releases resource>>"),
                        subsectionWithPath("_embedded.sw360:moderators").description("An array of all component moderators with email and link to their <<resources-user-get,User resource>>"),
                        subsectionWithPath("_embedded.sw360:vendors").description("An array of all component vendors with ful name and link to their <<resources-vendor-get,Vendor resource>>"),
                        subsectionWithPath("_embedded.sw360:attachments").description("An array of all component attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                        fieldWithPath("visbility").description("The visibility type of the component"),
                        fieldWithPath("setVisbility").description("The visibility of the component"),
                        fieldWithPath("setBusinessUnit").description("Whether or not a business unit is set for the component"),
                        fieldWithPath("mainLicenseIds").description("Main license ids of component"),
                        subsectionWithPath("_embedded.defaultVendor").description("Default vendor of component"),
                        fieldWithPath("defaultVendorId").description("Default vendor id of component")
                ));
    }

    @Test
    public void should_document_get_mycomponents_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);

        mockMvc.perform(get("/api/components/mycomponents")
                .header("Authorization", "Bearer " + accessToken).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(linkWithRel("curies").description("Curies are used for online documentation")),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:components.[]name")
                                        .description("The name of the component"),
                                subsectionWithPath("_embedded.sw360:components.[]componentType")
                                        .description("The component type, possible values are: "
                                                + Arrays.asList(ComponentType.values())),
                                subsectionWithPath("_embedded.sw360:components")
                                        .description("An array of <<resources-components, Components resources>>"),
                                subsectionWithPath("_links")
                                        .description("<<resources-index-links,Links>> to other resources"))));
    }

    @Test
    public void should_document_get_component_vulnerabilities() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/" + angularComponent.getId()+ "/vulnerabilities")
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
                            )
                        ));

    }

    @Test
    public void should_document_update_component_vulnerabilities() throws Exception {

        VulnerabilityState vulnerabilityState = new VulnerabilityState();
        Set<ReleaseVulnerabilityRelationDTO> releaseVulnerabilityRelationDTOS = new HashSet<>();
        ReleaseVulnerabilityRelationDTO releaseVulnerabilityRelationDTO = new ReleaseVulnerabilityRelationDTO();
        releaseVulnerabilityRelationDTO.setExternalId("12345");
        releaseVulnerabilityRelationDTO.setReleaseName("Angular 2.3.0");
        releaseVulnerabilityRelationDTOS.add(releaseVulnerabilityRelationDTO);
        vulnerabilityState.setReleaseVulnerabilityRelationDTOs(releaseVulnerabilityRelationDTOS);
        vulnerabilityState.setComment("Change status");
        vulnerabilityState.setVerificationState(VerificationState.NOT_CHECKED);

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/components/" + angularComponent.getId() + "/vulnerabilities")
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
                                    subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]externalId").description("The ReleaseVulnerabilityRelation of release of the vulnerability, possible values are: "),
                                    subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]comment").description("Any message to add while updating releases vulnerabilities"),
                                    subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]projectAction").description("The action of vulnerability"),
                                    subsectionWithPath("_embedded.sw360:vulnerabilityDTOes.[]priority").description("The action of vulnerability"),
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
    public void should_document_get_releases_by_component() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/" + angularComponent.getId()+ "/releases")
                        .contentType(MediaTypes.HAL_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]id").description("The Id of the releaseLinks"),
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]name").description("The name of the releaseLinks"),
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]version").description("The version of the releaseLinks"),
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]mainlineState").description("The mainlineState of the releaseLinks "+ Arrays.asList(MainlineState.values())),
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]clearingReport").description("The clearingReport of the releaseLinks "),
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]clearingReport.clearingReportStatus").description("The clearingReportStatus of the clearingReport "+Arrays.asList(ClearingReportStatus.values())),
                                subsectionWithPath("_embedded.sw360:releaseLinks.[]clearingState").description("The clearingState of the releaseLinks "+ Arrays.asList(ClearingState.values())),
                                subsectionWithPath("_embedded.sw360:releaseLinks").description("An array of <<resources-releases, releases resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_import_sbom_for_component() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file","file=@/bom.spdx.rdf".getBytes());
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/api/components/import/SBOM")
                .content(file.getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("type", "SPDX");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_prepare_import_sbom_for_component() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file","file=@/bom.spdx.rdf".getBytes());
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/api/components/prepareImport/SBOM")
                .content(file.getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("type", "SPDX");
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }
    
    @Test
    public void should_document_get_component_report() throws Exception{
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("withlinkedreleases", "true")
                        .param("mimetype", "xlsx")
                        .param("mailrequest", "false")
                        .param("module", "components")
                        .accept(MediaTypes.HAL_JSON))
             .andExpect(status().isOk())
             .andDo(this.documentationHandler.document(
                     requestParameters(
                             parameterWithName("withlinkedreleases").description("Projects with linked releases. Possible values are `<true|false>`"),
                             parameterWithName("mimetype").description("Projects download format. Possible values are `<xls|xlsx>`"),
                             parameterWithName("mailrequest").description("Downloading project report requirted mail link. Possible values are `<true|false>`"),
                             parameterWithName("module").description("module represent the project or component. Possible values are `<components|projects>`")
                     )));
    }
    
    @Test
    public void should_document_get_component_report_with_mail_req() throws Exception{
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("withlinkedreleases", "true")
                        .param("mimetype", "xlsx")
                        .param("mailrequest", "true")
                        .param("module", "components")
                        .accept(MediaTypes.HAL_JSON))
             .andExpect(status().isOk())
             .andDo(this.documentationHandler.document(
                     requestParameters(
                             parameterWithName("withlinkedreleases").description("components with linked releases. Possible values are `<true|false>`"),
                             parameterWithName("mimetype").description("components download format. Possible values are `<xls|xlsx>`"),
                             parameterWithName("module").description("module represent the project or component. Possible values are `<components|projects>`"),
                             parameterWithName("mailrequest").description("Downloading components report requirted mail link. Possible values are `<true|false>`")
                     ),responseFields(
                             subsectionWithPath("response").description("The response message displayed").optional()
                             )
                     ));
    }
}
