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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360AttachmentAwareClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.optionalFuture;

/**
 * <p>
 * A helper class providing utility methods related to the handling of
 * attachments.
 * </p>
 * <p>
 * Attachments are supported by multiple entity types. Therefore, it makes
 * sense to extract the functionality into a separate utility class.
 * </p>
 */
public class SW360AttachmentUtils {
    /**
     * String for the digits for a Hex-string conversion.
     */
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    /**
     * Name of the SHA-1 algorithm to calculate hashes.
     */
    private static final String ALG_SHA1 = "SHA-1";

    private static final Logger LOGGER = LoggerFactory.getLogger(SW360AttachmentUtils.class);

    private SW360AttachmentUtils() {
    }

    /**
     * Processes a request to upload multiple attachments. All attachment files
     * referenced by the passed in request are uploaded to the target entity. A
     * result object is returned with information about the single upload
     * operations.
     *
     * @param client             the client that handles a single upload operation
     * @param uploadRequest      the request to upload attachments
     * @param getAttachmentsFunc a function to access the existing attachments;
     *                           this is used to check for duplicates
     * @param <T>                the type of the target entity for the upload
     * @return a result object for the multi-upload operation
     */
    public static <T extends SW360HalResource<?, ?>> CompletableFuture<AttachmentUploadResult<T>>
    uploadAttachments(SW360AttachmentAwareClient<T> client, AttachmentUploadRequest<T> uploadRequest,
                      Function<? super T, Set<SW360SparseAttachment>> getAttachmentsFunc) {
        CompletableFuture<AttachmentUploadResult<T>> futResult =
                CompletableFuture.completedFuture(new AttachmentUploadResult<>(uploadRequest.getTarget()));

        for (AttachmentUploadRequest.Item item : uploadRequest.getItems()) {
            futResult = futResult.thenCompose(result -> {
                if (attachmentIsPotentialDuplicate(item.getPath(), getAttachmentsFunc.apply(result.getTarget()))) {
                    return CompletableFuture.completedFuture(result.addFailedUpload(item,
                            new SW360ClientException("Duplicate attachment file name: " +
                                    item.getPath().getFileName())));
                }

                return client
                        .uploadAndAttachAttachment(result.getTarget(), item.getPath(), item.getAttachmentType())
                        .handle((updatedEntity, ex) -> (updatedEntity != null) ?
                                result.addSuccessfulUpload(updatedEntity, item) :
                                result.addFailedUpload(item, ex));
            });
        }

        return futResult;
    }

