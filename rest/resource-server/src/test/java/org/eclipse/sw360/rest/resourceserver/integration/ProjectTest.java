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
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectTest extends TestIntegrationBase {
    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360ProjectService projectServiceMock;

    private List<Project> projectList;
    private User user;

    @Before
    public void before() throws TException {
        projectList = new ArrayList<>();

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

        user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.projectServiceMock.getProjectsSummaryForUserWithoutPagination(user)).willReturn(projectList);
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
    public void should_create_duplicate_project_with_dependency_network() throws IOException {
        HttpHeaders headers = getHeaders(port);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Duplicate Project");
        requestBody.put("version", "1.0.0");
        requestBody.put("dependencyNetwork", new ArrayList<>());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/network/duplicate/p001",
                        HttpMethod.POST,
                        new HttpEntity<>(requestBody, headers),
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_link_packages_to_project() throws IOException {
        HttpHeaders headers = getHeaders(port);
        Set<String> packages = new HashSet<>(Arrays.asList("pkg1", "pkg2"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/p001/link/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(packages, headers),
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_unlink_packages_from_project() throws IOException {
        HttpHeaders headers = getHeaders(port);
        Set<String> packages = new HashSet<>(Arrays.asList("pkg1", "pkg2"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/p001/unlink/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(packages, headers),
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_get_project_releases() throws IOException {
        HttpHeaders headers = getHeaders(port);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/p001/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_vulnerabilities_of_project() throws IOException {
        HttpHeaders headers = getHeaders(port);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/p001/vulnerabilities",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
