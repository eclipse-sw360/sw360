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

import org.eclipse.sw360.antenna.sw360.client.rest.SW360ReleaseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SW360AttachmentUtilsTest {
    /**
     * A file used by test cases.
     */
    private static final String TEST_FILE = "/__files/all_releases.json";

    /**
     * Constant for the SHA-1 hash of the test file. The hash was calculated
     * manually using the Linux sha1sum command line tool.
     */
    private static final String TEST_FILE_SHA1 = "7a5daedffafd0be187c351968592fefee4f648f4";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Returns a path to the test file from the test resources.
     *
     * @return the path to the test file
     * @throws URISyntaxException if the file cannot be resolved
     */
    private static Path testFile() throws URISyntaxException {
        return Paths.get(SW360AttachmentUtilsTest.class.getResource(TEST_FILE).toURI());
    }

    /**
     * Tests whether an attachment processor has correctly downloaded the test
     * attachment file. This is done by comparing against the expected hash.
     *
     * @param target the path to the downloaded file
     */
    private static void checkFileDownloaded(Path target) {
        String hash = SW360AttachmentUtils.calculateSha1Hash(target);
        assertThat(hash).isEqualTo(TEST_FILE_SHA1);
    }

    @Test
    public void testCalculateHash() throws NoSuchAlgorithmException, URISyntaxException {
        // manually calculated using the Linux md5sum command line tool
        final String expectedMd5 = "457b4178ee4d1129603a1a6465c8e9c1";
        MessageDigest digest = MessageDigest.getInstance("md5");

        String hash = SW360AttachmentUtils.calculateHash(testFile(), digest);
        assertThat(hash).isEqualTo(expectedMd5);
    }

    @Test(expected = SW360ClientException.class)
    public void testCalculateHashException() throws NoSuchAlgorithmException {
        Path file = Paths.get("non/existing/file.txt");
        MessageDigest digest = MessageDigest.getInstance("md5");

        SW360AttachmentUtils.calculateHash(file, digest);
    }

    @Test
    public void testCalculateSha1Hash() throws URISyntaxException {
        checkFileDownloaded(testFile());
    }

    @Test
    public void testAttachmentDownloadProcessor() throws URISyntaxException, IOException {
        Path downloadPath = folder.getRoot().toPath();
        String fileName = "downloadedAttachment.dat";
        SW360AttachmentUtils.AttachmentDownloadProcessor downloadProcessor =
                new SW360AttachmentUtils.AttachmentDownloadProcessor(downloadPath, fileName);

        try (InputStream stream = Files.newInputStream(testFile())) {
            Path path = downloadProcessor.processAttachmentStream(stream);
            checkFileDownloaded(path);
        }
    }

    @Test(expected = IOException.class)
    public void testAttachmentDownloadProcessorExistingFile() throws IOException, URISyntaxException {
        String fileName = "existingAttachmentFile.loc";
        Path path = folder.newFile(fileName).toPath();
        assertThat(Files.exists(path)).isTrue();
        SW360AttachmentUtils.AttachmentDownloadProcessor downloadProcessor =
                new SW360AttachmentUtils.AttachmentDownloadProcessor(path.getParent(), fileName);

        try (InputStream stream = Files.newInputStream(testFile())) {
            downloadProcessor.processAttachmentStream(stream);
        }
    }

    @Test
    public void testAttachmentDownloadProcessorOverrideFile() throws IOException, URISyntaxException {
        String fileName = "alreadyExisting.doc";
        Path path = folder.newFile(fileName).toPath();
        assertThat(Files.exists(path)).isTrue();
        SW360AttachmentUtils.AttachmentDownloadProcessor downloadProcessor =
                new SW360AttachmentUtils.AttachmentDownloadProcessor(path.getParent(), fileName,
                        StandardCopyOption.REPLACE_EXISTING);

        try (InputStream stream = Files.newInputStream(testFile())) {
            Path target = downloadProcessor.processAttachmentStream(stream);
            checkFileDownloaded(target);
        }
    }

    @Test(expected = IOException.class)
    public void testAttachmentDownloadProcessorNonExistingFolder() throws URISyntaxException, IOException {
        Path downloadPath = folder.getRoot().toPath().resolve("nonExistingDownloadPath");
        SW360AttachmentUtils.AttachmentDownloadProcessor downloadProcessor =
                new SW360AttachmentUtils.AttachmentDownloadProcessor(downloadPath, "file.dat");

        try (InputStream stream = Files.newInputStream(testFile())) {
            downloadProcessor.processAttachmentStream(stream);
        }
    }

    @Test
    public void testDownloadAttachment() {
        String attachmentId = "attach-0123456789";
        String fileName = "downloadedAttachment.doc";
        Path downloadPath = folder.getRoot().toPath().resolve("downloads");
        String releaseLink = "https://sw360.org/releases/1234567890";
        SW360Release release = new SW360Release();
        release.getLinks().setSelf(new Self(releaseLink));
        SW360SparseAttachment attachment = new SW360SparseAttachment();
        attachment.setFilename(fileName);
        attachment.getLinks().setSelf(new Self("https://sw360.org/attachments/" + attachmentId));
        SW360ReleaseClient releaseClient = mock(SW360ReleaseClient.class);
        when(releaseClient.processAttachment(eq(releaseLink), eq(attachmentId), any()))
                .thenAnswer((Answer<CompletableFuture<Path>>) invocationOnMock -> {
                    SW360AttachmentUtils.AttachmentDownloadProcessor processor =
                            invocationOnMock.getArgument(2);
                    assertThat(processor.getCopyOptions()).containsOnly(StandardCopyOption.REPLACE_EXISTING);
                    try (InputStream stream = Files.newInputStream(testFile())) {
                        return CompletableFuture.completedFuture(processor.processAttachmentStream(stream));
                    }
                });

        Optional<Path> optTarget = FutureUtils.block(SW360AttachmentUtils.downloadAttachment(releaseClient,
                release, attachment, downloadPath));
        assertThat(optTarget).isNotEmpty();
        Path target = optTarget.get();
        assertThat(target).isEqualTo(downloadPath.resolve(fileName));
        checkFileDownloaded(target);
    }

    @Test
    public void testAttachmentDownloadProcessorCreateParentFolders() throws URISyntaxException, IOException {
        Path downloadPath = folder.getRoot().toPath().resolve("deeply/nested/download/path");
        String fileName = "target.doc";
        Path targetPath = downloadPath.resolve(fileName);
        SW360AttachmentUtils.AttachmentDownloadProcessor downloadProcessor =
                new SW360AttachmentUtils.AttachmentDownloadProcessorCreateDownloadFolderWithParents(downloadPath,
                        fileName);

        try (InputStream stream = Files.newInputStream(testFile())) {
            assertThat(downloadProcessor.processAttachmentStream(stream)).isEqualTo(targetPath);
            checkFileDownloaded(targetPath);
        }
    }

    @Test
    public void testSafeCreateDirectoryMultiThreaded() throws InterruptedException {
        final int threadCount = 16;
        final AtomicInteger errorCount = new AtomicInteger();
        final Path directory = folder.getRoot().toPath().resolve("this/is/the/directory/for/all/of/my/downloads");
        final CountDownLatch latchStart = new CountDownLatch(threadCount);
        final CountDownLatch latchStop = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                latchStart.countDown();
                try {
                    latchStart.await(10, TimeUnit.SECONDS);
                    Path result = SW360AttachmentUtils.safeCreateDirectory(directory, true);
                    if(!result.equals(directory)) {
                        errorCount.incrementAndGet();
                    }
                } catch (InterruptedException | IOException e) {
                    errorCount.incrementAndGet();
                }
                latchStop.countDown();
            }).start();
        }

        assertThat(latchStop.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(Files.isDirectory(directory)).isTrue();
        assertThat(errorCount.get()).isEqualTo(0);
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testSafeCreateDirectoryExistingFile() throws IOException {
        Path directory = Files.createDirectories(folder.getRoot().toPath()
                .resolve("this/is/the/directory/for/all/of/my/downloads"));
        Path file = Files.write(directory.resolve("foo"), "some data".getBytes(StandardCharsets.UTF_8));

        SW360AttachmentUtils.safeCreateDirectory(file, false);
    }
}
