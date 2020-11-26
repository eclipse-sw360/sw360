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
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.SW360ProjectClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360Visibility;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.ProjectSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360ProjectType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360ReleaseLinkObjects;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.block;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SW360ProjectClientAdapterAsyncImplTest {
    private static final String PROJECT_VERSION = "1.0-projectVersion";
    private static final String PROJECT_NAME = "projectName";
    private final String PROJECT_LAST_INDEX = "12345";

    private SW360ProjectClientAdapterAsync projectClientAdapter;

    private SW360ProjectClient projectClient;

    private SW360Project projectWithLink;
    private LinkObjects linkObjects;

    @Before
    public void setUp() {
        projectClient = mock(SW360ProjectClient.class);
        projectClientAdapter = new SW360ProjectClientAdapterAsyncImpl(projectClient);

        String projectHref = "url/" + PROJECT_LAST_INDEX;
        Self projectSelf = new Self().setHref(projectHref);
        linkObjects = new LinkObjects()
                .setSelf(projectSelf);

        projectWithLink = new SW360Project();
        projectWithLink.setName(PROJECT_NAME);
        projectWithLink.setVersion(PROJECT_VERSION);
        projectWithLink.setDescription(PROJECT_NAME + " " + PROJECT_VERSION);
        projectWithLink.setProjectType(SW360ProjectType.PRODUCT);
        projectWithLink.setVisibility(SW360Visibility.BUISNESSUNIT_AND_MODERATORS);
        projectWithLink.setLinks(linkObjects);
    }

    @Test
    public void testGetProjectByNameAndVersion() {
        ProjectSearchParams searchParams = ProjectSearchParams.builder()
                .withName(PROJECT_NAME)
                .build();
        when(projectClient.search(searchParams))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(projectWithLink)));

        Optional<SW360Project> projectIdByNameAndVersion =
                block(projectClientAdapter.getProjectByNameAndVersion(PROJECT_NAME, PROJECT_VERSION));

        assertThat(projectIdByNameAndVersion).isPresent();
        assertThat(projectIdByNameAndVersion).contains(projectWithLink);
    }

    @Test
    public void testGetProjectByNameAndVersionNotFound() {
        when(projectClient.search(any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        Optional<SW360Project> optProject =
                block(projectClientAdapter.getProjectByNameAndVersion(PROJECT_NAME, PROJECT_VERSION));
        assertThat(optProject).isEmpty();
    }

    @Test
    public void testSearch() {
        ProjectSearchParams searchParams = ProjectSearchParams.builder()
                .withTag("testTag")
                .withBusinessUnit("testUnit")
                .build();
        when(projectClient.search(searchParams))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(projectWithLink)));

        List<SW360Project> projects = block(projectClientAdapter.search(searchParams));
        assertThat(projects).containsOnly(projectWithLink);
    }

    @Test
    public void testCreateProject() {
        SW360Project projectCreated = new SW360Project();
        projectCreated.setName(PROJECT_NAME);
        projectCreated.setVersion(PROJECT_VERSION);
        projectCreated.setDescription("a newly created project");
        when(projectClient.createProject(projectWithLink))
                .thenReturn(CompletableFuture.completedFuture(projectCreated));

        SW360Project result = block(projectClientAdapter.createProject(projectWithLink));

        assertThat(result).isEqualTo(projectCreated);
    }

    @Test
    public void testCreateProjectInvalidName() throws InterruptedException {
        SW360Project newProject = new SW360Project();
        newProject.setVersion(PROJECT_VERSION);
        CompletableFuture<SW360Project> future = projectClientAdapter.createProject(newProject);

        try {
            future.get();
            fail("Invalid project not detected!");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(SW360ClientException.class);
            assertThat(e.getCause().getMessage()).contains("invalid project");
        }
    }

    @Test
    public void testUpdateProject() {
        SW360Project projectUpdated = new SW360Project();
        projectUpdated.setName(PROJECT_NAME);
        projectUpdated.setVersion(PROJECT_VERSION);
        projectUpdated.setDescription("project that was updated");
        when(projectClient.updateProject(projectWithLink))
                .thenReturn(CompletableFuture.completedFuture(projectUpdated));

        SW360Project result = block(projectClientAdapter.updateProject(projectWithLink));

        assertThat(result).isEqualTo(projectUpdated);
    }

    @Test
    public void testUpdateProjectInvalidNoVersion() throws InterruptedException {
        SW360Project project = new SW360Project();
        project.setName(PROJECT_NAME);
        CompletableFuture<SW360Project> future = projectClientAdapter.updateProject(project);

        try {
            future.get();
            fail("Invalid project not detected!");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(SW360ClientException.class);
            assertThat(e.getCause().getMessage()).contains("invalid project");
        }
    }

    @Test
    public void testAddSW360ReleasesToSW360Project() {
        SW360ReleaseLinkObjects releaseLinkObjects = new SW360ReleaseLinkObjects();
        releaseLinkObjects.setSelf(linkObjects.getSelf());
        SW360Release release = new SW360Release();
        release.setLinks(releaseLinkObjects);
        when(projectClient.addReleasesToProject(eq(PROJECT_LAST_INDEX), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Collection<SW360Release> releases = Collections.singletonList(release);

        block(projectClientAdapter.addSW360ReleasesToSW360Project(PROJECT_LAST_INDEX, releases));

        verify(projectClient, atLeastOnce()).addReleasesToProject(eq(PROJECT_LAST_INDEX), any());
    }

    private void checkGetLinkedReleases(boolean transitive) {
        SW360SparseRelease rel1 = new SW360SparseRelease();
        rel1.setName("rel1");
        rel1.setVersion("1.0");
        SW360SparseRelease rel2 = new SW360SparseRelease();
        rel2.setName("rel2");
        rel2.setVersion("2.0");
        List<SW360SparseRelease> releases = Arrays.asList(rel1, rel2);
        when(projectClient.getLinkedReleases(PROJECT_LAST_INDEX, transitive))
                .thenReturn(CompletableFuture.completedFuture(releases));

        List<SW360SparseRelease> result =
                block(projectClientAdapter.getLinkedReleases(PROJECT_LAST_INDEX, transitive));
        assertThat(result).isEqualTo(releases);
    }

    @Test
    public void testGetLinkedReleasesNonTransitive() {
        checkGetLinkedReleases(false);
    }

    @Test
    public void testGetLinkedReleasesTransitive() {
        checkGetLinkedReleases(true);
    }
}
