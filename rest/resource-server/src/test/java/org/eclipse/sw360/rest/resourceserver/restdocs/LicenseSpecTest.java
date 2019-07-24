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
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class LicenseSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360LicenseService licenseServiceMock;

    private License license;

    @Before
    public void before() throws TException {
        license = new License();
        license.setId("Apache-2.0");
        license.setFullname("Apache License 2.0");
        license.setShortname("Apache 2.0");
        license.setText("placeholder for the Apache 2.0 license text");
        Map<String,String> externalIds = new HashMap<>();
        externalIds.put("SPDX", "Apache-2.0");
        externalIds.put("Trove", "License :: OSI Approved :: Apache Software License");
        license.setExternalIds(externalIds);
        license.setAdditionalData(Collections.singletonMap("Key", "Value"));

        License license2 = new License();
        license2.setId("MIT");
        license2.setFullname("The MIT License (MIT)");
        license2.setShortname("MIT");
        license2.setText("placeholder for the MIT license text");

        List<License> licenseList = new ArrayList<>();
        licenseList.add(license);
        licenseList.add(license2);

        given(this.licenseServiceMock.getLicenses()).willReturn(licenseList);
        given(this.licenseServiceMock.getLicenseById(eq(license.getId()))).willReturn(license);
    }

    @Test
    public void should_document_get_licenses() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/licenses")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:licenses").description("An array of <<resources-licenses, Licenses resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_license() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/licenses/" + license.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-licenses,Licenses resource>>")
                        ),
                        responseFields(
                                fieldWithPath("fullName").description("The full name of the license"),
                                fieldWithPath("shortName").description("The short name of the license, optional"),
                                fieldWithPath("externalIds").description("When releases are imported from other tools, the external ids can be stored here"),
                                fieldWithPath("additionalData").description("A place to store additional data used by external tools"),
                                fieldWithPath("text").description("The license's original text"),
                                fieldWithPath("checked").description("The information, whether the license is already checked, optional and defaults to true"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
