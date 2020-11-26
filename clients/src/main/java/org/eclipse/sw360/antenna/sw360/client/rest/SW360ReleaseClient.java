/*
 * Copyright (c) Bosch Software Innovations GmbH 2018-2019.
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
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ResourceUtils;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360ReleaseList;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * An SW360 REST client implementation providing basic functionality related to
 * the {@code /releases} endpoint.
 * </p>
 */
public class SW360ReleaseClient extends SW360AttachmentAwareClient<SW360Release> {
    /**
     * Tag for the query for a specific release.
     */
    static final String TAG_GET_RELEASE = "get_release";

    /**
     * Tag for the query that searches for releases by external IDs.
     */
    static final String TAG_GET_RELEASES_BY_EXTERNAL_IDS = "get_releases_by_external_ids";

    /**
     * Tag for the request to create a new release.
     */
    static final String TAG_CREATE_RELEASE = "post_create_release";

    /**
     * Tag for the request that modifies a release.
     */
    static final String TAG_UPDATE_RELEASE = "patch_update_release";

    /**
     * Tag for the request that deletes releases.
     */
    static final String TAG_DELETE_RELEASES = "delete_releases";

    private static final String RELEASES_ENDPOINT_APPENDIX = "releases";
    private static final String PATH_SEARCH_EXT_IDS = "searchByExternalIds";

    /**
     * Creates a new instance of {@code SW360ReleaseClient} with the passed in
     * dependencies.
     *
     * @param config   the client configuration
     * @param provider the provider for access tokens
     */
    public SW360ReleaseClient(SW360ClientConfig config, AccessTokenProvider provider) {
        super(config, provider);
    }

    @Override
    public Class<SW360Release> getHandledClassType() {
        return SW360Release.class;
    }

    /**
     * Returns a future with detail information about the release with the
     * given ID. If the release cannot be found, the future fails with a
     * {@link org.eclipse.sw360.antenna.http.utils.FailedRequestException} with
     * status code 404.
     *
     * @param releaseId the ID of the release in question
     * @return a future with details about this release
     */
    public CompletableFuture<SW360Release> getRelease(String releaseId) {
        return executeJsonRequest(HttpUtils.get(resourceUrl(RELEASES_ENDPOINT_APPENDIX, releaseId)),
                SW360Release.class, TAG_GET_RELEASE);
    }

    /**
     * Returns a future with a list of releases that match the external IDs
     * passed to this method.
     *
     * @param externalIds a map with the IDs to be matched and their values
     * @return a future with a list of the releases that could be matched
     */
    // KnownLimitation: this can not properly handle e.g. the hashes,
    // which are mapped to numbered keys like `hash_1=...`, `hash_2=...`, ...
    // but can change in the order of the values
    public CompletableFuture<List<SW360SparseRelease>> getReleasesByExternalIds(Map<String, ?> externalIds) {
        String url = getExternalIdUrl(externalIds);
        return executeJsonRequestWithDefault(HttpUtils.get(url), SW360ReleaseList.class,
                TAG_GET_RELEASES_BY_EXTERNAL_IDS, SW360ReleaseList::new)
                .thenApply(SW360ResourceUtils::getSw360SparseReleases);
    }

    private String getExternalIdUrl(Map<String, ?> externalIds) {
        return HttpUtils.addQueryParameters(resourceUrl(RELEASES_ENDPOINT_APPENDIX, PATH_SEARCH_EXT_IDS),
                externalIds);
    }

    /**
     * Creates a new release resource based on the data object passed in and
     * returns a future with the result.
     *
     * @param sw360Release the data object defining the release to be created
     * @return a future with the resulting entity
     */
    public CompletableFuture<SW360Release> createRelease(SW360Release sw360Release) {
        return executeJsonRequest(builder -> builder.method(RequestBuilder.Method.POST)
                        .uri(resourceUrl(RELEASES_ENDPOINT_APPENDIX))
                        .body(body -> body.json(sw360Release)),
                SW360Release.class, TAG_CREATE_RELEASE);
    }

    /**
     * Modifies an existing release based on a data object and returns a future
     * with the result.
     *
     * @param sw360Release the data object defining the release to be modified
     *                     and the new properties
     * @return a future with the resulting entity
     */
    public CompletableFuture<SW360Release> patchRelease(SW360Release sw360Release) {
        return executeJsonRequest(builder -> builder.method(RequestBuilder.Method.PATCH)
                        .uri(resourceUrl(RELEASES_ENDPOINT_APPENDIX, sw360Release.getId()))
                        .body(body -> body.json(sw360Release)),
                SW360Release.class, TAG_UPDATE_RELEASE);
    }

    /**
     * Triggers a DELETE operation for the releases identified by the given
     * IDs.
     *
     * @param idsToDelete a collection with the IDs of the releases to delete
     * @return a future with the {@code MultiStatusResponse} returned by the
     * server
     */
    public CompletableFuture<MultiStatusResponse> deleteReleases(Collection<String> idsToDelete) {
        return executeDeleteRequest(RELEASES_ENDPOINT_APPENDIX, idsToDelete, TAG_DELETE_RELEASES);
    }
}
