/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.databasesanitation.Sw360DatabaseSanitationService;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthority;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ibm.cloud.cloudant.v1.model.Attachment;

@RunWith(SpringJUnit4ClassRunner.class)
public class DatabaseSanitationSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    private Sw360DatabaseSanitationService sanitationService;

    private Component component,component1;
    private Release release,release1;
    private Project project, project1;
    private Attachment attachment,attachment1 ;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Before
    public void before() throws TException, IOException {
        Map<String, Map<String, List<String>>> responseMap = new HashMap<>();

        List<String> attachmentList = new ArrayList<>();
        attachment = new Attachment.Builder()
                .contentType("12345")
                .build();
        attachment1 = new Attachment.Builder()
                .contentType("23456")
                .build();
        attachmentList.add(attachment.contentType());
        attachmentList.add(attachment1.contentType());

        List<String> componentList = new ArrayList<>();
        component = new Component();
        component.setName("Angular");
        component.setId("23456");
        component.unsetType();
        component.unsetVisbility();
        component1 = new Component();
        component1.setId("12345");
        componentList.add(component.getId());
        componentList.add(component1.getId());

        List<String> releaseList = new ArrayList<>();
        release = new Release();
        release.setId("123456");
        release.setName("release");
        release.unsetType();

        release1 = new Release();
        release1.setId("223456");
        releaseList.add(release.getId());
        releaseList.add(release1.getId());

        List<String> projectList = new ArrayList<>();
        project = new Project();
        project.setName("Emerald Web");
        project.setId("12345");

        project1 = new Project();
        project1.setId("2324545");
        projectList.add(project1.getId());
        projectList.add(project.getId());

        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        Map<String, List<String>> componentResponse = new HashMap<>();
        componentResponse.put(component.getName(), componentList);

        Map<String, List<String>> releaseResponse = new HashMap<>();
        releaseResponse.put(release.getName(), releaseList);

        Map<String, List<String>> releaseSourcesResponse = new HashMap<>();
        releaseSourcesResponse.put("dummy_attachment", attachmentList);

        Map<String, List<String>> projectResponse = new HashMap<>();
        projectResponse.put(project.getName(), projectList);

        responseMap.put("duplicateReleases", releaseResponse);
        responseMap.put("duplicateReleaseSources", releaseSourcesResponse);
        responseMap.put("duplicateComponents", componentResponse);
        responseMap.put("duplicateProjects", projectResponse);
        given(this.sanitationService.duplicateIdentifiers(any())).willReturn(responseMap);

        when(sw360CustomUserDetailsService.loadUserByUsername("admin@sw360.org")).thenReturn(new org.springframework.security.core.userdetails.User("admin@sw360.org", encoder.encode("12345"), List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))));
    }

    @Test
    public void should_document_search_duplicate() throws Exception {
        mockMvc.perform(get("/api/databaseSanitation/searchDuplicate")
         .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
         .accept(MediaTypes.HAL_JSON))
         .andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }
}
