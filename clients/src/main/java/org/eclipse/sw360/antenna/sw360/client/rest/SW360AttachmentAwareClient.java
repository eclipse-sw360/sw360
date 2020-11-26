/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
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
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360Attachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * A base class for client implementations that deal with resources supporting
 * attachments.
 * </p>
 * <p>
 * The class provides methods to upload and download attachments linked to
 * another resource.
 * </p>
 *
 * @param <T> the type of the resource handled by this class
 */
public abstract class SW360AttachmentAwareClient<T extends SW360HalResource<?, ?>> extends SW360Client {
    /**
     * Tag for the request to upload an attachment.
     */
    static final String TAG_UPLOAD_ATTACHMENT = "post_upload_attachment";

    /**
     * Tag for the request to download an attachment.
     */
    static final String TAG_DOWNLOAD_ATTACHMENT = "get_download_attachment";

    /**
     * Tag for the request to delete attachments.
     */
    static final String TAG_DELETE_ATTACHMENT = "delete_attachments";

    private static final Logger LOGGER = LoggerFactory.getLogger(SW360AttachmentAwareClient.class);
    private static final String ATTACHMENTS_ENDPOINT = "/attachments";

    /**
     * Creates a new instance of {@code SW360AttachmentAwareClient} with the
     * dependencies passed in.
     *
     * @param config   the client configuration
     * @param provider the access token provider
     */
    protected SW360AttachmentAwareClient(SW360ClientConfig config, AccessTokenProvider provider) {
        super(config, provider);
    }

    /**
     * Returns the type of the resource class for which attachments are
     * managed by this client. This is needed to do the correct JSON
     * deserialization.
     *
     * @return the resource class managed by this client
     */
    protected abstract Class<T> getHandledClassType();

    /**
     * Uploads an attachment file, creates an entity for it, and assigns it to
     * the entity represented by the passed in object. The path provided must
     * point to an existing file. A future with the modified entity (the one
     * the attachment has been added to) is returned.
     *
     * @param itemToModify a data object defining the resource to attach the
     *                     file
     * @param fileToAttach the path to the file to be attached
     * @param kindToAttach the attachment type
     * @return a future with the entity that has been modified
     */
    public CompletableFuture<T> uploadAndAttachAttachment(T itemToModify, Path fileToAttach,
                                                          SW360AttachmentType kindToAttach) {
        if (!Files.exists(fileToAttach)) {
            LOGGER.warn("The file=[{}], which should be attached to release, does not exist", fileToAttach);
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IOException("File to upload does not exist: " + fileToAttach));
            return failedFuture;
        }

        SW360Attachment sw360Attachment = new SW360Attachment(fileToAttach, kindToAttach);
        final String self = itemToModify.getLinks().getSelf().getHref();
        return executeJsonRequest(builder -> builder.method(RequestBuilder.Method.POST)
                        .uri(resolveAgainstBase(self + ATTACHMENTS_ENDPOINT).toString())
                        .multiPart("attachment", part ->
                                part.json(sw360Attachment))
                        .multiPart("file", part ->
                                part.file(fileToAttach, HttpConstants.CONTENT_OCTET_STREAM)),
                getHandledClassType(), TAG_UPLOAD_ATTACHMENT);
    }

    /**
     * Fetches the content of an attachment and passes it to the
     * {@code AttachmentProcessor} provided. A future with the result produced
     * by the processor is returned. If the attachment cannot be resolved, the
     * resulting future fails with a
     * {@link org.eclipse.sw360.antenna.http.utils.FailedRequestException} with
     * status code 404; it contains an {@code IOException} if there was a
     * problem with the processor or if the server could not be contacted.
     *
     * @param itemHref     the base resource URL to access the attachment entity
     * @param attachmentId the ID of the attachment to be processed
     * @param processor    the object to process the attachment's content
     * @param <S>          the result type of the {@code AttachmentProcessor}
     * @return a future with the result produced by the
     * {@code AttachmentProcessor}
     */
    public <S> CompletableFuture<S> processAttachment(String itemHref, String attachmentId,
                                                      AttachmentProcessor<? extends S> processor) {
        String url = itemHref + "/attachments/" + attachmentId;
        return executeRequest(builder -> builder.uri(resolveAgainstBase(url).toString())
                        .header(HttpConstants.HEADER_ACCEPT, HttpConstants.CONTENT_OCTET_STREAM),
                response ->
                        processor.processAttachmentStream(response.bodyStream()), TAG_DOWNLOAD_ATTACHMENT);
    }

    /**
     * Deletes the specified attachments from the entity provided. A future
     * with the updated entity is returned. If successful, callers should
     * inspect the attachments of the entity to find out whether actually all
     * could be deleted. (SW360 does not allow deleting attachments already in
     * use; if only some of the attachments could be deleted, the request is
     * successful, but the resulting entity will still contain the problematic
     * ones.)
     *
     * @param entity        the entity to be updated
     * @param attachmentIds a list with the IDs of the attachments to be
     *                      deleted
     * @return a future with the updated entity
     */
    public CompletableFuture<T> deleteAttachments(T entity, Collection<String> attachmentIds) {
        String url = entity.getSelfLink().getHref() + ATTACHMENTS_ENDPOINT + "/" +
                String.join(",", attachmentIds);
        return executeJsonRequest(builder -> builder.uri(resolveAgainstBase(url).toString())
                        .method(RequestBuilder.Method.DELETE),
                getHandledClassType(), TAG_DELETE_ATTACHMENT);
    }

    /**
     * <p>
     * An interface for accessing and processing attachments from SW360
     * resources.
     * </p>
     * <p>
     * An object implementing this interface needs to be provided to the
     * {@link #processAttachment(String, String, AttachmentProcessor)} method.
     * It is invoked with the input stream for the attachment's content. This
     * stream can be consumed to produce an arbitrary result. An obvious use
     * case is copying the stream to a local file, which corresponds to an
     * attachment download operation. But by providing different processor
     * implementations, attachments can be handled in flexible ways.
     * </p>
     *
     * @param <R> the result type of this processor
     */
    @FunctionalInterface
    public interface AttachmentProcessor<R> {
        /**
         * Processes a stream with the content of an attachment and produces a
         * result based on this operation. Implementations do not have to close
         * the stream.
         *
         * @param stream the stream with the content of the attachment
         * @return the result produced by this processor
         * @throws IOException if an I/O error occurs
         */
        R processAttachmentStream(InputStream stream) throws IOException;
    }
}
