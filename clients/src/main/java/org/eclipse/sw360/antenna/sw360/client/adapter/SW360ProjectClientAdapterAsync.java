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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.ProjectSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Service interface for an adapter supporting asynchronous operations on SW360
 * project entities.
 * </p>
 */
public interface SW360ProjectClientAdapterAsync {
    /**
     * Returns the {@code SW360ProjectClient} used by this adapter. This client
     * can be used for low-level operations on project entities.
     *
     * @return the {@code SW360ProjectClient}
     */
    SW360ProjectClient getProjectClient();

    /**
     * Searches for a project based on its name and version. This method is
     * more convenient for this special use case than a generic search.
     *
     * @param projectName    the name of the project
     * @param projectVersion the desired project version
     * @return a future with an {@code Optional} with the project that was
     * found
     */
    CompletableFuture<Optional<SW360Project>> getProjectByNameAndVersion(String projectName,
                                                                         String projectVersion);

    /**
     * Searches for projects based on the given search criteria. This method
     * just delegates to the {@link SW360ProjectClient}.
     *
     * @param params the search parameters
     * @return a future with a list with the projects that were found
     */
    CompletableFuture<List<SW360Project>> search(ProjectSearchParams params);

    /**
     * Creates a new {@code SW360Project} entity based on the given data
     * object. The passed in object is validated whether it contains all
     * mandatory properties.
     *
     * @param project the data object defining the project properties
     * @return a future with the newly created {@code SW360Project} entity
     */
    CompletableFuture<SW360Project> createProject(SW360Project project);

    /**
     * Updates a project based on the passed in data object. The
     * {@code SW360Project} entity provided must reference an existing project.
     *
     * @param project the data object with the updated properties
     * @return a future with the updated {@code SW360Project} entity
     */
    CompletableFuture<SW360Project> updateProject(SW360Project project);

    /**
     * Assigns a number of releases to a project.
     *
     * @param projectId the ID of the project
     * @param releases  a collection with the {@code SW360Release} objects to be
     *                  assigned
     * @return a future that is completed when the operation is done
     */
    CompletableFuture<Void> addSW360ReleasesToSW360Project(String projectId, Collection<SW360Release> releases);

    /**
     * Returns a list with all the release entities that are linked to the
     * project specified.
     *
     * @param projectId  the ID of the project
     * @param transitive if <strong>true</strong>, the releases assigned to
     *                   directly linked releases are retrieved as well
     * @return a future with the list with the found release entities
     */
    CompletableFuture<List<SW360SparseRelease>> getLinkedReleases(String projectId, boolean transitive);
}
