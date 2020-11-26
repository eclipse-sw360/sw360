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

import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpUtils;
import org.eclipse.sw360.antenna.sw360.client.rest.MultiStatusResponse;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * A helper class providing functionality related to deleting entities.
 * </p>
 * <p>
 * SW360 at some places supports deleting multiple entities at once. This class
 * offers methods to support such multi-delete operations and to deal with the
 * multi-status responses they return.
 * </p>
 */
class SW360DeleteUtils {
    /**
     * Private constructor to prevent instantiation.
     */
    private SW360DeleteUtils() {
    }

    /**
     * Deletes the entities specified using the given {@code EntityDeleter}.
     * This method checks whether the collection with IDs to delete is not
     * empty; in this case, the delete operation is skipped.
     *
     * @param deleter     the object handling delete requests
     * @param idsToDelete a collection with the resource IDs to delete
     * @return a future with the result of the multi-delete operation
     */
    public static CompletableFuture<MultiStatusResponse> deleteEntities(EntityDeleter deleter,
                                                                        Collection<String> idsToDelete) {
        return idsToDelete.isEmpty() ?
                CompletableFuture.completedFuture(new MultiStatusResponse(Collections.emptyMap())) :
                deleter.deleteEntities(idsToDelete);
    }

    /**
     * Deletes a single entity using the given {@code EntityDeleter} and
     * evaluates the {@link MultiStatusResponse} returned by the server. This
     * method yields a failed future if the response from the server indicates
     * that the entity could not be deleted or if an unexpected response is
     * received.
     *
     * @param deleter      the object handling delete requests
     * @param resourceId   the ID of the resource entity to be deleted
     * @param resourceType a string for the resource type, to be used when
     *                     generating error messages
     * @return a future indicating the success of the operation
     */
    public static CompletableFuture<Void> deleteEntity(EntityDeleter deleter, String resourceId, String resourceType) {
        return deleter.deleteEntities(Collections.singleton(resourceId))
                .thenApply(status -> {
                    checkResponse(status, resourceId, resourceType);
                    return null;
                });
    }

    /**
     * Checks whether the given multi-status response reports a successful
     * delete operation on the entity specified. If this is not the case, an
     * exception is thrown, which will then cause the result future to fail.
     *
     * @param status       the status response to be checked
     * @param resourceId   the ID of the resource entity to be deleted
     * @param resourceType a string for the resource type
     */
    private static void checkResponse(MultiStatusResponse status, String resourceId,
                                      String resourceType) {
        if (status.responseCount() != 1 || !status.hasResourceId(resourceId)) {
            throw new SW360ClientException("Unexpected multi-status response. Expected a response that " +
                    "contains only a status for " + resourceId + ", but got: " + status);
        }
        if (!HttpUtils.isSuccessStatus(status.getStatus(resourceId))) {
            FailedRequestException requestException =
                    new FailedRequestException(generateDeleteTag(resourceId, resourceType),
                            status.getStatus(resourceId));
            throw new SW360ClientException("Delete operation failed", requestException);
        }
    }

    /**
     * Generates the tag for an exception indicating a failed request to delete
     * the resource specified.
     *
     * @param resourceId   the ID of the resource entity
     * @param resourceType the type of the resource
     * @return the tag to describe the failed request
     */
    private static String generateDeleteTag(String resourceId, String resourceType) {
        return "delete " + resourceType + " " + resourceId;
    }

    /**
     * A specialized function interface to invoke the entity-specific delete
     * operation for a given collection of IDs.
     */
    @FunctionalInterface
    interface EntityDeleter {
        /**
         * Asynchronously deletes the entities with the IDs specified.
         *
         * @param idsToDelete a collection with the IDs to delete
         * @return a future with the result of the multi-delete operation
         */
        CompletableFuture<MultiStatusResponse> deleteEntities(Collection<String> idsToDelete);
    }
}
