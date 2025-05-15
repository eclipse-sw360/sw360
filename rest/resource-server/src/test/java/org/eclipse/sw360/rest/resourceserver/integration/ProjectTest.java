/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360ProjectService projectServiceMock;

    @Before
    public void before() throws TException {
        Set<Project> projectList = new HashSet<>();

        // Create sample projects
        Project project1 = new Project();
        project1.setId("p001");
        project1.setName("Project Alpha");
        project1.setDescription("Project Alpha description");
        project1.setProjectType(ProjectType.CUSTOMER);
        project1.setBusinessUnit("Group A");
        project1.setVersion("1.0.0");
        project1.setState(ProjectState.ACTIVE);
        project1.setClearingState(ProjectClearingState.CLOSED);

        Project project2 = new Project();
        project2.setId("p002");
        project2.setName("Project Beta");
        project2.setDescription("Project Beta description");
        project2.setProjectType(ProjectType.INTERNAL);
        project2.setBusinessUnit("Group B");
        project2.setVersion("2.0.0");
        project2.setState(ProjectState.PHASE_OUT);
        project2.setClearingState(ProjectClearingState.IN_PROGRESS);

        projectList.add(project1);
        projectList.add(project2);

        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(projectList);
        given(this.projectServiceMock.getProjectsSummaryForUserWithoutPagination(any())).willReturn(projectList.stream().toList());

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
    }

    @Test
    public void should_get_all_projects() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "projects", 2);
    }

    @Test
    public void should_get_all_projects_paginated() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?page=0&page_entries=1",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }
}
