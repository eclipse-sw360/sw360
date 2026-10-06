/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CustomWelcomePageSpecTest extends TestRestDocsSpecBase {

    @Test
    public void should_document_disabled_custom_welcome_page() throws Exception {
        given(sw360ConfigurationsServiceMock.getSW360ConfigFromDb(ConfigFor.SW360_CONFIGURATION))
                .willReturn(Map.of(SW360ConfigKeys.CUSTOM_WELCOME_PAGE, "false"));

        mockMvc.perform(get("/api/customWelcomePage")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNoContent())
                .andDo(this.documentationHandler.document());
    }
}
