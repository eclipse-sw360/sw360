/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.clients.rest.resource.projects.*;
import org.eclipse.sw360.http.utils.FailedRequestException;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.eclipse.sw360.clients.adapter.SW360ComponentClientAdapterAsync;
import org.eclipse.sw360.clients.adapter.SW360ConnectionFactory;
import org.eclipse.sw360.clients.adapter.SW360ProjectClientAdapterAsync;
import org.eclipse.sw360.clients.adapter.SW360ReleaseClientAdapterAsync;
import org.eclipse.sw360.clients.rest.resource.components.SW360Component;
import org.eclipse.sw360.clients.rest.resource.licenses.SW360License;
import org.eclipse.sw360.clients.rest.resource.projects.ProjectSearchParams;
import org.eclipse.sw360.clients.rest.resource.projects.SW360Project;
import org.eclipse.sw360.clients.rest.resource.projects.SW360ProjectType;
import org.eclipse.sw360.clients.rest.resource.projects.SW360ProjectDTO;
import org.eclipse.sw360.clients.rest.resource.projects.SW360ReleaseLinkJSON;
import org.eclipse.sw360.clients.rest.resource.releases.SW360Release;
import org.eclipse.sw360.clients.rest.resource.releases.SW360SparseRelease;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SW360ProjectClientIT extends AbstractMockServerTest {
    /**
     * The names of the projects defined in the test data.
     */
    private static final String[] PROJECT_NAMES = {
            "Project_Foo", "Project_Bar", "Project_other", "Project_test"
    };

    private SW360ProjectClient projectClient;
    private SW360ReleaseClient releaseClient;
    private SW360ComponentClient componentClient;

    @Before
    public void setUp() {
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ConnectionFactory scf = new SW360ConnectionFactory();
            SW360ProjectClientAdapterAsync projectClientAsync = scf.newConnection(createClientConfig())
                    .getProjectAdapterAsync();
            projectClient = projectClientAsync.getProjectClient();
            SW360ReleaseClientAdapterAsync releaseClientAsync = scf.newConnection(createClientConfig())
                    .getReleaseAdapterAsync();
            SW360ComponentClientAdapterAsync componentClientAsync = scf.newConnection(createClientConfig())
                    .getComponentAdapterAsync();
            componentClient = componentClientAsync.getComponentClient();
            releaseClient = releaseClientAsync.getReleaseClient();
        } else {
            projectClient = new SW360ProjectClient(createClientConfig(), createMockTokenProvider());
            prepareAccessTokens(projectClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
        }
    }

    /**
     * Checks whether the expected test projects have been retrieved.
     *
     * @param projects the projects to be checked
     */
    private static void checkTestProjects(Collection<? extends SW360ProjectDTO> projects) {
        List<String> actualProjectNames = projects.stream()
                .map(SW360ProjectDTO::getName)
                .collect(Collectors.toList());
        assertThat(actualProjectNames).containsExactlyInAnyOrder(PROJECT_NAMES);
        assertHasLinks(projects);
    }

    @Test
    public void testSearchByCriteriaDefined() throws IOException {
        final String projectName = "example-project-IT";
        final SW360ProjectType projectType = SW360ProjectType.SERVICE;
        final String businessUnit = "DEPARTMENT";
        
        SW360ProjectDTO projectIT = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        projectIT.setProjectType(projectType);
        projectIT.setBusinessUnit(businessUnit);
        
        String projId = createProject(projectIT);
        wireMockRule.stubFor(get(urlPathEqualTo("/projects"))
                .withQueryParam("name", equalTo(projectName))
                .withQueryParam("type", equalTo(projectType.name()))
                .withQueryParam("group", equalTo(businessUnit))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_projects.json")));
        ProjectSearchParams searchParams = ProjectSearchParams.builder()
                .withName(projectName)
                .withBusinessUnit(businessUnit)
                .withType(projectType).build();

        List<SW360ProjectDTO> projects = waitFor(projectClient.search(searchParams));
        deleteProject(projId);
        if (RUN_REST_INTEGRATION_TEST) {
            assertTrue(projects.get(0).getName().equals(projectIT.getName()));
            assertTrue(projects.get(0).getVersion().equals(projectIT.getVersion()));
            assertTrue(projects.get(0).getId().equals(projId));
        } else {
            checkTestProjects(projects);
        }
    }

    @Test
    public void testSearchByCriteriaUndefined() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/projects"))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK).withBodyFile("all_projects.json")));
        SW360ProjectDTO projectIT1 = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        projectIT1.setName("Project_Foo");
        String projId1 = createProject(projectIT1);
        SW360ProjectDTO projectIT2 = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        projectIT2.setName("Project_Bar");
        String projId2 = createProject(projectIT2);
        SW360ProjectDTO projectIT3 = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        projectIT3.setName("Project_other");
        String projId3 = createProject(projectIT3);
        SW360ProjectDTO projectIT4 = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        projectIT4.setName("Project_test");
        String projId4 = createProject(projectIT4);
        List<SW360ProjectDTO> projects = waitFor(projectClient.search(ProjectSearchParams.ALL_PROJECTS));
        
        deleteProject(projId1);
        deleteProject(projId2);
        deleteProject(projId3);
        deleteProject(projId4);
        
        checkTestProjects(projects);
        if (!RUN_REST_INTEGRATION_TEST) {
            assertThat(getAllServeEvents().get(0).getRequest().getQueryParams()).isEmpty();
        }
    }

    @Test
    public void testSearchEmptyResult() throws IOException {
        final String name = "zoo";
        wireMockRule.stubFor(get(urlPathEqualTo("/projects"))
                .withQueryParam("name", equalTo(name))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));
        ProjectSearchParams params = ProjectSearchParams.builder()
                .withName(name)
                .build();

        List<SW360ProjectDTO> projects = waitFor(projectClient.search(params));
        assertThat(projects).isEmpty();
    }

    @Test
    public void testSearchNoContent() throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/projects"))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));
        ProjectSearchParams params = ProjectSearchParams.builder()
                .withName("")
                .build();

        List<SW360ProjectDTO> projects = waitFor(projectClient.search(params));
        assertThat(projects).isEmpty();
        if(!RUN_REST_INTEGRATION_TEST) {
            assertThat(getAllServeEvents().get(0).getRequest().getQueryParams()).isEmpty();
        }
    }

    @Test
    public void testSearchError() {
        wireMockRule.stubFor(
                get(urlPathEqualTo("/projects")).willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        CompletableFuture<List<SW360ProjectDTO>> projectFuture = null;
        if (RUN_REST_INTEGRATION_TEST) {
            projectFuture = CompletableFuture.supplyAsync(() -> {
                throw new CompletionException(new FailedRequestException(SW360ProjectClient.TAG_SEARCH_PROJECTS,
                        HttpConstants.STATUS_ERR_BAD_REQUEST));
            });
        } else {
            projectFuture = projectClient.search(ProjectSearchParams.ALL_PROJECTS);
        }

        FailedRequestException exception = expectFailedRequest(projectFuture, HttpConstants.STATUS_ERR_BAD_REQUEST);

        assertThat(exception.getTag()).isEqualTo(SW360ProjectClient.TAG_SEARCH_PROJECTS);
    }

    @Test
    public void testCreateProject() throws IOException {
        SW360ProjectDTO project = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        String projectJson = toJson(project);
        wireMockRule.stubFor(post(urlPathEqualTo("/projects")).withRequestBody(equalToJson(projectJson))
                .willReturn(aJsonResponse(HttpConstants.STATUS_CREATED).withBody(projectJson)));

        SW360Project createdProject = waitFor(projectClient.createProject(project));
        deleteProject(createdProject.getId());
        assertTrue(createdProject.getName().equals(project.getName()));
        assertTrue(createdProject.getVersion().equals(project.getVersion()));
    }

    @Test
    public void testUpdateProject() throws IOException {
        SW360ProjectDTO project = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        SW360Project updProject = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360Project.class);
        if (RUN_REST_INTEGRATION_TEST) {
            updProject = waitFor(projectClient.createProject(project));
            assertTrue(updProject.getName().equals(project.getName()));
            assertTrue(updProject.getVersion().equals(project.getVersion()));
        }
        updProject.setVersion("updatedVersion");
        SW360ProjectDTO updatedProject = objectMapper.convertValue(updProject, SW360ProjectDTO.class);
        String projectJson = toJson(project);
        String updProjectJson = toJson(updProject);
        wireMockRule.stubFor(patch(urlPathEqualTo("/projects/" + project.getId()))
                .withRequestBody(equalTo(updProjectJson))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBody(updProjectJson)));

        SW360Project result = waitFor(projectClient.updateProject(updatedProject));
        deleteProject(updProject.getId());
        assertTrue(updatedProject.getName().equals(result.getName()));
        assertTrue(updatedProject.getVersion().equals(result.getVersion()));
    }

    @Test
    public void testAddReleasesToProject() throws IOException {
        SW360ProjectDTO projectIT = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        String projId = createProject(projectIT);
        List<String> releases = Arrays.asList("release1", "releaseMe", "releaseParty");
        SW360Release release = null;
        SW360Release release2 = null;
        if (RUN_REST_INTEGRATION_TEST) {
            SW360Component component = SW360ReleaseClientIT.componentFromJsonForIntegrationTest();
            component.setName("TestProject");
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            release = waitFor(releaseClient.createRelease(sw360Release));
            assertNotNull(release);
            SW360Release sw360Release2 = new SW360Release();
            sw360Release2.setComponentId(createdComponent.getId());
            sw360Release2.setVersion("1.2");
            release2 = waitFor(releaseClient.createRelease(sw360Release2));
            assertNotNull(release2);
            releases = Arrays.asList(release.getId(), release2.getId());
        }

        String urlPath = "/projects/" + projId + "/releases";
        wireMockRule.stubFor(post(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_ACCEPTED)));

        waitFor(projectClient.addReleasesToProject(projId, releases));
        if (RUN_REST_INTEGRATION_TEST) {
            List<SW360SparseRelease> releasesLinked = waitFor(projectClient.getLinkedReleases(projId, false));
            assertThat(releasesLinked).hasSize(2);
        }
        if (!RUN_REST_INTEGRATION_TEST) {
            wireMockRule.verify(postRequestedFor(urlPathEqualTo(urlPath)).withRequestBody(equalTo(toJson(releases))));
        }
        deleteProject(projId);
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ReleaseClientIT.cleanupRelease(release, releaseClient);
            SW360ReleaseClientIT.cleanupRelease(release2, releaseClient);
            SW360ReleaseClientIT.cleanupComponent(componentClient);
        }
    }

    @Test
    public void testAddReleasesToProjectError() throws IOException {
        wireMockRule.stubFor(post(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_ERR_BAD_REQUEST)));
        SW360ProjectDTO projectIT = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        String projId = createProject(projectIT);
        FailedRequestException exception =
                expectFailedRequest(projectClient.addReleasesToProject(projId,
                        Arrays.asList("foo", "bar")), HttpConstants.STATUS_ERR_BAD_REQUEST);
        deleteProject(projId);
        assertThat(exception.getTag()).isEqualTo(SW360ProjectClient.TAG_ADD_RELEASES_TO_PROJECT);
    }

    /**
     * Helper method for testing whether releases linked to a project can be
     * queried.
     *
     * @param transitive flag whether transitive releases should be fetched
     */
    private void checkLinkedReleases(boolean transitive) throws IOException {
        SW360ProjectDTO projectIT = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        List<String> releases = Arrays.asList("release1", "releaseMe", "releaseParty");
        SW360Release release = null;
        SW360Release release2 = null;
        if (RUN_REST_INTEGRATION_TEST) {
            SW360Component component = SW360ReleaseClientIT.componentFromJsonForIntegrationTest();
            component.setName("TestProject");
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            release = waitFor(releaseClient.createRelease(sw360Release));
            assertNotNull(release);
            SW360Release sw360Release2 = new SW360Release();
            sw360Release2.setComponentId(createdComponent.getId());
            sw360Release2.setVersion("1.2");
            release2 = waitFor(releaseClient.createRelease(sw360Release2));
            assertNotNull(release2);
            releases = Arrays.asList(release.getId(), release2.getId());
        }
        List<SW360ReleaseLinkJSON> dependencyNetwork= new ArrayList<>();
        SW360ReleaseLinkJSON releaseLinkJSON1 = new SW360ReleaseLinkJSON(releases.get(0), Collections.emptyList(), "CONTAINED", "MAINLINE", "","","");
        SW360ReleaseLinkJSON releaseLinkJSON2 = new SW360ReleaseLinkJSON(releases.get(1), Collections.emptyList(), "CONTAINED", "MAINLINE", "","","");
        dependencyNetwork.add(releaseLinkJSON1);
        dependencyNetwork.add(releaseLinkJSON2);
        projectIT.setDependencyNetwork(dependencyNetwork);
        String projId = createProject(projectIT);

        if (RUN_REST_INTEGRATION_TEST) {
            List<SW360SparseRelease> releasesLinked = waitFor(projectClient.getLinkedReleases(projId, false));
            assertThat(releasesLinked).hasSize(2);
        }

        String urlPathGet = "/projects/" + projId + "/releases";
        wireMockRule.stubFor(get(urlPathEqualTo(urlPathGet))
                .withQueryParam("transitive", equalTo(String.valueOf(transitive)))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_releases.json")));

        List<SW360SparseRelease> releasesFetched = waitFor(projectClient.getLinkedReleases(projId, transitive));
        deleteProject(projId);
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ReleaseClientIT.cleanupRelease(release, releaseClient);
            SW360ReleaseClientIT.cleanupRelease(release2, releaseClient);
            SW360ReleaseClientIT.cleanupComponent(componentClient);
        }
        assertThat(releasesFetched).hasSize(RUN_REST_INTEGRATION_TEST ? 2 : 6);
        assertHasLinks(releasesFetched);
    }

    @Test
    public void testGetLinkedReleases() throws IOException {
        checkLinkedReleases(false);
    }

    @Test
    public void testGetLinkedReleasesTransitive() throws IOException {
        checkLinkedReleases(true);
    }

    @Test
    public void testGetLinkedReleasesError() {
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_ERR_NOT_FOUND)));

        FailedRequestException exception =
                expectFailedRequest(projectClient.getLinkedReleases("projectID", false),
                        HttpConstants.STATUS_ERR_NOT_FOUND);
        assertThat(exception.getTag()).isEqualTo(SW360ProjectClient.TAG_GET_LINKED_RELEASES);
    }

    @Test
    public void testGetLinkedReleasesNoContent() throws IOException {
        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(HttpConstants.STATUS_NO_CONTENT)));
        SW360ProjectDTO projectIT = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        String projId = createProject(projectIT);
        List<SW360SparseRelease> releases = waitFor(projectClient.getLinkedReleases(projId, false));
        deleteProject(projId);
        assertThat(releases).isEmpty();
    }

    @Test
    public void checkDirectDependenciesOfRelease() throws IOException {
        SW360ProjectDTO projectIT = readTestJsonFile(resolveTestFileURL("projectIT.json"), SW360ProjectDTO.class);
        List<String> releases = Arrays.asList("release1", "releaseMe", "releaseParty");
        SW360Release release = null;
        SW360Release release2 = null;
        if (RUN_REST_INTEGRATION_TEST) {
            SW360Component component = SW360ReleaseClientIT.componentFromJsonForIntegrationTest();
            component.setName("TestProject");
            SW360Component createdComponent = waitFor(componentClient.createComponent(component));
            SW360Release sw360Release = new SW360Release();
            sw360Release.setComponentId(createdComponent.getId());
            sw360Release.setVersion("1.1");
            release = waitFor(releaseClient.createRelease(sw360Release));
            assertNotNull(release);
            SW360Release sw360Release2 = new SW360Release();
            sw360Release2.setComponentId(createdComponent.getId());
            sw360Release2.setVersion("1.2");
            release2 = waitFor(releaseClient.createRelease(sw360Release2));
            assertNotNull(release2);
            releases = Arrays.asList(release.getId(), release2.getId());
        }
        List<SW360ReleaseLinkJSON> dependencyNetwork= new ArrayList<>();
        SW360ReleaseLinkJSON releaseLinkJSON1 = new SW360ReleaseLinkJSON(releases.get(0), Collections.emptyList(), "CONTAINED", "MAINLINE", "","","");
        SW360ReleaseLinkJSON releaseLinkJSON2 = new SW360ReleaseLinkJSON(releases.get(1), Collections.emptyList(), "CONTAINED", "MAINLINE", "","","");
        releaseLinkJSON1.setReleaseLink(Collections.singletonList(releaseLinkJSON2));
        dependencyNetwork.add(releaseLinkJSON1);
        projectIT.setDependencyNetwork(dependencyNetwork);
        String projId = createProject(projectIT);

        if (RUN_REST_INTEGRATION_TEST) {
            List<SW360SparseRelease> releasesLinked = waitFor(projectClient.getDirectDependenciesOfRelease(projId, releases.get(0)));
            assertThat(releasesLinked).hasSize(1);
        }

        String urlPathGet = "/projects/network/" + projId + "/releases/" + releases.get(0);
        wireMockRule.stubFor(get(urlPathEqualTo(urlPathGet))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile("all_direct_dependencies_of_release.json")));

        List<SW360SparseRelease> releasesFetched = waitFor(projectClient.getDirectDependenciesOfRelease(projId, releases.get(0)));
        deleteProject(projId);
        if (RUN_REST_INTEGRATION_TEST) {
            SW360ReleaseClientIT.cleanupRelease(release, releaseClient);
            SW360ReleaseClientIT.cleanupRelease(release2, releaseClient);
            SW360ReleaseClientIT.cleanupComponent(componentClient);
        }
        assertThat(releasesFetched).hasSize(RUN_REST_INTEGRATION_TEST ? 1 : 6);
        assertHasLinks(releasesFetched);
    }
    private void deleteProject(String projectId) throws IOException {
        if (RUN_REST_INTEGRATION_TEST) {
            Integer statusCode = waitFor(projectClient.deleteProject(projectId));
            assertThat(statusCode).isEqualTo(200);
        }
    }

    private String createProject(SW360ProjectDTO project) throws IOException {
        if (RUN_REST_INTEGRATION_TEST) {
            SW360Project result = waitFor(projectClient.createProject(project));
            assertTrue(result.getName().equals(project.getName()));
            assertTrue(result.getVersion().equals(project.getVersion()));
            return result.getId();
        }

        return null;
    }
}
