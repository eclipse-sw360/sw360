/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class PackageSpecTest extends TestRestDocsSpecBase {

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

    private Package package1;
    private Package package2;
    private Set<String> licenseIds;

    @Before
    public void before() throws TException, IOException {
        Set<Attachment> setOfAttachment = new HashSet<Attachment>();
        Attachment att1 = new Attachment("1234", "test.zip").setAttachmentType(AttachmentType.SOURCE)
                .setCreatedBy("user@sw360.org").setSha1("da373e491d312365483589ee9457bc316783").setCreatedOn("2021-04-27")
                .setCreatedTeam("DEPARTMENT");
        Attachment att2 = att1.deepCopy().setAttachmentType(AttachmentType.BINARY).setCreatedComment("Created Comment")
                .setCheckStatus(CheckStatus.ACCEPTED).setCheckedComment("Checked Comment").setCheckedOn("2021-04-27")
                .setCheckedBy("admin@sw360.org").setCheckedTeam("DEPARTMENT1");

        setOfAttachment.add(att1);
        setOfAttachment.add(att2);

        Release testRelease = new Release().setAttachments(setOfAttachment).setId("98745").setName("Test Release")
                .setVersion("2").setComponentId("17653524").setCreatedOn("2021-04-27").setCreatedBy("admin@sw360.org");

        given(this.releaseServiceMock.getReleaseForUserById(eq(testRelease.getId()), any())).willReturn(testRelease);

        licenseIds = new HashSet<>();
        licenseIds.add("MIT");
        licenseIds.add("GPL");

        package1 = new Package("angular-sanitize", "1.8.2", "pkg:npm/angular-sanitize@1.8.2", CycloneDxComponentType.FRAMEWORK)
                        .setId("122357345")
                        .setCreatedBy("admin@sw360.org")
                        .setCreatedOn("2023-01-02")
                        .setVcs("git+https://github.com/angular/angular.js.git")
                        .setHomepageUrl("http://angularjs.org")
                        .setLicenseIds(licenseIds)
                        .setReleaseId(testRelease.getId())
                        .setPackageManager(PackageManager.NPM)
                        .setDescription("Sanitizes an html string by stripping all potentially dangerous tokens.");

        package2 = new Package()
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

        when(this.packageServiceMock.createPackage(any(), any())).then(invocation ->
        new Package(package1));

        List<Package> packageList = new ArrayList<>();
        packageList.add(package1);
        packageList.add(package2);

        given(this.packageServiceMock.getPackageForUserById(eq(package1.getId()))).willReturn(package1);
        given(this.packageServiceMock.getPackageForUserById(eq(package2.getId()))).willReturn(package2);
        given(this.packageServiceMock.deletePackage(eq(package1.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.packageServiceMock.getPackagesForUser()).willReturn(packageList);
        given(this.packageServiceMock.searchPackage(eq("name"), any(), eq(true))).willReturn(List.of(package1));
        given(this.packageServiceMock.searchPackage(eq("packageManager"), any(), eq(false))).willReturn(packageList);
        given(this.packageServiceMock.getTotalPackagesCounts()).willReturn(packageList.size());



        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        given(this.userServiceMock.getUserByEmailOrExternalId("user@sw360.org")).willReturn(
                new User("user@sw360.org", "sw360").setId("12345670089"));
        given(this.userServiceMock.getUserByEmail("user@sw360.org")).willReturn(
                new User("user@sw360.org", "sw360").setId("12345670089"));
    }

    @Test
    public void should_document_get_packages() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/packages")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("page").description("Page of packages"),
                                parameterWithName("page_entries").description("Amount of packages per page"),
                                parameterWithName("sort").description("Defines order of the packages")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:packages.[]name").description("The name of the package"),
                                subsectionWithPath("_embedded.sw360:packages.[]version").description("The package version"),
                                subsectionWithPath("_embedded.sw360:packages.[]purl").description("The package manager type, possible values are: " + Arrays.asList(PackageManager.values())),
                                subsectionWithPath("_embedded.sw360:packages").description("An array of <<resources-packages, Packages resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of packages per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing packages"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_packages_with_all_details() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/packages")
                .header("Authorization", "Bearer " + accessToken)
                .param("allDetails", "true")
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("allDetails").description("Flag to get packages with all details. Possible values are `<true|false>`"),
                                parameterWithName("page").description("Page of packages"),
                                parameterWithName("page_entries").description("Amount of packages per page"),
                                parameterWithName("sort").description("Defines order of the packages")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:packages.[]name").description("The name of the package"),
                                fieldWithPath("_embedded.sw360:packages.[]version").description("The version of the package"),
                                fieldWithPath("_embedded.sw360:packages.[]packageType").description("The package type, possible values are: " + Arrays.asList(CycloneDxComponentType.values())),
                                fieldWithPath("_embedded.sw360:packages.[]createdOn").description("The date of creation of the package"),
                                fieldWithPath("_embedded.sw360:packages.[]packageManager").description("The type of package manager"),
                                fieldWithPath("_embedded.sw360:packages.[]purl").description("Package URL"),
                                fieldWithPath("_embedded.sw360:packages.[]vcs").description("VCS(Version Control System) is the URL of the source code"),
                                fieldWithPath("_embedded.sw360:packages.[]homepageUrl").description("URL of the package website"),
                                fieldWithPath("_embedded.sw360:packages.[]licenseIds").description("The associated licenses").optional(),
                                fieldWithPath("_embedded.sw360:packages.[]description").description("Description of the package"),
                                subsectionWithPath("_embedded.sw360:packages.[]_embedded.sw360:release").description("The release to which the package is linked").optional(),
                                subsectionWithPath("_embedded.sw360:packages.[]_embedded.createdBy").description("The user who created this component"),
                                subsectionWithPath("_embedded.sw360:packages.[]_links").description("Self <<resources-index-links,Links>> to Package resource"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of packages per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing packages"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_package() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/packages/" + package1.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-packages,Packages resource>>")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the package"),
                                fieldWithPath("version").description("The version of the package"),
                                fieldWithPath("packageType").description("The package type, possible values are: " + Arrays.asList(CycloneDxComponentType.values())),
                                fieldWithPath("createdOn").description("The date of creation of the package"),
                                fieldWithPath("packageManager").description("The type of package manager"),
                                fieldWithPath("purl").description("Package URL"),
                                fieldWithPath("vcs").description("VCS(Version Control System) is the URL of the source code"),
                                fieldWithPath("homepageUrl").description("URL of the package website"),
                                fieldWithPath("licenseIds").description("The associated licenses"),
                                fieldWithPath("description").description("Description of the package"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:release").description("The release to which the package is linked"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this component")
                        )));


    }

    @Test
    public void should_document_get_packages_by_name() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/packages")
                .header("Authorization", "Bearer " + accessToken)
                .param("name", package1.getName())
                .param("exactMatch", "true")
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("name").description("The name of the package"),
                                parameterWithName("exactMatch").description("If the exactMatch parameter is set to true, "
                                        + "packages will be fetched by name exactly matching the search input"),
                                parameterWithName("page").description("Page of packages"),
                                parameterWithName("page_entries").description("Amount of packages per page"),
                                parameterWithName("sort").description("Defines order of the packages")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:packages.[]name").description("The name of the package"),
                                subsectionWithPath("_embedded.sw360:packages.[]version").description("The package version"),
                                subsectionWithPath("_embedded.sw360:packages.[]purl").description("The package manager type, possible values are: " + Arrays.asList(PackageManager.values())),
                                subsectionWithPath("_embedded.sw360:packages").description("An array of <<resources-packages, Packages resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of packages per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing packages"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_packages_by_package_manager() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/packages")
                .header("Authorization", "Bearer " + accessToken)
                .param("packageManager", PackageManager.NPM.toString())
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "name,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestParameters(
                                parameterWithName("packageManager").description("Type of the package manager"),
                                parameterWithName("page").description("Page of packages"),
                                parameterWithName("page_entries").description("Amount of packages per page"),
                                parameterWithName("sort").description("Defines order of the packages")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:packages.[]name").description("The name of the package"),
                                subsectionWithPath("_embedded.sw360:packages.[]version").description("The package version"),
                                subsectionWithPath("_embedded.sw360:packages.[]purl").description("The package manager type, possible values are: " + Arrays.asList(PackageManager.values())),
                                subsectionWithPath("_embedded.sw360:packages").description("An array of <<resources-packages, Packages resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of packages per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing packages"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }


    @Test
    public void should_document_create_package() throws Exception {
        Map<String, Object> pkg = new LinkedHashMap<>();

        pkg.put("name", "angular-sanitize");
        pkg.put("version", "1.8.2");
        pkg.put("packageType", CycloneDxComponentType.LIBRARY);
        pkg.put("purl", "pkg:npm/angular-sanitize@1.8.2");
        pkg.put("vcs", "git+https://github.com/angular/angular.js.git");
        pkg.put("homepageUrl", "https://github.com/angular/angular-sanitize");
        pkg.put("licenseIds", licenseIds);
        pkg.put("releaseId", "69da0e106074ab7b3856df5fbc3e");
        pkg.put("description", "Sanitizes a html string by stripping all potentially dangerous tokens.");

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(
                post("/api/packages")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(pkg))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("_embedded.createdBy.email", Matchers.is("admin@sw360.org")))
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("name").description("The name of the package"),
                                fieldWithPath("version").description("The version of the package"),
                                fieldWithPath("packageType").description("The package type, possible values are: " + Arrays.asList(CycloneDxComponentType.values())),
                                fieldWithPath("purl").description("Package URL"),
                                fieldWithPath("vcs").description("VCS(Version Control System) is the URL of the source code"),
                                fieldWithPath("homepageUrl").description("URL of the package website"),
                                fieldWithPath("licenseIds").description("The associated licenses"),
                                fieldWithPath("releaseId").description("Id of the linked release"),
                                fieldWithPath("description").description("Description of the package")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the package"),
                                fieldWithPath("version").description("The version of the package"),
                                fieldWithPath("packageType").description("The package type, possible values are: " + Arrays.asList(CycloneDxComponentType.values())),
                                fieldWithPath("createdOn").description("The date of creation of the package"),
                                fieldWithPath("packageManager").description("The type of package manager"),
                                fieldWithPath("purl").description("Package URL"),
                                fieldWithPath("vcs").description("VCS(Version Control System) is the URL of the source code"),
                                fieldWithPath("homepageUrl").description("URL of the package website"),
                                fieldWithPath("licenseIds").description("The associated licenses"),
                                fieldWithPath("description").description("Description of the package"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:release").description("The release to which the package is linked"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this component")
                        )));
    }

    @Test
    public void should_document_update_package() throws Exception {
        Package updatePackage = new Package()
                                    .setHomepageUrl("https://angularJS.org")
                                    .setDescription("Updated Description");
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/packages/" + package1.getId())
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(updatePackage))
                .header("Authorization", "Bearer" + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("homepageUrl").description("URL of the package website"),
                                fieldWithPath("description").description("Description of the package")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the package"),
                                fieldWithPath("version").description("The version of the package"),
                                fieldWithPath("packageType").description("The package type, possible values are: " + Arrays.asList(CycloneDxComponentType.values())),
                                fieldWithPath("createdOn").description("The date of creation of the package"),
                                fieldWithPath("packageManager").description("The type of package manager"),
                                fieldWithPath("purl").description("Package URL"),
                                fieldWithPath("vcs").description("VCS(Version Control System) is the URL of the source code"),
                                fieldWithPath("homepageUrl").description("URL of the package website"),
                                fieldWithPath("licenseIds").description("The associated licenses"),
                                fieldWithPath("description").description("Description of the package"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                subsectionWithPath("_embedded.sw360:release").description("The release to which the package is linked"),
                                subsectionWithPath("_embedded.createdBy").description("The user who created this component"))
                        ));
    }

    @Test
    public void should_document_delete_package() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(delete("/api/packages/" + package1.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }
}