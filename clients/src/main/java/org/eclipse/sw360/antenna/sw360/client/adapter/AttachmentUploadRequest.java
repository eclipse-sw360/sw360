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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A data class that represents a request to upload multiple attachments for an
 * entity.
 * </p>
 * <p>
 * It is possible to upload multiple attachments in a single request. A request
 * is created using a builder that offers methods to add the items to be
 * uploaded.
 * </p>
 * @param <T> the type of the entity to upload attachments to
 */
public final class AttachmentUploadRequest<T extends SW360HalResource<?, ?>> {
    /**
     * The entity that is the target for uploads.
     */
    private final T target;

    /**
     * Stores the items to be uploaded.
     */
    private final List<Item> items;

    /**
     * Creates a new instance of {@code AttachmentUploadRequest} with the items
     * to be uploaded.
     *
     * @param target the target entity of the uploads
     * @param items  a list with the items to be uploaded
     */
    private AttachmentUploadRequest(T target, List<Item> items) {
        this.target = target;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * Returns a new {@code Builder} to define a request to upload attachments
     * to the given entity.
     *
     * @param target the target of the upload operation
     * @return the builder to define the upload request
     */
    public static <T extends SW360HalResource<?, ?>> Builder<T> builder(T target) {
        return new Builder<>(target);
    }

    /**
     * Returns the target entity to which attachments are to be uploaded.
     *
     * @return the target entity for attachment uploads
     */
    public T getTarget() {
        return target;
    }

    /**
     * Returns a list with the items that are to be uploaded.
     *
     * @return a list with the items to be uploaded
     */
    public List<Item> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttachmentUploadRequest<?> request = (AttachmentUploadRequest<?>) o;
        return Objects.equals(getTarget(), request.getTarget()) &&
                items.equals(request.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTarget(), items);
    }

    @Override
    public String toString() {
        return "AttachmentUploadRequest{" +
                "target=" + target +
                ", items=" + items +
                '}';
    }

    /**
     * A simple data class representing a single attachment item to be
     * uploaded.
     */
    public static final class Item {
        /**
         * The path of the file to be uploaded.
         */
        private final Path path;

        /**
         * The type of the attachment.
         */
        private final SW360AttachmentType attachmentType;

        /**
         * Creates a new instance of {@code Item} with the given properties.
         *
         * @param path           the path of the file to be uploaded
         * @param attachmentType the attachment type
         */
        public Item(Path path, SW360AttachmentType attachmentType) {
            this.path = path;
            this.attachmentType = attachmentType;
        }

        /**
         * Returns the path to the document that is to be uploaded.
         *
         * @return the path to be uploaded
         */
        public Path getPath() {
            return path;
        }

        /**
         * Returns the type of the new attachment.
         *
         * @return the attachment type
         */
        public SW360AttachmentType getAttachmentType() {
            return attachmentType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return Objects.equals(getPath(), item.getPath()) &&
                    getAttachmentType() == item.getAttachmentType();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPath(), getAttachmentType());
        }

        @Override
        public String toString() {
            return "Item{" +
                    "path=" + path +
                    ", attachmentType=" + attachmentType +
                    '}';
        }
    }

    /**
     * A builder class for creating {@link AttachmentUploadRequest} instances.
     */
    public static class Builder<T extends SW360HalResource<?, ?>> {
        /**
         * The entity to upload attachments to.
         */
        private final T target;

        /**
         * Stores the items to be uploaded.
         */
        private final List<Item> items;

        private Builder(T target) {
            this.target = target;
            items = new LinkedList<>();
        }

        /**
         * Adds an attachment to be uploaded to the request to be created.
         *
         * @param attachmentPath the path to the document to be uploaded
         * @param attachmentType the type of the resulting attachment
         * @return this builder
         */
        public Builder<T> addAttachment(Path attachmentPath, SW360AttachmentType attachmentType) {
            items.add(new Item(attachmentPath, attachmentType));
            return this;
        }

        /**
         * Creates the request to upload attachments based on the data added to
         * this builder so far.
         *
         * @return the newly created {@code AttachmentUploadRequest}
         */
        public AttachmentUploadRequest<T> build() {
            return new AttachmentUploadRequest<>(target, items);
        }
    }
}