    /**
     * Downloads a specific attachment file assigned to an entity to a local
     * folder on the hard disk. The directory is created if it does not exist
     * yet (but not any non-existing parent components). Result is an
     * {@code Optional} with the path to the file that has been downloaded. If
     * the requested attachment cannot be resolved, the {@code Optional} is
     * empty.
     *
     * @param client       the client that handles the download operation
     * @param entity       the entity to which the attachment belongs
     * @param attachment   the attachment that is to be downloaded
     * @param downloadPath the path where to store the downloaded file
     * @param <T>          the type of the entity that owns the attachment
     * @return a future with the {@code Optional} containing the path to the
     * file that was downloaded
     */
    public static <T extends SW360HalResource<?, ?>> CompletableFuture<Optional<Path>>
    downloadAttachment(SW360AttachmentAwareClient<? extends T> client, T entity, SW360SparseAttachment attachment,
                       Path downloadPath) {
        return Optional.ofNullable(entity.getSelfLink())
                .map(self -> {
                    AttachmentDownloadProcessor downloadProcessor =
                            defaultAttachmentDownloadProcessor(attachment, downloadPath);
                    return optionalFuture(client.processAttachment(self.getHref(), attachment.getId(),
                            downloadProcessor));
                })
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * Returns an {@link SW360AttachmentAwareClient.AttachmentProcessor} to
     * download the given attachment to a specific download folder. The
     * processor is configured with default options: it creates a non-existing
     * download folder (but not any missing parent directories) and overrides
     * an already existing local file. The file name is obtained from the
     * attachment object.
     *
     * @param attachment   the attachment to be downloaded
     * @param downloadPath the download path
     * @return the default processor to download this attachment
     */
    public static AttachmentDownloadProcessor defaultAttachmentDownloadProcessor(SW360SparseAttachment attachment,
                                                                                 Path downloadPath) {
        return defaultAttachmentDownloadProcessor(attachment.getFilename(), downloadPath);
    }

    /**
     * Returns an {@link SW360AttachmentAwareClient.AttachmentProcessor} to
     * download an attachment file to a specific download folder. This method
     * is analogous to
     * {@link #defaultAttachmentDownloadProcessor(SW360SparseAttachment, Path)},
     * but the file name is specified directly and thus can deviate from the
     * original attachment file name.
     *
     * @param fileName     the name under which the file should be saved
     * @param downloadPath the download path
     * @return the default processor to download this attachment
     */
    public static AttachmentDownloadProcessor defaultAttachmentDownloadProcessor(String fileName, Path downloadPath) {
        return new AttachmentDownloadProcessorCreateDownloadFolder(downloadPath,
                fileName, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Calculates a hash value on the content of the file specified. This
     * functionality is useful in relation with attachments, as by comparing
     * the hashes it can be determined whether a local file is identical to an
     * attachment that was uploaded to SW360.
     *
     * @param file   the path to the file for which the hash should be computed
     * @param digest the digest for doing the calculation
     * @return the (Hex-encoded) hash value for this file
     * @throws SW360ClientException if an error occurs
     */
    public static String calculateHash(Path file, MessageDigest digest) {
        try (DigestInputStream din = new DigestInputStream(Files.newInputStream(file), digest)) {
            IOUtils.copy(din, NullOutputStream.NULL_OUTPUT_STREAM);
        } catch (IOException e) {
            throw new SW360ClientException("Could not calculate hash for file " + file, e);
        }
        return toHexString(digest.digest());
    }

    /**
     * Calculates a SHA1 hash value on the content of the file specified. This
     * is a specialization to the {@link #calculateHash(Path, MessageDigest)}
     * method, which uses a {@code MessageDigest} for SHA1 calculation. This
     * algorithm is used by SW360 per default to calculate hashes on uploaded
     * attachments.
     *
     * @param file the path to the file for which the hash should be computed
     * @return the (Hex-encoded) hash value for this file
     * @throws SW360ClientException if an error occurs
     */
    public static String calculateSha1Hash(Path file) {
        try {
            return calculateHash(file, MessageDigest.getInstance(ALG_SHA1));
        } catch (NoSuchAlgorithmException e) {
            // This cannot happen as every implementation of the Java platform must support this algorithm
            throw new AssertionError("SHA-1 algorithm not supported");
        }
    }

    /**
     * Creates a directory (and optionally all missing parent directories) if
     * necessary. The method can be used in a concurrent environment and
     * handles the case that the directory has already been created by another
     * thread.
     *
     * @param path        the path of the directory to be created
     * @param withParents flag whether parent directories are to be created
     * @return the path to the newly created directory
     * @throws IOException if an error occurs
     */
    static Path safeCreateDirectory(Path path, boolean withParents) throws IOException {
        while (!Files.exists(path)) {
            try {
                LOGGER.info("Creating attachment download path {}.", path);
                return withParents ? Files.createDirectories(path) : Files.createDirectory(path);
            } catch (FileAlreadyExistsException e) {
                // ignore; this may be caused by a concurrent attempt to create the directory
                LOGGER.debug("Concurrent creation of directory {}. Ignoring exception.", path);
            }
        }

        if (!Files.isDirectory(path)) {
            throw new FileAlreadyExistsException(path.toString());
        }
        return path;
    }

    /**
     * Checks whether an attachment with a specific name already exists for the
     * target entity.
     *
     * @param attachment  the path to the local attachment file
     * @param attachments the attachments assigned to the target entity
     * @return a flag whether the attachment is duplicate
     */
    private static boolean attachmentIsPotentialDuplicate(Path attachment, Set<SW360SparseAttachment> attachments) {
        return attachments.stream()
                .anyMatch(attachment1 -> attachment1.getFilename().equals(attachment.getFileName().toString()));
    }

    /**
     * Converts the given byte array to a string with hexadecimal digits.
     *
     * @param bytes the array of bytes
     * @return the resulting hex string
     */
    private static String toHexString(byte[] bytes) {
        char[] encoded = new char[bytes.length * 2];
        int idx = 0;
        for (byte b : bytes) {
            encoded[idx++] = HEX_DIGITS[b >> 4 & 0xF];
            encoded[idx++] = HEX_DIGITS[b & 0xF];
        }
        return new String(encoded);
    }

    /**
     * <p>
     * A special {@code AttachmentProcessor} implementation to download an
     * attachment file to the local hard disk.
     * </p>
     * <p>
     * An instance is configured with the path to the folder where to store the
     * attachment and the target file name. For this implementation, the
     * download folder must exist. When invoked with the content stream of an
     * attachment file, the stream is copied into a file in the download folder
     * with the name specified. The {@code CopyOption} objects passed to the
     * constructor are taken into account.
     * </p>
     * <p>
     * The class may be extended to offer more flexibility with regards to the
     * download folder. For instance, a derived class may create the folder if
     * it does not exist yet.
     * </p>
     */
    public static class AttachmentDownloadProcessor implements SW360AttachmentAwareClient.AttachmentProcessor<Path> {
        /**
         * The path where to store the downloaded attachment.
         */
        private final Path downloadPath;

        /**
         * The file name to be used for the downloaded attachment.
         */
        private final String fileName;

        /**
         * The options to be applied for the copy stream operation.
         */
        private final CopyOption[] copyOptions;

        /**
         * Creates a new instance of {@code AttachmentDownloadProcessor} and
         * initializes it with all the properties required for a download
         * operation.
         *
         * @param downloadPath the path where to store the attachment
         * @param fileName     the file name to be used
         * @param options      an arbitrary number of {@code CopyOption} flags
         */
        public AttachmentDownloadProcessor(Path downloadPath, String fileName, CopyOption... options) {
            this.downloadPath = downloadPath;
            this.fileName = fileName;
            copyOptions = options;
        }

        /**
         * Returns the path where the downloaded attachment file is stored.
         *
         * @return the path to the download folder
         */
        public Path getDownloadPath() {
            return downloadPath;
        }

        /**
         * Returns the file name to be used for the downloaded attachment.
         *
         * @return the target file name
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Returns the configured options to be applied during the stream copy
         * operation.
         *
         * @return an array with {@code CopyOption} objects that are applied
         * when copying the attachment stream to disk
         */
        public CopyOption[] getCopyOptions() {
            return copyOptions.clone();
        }

        @Override
        public Path processAttachmentStream(InputStream stream) throws IOException {
            Path target = getTargetPath();
            LOGGER.info("Downloading attachment to {}.", target);
            long size = Files.copy(stream, target, copyOptions);
            LOGGER.debug("Downloaded {} bytes to {}", size, target);
            return target;
        }

        /**
         * Obtains the target path of the copy operation. A file is created at
         * this part, and the content of the attachment stream is copied into
         * it. This implementation generates the path from the download folder
         * and the file name configured. Non existing paths are not handled and
         * cause the download operation to fail.
         *
         * @return the path to the target file of the download operation
         * @throws IOException if an exception occurs
         */
        protected Path getTargetPath() throws IOException {
            return getDownloadPath().resolve(getFileName());
        }
    }

    /**
     * <p>
     * A specialized {@code AttachmentDownloadProcessor} that creates the
     * download folder if necessary.
     * </p>
     * <p>
     * Before storing the attachment file, this class checks whether the
     * configured download folder exists. If not, it is created now.
     * None-existing parent folders are not handled and cause the download
     * operation to fail.
     * </p>
     * <p>
     * An instance of this class is used for the default attachment download
     * operation supported by {@link SW360AttachmentUtils}.
     * </p>
     */
    public static class AttachmentDownloadProcessorCreateDownloadFolder extends AttachmentDownloadProcessor {
        /**
         * Creates a new instance of
         * {@code AttachmentDownloadProcessorCreateDownloadFolder} and
         * initializes it with all the properties required for a download
         * operation.
         *
         * @param downloadPath the path where to store the attachment
         * @param fileName     the file name to be used
         * @param options      an arbitrary number of {@code CopyOption} flags
         */
        public AttachmentDownloadProcessorCreateDownloadFolder(Path downloadPath, String fileName, CopyOption... options) {
            super(downloadPath, fileName, options);
        }

        @Override
        protected Path getTargetPath() throws IOException {
            safeCreateDirectory(getDownloadPath(), false);
            return super.getTargetPath();
        }
    }

    /**
     * <p>
     * A specialized {@code AttachmentDownloadProcessor} that creates the
     * download folder and all its parent directories if necessary.
     * </p>
     * <p>
     * While {@link AttachmentDownloadProcessorCreateDownloadFolder} only
     * creates the last component of the download path if it does not exist,
     * this class can create a whole folder structure. It tries to create all
     * the path components of the download path that do not exist yet.
     * </p>
     */
    public static class AttachmentDownloadProcessorCreateDownloadFolderWithParents
            extends AttachmentDownloadProcessor {

        /**
         * Creates a new instance of {@code AttachmentDownloadProcessor} and
         * initializes it with all the properties required for a download
         * operation.
         *
         * @param downloadPath the path where to store the attachment
         * @param fileName     the file name to be used
         * @param options      an arbitrary number of {@code CopyOption} flags
         */
        public AttachmentDownloadProcessorCreateDownloadFolderWithParents(Path downloadPath, String fileName,
                                                                          CopyOption... options) {
            super(downloadPath, fileName, options);
        }

        @Override
        protected Path getTargetPath() throws IOException {
            safeCreateDirectory(getDownloadPath(), true);
            return super.getTargetPath();
        }
    }
}
