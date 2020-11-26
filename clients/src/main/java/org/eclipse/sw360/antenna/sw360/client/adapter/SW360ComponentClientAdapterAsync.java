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

import org.eclipse.sw360.antenna.sw360.client.rest.MultiStatusResponse;
import org.eclipse.sw360.antenna.sw360.client.rest.PagingResult;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360ComponentClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.ComponentSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360SparseComponent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Service interface for an adapter supporting asynchronous operations on SW360
 * component entities.
 * </p>
 */
public interface SW360ComponentClientAdapterAsync {
    /**
     * Returns the {@code SW360ComponentClient} object used by this adapter to
     * interact with the SW360 server. Clients may use this object as well if
     * they need access to a low-level API.
     *
     * @return the underlying {@code SW360ComponentClient}
     */
    SW360ComponentClient getComponentClient();

    /**
     * Creates a new component in SW360 based on the passed in data object.
     *
     * @param component the object describing the new component
     * @return a future with the newly created component entity
     */
    CompletableFuture<SW360Component> createComponent(SW360Component component);

    /**
     * Queries a component by its ID. Result is an {@code Optional}, which is
     * empty if the component ID cannot be resolved.
     *
     * @param componentId the ID of the desired component
     * @return a future with an {@code Optional} containing the resulting
     * component
     */
    CompletableFuture<Optional<SW360Component>> getComponentById(String componentId);

    /**
     * Queries a component by its name. Tries to find a component whose name
     * matches the passed in string exactly. Result is an {@code Optional},
     * which is empty if no matching component can be found.
     *
     * @return a future with an {@code Optional} containing the resulting
     * component
     */
    CompletableFuture<Optional<SW360Component>> getComponentByName(String componentName);

    /**
     * Searches for components based on the criteria provided. Like
     * {@link #searchWithPaging(ComponentSearchParams)}, this method supports
     * arbitrary searches, but it ignores the paging information in the result
     * and only returns the list of entities found. So using this method is
     * more convenient if the caller is not interested in paging.
     *
     * @param searchParams the object with search parameters
     * @return a future with the list of the components found by the search
     */
    CompletableFuture<List<SW360SparseComponent>> search(ComponentSearchParams searchParams);

    /**
     * Searches for components based on the criteria provided and returns a
     * {@code PagingResult} with the entities found and additional paging
     * information. Note that paging information is available only if the
     * search parameters make use of the paging mechanism; otherwise, in the
     * result object only the list with entities is populated.
     *
     * @param searchParams the object with search parameters
     * @return a future with the object representing the result of the search
     */
    CompletableFuture<PagingResult<SW360SparseComponent>> searchWithPaging(ComponentSearchParams searchParams);

    /**
     * Updates a component based on the data object passed in.
     *
     * @param component the data object describing the component to update
     * @return a future with the updated component
     */
    CompletableFuture<SW360Component> updateComponent(SW360Component component);

    /**
     * Triggers a multi-delete operation for the components with the IDs
     * specified. Returns a {@code MultiStatusResponse} that allows checking
     * whether all the components could be deleted successfully.
     *
     * @param idsToDelete a collection with the IDs of components to delete
     * @return a future with the {@code MultiStatusResponse} with the results
     * of the operation
     */
    CompletableFuture<MultiStatusResponse> deleteComponents(Collection<String> idsToDelete);

    /**
     * Deletes the component with the given ID. This is a convenience method for
     * the special case that only a single component should be deleted. It
     * inspects the {@link MultiStatusResponse} returned by SW360 and returns a
     * failed future if the operation was not successful.
     *
     * @param componentId the ID of the component to be deleted
     * @return a future indicating the result of the operation
     */
    CompletableFuture<Void> deleteComponent(String componentId);
}
