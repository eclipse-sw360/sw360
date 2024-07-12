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
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.formParameters;

@RunWith(SpringJUnit4ClassRunner.class)
public class VendorSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360VendorService vendorServiceMock;

    private Vendor vendor;

    @Before
    public void before() throws TException{
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

        List<Vendor> vendorList = new ArrayList<>();
        vendorList.add(vendor);
        vendorList.add(vendor2);

        given(this.vendorServiceMock.getVendors()).willReturn(vendorList);
        given(this.vendorServiceMock.getVendorById(eq(vendor.getId()))).willReturn(vendor);
        given(this.vendorServiceMock.exportExcel()).willReturn(ByteBuffer.allocate(10000));

        when(this.vendorServiceMock.createVendor(any())).then(invocation ->
        new Vendor ("Apache", "Apache Software Foundation", "https://www.apache.org/").setId("987567468"));
    }

    @Test
    public void should_document_get_vendors() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/vendors")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        formParameters(
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
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/vendors/" + vendor.getId())
                .header("Authorization", "Bearer " + accessToken)
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
    public void should_document_create_vendor() throws Exception {
        Map<String, Object> vendor = new HashMap<>();
        vendor.put("fullName", "Apache Software Foundation");
        vendor.put("shortName", "Apache");
        vendor.put("url", "https://www.apache.org/");
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(post("/api/vendors/")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(vendor))
                .header("Authorization", "Bearer " + accessToken))
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
    public void should_document_get_export_vendor() throws Exception{
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/vendors/exportVendorDetails")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }
}
