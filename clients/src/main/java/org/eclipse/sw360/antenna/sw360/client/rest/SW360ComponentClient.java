/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Verifa Oy 2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest;

import org.eclipse.sw360.antenna.http.RequestBuilder;
import org.eclipse.sw360.antenna.http.utils.HttpUtils;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.ComponentSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentList;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ResourceUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * An SW360 REST client implementation providing basic functionality related to
 * the {@code /components} endpoint.
 * </p>
 */
public class SW360ComponentClient extends SW360Client {
    /**
     * Tag for the query that returns all components in the system.
     */
    static final String TAG_GET_COMPONENTS = "get_components";

    /**
     * Tag for the query that returns details about a specific component.
     */
    static final String TAG_GET_COMPONENT = "get_component";

    /**
     * Tag for the request to create a new component.
     */
    static final String TAG_CREATE_COMPONENT = "post_create_component";

    /**
     * Tag for the request to update a component.
     */
    static final String TAG_UPDATE_COMPONENT = "patch_update_component";

    /**
     * Tag for the request to delete components.
     */
    static final String TAG_DELETE_COMPONENTS = "delete_components";

    private static final String COMPONENTS_ENDPOINT = "components";

    /**
     * URL query parameter for the name search criterion.
     */
    private static final String PARAM_NAME = "name";

    /**
     * URL query parameter for the component type search criterion.
     */
    private static final String PARAM_TYPE = "type";

    /**
     * URL query parameter for the page index.
     */
    private static final String PARAM_PAGE = "page";

    /**
     * URL query parameter for the page size.
     */
    private static final String PARAM_PAGE_SIZE = "page_entries";

    /**
     * URL query parameter for the sort order.
     */
    private static final String PARAM_SORT = "sort";

    /**
     * URL query parameter for the fields to retrieve.
     */
    private static final String PARAM_FIELDS = "fields";

    /**
     * Creates a new instance of {@code SW360ComponentClient} and initializes
     * it with the passed in dependencies.
     *
     * @param config   the client configuration
     * @param provider the provider for access tokens
     */
    public SW360ComponentClient(SW360ClientConfig config, AccessTokenProvider provider) {
        super(config, provider);
    }

    /**
     * Returns a future with detail information about the component with the ID
     * provided. If the component cannot be found, the future fails with a
     * {@link org.eclipse.sw360.antenna.http.utils.FailedRequestException} with
     * status code 404.
     *
     * @param componentId the ID of the component in question
     * @return a future with details about this component
     */
    public CompletableFuture<SW360Component> getComponent(String componentId) {
        return executeJsonRequest(HttpUtils.get(resourceUrl(COMPONENTS_ENDPOINT, componentId)),
                SW360Component.class, TAG_GET_COMPONENT);
    }

    /**
     * Executes a search query based on the parameters provided and returns a
     * future with a result object. The result contains the entities matched by
     * the search criteria and paging-related metadata if available. (If the
     * search parameters do not use paging, the paging-related objects in the
     * result are <strong>null</strong>.)
     *
     * @param searchParams the object with search parameters
     * @return a future with an object holding the search results
     */
    public CompletableFuture<PagingResult<SW360SparseComponent>> search(ComponentSearchParams searchParams) {
        Map<String, Object> params = createSearchQueryParameters(searchParams);
        String url = HttpUtils.addQueryParameters(resourceUrl(COMPONENTS_ENDPOINT), params, true);
        return executeJsonRequestWithDefault(HttpUtils.get(url), SW360ComponentList.class,
                TAG_GET_COMPONENTS, SW360ComponentList::new)
                .thenApply(SW360ComponentClient::createPagingComponentResult);
    }

    /**
     * Creates a new component based on the data object passed in and returns a
     * future with the result.
     *
     * @param sw360Component a data object for the component to be created
     * @return a future with the new entity that has been created
     */
    public CompletableFuture<SW360Component> createComponent(SW360Component sw360Component) {
        return executeJsonRequest(builder -> builder.method(RequestBuilder.Method.POST)
                        .uri(resourceUrl(COMPONENTS_ENDPOINT))
                        .body(body -> body.json(sw360Component)),
                SW360Component.class, TAG_CREATE_COMPONENT);
    }

    /**
     * Updates a release based on the data object passed in and returns a
     * future with the result.
     *
     * @param component a data object for the component to be updated
     * @return a future with the updated component entity
     */
    public CompletableFuture<SW360Component> patchComponent(SW360Component component) {
        return executeJsonRequest(builder -> builder.method(RequestBuilder.Method.PATCH)
                        .uri(resourceUrl(COMPONENTS_ENDPOINT, component.getId()))
                        .body(body -> body.json(component)),
                SW360Component.class, TAG_UPDATE_COMPONENT);
    }

    /**
     * Triggers a DELETE operation for the components identified by the given
     * IDs.
     *
     * @param idsToDelete a collection with the IDs of the components to delete
     * @return a future with the {@code MultiStatusResponse} returned by the
     * server
     */
    public CompletableFuture<MultiStatusResponse> deleteComponents(Collection<String> idsToDelete) {
        return executeDeleteRequest(COMPONENTS_ENDPOINT, idsToDelete, TAG_DELETE_COMPONENTS);
    }

    /**
     * Generates a map with query parameters for a search based on the
     * parameters object provided.
     *
     * @param searchParams the search parameters
     * @return the map with corresponding query parameters
     */
    private static Map<String, Object> createSearchQueryParameters(ComponentSearchParams searchParams) {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_NAME, searchParams.getName());
        params.put(PARAM_TYPE, searchParams.getComponentType());
        params.put(PARAM_PAGE, searchParams.getPageIndex());
        params.put(PARAM_PAGE_SIZE, searchParams.getPageSize());
        params.put(PARAM_SORT, multiParam(searchParams.getOrderClauses()));
        params.put(PARAM_FIELDS, multiParam(searchParams.getFields()));
        return params;
    }

    /**
     * Generates a string representation for a query parameter with multiple
     * values.
     *
     * @param values the list with parameter values
     * @return the string representation of this query parameter
     */
    private static String multiParam(List<String> values) {
        return String.join(",", values);
    }

    /**
     * Converts the given component list to a paging result.
     *
     * @param componentList the component list
     * @return the result with the components and paging information
     */
    private static PagingResult<SW360SparseComponent> createPagingComponentResult(SW360ComponentList componentList) {
        List<SW360SparseComponent> components = SW360ResourceUtils.getSw360SparseComponents(componentList);
        return new PagingResult<>(components, componentList.getPage(), componentList.getLinks());
    }
}
