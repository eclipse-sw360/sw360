/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.cvesearch.Sw360CveSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CveSearchSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360CveSearchService cveSearchServiceMock;

    private final VulnerabilityUpdateStatus updateStatus = new VulnerabilityUpdateStatus()
            .setRequestStatus(RequestStatus.SUCCESS);

    @BeforeEach
    public void before() throws Exception {
        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(sw360User);

        given(this.cveSearchServiceMock.updateForRelease(anyString())).willReturn(updateStatus);
        given(this.cveSearchServiceMock.updateForComponent(anyString())).willReturn(updateStatus);
        given(this.cveSearchServiceMock.updateForProject(anyString())).willReturn(updateStatus);
        given(this.cveSearchServiceMock.fullUpdate()).willReturn(updateStatus);
        given(this.cveSearchServiceMock.update()).willReturn(RequestStatus.SUCCESS);
        given(this.cveSearchServiceMock.findCpes(eq("apache"), eq("httpd"), eq("2.4.1")))
                .willReturn(Set.of("cpe:2.3:a:apache:httpd:2.4.1:*:*:*:*:*:*:*"));
    }

    @Test
    public void should_document_update_for_release() throws Exception {
        mockMvc.perform(post("/api/cvesearch/releases/release123")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_update_for_component() throws Exception {
        mockMvc.perform(post("/api/cvesearch/components/component123")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_update_for_project() throws Exception {
        mockMvc.perform(post("/api/cvesearch/projects/project123")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_full_update() throws Exception {
        mockMvc.perform(post("/api/cvesearch/full-update")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_update() throws Exception {
        mockMvc.perform(post("/api/cvesearch/update")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_find_cpes() throws Exception {
        mockMvc.perform(post("/api/cvesearch/cpes")
                .param("vendor", "apache")
                .param("product", "httpd")
                .param("version", "2.4.1")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }
}
