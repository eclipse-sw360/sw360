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
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReleaseSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockBean
    private Sw360LicenseService licenseServiceMock;

    private Release release;
    private Attachment attachment;
    Component component;

    private String releaseId = "3765276512";

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

        Map<String, Set<String>> externalIds = new HashMap<>();
        externalIds.put("mainline-id-component", new HashSet<>(Arrays.asList("1432", "4876")));

        component = new Component();
        component.setId("17653524");
        component.setName("Angular");
        component.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        component.setCreatedOn("2016-12-15");
        component.setCreatedBy("admin@sw360.org");
        component.setComponentType(ComponentType.OSS);
        component.setVendorNames(new HashSet<>(Collections.singletonList("Google")));
        component.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "john@sw360.org")));

        List<Release> releaseList = new ArrayList<>();
        release = new Release();
        release.setId(releaseId);
        release.setName("Angular");
        release.setCpeid("cpe:/a:Google:Angular:2.3.0:");
        release.setReleaseDate("2016-12-07");
        release.setVersion("2.3.0");
        release.setCreatedOn("2016-12-18");
        release.setCreatedBy("admin@sw360.org");
        release.setDownloadurl("http://www.google.com");
        release.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setComponentId(component.getId());
        release.setClearingState(ClearingState.APPROVED);
        release.setMainlineState(MainlineState.OPEN);
        release.setExternalIds(Collections.singletonMap("mainline-id-component", "1432"));
        release.setAdditionalData(Collections.singletonMap("Key", "Value"));
        release.setAttachments(attachmentList);
        release.setLanguages(new HashSet<>(Arrays.asList("C++", "Java")));
        release.setMainLicenseIds(new HashSet<>(Arrays.asList("GPL-2.0-or-later", "Apache-2.0")));
        release.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        release.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Java SE", ".NET")));
        releaseList.add(release);

        Release release2 = new Release();
        release2.setId("3765276512");
        release2.setName("Angular");
        release2.setCpeid("cpe:/a:Google:Angular:2.3.1:");
        release2.setReleaseDate("2016-12-15");
        release2.setVersion("2.3.1");
        release2.setCreatedOn("2016-12-18");
        release2.setCreatedBy("admin@sw360.org");
        release2.setDownloadurl("http://www.google.com");
        release2.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release2.setComponentId(component.getId());
        release2.setClearingState(ClearingState.APPROVED);
        release2.setMainlineState(MainlineState.MAINLINE);
        release2.setExternalIds(Collections.singletonMap("mainline-id-component", "4876"));
        release2.setLanguages(new HashSet<>(Arrays.asList("C++", "Java")));
        release2.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        release2.setSoftwarePlatforms(new HashSet<>(Arrays.asList("Java SE", ".NET")));
        releaseList.add(release2);

        given(this.releaseServiceMock.getReleasesForUser(anyObject())).willReturn(releaseList);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release.getId()), anyObject())).willReturn(release);
        given(this.releaseServiceMock.deleteRelease(eq(release.getId()), anyObject())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.searchByExternalIds(eq(externalIds), anyObject())).willReturn((new HashSet<>(releaseList)));
        given(this.releaseServiceMock.convertToEmbeddedWithExternalIds(eq(release))).willReturn(
                new Release("Angular", "2.3.0", component.getId())
                        .setId(releaseId)
                        .setExternalIds(Collections.singletonMap("mainline-id-component", "1432")));
        given(this.releaseServiceMock.convertToEmbeddedWithExternalIds(eq(release2))).willReturn(
                new Release("Angular", "2.3.1", component.getId())
                        .setId("3765276512")
                        .setExternalIds(Collections.singletonMap("mainline-id-component", "4876")));
        when(this.releaseServiceMock.createRelease(anyObject(), anyObject())).then(invocation ->
                new Release("Test Release", "1.0", component.getId())
                        .setId("1234567890")
                        .setCreatedOn(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

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
                        .setShortname("GPL-2.0-or-later")
                        .setId(UUID.randomUUID().toString()));
    }

    @Test
    public void should_document_get_releases() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:releases[]name").description("The name of the release, optional"),
                                fieldWithPath("_embedded.sw360:releases[]version").description("The version of the release"),
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
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
                                fieldWithPath("_embedded.sw360:releases[]name").description("The name of the release, optional"),
                                fieldWithPath("_embedded.sw360:releases[]version").description("The version of the release"),
                                fieldWithPath("_embedded.sw360:releases[]cpeId").description("The cpeId of the release, optional"),
                                fieldWithPath("_embedded.sw360:releases[]releaseDate").description("The releaseDate of the release, optional"),
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
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
                                fieldWithPath("cpeId").description("CpeId of the release"),
                                fieldWithPath("clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                                fieldWithPath("cpeId").description("The CPE id"),
                                fieldWithPath("releaseDate").description("The date of this release"),
                                fieldWithPath("createdOn").description("The creation date of the internal sw360 release"),
                                fieldWithPath("mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                                fieldWithPath("downloadurl").description("the download url of the release"),
                                fieldWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("languages").description("The language of the component"),
                                fieldWithPath("_embedded.sw360:licenses").description("An array of all main licenses with their fullName and link to their <<resources-license-get,License resource>>"),
                                fieldWithPath("operatingSystems").description("The OS on which the release operates"),
                                fieldWithPath("softwarePlatforms").description("The software platforms of the component"),
                                fieldWithPath("_embedded.sw360:moderators").description("An array of all release moderators with email and link to their <<resources-user-get,User resource>>"),
                                fieldWithPath("_embedded.sw360:attachments").description("An array of all release attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
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
    public void should_document_update_release() throws Exception {
        Release updateRelease = new Release();
        release.setName("Updated release");

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(patch("/api/releases/" + releaseId)
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(updateRelease))
                .header("Authorization", "Bearer" + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-release,Release resource>>"),
                                linkWithRel("sw360:component").description("The link to the corresponding component"),
                                linkWithRel("curies").description("The curies for documentation")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the release, optional"),
                                fieldWithPath("version").description("The version of the release"),
                                fieldWithPath("createdBy").description("Email of the release creator"),
                                fieldWithPath("cpeId").description("CpeId of the release"),
                                fieldWithPath("clearingState").description("The clearing of the release, possible values are " + Arrays.asList(ClearingState.values())),
                                fieldWithPath("cpeId").description("The CPE id"),
                                fieldWithPath("releaseDate").description("The date of this release"),
                                fieldWithPath("createdOn").description("The creation date of the internal sw360 release"),
                                fieldWithPath("mainlineState").description("the mainline state of the release, possible values are: " + Arrays.asList(MainlineState.values())),
                                fieldWithPath("downloadurl").description("the download url of the release"),
                                fieldWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("languages").description("The language of the component"),
                                fieldWithPath("_embedded.sw360:licenses").description("An array of all main licenses with their fullName and link to their <<resources-license-get,License resource>>"),
                                fieldWithPath("operatingSystems").description("The OS on which the release operates"),
                                fieldWithPath("softwarePlatforms").description("The software platforms of the component"),
                                fieldWithPath("_embedded.sw360:moderators").description("An array of all release moderators with email and link to their <<resources-user-get,User resource>>"),
                                fieldWithPath("_embedded.sw360:attachments").description("An array of all release attachments and link to their <<resources-attachment-get,Attachment resource>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )
                ));
    }

    @Test
    public void should_document_get_release_attachment_info() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release.getId() + "/attachments")
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
    public void should_document_get_release_attachment() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/" + release.getId() + "/attachments/" + attachment.getAttachmentContentId())
                .header("Authorization", "Bearer " + accessToken)
                .accept("application/*"))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_get_releases_by_externalIds() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/releases/searchByExternalIds?mainline-id-component=1432&mainline-id-component=4876")
                .contentType(MediaTypes.HAL_JSON)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("_embedded.sw360:releases").description("An array of <<resources-releases, Releases resources>>"),
                                fieldWithPath("_embedded.sw360:releases[]name").description("The name of the release, optional"),
                                fieldWithPath("_embedded.sw360:releases[]version").description("The version of the release"),
                                fieldWithPath("_embedded.sw360:releases[]externalIds").description("External Ids of the release"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_create_release() throws Exception {
        Map<String, String> release = new HashMap<>();
        release.put("name", "Test Release");
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
                                fieldWithPath("name").description("The name of the new release"),
                                fieldWithPath("version").description("The version of the new release"),
                                fieldWithPath("componentId").description("The componentId of the origin component")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the release, optional"),
                                fieldWithPath("version").description("The version of the release"),
                                fieldWithPath("createdOn").description("The creation date of the internal sw360 release"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_upload_attachment_to_release() throws Exception {
        testAttachmentUpload("/api/releases/", releaseId);
    }
}
