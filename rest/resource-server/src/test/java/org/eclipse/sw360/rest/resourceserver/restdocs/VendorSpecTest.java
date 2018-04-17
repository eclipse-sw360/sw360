/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void before() {
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
    }

    @Test
    public void should_document_get_vendors() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/vendors")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:vendors[]fullName").description("The full name of the vendor"),
                                fieldWithPath("_embedded.sw360:vendors").description("An array of <<resources-vendors, Vendors resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
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
                                fieldWithPath("fullName").description("The full name of the vendor"),
                                fieldWithPath("shortName").description("The short name of the vendor, optional"),
                                fieldWithPath("url").description("The vendor's home page URL"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
