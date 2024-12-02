/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.rest.resourceserver.actuator.SW360ConfigActuator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigSpecTest extends TestRestDocsSpecBase{

    @MockBean
    private SW360ConfigActuator restConfigActuatorMock;

    Map<String, String> properties;

    {
        properties = new HashMap<>();
        properties.put("admin.private.project.access.enabled", "true");
        properties.put("clearing.teams", "org1,org2,org3");
        properties.put("rest.apitoken.read.validity.days", "90");
        properties.put("rest.write.access.usergroup", "ADMIN");
    }

    @Test
    public void should_document_get_config() throws Exception {
        given(this.restConfigActuatorMock.config()).willReturn(properties);

        mockMvc.perform(get("/api/config")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("['admin.private.project.access.enabled']").description("Sample boolean property."),
                                fieldWithPath("['clearing.teams']").description("Sample set property (separated by comma)."),
                                fieldWithPath("['rest.apitoken.read.validity.days']").description("Sample integer property."),
                                fieldWithPath("['rest.write.access.usergroup']").description("Sample string property.")
                        )
                ));
    }

    @Test
    public void should_document_get_single_config() throws Exception {
        given(this.restConfigActuatorMock.config("rest.apitoken.read.validity.days")).willReturn(properties.get("rest.apitoken.read.validity.days"));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/config/{key}", "rest.apitoken.read.validity.days")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andDo(this.documentationHandler.document(
                        pathParameters(parameterWithName("key").description("Property key"))
                ));
    }
}
