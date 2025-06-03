/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Obligation.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.obligation.Sw360ObligationService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

@RunWith(SpringJUnit4ClassRunner.class)
public class ObligationSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360ObligationService obligationServiceMock;

    private Obligation obligation;

    @Before
    public void before() throws TException {
        obligation  = new Obligation();
        obligation.setId("888888888");
        obligation.setText("License Obligation");
        obligation.setTitle("Obligation 1");
        obligation.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);
        obligation.setObligationType(ObligationType.PERMISSION);

        Obligation obligation2 = new Obligation();
        obligation2.setId("99999999");
        obligation2.setText("Organisation Obligation");
        obligation2.setTitle("Obligation 2");
        obligation2.setObligationLevel(ObligationLevel.ORGANISATION_OBLIGATION);
        obligation2.setObligationType(ObligationType.RISK);

        List<Obligation> obligationList = new ArrayList<>();
        obligationList.add(obligation);
        obligationList.add(obligation2);

        given(this.obligationServiceMock.getObligations()).willReturn(obligationList);
        given(this.obligationServiceMock.getObligationById(eq(obligation.getId()))).willReturn(obligation);
        given(this.obligationServiceMock.deleteObligation(eq(obligation.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789").setUserGroup(UserGroup.ADMIN));

        when(this.obligationServiceMock.createObligation(any(), any())).then(invocation ->
        new Obligation("This is the text of my Test Obligation")
                .setId("1234567890")
                .setTitle("Test Obligation")
                .setObligationLevel(ObligationLevel.LICENSE_OBLIGATION)
                .setObligationType(ObligationType.PERMISSION));
    }

    @Test
    public void should_document_get_obligations() throws Exception {
        mockMvc.perform(get("/api/obligations")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .param("page", "0")
                .param("page_entries", "5")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                                parameterWithName("page").description("Page of obligations"),
                                parameterWithName("page_entries").description("Amount of obligations per page")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:obligations[]title").description("The title of the obligation"),
                                subsectionWithPath("_embedded.sw360:obligations[]obligationType").description("The obligationType of the obligation"),
                                subsectionWithPath("_embedded.sw360:obligations").description("An array of <<resources-obligations, Obligations resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of obligations per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing obligations"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_get_obligation() throws Exception {
        mockMvc.perform(get("/api/obligations/" + obligation.getId())
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-obligations, Obligations resource>>")
                        ),
                        responseFields(
                                fieldWithPath("title").description("The title of the obligation"),
                                fieldWithPath("text").description("The text of the obligation"),
                                fieldWithPath("obligationLevel").description("The level of the obligation: [ORGANISATION_OBLIGATION, PROJECT_OBLIGATION, COMPONENT_OBLIGATION, LICENSE_OBLIGATION]"),
                                fieldWithPath("obligationType").description("The type of the obligation: [PERMISSION, RISK, EXCEPTION, RESTRICTION, OBLIGATION]"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_create_obligation() throws Exception {
        Map<String, String> obligation = new HashMap<>();
        obligation.put("title", "Test Obligation");
        obligation.put("text", "This is the text of my Test Obligation");
        obligation.put("obligationLevel", ObligationLevel.LICENSE_OBLIGATION.toString());
        obligation.put("obligationType", ObligationType.PERMISSION.toString());

        this.mockMvc.perform(post("/api/obligations")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(obligation))
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("text", Matchers.is("This is the text of my Test Obligation")))
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("title").description("The title of the obligation"),
                                fieldWithPath("text").description("The text of the obligation"),
                                fieldWithPath("obligationLevel").description("The level of the obligation: [COMPONENT_OBLIGATION, ORGANISATION_OBLIGATION, PROJECT_OBLIGATION, LICENSE_OBLIGATION]"),
                                fieldWithPath("obligationType").description("The type of the obligation: [RESTRICTION, OBLIGATION, PERMISSION, EXCEPTION, RISK]")
                        ),
                        responseFields(
                                fieldWithPath("title").description("The title of the obligation"),
                                fieldWithPath("text").description("The text of the obligation"),
                                fieldWithPath("obligationLevel").description("The level of the obligation: [COMPONENT_OBLIGATION, ORGANISATION_OBLIGATION, PROJECT_OBLIGATION, LICENSE_OBLIGATION]"),
                                fieldWithPath("obligationType").description("The type of the obligation: [RESTRICTION, OBLIGATION, PERMISSION, EXCEPTION, RISK]"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_delete_obligations() throws Exception {
        mockMvc.perform(delete("/api/obligations/" + obligation.getId() + ",1234,5678")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isMultiStatus())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("[].resourceId").description("id of the deleted resource"),
                                fieldWithPath("[].status").description("status of the delete operation")
                        )));
    }
}
