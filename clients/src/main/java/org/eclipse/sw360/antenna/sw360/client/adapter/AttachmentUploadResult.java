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

import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * A data class holding the result of a (multi) attachment upload request.
 * </p>
 * <p>
 * When uploading multiple attachments using a single request, every single
 * upload might fail. With this class it is possible to get detailed
 * information about all the uploads that were made and which of them were
 * successful. The updated {@link SW360Release} object is also available.
 * </p>
 *
 * @param <T> the type of the entity for which attachments were uploaded
 */
public final class AttachmentUploadResult<T extends SW360HalResource<?, ?>> {
    /**
     * Stores the target entity of the upload operation.
     */
    private final T target;

    /**
     * Stores information about the attachments that could be uploaded.
     */
    private final Set<AttachmentUploadRequest.Item> successfulUploads;

    /**
     * A map with information about attachments that could not be uploaded and
     * the reasons for the failed uploads.
     */
    private final Map<AttachmentUploadRequest.Item, Throwable> failedUploads;

    /**
     * Creates a new instance of {@code AttachmentUploadResult} that references
     * the given target entity.
     *
     * @param target the entity that was the target for uploads
     */
    public AttachmentUploadResult(T target) {
        this(target, Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * Internal constructor to create an instance of
     * {@code AttachmentUploadResult} with all information.
     *
     * @param target            the entity that was the target for uploads
     * @param successfulUploads a set with successful uploads
     * @param failedUploads     a map with failed uploads
     */
    private AttachmentUploadResult(T target,
                                   Set<AttachmentUploadRequest.Item> successfulUploads,
                                   Map<AttachmentUploadRequest.Item, Throwable> failedUploads) {
        this.target = target;
        this.successfulUploads = successfulUploads;
        this.failedUploads = failedUploads;
    }

    /**
     * Creates a new instance of {@code AttachmentUploadResult} with the
     * information provided.
     *
     * @param target            the entity that was the target for uploads
     * @param successfulUploads a set with successful uploads
     * @param failedUploads     a map with failed uploads
     * @param <T>               the type of the target entity
     * @return the newly created result
     */
    public static <T extends SW360HalResource<?, ?>>
    AttachmentUploadResult<T> newResult(T target,
                                        Set<AttachmentUploadRequest.Item> successfulUploads,
                                        Map<AttachmentUploadRequest.Item, Throwable> failedUploads) {
        return new AttachmentUploadResult<>(target, Collections.unmodifiableSet(new HashSet<>(successfulUploads)),
                Collections.unmodifiableMap(new HashMap<>(failedUploads)));
    }

    /**
     * Returns the entity that is the target of the upload operation.
     *
     * @return the updated entity to which attachments have been uploaded
     */
    public T getTarget() {
        return target;
    }

    /**
     * Returns a flag whether the whole upload operation was successful. If
     * this method returns <strong>true</strong>, all the single attachment
     * uploads have been successful; otherwise, there was at least one failure.
     * More information about successful and failed uploads is then available
     * through the other methods of this class.
     *
     * @return a flag whether all attachments could be uploaded successfully
     */
    public boolean isSuccess() {
        return failedUploads.isEmpty();
    }

    /**
     * Returns a set with information about all the attachment items that could
     * be uploaded successfully.
     *
     * @return a set with successfully uploaded attachment items
     */
    public Set<AttachmentUploadRequest.Item> successfulUploads() {
        return successfulUploads;
    }

    /**
     * Returns a map with information about all the attachment items that could
     * not be uploaded successfully. For each item whose upload caused a
     * failure, the corresponding exception can be queried.
     *
     * @return a map with the failed attachment items and the corresponding
     * exceptions
     */
    public Map<AttachmentUploadRequest.Item, Throwable> failedUploads() {
        return failedUploads;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttachmentUploadResult<?> result = (AttachmentUploadResult<?>) o;
        return Objects.equals(getTarget(), result.getTarget()) &&
                successfulUploads.equals(result.successfulUploads) &&
                failedUploads.equals(result.failedUploads);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTarget(), successfulUploads, failedUploads);
    }

    @Override
    public String toString() {
        return "AttachmentUploadResult{" +
                "target=" + target +
                ", successfulUploads=" + successfulUploads +
                ", failedUploads=" + failedUploads +
                '}';
    }

    /**
     * Returns a copy of this object that contains the given attachment item as
     * a successful upload.
     *
     * @param updatedTarget the updated target entity
     * @param item          the item that was uploaded successfully
     * @return the modified copy of this object
     */
    AttachmentUploadResult<T> addSuccessfulUpload(T updatedTarget, AttachmentUploadRequest.Item item) {
        Set<AttachmentUploadRequest.Item> updatedSet = new HashSet<>(successfulUploads);
        updatedSet.add(item);
        return new AttachmentUploadResult<>(updatedTarget, Collections.unmodifiableSet(updatedSet), failedUploads);
    }

    /**
     * Returns a copy of this object that contains the given attachment item as
     * a failed upload, together with the corresponding exception.
     *
     * @param item      the item that could not be uploaded
     * @param exception the exception causing the upload to fail
     * @return the modified copy of this object
     */
    AttachmentUploadResult<T> addFailedUpload(AttachmentUploadRequest.Item item, Throwable exception) {
        Map<AttachmentUploadRequest.Item, Throwable> updatedMap = new HashMap<>(failedUploads);
        updatedMap.put(item, exception);
        return new AttachmentUploadResult<>(getTarget(), successfulUploads, Collections.unmodifiableMap(updatedMap));
    }
}
