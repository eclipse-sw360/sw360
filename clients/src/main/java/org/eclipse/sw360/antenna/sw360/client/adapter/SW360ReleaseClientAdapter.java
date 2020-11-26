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

import org.eclipse.sw360.antenna.sw360.client.rest.SW360AttachmentAwareClient;
import org.eclipse.sw360.antenna.sw360.client.rest.MultiStatusResponse;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360ReleaseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Service interface for an adapter supporting operations on SW360 release
 * entities.
 * </p>
 */
public interface SW360ReleaseClientAdapter {
    /**
     * Returns the {@code SW360ReleaseClient} used by this adapter. The client
     * can be used for low-level operations against the SW360 releases
     * endpoint.
     *
     * @return the {@code SW360ReleaseClient}
     */
    SW360ReleaseClient getReleaseClient();

    /**
     * Creates a new release in SW360 based on the passed in entity. The given
     * entity must have all mandatory properties defined. It is assigned to the
     * component it references; the component instance is created if necessary.
     *
     * @param release the entity describing the release to be created
     * @return the newly created release entity
     */
    SW360Release createRelease(SW360Release release);


    /**
     * Tries to find the release with the given ID. Result is an
     * {@code Optional}; if the release ID cannot be resolved, the
     * {@code Optional} is empty.
     *
     * @param releaseId the ID of the release in question
     * @return an {@code Optional} with the release found
     */
    Optional<SW360Release> getReleaseById(String releaseId);


    /**
     * Tries to transform a sparse release into a full one. This method looks
     * up the release the given object points to and returns a data object with
     * all its properties. If the release cannot be resolved, result is an
     * empty {@code Optional}.
     *
     * @param sparseRelease the sparse release object
     * @return an {@code Optional} with the release found
     */
    Optional<SW360Release> enrichSparseRelease(SW360SparseRelease sparseRelease);

    /**
     * Searches for a release based on the given external IDs. This method
     * performs a search on the releases using the external IDs as criterion.
     * If no match is found, result is an empty {@code Optional}. If the search
     * yields multiple results, an exception is thrown. Result is a sparse
     * release, which can be converted to a full {@link SW360Release} object
     * by using the {@link #enrichSparseRelease(SW360SparseRelease)} method.
     *
     * @param externalIds a map with the external IDs to search for
     * @return an {@code Optional} with the release that was found
     */
    Optional<SW360SparseRelease> getSparseReleaseByExternalIds(Map<String, ?> externalIds);

    /**
     * Searches for a release based on the component name and the release
     * version. This method obtains the component associated with this release
     * and filters its releases for the correct version. Result is an empty
     * {@code Optional} if no matching version is found. Otherwise, a sparse
     * release is returned, which can be converted to a full
     * {@link SW360Release} by using the
     * {@link #enrichSparseRelease(SW360SparseRelease)} method.
     *
     * @param componentName the name of the component affected
     * @param version       the version of the desired release
     * @return an {@code Optional} with the release that was found
     */
    Optional<SW360SparseRelease> getSparseReleaseByNameAndVersion(String componentName, String version);

    /**
     * Tries to retrieve the release with the given version from the passed in
     * {@code SW360Component} entity. If the component has a release with the
     * version specified, all its properties are loaded and returned in an
     * entity object; otherwise, result is an empty {@code Optional}.
     *
     * @param component      the {@code SW360Component} entity
     * @param releaseVersion the version of the release in question
     * @return a future with an {@code Optional} with the release found
     */
    Optional<SW360Release> getReleaseByVersion(SW360Component component, String releaseVersion);

    /**
     * Uploads an arbitrary number of attachments for a release.
     *
     * @param uploadRequest the request with the attachments to be uploaded
     * @return the result of the upload operation
     */
    AttachmentUploadResult<SW360Release> uploadAttachments(AttachmentUploadRequest<SW360Release> uploadRequest);

    /**
     * Tries to download an attachment from a release. If it can be resolved,
     * the attachment file is written into the download path provided. This
     * directory is created if it does not exist (but not any parent
     * directories).
     *
     * @param release      the release entity
     * @param attachment   the attachment to be downloaded
     * @param downloadPath the path where to store the downloaded file
     * @return an {@code Optional} with the path to the file that has been
     * written
     */
    Optional<Path> downloadAttachment(SW360Release release, SW360SparseAttachment attachment, Path downloadPath);

    /**
     * Processes an attachment of a release using the processor specified. This
     * method opens a stream to download the attachment and passes the stream
     * to the {@code AttachmentProcessor}. The processor can then decide how to
     * deal with the content of the attachment and produce a corresponding
     * result. While the {@code downloadAttachment()} method handles the
     * default download use case, this method can be used to customize this use
     * case.
     *
     * @param release      the release entity
     * @param attachmentId the ID of the attachment in question
     * @param processor    the processor to handle the attachment stream
     * @param <T>          the result type of the {@code AttachmentProcessor}
     * @return the result produced by the {@code AttachmentProcessor}
     */
    <T> T processAttachment(SW360Release release, String attachmentId,
                            SW360AttachmentAwareClient.AttachmentProcessor<? extends T> processor);

    /**
     * Deletes the attachments with the given IDs from the release specified.
     * Note that this operation is successful even if some of the attachments
     * could not be deleted; therefore, the set of attachments in the release
     * returned should be checked to find out if some delete operations failed.
     *
     * @param release       the release entity
     * @param attachmentIds a collection with the IDs of the attachments to be
     *                      deleted
     * @return the updated release entity
     */
    SW360Release deleteAttachments(SW360Release release, Collection<String> attachmentIds);

    /**
     * Updates a release. The release is updated in the database based on the
     * properties of the passed in entity.
     *
     * @param release the release to be updated
     * @return the updated release
     */
    SW360Release updateRelease(SW360Release release);

    /**
     * Triggers a multi-delete operation for the releases with the IDs
     * specified. Returns a {@code MultiStatusResponse} that allows checking
     * whether all the releases could be deleted successfully.
     *
     * @param idsToDelete a collection with the IDs of releases to delete
     * @return a {@code MultiStatusResponse} with the results of the operation
     */
    MultiStatusResponse deleteReleases(Collection<String> idsToDelete);

    /**
     * Deletes the release with the given ID. This is a convenience method for
     * the special case that only a single release should be deleted. It
     * inspects the {@link MultiStatusResponse} returned by SW360 and throws an
     * exception if the operation was not successful.
     *
     * @param releaseId the ID of the release to be deleted
     * @throws org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException if the release could not be deleted
     */
    void deleteRelease(String releaseId);
}
