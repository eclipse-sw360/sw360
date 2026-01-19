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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.ByteBuffer;
import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;

@RunWith(SpringJUnit4ClassRunner.class)
public class VendorSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360VendorService vendorServiceMock;

    private Vendor vendor;
    private Vendor vendor3;

    @Before
    public void before() throws TException, ResourceClassNotFoundException {
        vendor = new Vendor();
        vendor.setId("876876776");
        vendor.setFullname("Google Inc.");
        vendor.setShortname("Google");
        vendor.setUrl("https://google.com");

        Vendor vendor2 = new Vendor();
        vendor2.setId("987567468");
        vendor2.setFullname("Pivotal Software, Inc.");
        vendor2.setShortname("Pivotal");
        vendor2.setUrl("https://pivotal.io/");

        vendor3 = new Vendor();
        vendor3.setId("987567468");
        vendor3.setFullname("AMazon Ltd");
        vendor3.setShortname("AMazon");
        vendor3.setUrl("https://AMazon.io/");

        List<Vendor> vendorList = new ArrayList<>();
        vendorList.add(vendor);
        vendorList.add(vendor2);

        Set<Release> releases = new HashSet<>();
        Release release1 = new Release();
        release1.setId("12345");
        release1.setName("Release_1");
        release1.setVersion("1.0.0");
        release1.setVendor(vendor);

        Release release2 = new Release();
        release2.setId("123456");
        release2.setName("Release_2");
        release2.setVersion("2.0.0");
        release2.setVendor(vendor);

        releases.add(release1);
        releases.add(release2);

        PaginationData pageData = new PaginationData();
        pageData.setAscending(true);
        pageData.setRowsPerPage(10);
        pageData.setDisplayStart(0);
        pageData.setSortColumnNumber(0);
        Map<PaginationData, List<Vendor>> paginatedVendors = Map.of(pageData, vendorList);

        given(this.vendorServiceMock.getAllReleaseList(eq(vendor.getId()))).willReturn(releases);
        given(this.vendorServiceMock.getVendors(any())).willReturn(paginatedVendors);
        given(this.vendorServiceMock.mergeVendors(eq(vendor.getId()),eq(vendor2.getId()), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vendorServiceMock.vendorUpdate(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vendorServiceMock.getVendorById(eq(vendor.getId()))).willReturn(vendor);
        given(this.vendorServiceMock.deleteVendorByid(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vendorServiceMock.exportExcel()).willReturn(ByteBuffer.allocate(10000));
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789").setUserGroup(UserGroup.ADMIN));

        when(this.vendorServiceMock.createVendor(any())).then(invocation ->
        new Vendor ("Apache", "Apache Software Foundation", "https://www.apache.org/").setId("987567468"));
    }

    @Test
    public void should_document_get_vendors() throws Exception {
        mockMvc.perform(get("/api/vendors")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .queryParam("page", "0")
                .queryParam("page_entries", "5")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of vendors"),
                                parameterWithName("page_entries").description("Amount of vendors per page")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:vendors.[]fullName").description("The full name of the vendor"),
                                subsectionWithPath("_embedded.sw360:vendors.[]shortName").description("The Short Name of the vendor"),
                                subsectionWithPath("_embedded.sw360:vendors.[]url").description("The Url of the vendor"),
                                subsectionWithPath("_embedded.sw360:vendors").description("An array of <<resources-vendors, Vendors resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of vendors per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing vendors"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_vendor() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendor.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-vendors,Vendors resource>>")
                        ),
                        responseFields(
                                subsectionWithPath("fullName").description("The full name of the vendor"),
                                subsectionWithPath("shortName").description("The short name of the vendor, optional"),
                                subsectionWithPath("url").description("The vendor's home page URL"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_vendor_releases() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendor.getId() + "/releases")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:releases.[]id").description("Id of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]name").description("The name of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]version").description("The version of the release"),
                                subsectionWithPath("_embedded.sw360:releases.[]_links").description("Self <<resources-index-links,Links>> to Release resource\""),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_delete_vendor() throws Exception {
        mockMvc.perform(delete("/api/vendors/" + vendor.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_create_vendor() throws Exception {
        Map<String, Object> vendor = new HashMap<>();
        vendor.put("fullName", "Apache Software Foundation");
        vendor.put("shortName", "Apache");
        vendor.put("url", "https://www.apache.org/");
        mockMvc.perform(post("/api/vendors")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(vendor))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-vendors,Vendors resource>>")
                        ),
                        requestFields(
                                fieldWithPath("fullName").description("The full name of the vendor"),
                                fieldWithPath("shortName").description("The short name of the vendor"),
                                fieldWithPath("url").description("The vendor's home page URL")
                        ),
                        responseFields(
                                subsectionWithPath("fullName").description("The full name of the vendor"),
                                subsectionWithPath("shortName").description("The short name of the vendor, optional"),
                                subsectionWithPath("url").description("The vendor's home page URL"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_vendor() throws Exception {
        Map<String, Object> updateVendor = new HashMap<>();
        updateVendor.put("fullName", "Amazon Ltd");
        updateVendor.put("shortName", "Amazon");
        updateVendor.put("url", "https://Amazon.io/");
        mockMvc.perform(patch("/api/vendors/" + vendor3.getId())
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updateVendor))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_export_vendor() throws Exception{
        mockMvc.perform(get("/api/vendors/exportVendorDetails")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }

    @Test
    public void should_document_merge_vendor() throws Exception{
        mockMvc.perform(patch("/api/vendors/mergeVendors")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(vendor))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .param("mergeTargetId", "87654321")
                        .param("mergeSourceId", "17653524")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("mergeSourceId").description("Id of a source vendor to merge"),
                                parameterWithName("mergeTargetId").description("Id of a target vendor to merge")
                        ),
                        requestFields(
                                fieldWithPath("fullName").description("The full name of the vendor"),
                                fieldWithPath("shortName").description("The short name of the vendor"),
                                fieldWithPath("url").description("The vendor's home page URL"),
                                fieldWithPath("type").description("The type of document")
                        )));
    }
}
