/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.ProjectSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter implementation for the SW360 projects endpoint.
 */
class SW360ProjectClientAdapterAsyncImpl implements SW360ProjectClientAdapterAsync {
    private final SW360ProjectClient projectClient;

    public SW360ProjectClientAdapterAsyncImpl(SW360ProjectClient client) {
        projectClient = client;
    }

    @Override
    public SW360ProjectClient getProjectClient() {
        return projectClient;
    }

    @Override
    public CompletableFuture<Optional<SW360Project>> getProjectByNameAndVersion(String projectName, String projectVersion) {
        ProjectSearchParams nameSearchParams = ProjectSearchParams.builder()
                .withName(projectName)
                .build();
        return getProjectClient().search(nameSearchParams)
                .thenApply(projects -> projects.stream()
                        .filter(pr -> SW360ProjectAdapterUtils.hasEqualCoordinates(pr, projectName, projectVersion))
                        .findAny());
    }

    @Override
    public CompletableFuture<List<SW360Project>> search(ProjectSearchParams params) {
        return getProjectClient().search(params);
    }

    @Override
    public CompletableFuture<SW360Project> createProject(SW360Project project) {
        return validateProjectAndProcess(project, getProjectClient()::createProject);
    }

    @Override
    public CompletableFuture<SW360Project> updateProject(SW360Project project) {
        return validateProjectAndProcess(project, getProjectClient()::updateProject);
    }

    @Override
    public CompletableFuture<Void> addSW360ReleasesToSW360Project(String id, Collection<SW360Release> releases) {
        List<String> releaseLinks = releases.stream()
                .map(SW360Release::getLinks)
                .filter(Objects::nonNull)
                .map(LinkObjects::getSelf)
                .filter(Objects::nonNull)
                .map(Self::getHref)
                .collect(Collectors.toList());
        return getProjectClient().addReleasesToProject(id, releaseLinks);
    }

    @Override
    public CompletableFuture<List<SW360SparseRelease>> getLinkedReleases(String projectId, boolean transitive) {
        return getProjectClient().getLinkedReleases(projectId, transitive);
    }

    /**
     * Validates the given project entity and then executes an action on it.
     * All mandatory properties must have been set. If this is the case, the
     * given function is invoked on the project.
     *
     * @param project the project to be processed
     * @param func    the processing function
     * @return the result of the processing function
     */
    private static CompletableFuture<SW360Project> validateProjectAndProcess(SW360Project project,
                                                                             Function<SW360Project, CompletableFuture<SW360Project>> func) {
        if (!SW360ProjectAdapterUtils.isValidProject(project)) {
            Throwable exception = new SW360ClientException("Can not create invalid project with name=" +
                    project.getName() + " and version=" + project.getVersion());
            return FutureUtils.failedFuture(exception);
        }
        return func.apply(project);
    }
}
