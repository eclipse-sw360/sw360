/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.configuration.SW360ConfigurationsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ConfigurationsSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private SW360ConfigurationsService sw360ConfigurationsService;

    @Before
    public void before() throws TException, IOException {
        Map<String, String> configsFromProperties = Map.of(
            "enable.flexible.project.release.relationship", "true",
            "svm.component.id", "svm_component_id"
        );

        Map<String, String> configsFromDb = Map.of(
            "spdx.document.enabled", "true",
            "sw360.tool.name", "SW360"
        );

        Map<String, String> allConfigs = new HashMap<>();
        allConfigs.putAll(configsFromProperties);
        allConfigs.putAll(configsFromDb);

        given(sw360ConfigurationsService.getSW360ConfigFromProperties()).willReturn(configsFromProperties);
        given(sw360ConfigurationsService.getSW360ConfigFromDb()).willReturn(configsFromDb);
        given(sw360ConfigurationsService.getSW360Configs()).willReturn(allConfigs);
        given(sw360ConfigurationsService.updateSW360Configs(any(), any())).willReturn(RequestStatus.SUCCESS);
    }

    @Test
    public void should_document_get_all_configurations() throws Exception {
        mockMvc.perform(get("/api/configurations")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_changeable_configurations() throws Exception {
        mockMvc.perform(get("/api/configurations")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("changeable", "true")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                            parameterWithName("changeable")
                                .description("Filter changeable (true) or not changeable (false) configuration. By default lists all")
                        )
                ));
    }

    @Test
    public void should_document_get_not_changeable_configurations() throws Exception {
        mockMvc.perform(get("/api/configurations")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .queryParam("changeable", "false")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        queryParameters(
                            parameterWithName("changeable")
                                .description("Filter changeable (true) or not changeable (false) configuration. By default lists all")
                        )
                ));
    }

    @Test
    public void should_document_update_changeable_configurations() throws Exception {
        Map<String, String> updatedConfigurations = Map.of("spdx.document.enabled", "false");
        mockMvc.perform(patch("/api/configurations")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(updatedConfigurations))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }
}
