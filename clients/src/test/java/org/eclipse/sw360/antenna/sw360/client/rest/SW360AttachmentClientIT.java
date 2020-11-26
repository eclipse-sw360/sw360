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
package org.eclipse.sw360.antenna.sw360.client.rest;

import org.apache.commons.io.IOUtils;
import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360Attachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.antenna.http.utils.HttpUtils.waitFor;

public class SW360AttachmentClientIT extends AbstractMockServerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private SW360ReleaseClient attachmentClient;

    @Before
    public void setUp() {
        attachmentClient = new SW360ReleaseClient(createClientConfig(), createMockTokenProvider());
        prepareAccessTokens(attachmentClient.getTokenProvider(), CompletableFuture.completedFuture(ACCESS_TOKEN));
    }

    /**
     * Returns an {@code AttachmentProcessor} that does not expect to be
     * invoked.
     *
     * @return the dummy processor
     */
    private static SW360AttachmentAwareClient.AttachmentProcessor<Void> dummyAttachmentProcessor() {
        return stream -> {
            throw new UnsupportedOperationException("Unexpected invocation");
        };
    }

    @Test
    public void testUploadAttachment() throws URISyntaxException, IOException {
        String urlPath = "/releases/rel1234567890";
        String selfUrl = "https://some.uri.to.be.replaced" + urlPath;
        Path attachmentPath = Paths.get(resolveTestFileURL("license.json").toURI());
        byte[] attachmentContent = Files.readAllBytes(attachmentPath);
        SW360Attachment attachment = new SW360Attachment(attachmentPath, SW360AttachmentType.DOCUMENT);
        SW360Release release = new SW360Release();
        release.getLinks().setSelf(new Self(selfUrl));

        wireMockRule.stubFor(post(urlPathEqualTo(urlPath + "/attachments"))
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("attachment")
                                .withHeader("Content-Type", containing("application/json"))
                                .withBody(equalToJson(toJson(attachment)))
                )
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("file")
                                .withBody(binaryEqualTo(attachmentContent))
                ).willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBody(toJson(release))));

        SW360Release modifiedRelease =
                waitFor(attachmentClient.uploadAndAttachAttachment(release, attachmentPath,
                        SW360AttachmentType.DOCUMENT));
        assertThat(modifiedRelease).isEqualTo(release);
        wireMockRule.verify(postRequestedFor(urlPathEqualTo(urlPath + "/attachments")));
    }

    @Test
    public void testUploadAttachmentNonExistingFile() {
        Path attachmentPath = temporaryFolder.getRoot().toPath().resolve("nonExistingFile.txt");
        SW360Release release = new SW360Release();

        extractException(attachmentClient.uploadAndAttachAttachment(release, attachmentPath,
                SW360AttachmentType.DOCUMENT), IOException.class);
        assertThat(wireMockRule.getAllServeEvents()).hasSize(0);
    }

    @Test
    public void testUploadAttachmentError() throws URISyntaxException {
        String urlPath = "/releases/rel1234567890";
        String selfUrl = wireMockRule.baseUrl() + urlPath;
        Path attachmentPath = Paths.get(resolveTestFileURL("license.json").toURI());
        SW360Release release = new SW360Release();
        release.getLinks().setSelf(new Self(selfUrl));
        wireMockRule.stubFor(post(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_BAD_REQUEST)));

        FailedRequestException exception = expectFailedRequest(
                attachmentClient.uploadAndAttachAttachment(release, attachmentPath, SW360AttachmentType.DOCUMENT),
                HttpConstants.STATUS_ERR_BAD_REQUEST);
        assertThat(exception.getTag()).isEqualTo(SW360AttachmentAwareClient.TAG_UPLOAD_ATTACHMENT);
    }

    @Test
    public void testProcessAttachment() throws IOException {
        final String attachmentID = "test-attachment-id";
        final String itemRef = "/testComponent";
        final String testFile = "project.json";
        SW360Project expData = readTestJsonFile(resolveTestFileURL(testFile), SW360Project.class);
        SW360AttachmentAwareClient.AttachmentProcessor<SW360Project> processor = stream ->
                objectMapper.readValue(stream, SW360Project.class);
        wireMockRule.stubFor(get(urlPathEqualTo(itemRef + "/attachments/" + attachmentID))
                .withHeader(HttpConstants.HEADER_ACCEPT, equalTo(HttpConstants.CONTENT_OCTET_STREAM))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBodyFile(testFile)));

        SW360Project actResult =
                waitFor(attachmentClient.processAttachment("https://host.to.be.ignored" + itemRef,
                        attachmentID, processor));
        assertThat(actResult).isEqualTo(expData);
    }

    @Test
    public void testProcessAttachmentNotFound() {
        final String attachmentId = "non-existing-attachment";
        SW360AttachmentAwareClient.AttachmentProcessor<Void> processor = dummyAttachmentProcessor();
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_ERR_NOT_FOUND)));

        FailedRequestException exception =
                expectFailedRequest(attachmentClient.processAttachment(wireMockRule.baseUrl(), attachmentId,
                        processor),
                        HttpConstants.STATUS_ERR_NOT_FOUND);
        assertThat(exception.getTag()).isEqualTo(SW360AttachmentAwareClient.TAG_DOWNLOAD_ATTACHMENT);
    }

    @Test
    public void testProcessAttachmentNoContent() throws IOException {
        final String attachmentId = "no-content-attachment";
        SW360AttachmentAwareClient.AttachmentProcessor<byte[]> processor = stream -> {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(stream, bos);
            return bos.toByteArray();
        };
        wireMockRule.stubFor(get(anyUrl())
                .willReturn(aResponse().withStatus(HttpConstants.STATUS_OK)));

        byte[] data = waitFor(attachmentClient.processAttachment(wireMockRule.baseUrl(), attachmentId,
                processor));
        assertThat(data.length).isEqualTo(0);
    }

    @Test
    public void testDeleteAttachments() throws IOException {
        String urlPath = "/releases/rel1234567890";
        String selfUrl = "https://some.uri.to.be.replaced" + urlPath;
        SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        release.getLinks().setSelf(new Self(selfUrl));
        SW360Release releaseUpdated = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        releaseUpdated.getEmbedded().setAttachments(null);
        String attachId1 = "at-first-to-delete";
        String attachId2 = "at-second-to-delete";
        wireMockRule.stubFor(delete(urlPathEqualTo(urlPath + "/attachments/" + attachId1 + "," + attachId2))
                .willReturn(aJsonResponse(HttpConstants.STATUS_OK)
                        .withBody(toJson(releaseUpdated))));

        SW360Release result = waitFor(attachmentClient.deleteAttachments(release, Arrays.asList(attachId1, attachId2)));
        assertThat(result).isEqualTo(releaseUpdated);
    }

    @Test
    public void testDeleteAttachmentsError() throws IOException {
        SW360Release release = readTestJsonFile(resolveTestFileURL("release.json"), SW360Release.class);
        wireMockRule.stubFor(delete(anyUrl())
                .willReturn(aJsonResponse(HttpConstants.STATUS_ERR_NOT_FOUND)));

        FailedRequestException exception =
                expectFailedRequest(attachmentClient.deleteAttachments(release, Collections.singleton("foo")),
                        HttpConstants.STATUS_ERR_NOT_FOUND);
        assertThat(exception.getTag()).isEqualTo(SW360AttachmentAwareClient.TAG_DELETE_ATTACHMENT);
    }
}
