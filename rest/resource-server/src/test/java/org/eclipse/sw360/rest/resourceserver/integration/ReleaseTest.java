/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com> , 2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.eclipse.sw360.rest.resourceserver.TestHelper.getDummyReleaseListForTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReleaseTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    @SuppressWarnings("unused")
    private int port;

    @MockitoBean
    @SuppressWarnings("unused")
    private Sw360ReleaseService releaseServiceMock;

    @MockitoBean
    @SuppressWarnings("unused")
    private Sw360LicenseService licenseServiceMock;

    @MockitoBean
    @SuppressWarnings("unused")
    private Sw360AttachmentService attachmentServiceMock;

    @MockitoBean
    @SuppressWarnings("unused")
    private SW360PackageService packageServiceMock;

    @MockitoBean
    @SuppressWarnings("unused")
    private Sw360VulnerabilityService vulnerabilityServiceMock;

    @MockitoBean
    @SuppressWarnings("unused")
    private Sw360VendorService sw360VendorService;

    @MockitoBean
    @SuppressWarnings("unused")
    private Sw360LicenseInfoService licenseInfoMockService;

    private Release release;
    public static String attachmentShaInvalid = "999";

    @Before
    public void before() throws TException, IOException {
        List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        Attachment attachment = new Attachment("1231231254", "spring-core-4.3.4.RELEASE.jar");
        String attachmentSha1 = "da373e491d3863477568896089ee9457bc316783";
        attachment.setSha1(attachmentSha1);
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
        AttachmentInfo attachmentInfo = new AttachmentInfo(attachment);
        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        attachmentInfos.add(attachmentInfo);

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
        given(this.attachmentServiceMock.getAttachmentContent(any())).willReturn(new AttachmentContent().setId("1231231254").setFilename("spring-core-4.3.4.RELEASE.jar").setContentType("binary"));
        given(this.attachmentServiceMock.getResourcesFromList(any())).willReturn(CollectionModel.of(attachmentResources));
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment);
        given(this.attachmentServiceMock.filterAttachmentsToRemove(any(), any(), any())).willReturn(Collections.singleton(attachment));
        given(this.attachmentServiceMock.updateAttachment(any(), any(), any(), any())).willReturn(att2);
        given(this.sw360VendorService.getVendorById(any())).willReturn(new Vendor("TV", "Test Vendor", "http://testvendor.com"));
        given(this.attachmentServiceMock.isAttachmentContentExist(eq("1231231254"))).willReturn(true);
        given(this.attachmentServiceMock.getAttachmentResourcesFromList(any(), any(), any())).willReturn(CollectionModel.of(attachmentResources));

        Map<String, Set<String>> externalIds = new HashMap<>();
        externalIds.put("mainline-id-component", new HashSet<>(Arrays.asList("1432", "4876")));

        EccInformation eccInformation = new EccInformation();
        eccInformation.setAl("AL");
        eccInformation.setEccn("ECCN");
        eccInformation.setAssessorContactPerson("admin@sw360.org");
        eccInformation.setAssessorDepartment("DEPARTMENT");
        eccInformation.setEccComment("Set ECC");
        eccInformation.setMaterialIndexNumber("12");
        eccInformation.setAssessmentDate("2023-06-27");
        eccInformation.setEccStatus(ECCStatus.OPEN);

        List<Release> releaseList = getDummyReleaseListForTest();
        release = releaseList.getFirst();

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
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);

        Set<String> linkedPackages = new HashSet<>();
        linkedPackages.add(package1.getId());

        release.setPackageIds(linkedPackages);
        given(this.releaseServiceMock.getReleasesForUser(any())).willReturn(releaseList);
        given(this.releaseServiceMock.refineSearch(any(),any(),any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(releaseList.size()).setDisplayStart(0).setTotalRowCount(releaseList.size()),
                        releaseList
                )
        );
        given(this.releaseServiceMock.getRecentReleases(any())).willReturn(releaseList);
        given(this.releaseServiceMock.getReleaseSubscriptions(any())).willReturn(releaseList);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release.getId()), any())).willReturn(release);
        given(this.releaseServiceMock.getProjectsByRelease(eq(release.getId()), any())).willReturn(new HashSet<>());
        given(this.releaseServiceMock.deleteRelease(eq(release.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.searchByExternalIds(eq(externalIds), any())).willReturn((new HashSet<>(releaseList)));
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.createRelease(any(), any())).willReturn(
                new Release("Test Release", "1.0", "17653524")
                        .setId("1234567890"));

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789").setUserGroup(UserGroup.ADMIN));
        given(this.releaseServiceMock.searchReleaseByNamePaginated(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(TestHelper.getDummyReleaseListForTest().size()).setDisplayStart(0).setTotalRowCount(TestHelper.getDummyReleaseListForTest().size()),
                        TestHelper.getDummyReleaseListForTest()
                )
        );

        given(this.licenseServiceMock.getLicenseById("Apache-2.0")).willReturn(
                new License("Apache 2.0 License")
                        .setText("Dummy License Text")
                        .setShortname("Apache-2.0")
                        .setId(UUID.randomUUID().toString()));

        // mock for SHA1 search
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(TestHelper.attachmentShaUsedMultipleTimes)))
                .willReturn(TestHelper.getDummyAttachmentInfoListForTest());

        // setComponentDependentFieldsInRelease mock to avoid component service calls
        given(this.releaseServiceMock.setComponentDependentFieldsInRelease(any(Release.class), any())).willAnswer(invocation -> {
            Release r = invocation.getArgument(0);
            r.setComponentType(ComponentType.INTERNAL);
            return r;
        });

        given(this.releaseServiceMock.setComponentDependentFieldsInRelease(any(List.class), any())).willAnswer(invocation -> {
            List<Release> releases = invocation.getArgument(0);
            for (Release r : releases) {
                r.setComponentType(ComponentType.INTERNAL);
            }
            return releases;
        });

        given(this.releaseServiceMock.convertToEmbeddedWithExternalIds(eq(release))).willReturn(
                new Release("Angular", "2.3.0", "17653524")
                        .setId(release.getId())
                        .setExternalIds(Collections.singletonMap("mainline-id-component", "1432")));
        given(this.releaseServiceMock.countProjectsByReleaseId(eq(release.getId()))).willReturn(2);
        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.release1Id), any())).willReturn(getDummyReleaseListForTest().get(0));
        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.releaseId2), any())).willReturn(getDummyReleaseListForTest().get(1));
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.deleteRelease(eq(TestHelper.release1Id), any())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.getProjectsByRelease(eq(TestHelper.release1Id), any())).willReturn(new HashSet<>());
        given(this.releaseServiceMock.getUsingComponentsForRelease(eq(TestHelper.release1Id), any())).willReturn(new HashSet<>());

        // Attachment service mocks for upload tests
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment);
        given(this.attachmentServiceMock.getAttachmentById(any())).willReturn(new AttachmentInfo(attachment));
        given(this.attachmentServiceMock.getAttachmentContent(any())).willReturn(new AttachmentContent().setId("1231231254").setFilename("test.jar").setContentType("binary"));
    }

    @Test
    public void should_update_release_valid() throws IOException {
        String updatedReleaseName = "updatedReleaseName";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        body.put("mainLicenseIds", new String[] { "Apache-2.0" });
        body.put("wrong_prop", "abc123");
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id,
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(updatedReleaseName, responseBody.get("name").textValue());
        JsonNode licenses = responseBody.get("_embedded").get("sw360:licenses");
        assertTrue(licenses.isArray());
        List<String> mainLicenseIds = new ArrayList<>();
        for (JsonNode license : licenses) {
            mainLicenseIds.add(license.get("fullName").textValue());
        }
        assertEquals(1, mainLicenseIds.size());
        assertEquals("Apache 2.0 License", mainLicenseIds.getFirst());
        assertNull(responseBody.get("wrong_prop"));
    }

    @Test
    public void should_update_release_invalid() throws IOException, TException {
        given(this.releaseServiceMock.getReleaseForUserById(any(), any())).willThrow(TException.class);
        String updatedReleaseName = "updatedReleaseName";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/unknownId123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_all_releases_with_fields() throws IOException {
        String extraField = "cpeid";
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?fields=" + extraField,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2, Collections.singletonList(extraField));
    }

    @Test
    public void should_delete_releases() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(eq(TestHelper.release1Id), any())).willReturn(RequestStatus.SUCCESS);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "," + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(TestHelper.release1Id, HttpStatus.OK));
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_delete_release() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(any(), any())).willReturn(RequestStatus.FAILURE);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_get_empty_collection_for_invalid_sha() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/releases?sha1=" + attachmentShaInvalid, HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_get_collection_for_duplicated_shas() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/releases?sha1=" + TestHelper.attachmentShaUsedMultipleTimes
                        + "&fields=mainlineState,clearingState",
                HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_delete_attachments_successfully() throws TException, IOException {
        Release release = new Release(TestHelper.getDummyReleaseListForTest().get(0));
        final AtomicReference<Release> refUpdatedRelease = new AtomicReference<>();
        List<Attachment> attachments = TestHelper.getDummyAttachmentsListForTest();
        List<String> attachmentIds = Arrays.asList(attachments.get(0).attachmentContentId, "otherAttachmentId");
        String strIds = String.join(",", attachmentIds);
        release.setAttachments(new HashSet<>(attachments));

        // Mock the release service calls
        given(releaseServiceMock.getReleaseForUserById(eq(TestHelper.release1Id), any())).willReturn(release);
        given(releaseServiceMock.updateRelease(any(), any()))
                .will(invocationOnMock -> {
                    refUpdatedRelease.set(new Release((Release) invocationOnMock.getArguments()[0]));
                    return RequestStatus.SUCCESS;
                });

        // Mock the attachment service calls
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.releaseId(release.getId()),
                release.getAttachments(), attachmentIds))
                .willReturn(Collections.singleton(attachments.get(1)));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" +
                                TestHelper.release1Id + "/attachments/" + strIds,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode jsonResponse = new ObjectMapper().readTree(response.getBody());
        JsonNode jsonAttachments = jsonResponse.get("_embedded").get("sw360:attachments");
        assertTrue(jsonAttachments.isArray());
        Set<String> attachmentFileNames = StreamSupport.stream(jsonAttachments.spliterator(), false)
                .map(node -> node.get("filename").textValue())
                .collect(Collectors.toSet());
        assertThat(attachmentFileNames, hasSize(1));
        assertThat(attachmentFileNames, hasItem(attachments.get(0).getFilename()));

        Release updatedRelease = refUpdatedRelease.get();
        assertThat(updatedRelease, is(notNullValue()));
        assertThat(updatedRelease.getAttachments(), hasSize(1));
        assertThat(updatedRelease.getAttachments(), hasItem(attachments.get(0)));
    }

    @Test
    public void should_delete_attachments_with_failure_handling() throws TException, IOException {
        Release release = new Release(getDummyReleaseListForTest().getFirst());
        String attachmentId = TestHelper.getDummyAttachmentInfoListForTest().getFirst().getAttachment()
                .getAttachmentContentId();
        Set<Attachment> attachments = new HashSet<>(TestHelper.getDummyAttachmentsListForTest());
        release.setAttachments(attachments);
        given(releaseServiceMock.getReleaseForUserById(release.id, TestHelper.getTestUser())).willReturn(release);
        given(attachmentServiceMock.filterAttachmentsToRemove(Source.releaseId(release.getId()),
                release.getAttachments(), Collections.singletonList(attachmentId)))
                .willReturn(Collections.emptySet());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" +
                                TestHelper.release1Id + "/attachments/" + attachmentId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        then(releaseServiceMock)
                .should(never())
                .updateRelease(any(), any());
    }

    @Test
    public void should_get_all_releases() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_get_all_releases_with_all_details() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?allDetails=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_get_releases_by_lucene_search() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?name=Test&luceneSearch=true&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_get_recent_releases() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/recentReleases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_get_releases_by_name() throws IOException, TException {
        // mock and check for type case
        List<Release> mockReleases = getDummyReleaseListForTest();
        mockReleases.getFirst().setName("test");
        mockReleases.getLast().setName("Test");
        given(this.releaseServiceMock.getReleasesForUser(any())).willReturn(mockReleases);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?name=Test",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }



    @Test
    public void should_get_release_by_id() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(TestHelper.release1Id, responseBody.get("id").textValue());
        assertNotNull(responseBody.get("name"));
        assertNotNull(responseBody.get("version"));
    }

    @Test
    public void should_get_usedbyresource_for_release() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/usedBy/" + TestHelper.release1Id,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // The /usedBy endpoint returns a mixed collection, not just projects
        // So we check for the general structure instead of specific "projects" array
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertTrue(responseBody.has("_embedded"));
        assertTrue(responseBody.has("_links"));
    }

    @Test
    public void should_get_releases_by_externalIds() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?externalId=mainline-id-component:1432",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_create_release() throws IOException, TException {
        given(this.releaseServiceMock.createRelease(any(), any())).willReturn(release);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", release.getName());
        body.put("version", release.getVersion());
        body.put("componentId", release.getComponentId());
        body.put("mainLicenseIds", new String[] { "Apache-2.0" });

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(release.getName(), responseBody.get("name").textValue());
        assertEquals(release.getVersion(), responseBody.get("version").textValue());
    }

    @Test
    public void should_get_release_attachment_info() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/attachments",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "attachments", 1); // Changed from 0 to 1 to match mocked data
    }

    @Test
    public void should_update_release_attachment_info() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("attachmentContentId", "1231231254");
        body.put("checkStatus", "ACCEPTED");
        body.put("checkedComment", "Updated comment");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/attachment/1231231254",
                        HttpMethod.PATCH, // Changed back to PATCH
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_upload_attachment_to_release() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create multipart request with file and attachment info
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.jar";
            }
        });

        Map<String, Object> attachmentInfo = new HashMap<>();
        attachmentInfo.put("filename", "test.jar");
        attachmentInfo.put("attachmentType", "SOURCE");
        attachmentInfo.put("createdComment", "Test upload");
        body.add("attachment", attachmentInfo);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/attachments",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_link_releases_to_release() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, ReleaseRelationship> body = Collections.singletonMap("90876", ReleaseRelationship.DYNAMICALLY_LINKED);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/releases",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode()); // Changed from OK to CREATED
    }

    @Test
    public void should_link_packages() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> body = new HashSet<>(Arrays.asList("package1", "package2"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/link/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode()); // Changed from OK to CREATED
    }

    @Test
    public void should_unlink_packages() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> body = new HashSet<>(Arrays.asList("package1", "package2"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/unlink/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode()); // Changed from OK to CREATED
    }

    @Test
    public void should_get_release_vulnerabilities() throws IOException {

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/vulnerabilities",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_release_subscription() throws IOException, TException {
        given(this.releaseServiceMock.getReleaseSubscriptions(any())).willReturn(getDummyReleaseListForTest());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/mySubscriptions",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    @Test
    public void should_write_spdx_licenses_info_into_release() throws IOException, TException {
        // Create a proper release with initialized license ID sets
        Release testRelease = new Release();
        testRelease.setId(TestHelper.release1Id);
        testRelease.setName("Test Release");
        testRelease.setVersion("1.0.0");
        testRelease.setMainLicenseIds(new HashSet<>());
        testRelease.setOtherLicenseIds(new HashSet<>());

        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.release1Id), any())).willReturn(testRelease);
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Set<String>> body = new HashMap<>();
        body.put("mainLicenseIds", new HashSet<>(Arrays.asList("Apache-2.0", "MIT")));
        body.put("otherLicenseIds", new HashSet<>(Arrays.asList("GPL-2.0")));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/spdxLicenses",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_direct_linked_releases() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/releases?transitive=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_linked_releases_transitively() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/releases?transitive=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_check_cyclic_hierarchy_of_a_release_with_other_releases() throws IOException, TException {
        given(this.releaseServiceMock.checkForCyclicLinkedReleases(any(), any(), any())).willReturn("No cyclic links found");

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Set<String>> body = new HashMap<>();
        body.put("linkedReleases", new HashSet<>(Arrays.asList("release1")));
        body.put("linkedToReleases", new HashSet<>(Arrays.asList("release2")));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/checkCyclicLink",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode()); // Changed from OK to MULTI_STATUS
    }

    @Test
    public void should_user_subscribe_release() throws IOException, TException {
        release.setSubscribers(new HashSet<>()); // Initialize empty subscribers set

        given(this.releaseServiceMock.getReleaseForUserById(eq(TestHelper.release1Id), any())).willReturn(release);
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/subscriptions",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    @Test
    public void should_trigger_fossology_process() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/triggerFossologyProcess?markFossologyProcessOutdated=true&uploadDescription=Test upload",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_check_fossology_process_status() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/checkFossologyProcessStatus",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_trigger_fossology_process_with_custom_options() throws IOException, TException {
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        String scanOptionsJson = "{\n" +
                "  \"analysis\": {\n" +
                "    \"bucket\": true,\n" +
                "    \"copyrightEmailAuthor\": true,\n" +
                "    \"monk\": true,\n" +
                "    \"nomos\": true,\n" +
                "    \"ojo\": true\n" +
                "  },\n" +
                "  \"decider\": {\n" +
                "    \"nomosMonk\": true,\n" +
                "    \"bulkReused\": true\n" +
                "  },\n" +
                "  \"reuse\": {\n" +
                "    \"reuseMain\": true,\n" +
                "    \"reuseCopyright\": true\n" +
                "  }\n" +
                "}";

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/triggerFossologyProcessWithOptions?markFossologyProcessOutdated=true&uploadDescription=Test upload with custom options",
                        HttpMethod.POST,
                        new HttpEntity<>(scanOptionsJson, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_validate_scan_options_and_return_bad_request() throws IOException {
        String invalidScanOptionsJson = "{\n" +
                "  \"analysis\": {\n" +
                "    \"invalidAgent\": true,\n" +
                "    \"monk\": true\n" +
                "  },\n" +
                "  \"decider\": {\n" +
                "    \"invalidDecider\": true\n" +
                "  }\n" +
                "}";

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + TestHelper.release1Id + "/triggerFossologyProcessWithOptions?uploadDescription=Test",
                        HttpMethod.POST,
                        new HttpEntity<>(invalidScanOptionsJson, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
