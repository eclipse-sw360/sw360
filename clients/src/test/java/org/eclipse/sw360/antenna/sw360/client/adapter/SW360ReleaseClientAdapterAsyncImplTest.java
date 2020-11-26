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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360AttachmentAwareClient;
import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.rest.MultiStatusResponse;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360ReleaseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360ReleaseEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.block;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SW360ReleaseClientAdapterAsyncImplTest {
    private static final String RELEASE_DOWNLOAD_URL = "https://organisation-test.org/";
    private static final String RELEASE_CLEARING_STATE = "PROJECT_APPROVED";
    private static final String RELEASE_DECLEARED_LICENSE = "The-Test-License";
    private static final String RELEASE_OBSERVED_LICENSE = "A-Test-License";
    private static final String RELEASE_RELEASE_TAG_URL = "https://gitTool.com/project/repository";
    private static final String RELEASE_SOFTWAREHERITGAE_ID = "swh:1:rel:1234512345123451234512345123451234512345";
    private static final String RELEASE_HASH1 = "b2a4d4ae21c789b689dd162deb819665567f481c";
    private static final String RELEASE_CHANGESTATUS = "AS_IS";
    private static final String RELEASE_COPYRIGHT = "Copyright xxxx Some Copyright Enterprise";
    private static final String ID = "12345";
    private static final String RELEASE_VERSION1 = "1.0.0";
    private static final String RELEASE_HREF = "https://sw360.eclipse.org/api/releases/" + ID;


    private SW360ReleaseClientAdapterAsync releaseClientAdapter;

    private SW360ReleaseClient releaseClient;
    private SW360ComponentClientAdapterAsync componentClientAdapter;
    private SW360Release release;

    @Before
    public void setUp() throws MalformedPackageURLException {
        releaseClient = mock(SW360ReleaseClient.class);
        componentClientAdapter = mock(SW360ComponentClientAdapterAsync.class);
        release = mkSW360Release("releaseName");
        releaseClientAdapter = new SW360ReleaseClientAdapterAsyncImpl(releaseClient, componentClientAdapter);
    }

    @Test
    public void testCreateRelease() {
        SW360SparseRelease sparseRelease = new SW360SparseRelease()
                .setVersion(release.getVersion() + "-noMatch");
        String componentHref = "url/" + ID;
        Self componentSelf = new Self().setHref(componentHref);
        LinkObjects links = new LinkObjects()
                .setSelf(componentSelf);
        SW360Component component = getSw360Component(sparseRelease, "componentName");
        component.setLinks(links);

        when(componentClientAdapter.getComponentByName(release.getName()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(component)));
        when(releaseClient.createRelease(release))
                .thenReturn(CompletableFuture.completedFuture(release));

        SW360Release createdRelease = block(releaseClientAdapter.createRelease(this.release));

        assertThat(createdRelease).isEqualTo(release);
        verify(releaseClient).createRelease(release);
        assertThat(release.getComponentId()).isEqualTo(component.getId());
    }

    @Test
    public void testCreateReleaseInvalid() {
        release.setName("");

        try {
            block(releaseClientAdapter.createRelease(release));
            fail("Invalid release not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).contains("Cannot create release", release.getName());
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void testCreateReleaseWithID() {
        addSelfLink(release);

        CompletableFuture<SW360Release> futResult = releaseClientAdapter.createRelease(this.release);
        try {
            block(futResult);
            fail("Release ID not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).contains("Cannot create release", release.getName());
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void testCreateReleaseAlreadyPresent() {
        SW360SparseRelease sparseRelease = new SW360SparseRelease()
                .setVersion(release.getVersion());
        String componentHref = "url/" + ID;
        Self componentSelf = new Self().setHref(componentHref);
        LinkObjects links = new LinkObjects()
                .setSelf(componentSelf);
        SW360Component component = getSw360Component(sparseRelease, "componentName");
        component.setLinks(links);
        when(componentClientAdapter.getComponentByName(release.getName()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(component)));

        try {
            block(releaseClientAdapter.createRelease(release));
            fail("Existing release not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).contains("release already exists");
        }
    }

    @Test
    public void testCreateReleaseNewComponent() {
        SW360Component componentForRelease = SW360ComponentAdapterUtils.createFromRelease(release);
        SW360Component componentCreated = new SW360Component();
        componentCreated.setName(release.getName());
        componentCreated.getLinks().setSelf(new Self("https://components.org/" + ID));
        when(componentClientAdapter.getComponentByName(release.getName()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(componentClientAdapter.createComponent(componentForRelease))
                .thenReturn(CompletableFuture.completedFuture(componentCreated));
        SW360Release releaseCreated = new SW360Release();
        releaseCreated.setName(release.getName() + "_new");
        when(releaseClient.createRelease(release))
                .thenAnswer(invocationOnMock -> {
                    SW360Release r = invocationOnMock.getArgument(0);
                    assertThat(r.getComponentId()).isEqualTo(ID);
                    return CompletableFuture.completedFuture(releaseCreated);
                });

        SW360Release result = block(releaseClientAdapter.createRelease(release));
        assertThat(result).isEqualTo(releaseCreated);
    }

    private static SW360SparseAttachment createAttachment(String file) {
        SW360SparseAttachment attachment = new SW360SparseAttachment();
        attachment.setFilename(file);
        return attachment;
    }

    private static SW360Release createReleaseWithAttachments(String... files) {
        SW360Release release = new SW360Release();
        release.setEmbedded(createEmbeddedReleaseWithAttachments(files));
        return release;
    }

    private static SW360ReleaseEmbedded createEmbeddedReleaseWithAttachments(String... files) {
        SW360ReleaseEmbedded releaseEmbedded = new SW360ReleaseEmbedded();
        Set<SW360SparseAttachment> attachmentSet = Arrays.stream(files)
                .map(SW360ReleaseClientAdapterAsyncImplTest::createAttachment)
                .collect(Collectors.toSet());
        releaseEmbedded.setAttachments(attachmentSet);
        return releaseEmbedded;
    }

    @Test
    public void testUploadAttachmentsSuccess() {
        Path uploadPath1 = Paths.get("file1.doc");
        SW360AttachmentType attachmentType1 = SW360AttachmentType.DOCUMENT;
        Path uploadPath2 = Paths.get("sources.zip");
        SW360AttachmentType attachmentType2 = SW360AttachmentType.SOURCE;
        release.setEmbedded(createEmbeddedReleaseWithAttachments());
        SW360Release updatedRelease1 = createReleaseWithAttachments("attach1");
        SW360Release updatedRelease2 = createReleaseWithAttachments("attach1", "attach2");

        AttachmentUploadRequest<SW360Release> uploadRequest = AttachmentUploadRequest.builder(release)
                .addAttachment(uploadPath1, attachmentType1)
                .addAttachment(uploadPath2, attachmentType2)
                .build();
        when(releaseClient.uploadAndAttachAttachment(release, uploadPath1, attachmentType1))
                .thenReturn(CompletableFuture.completedFuture(updatedRelease1));
        when(releaseClient.uploadAndAttachAttachment(updatedRelease1, uploadPath2, attachmentType2))
                .thenReturn(CompletableFuture.completedFuture(updatedRelease2));

        AttachmentUploadResult<SW360Release> result = block(releaseClientAdapter.uploadAttachments(uploadRequest));

        assertThat(result.getTarget()).isEqualTo(updatedRelease2);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.failedUploads()).isEmpty();
        assertThat(result.successfulUploads()).contains(new AttachmentUploadRequest.Item(uploadPath1, attachmentType1),
                new AttachmentUploadRequest.Item(uploadPath2, attachmentType2));
    }

    @Test
    public void testUploadAttachmentsWithFailures() {
        Path uploadPath1 = Paths.get("failedUpload1.err");
        SW360AttachmentType attachmentType1 = SW360AttachmentType.DESIGN;
        Throwable failure1 = new IOException("I/O exception during upload");
        Path uploadPath2 = Paths.get("failedUpload2.exc");
        SW360AttachmentType attachmentType2 = SW360AttachmentType.BINARY_SELF;
        Throwable failure2 = new SW360ClientException("Forbidden upload");
        Path uploadPath3 = Paths.get("success.yes");
        SW360AttachmentType attachmentType3 = SW360AttachmentType.CLEARING_REPORT;
        release.setEmbedded(createEmbeddedReleaseWithAttachments());
        SW360Release updatedRelease = createReleaseWithAttachments("attach1");

        AttachmentUploadRequest<SW360Release> uploadRequest = AttachmentUploadRequest.builder(release)
                .addAttachment(uploadPath1, attachmentType1)
                .addAttachment(uploadPath2, attachmentType2)
                .addAttachment(uploadPath3, attachmentType3)
                .build();
        when(releaseClient.uploadAndAttachAttachment(release, uploadPath1, attachmentType1))
                .thenReturn(FutureUtils.failedFuture(failure1));
        when(releaseClient.uploadAndAttachAttachment(release, uploadPath2, attachmentType2))
                .thenReturn(FutureUtils.failedFuture(failure2));
        when(releaseClient.uploadAndAttachAttachment(release, uploadPath3, attachmentType3))
                .thenReturn(CompletableFuture.completedFuture(updatedRelease));

        AttachmentUploadResult<SW360Release> result = block(releaseClientAdapter.uploadAttachments(uploadRequest));
        assertThat(result.getTarget()).isEqualTo(updatedRelease);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.successfulUploads())
                .containsOnly(new AttachmentUploadRequest.Item(uploadPath3, attachmentType3));
        assertThat(result.failedUploads()).hasSize(2);
        assertThat(result.failedUploads().get(new AttachmentUploadRequest.Item(uploadPath1, attachmentType1)))
                .isEqualTo(failure1);
        assertThat(result.failedUploads().get(new AttachmentUploadRequest.Item(uploadPath2, attachmentType2)))
                .isEqualTo(failure2);
    }

    @Test
    public void testUploadAttachmentsDuplicate() {
        String fileName = "alreadyExistingAttachment.1st";
        Path uploadPath = Paths.get(fileName);
        SW360AttachmentType attachmentType = SW360AttachmentType.LEGAL_EVALUATION;
        release.setEmbedded(createEmbeddedReleaseWithAttachments(fileName));
        AttachmentUploadRequest<SW360Release> uploadRequest = AttachmentUploadRequest.builder(release)
                .addAttachment(uploadPath, attachmentType)
                .build();

        AttachmentUploadResult<SW360Release> result = block(releaseClientAdapter.uploadAttachments(uploadRequest));
        assertThat(result.getTarget()).isEqualTo(release);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.failedUploads().get(new AttachmentUploadRequest.Item(uploadPath, attachmentType)))
                .isInstanceOf(SW360ClientException.class);
        verifyZeroInteractions(releaseClient);
    }

    @Test
    public void testGetReleaseByExternalIds() {
        SW360SparseRelease sparseRelease = new SW360SparseRelease();
        Map<String, String> externalIds = new HashMap<>();

        when(releaseClient.getReleasesByExternalIds(externalIds))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(sparseRelease)));

        Optional<SW360SparseRelease> releaseByExternalIds =
                block(releaseClientAdapter.getSparseReleaseByExternalIds(externalIds));

        assertThat(releaseByExternalIds).isPresent();
        assertThat(releaseByExternalIds).hasValue(sparseRelease);
        verify(releaseClient).getReleasesByExternalIds(externalIds);
    }

    @Test
    public void testGetReleaseByExternalIdsNotFound() {
        Map<String, String> externalIds = new HashMap<>();
        when(releaseClient.getReleasesByExternalIds(externalIds))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        Optional<SW360SparseRelease> releaseByExternalIds =
                block(releaseClientAdapter.getSparseReleaseByExternalIds(externalIds));
        assertThat(releaseByExternalIds).isEmpty();
    }

    @Test
    public void testGetReleaseByExternalIdsMultiple() {
        Map<String, String> externalIds = new HashMap<>();
        when(releaseClient.getReleasesByExternalIds(externalIds))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        new SW360SparseRelease(), new SW360SparseRelease())));

        try {
            block(releaseClientAdapter.getSparseReleaseByExternalIds(externalIds));
            fail("Multiple results not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).contains("Multiple releases");
        }
    }

    @Test
    public void testGetReleaseByNameAndVersion() {
        SW360SparseRelease sparseRelease = new SW360SparseRelease()
                .setVersion(RELEASE_VERSION1);
        String componentName = "componentName";
        SW360Component component = getSw360Component(sparseRelease, componentName);
        when(componentClientAdapter.getComponentByName(release.getName()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(component)));

        Optional<SW360SparseRelease> releaseByNameAndVersion =
                block(releaseClientAdapter.getSparseReleaseByNameAndVersion(release.getName(), release.getVersion()));

        assertThat(releaseByNameAndVersion).isPresent();
        assertThat(releaseByNameAndVersion).hasValue(sparseRelease);
    }

    private static SW360Component getSw360Component(SW360SparseRelease sparseRelease, String componentName) {
        SW360ComponentEmbedded embedded = new SW360ComponentEmbedded();
        embedded.setReleases(Collections.singletonList(sparseRelease));
        SW360Component component = new SW360Component()
                .setName(componentName);
        component.setEmbedded(embedded);
        return component;
    }

    @Test
    public void testGetReleaseByVersion() {
        SW360SparseRelease sparseRelease = new SW360SparseRelease()
                .setVersion(RELEASE_VERSION1);
        String releaseHref = "url/" + ID;
        Self releaseSelf = new Self().setHref(releaseHref);
        LinkObjects links = new LinkObjects();
        links.setSelf(releaseSelf);
        sparseRelease.setLinks(links);
        String componentName = "componentName";
        SW360Component component = getSw360Component(sparseRelease, componentName);
        when(releaseClient.getRelease(ID))
                .thenReturn(CompletableFuture.completedFuture(release));

        final Optional<SW360Release> releaseByVersion =
                block(releaseClientAdapter.getReleaseByVersion(component, release.getVersion()));

        assertThat(releaseByVersion).isPresent();
        assertThat(releaseByVersion).hasValue(release);
        verify(releaseClient, times(1)).getRelease(ID);
    }

    @Test
    public void testGetReleaseByVersionNoComponent() {
        Optional<SW360Release> optRelease = block(releaseClientAdapter.getReleaseByVersion(null, RELEASE_VERSION1));

        assertThat(optRelease).isEmpty();
    }

    @Test
    public void testDownloadAttachment() {
        addSelfLink(release);

        String attachmentId = "attach-9383764983";
        String fileName = "theAttachmentFile.tst";
        SW360SparseAttachment sparseAttachment = new SW360SparseAttachment();
        sparseAttachment.getLinks().setSelf(new Self("https://attachments.org/attachments/" + attachmentId));
        sparseAttachment.setFilename(fileName);

        Path orgPath = Paths.get("download");
        Path resultPath = Paths.get("downloadResult");

        when(releaseClient.processAttachment(eq(RELEASE_HREF), eq(attachmentId), any()))
                .thenReturn(CompletableFuture.completedFuture(resultPath));

        Optional<Path> downloadPath = block(releaseClientAdapter.downloadAttachment(release, sparseAttachment, orgPath));

        assertThat(downloadPath).isPresent();
        assertThat(downloadPath).hasValue(resultPath);

        ArgumentCaptor<SW360AttachmentUtils.AttachmentDownloadProcessorCreateDownloadFolder> captor =
                ArgumentCaptor.forClass(SW360AttachmentUtils.AttachmentDownloadProcessorCreateDownloadFolder.class);
        verify(releaseClient).processAttachment(eq(RELEASE_HREF), eq(attachmentId), captor.capture());
        SW360AttachmentUtils.AttachmentDownloadProcessorCreateDownloadFolder processor = captor.getValue();
        assertThat(processor.getDownloadPath()).isEqualTo(orgPath);
        assertThat(processor.getFileName()).isEqualTo(fileName);
        assertThat(processor.getCopyOptions()).contains(StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testProcessAttachment() {
        final String attachmentId = "attach-test-id";
        final Integer result = 42;
        @SuppressWarnings("unchecked")
        SW360AttachmentAwareClient.AttachmentProcessor<Integer> processor =
                mock(SW360AttachmentAwareClient.AttachmentProcessor.class);
        addSelfLink(release);
        when(releaseClient.processAttachment(RELEASE_HREF, attachmentId, processor))
                .thenReturn(CompletableFuture.completedFuture(result));

        Integer processResult = block(releaseClientAdapter.processAttachment(release, attachmentId, processor));
        assertThat(processResult).isEqualTo(result);
    }

    @Test
    public void testProcessAttachmentNoReleaseId() {
        SW360Release undefinedRelease = new SW360Release();
        SW360AttachmentUtils.AttachmentDownloadProcessor processor =
                SW360AttachmentUtils.defaultAttachmentDownloadProcessor("test.doc",
                        Paths.get("irrelevant", "path"));

        try {
            block(releaseClientAdapter.processAttachment(undefinedRelease, "1234", processor));
            fail("No exception thrown");
        } catch (SW360ClientException e) {
            assertThat(e.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(e.getMessage()).contains("no ID");
        }
    }

    @Test
    public void testDeleteAttachments() {
        SW360Release updatedRelease = mock(SW360Release.class);
        Collection<String> attachmentIds = Arrays.asList("at1", "at2", "atMore");
        when(releaseClient.deleteAttachments(release, attachmentIds))
                .thenReturn(CompletableFuture.completedFuture(updatedRelease));

        SW360Release result = block(releaseClientAdapter.deleteAttachments(release, attachmentIds));
        assertThat(result).isEqualTo(updatedRelease);
    }

    @Test
    public void testDeleteAttachmentsEmptyList() {
        SW360Release result = block(releaseClientAdapter.deleteAttachments(release, Collections.emptyList()));

        assertThat(result).isSameAs(release);
    }

    @Test
    public void testUpdateRelease() throws MalformedPackageURLException {
        SW360Release updatedRelease = mkSW360Release("updatedRelease");
        release.getLinks().setSelf(new Self("https://releases.org/" + ID));
        when(releaseClient.patchRelease(release)).thenReturn(CompletableFuture.completedFuture(updatedRelease));

        assertThat(block(releaseClientAdapter.updateRelease(release))).isEqualTo(updatedRelease);
    }

    @Test
    public void testUpdateReleaseInvalid() throws MalformedPackageURLException {
        SW360Release updatedRelease = mkSW360Release("updatedRelease");
        updatedRelease.setVersion("");

        CompletableFuture<SW360Release> futUpdate = releaseClientAdapter.updateRelease(updatedRelease);
        try {
            block(futUpdate);
            fail("Invalid release not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).contains("Cannot update release", updatedRelease.getName());
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void testUpdateReleaseNoId() {
        SW360Release updateRelease = new SW360Release();
        updateRelease.setName("newRelease")
                .setVersion(RELEASE_VERSION1);

        CompletableFuture<SW360Release> futUpdate = releaseClientAdapter.updateRelease(updateRelease);
        try {
            block(futUpdate);
            fail("Invalid release not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getMessage()).contains("Cannot update release", updateRelease.getName());
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void testDeleteReleases() {
        Collection<String> idsToDelete = Arrays.asList(ID, "otherReleaseToDelete");
        MultiStatusResponse response = new MultiStatusResponse(Collections.singletonMap(ID, HttpConstants.STATUS_OK));
        when(releaseClient.deleteReleases(idsToDelete))
                .thenReturn(CompletableFuture.completedFuture(response));

        MultiStatusResponse result = block(releaseClientAdapter.deleteReleases(idsToDelete));
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testDeleteSingleReleaseSuccess() {
        MultiStatusResponse response = new MultiStatusResponse(Collections.singletonMap(ID, HttpConstants.STATUS_OK));
        Set<String> idsToDelete = Collections.singleton(ID);
        when(releaseClient.deleteReleases(idsToDelete))
                .thenReturn(CompletableFuture.completedFuture(response));

        block(releaseClientAdapter.deleteRelease(ID));
        verify(releaseClient).deleteReleases(idsToDelete);

    }

    @Test
    public void testDeleteSingleReleaseFailure() {
        MultiStatusResponse response =
                new MultiStatusResponse(Collections.singletonMap(ID, HttpConstants.STATUS_ERR_NOT_FOUND));
        when(releaseClient.deleteReleases(Collections.singleton(ID)))
                .thenReturn(CompletableFuture.completedFuture(response));

        try {
            block(releaseClientAdapter.deleteRelease(ID));
            fail("Failure response not detected");
        } catch (SW360ClientException e) {
            assertThat(e.getCause()).isInstanceOf(FailedRequestException.class);
            FailedRequestException reqEx = (FailedRequestException) e.getCause();
            assertThat(reqEx.getStatusCode()).isEqualTo(HttpConstants.STATUS_ERR_NOT_FOUND);
            assertThat(reqEx.getTag()).isEqualTo("delete release " + ID);
        }
    }

    private static SW360Release mkSW360Release(String name) throws MalformedPackageURLException {
        SW360Release sw360Release = new SW360Release();

        sw360Release.setVersion(RELEASE_VERSION1);

        sw360Release.setDownloadurl(RELEASE_DOWNLOAD_URL);
        sw360Release.setClearingState(RELEASE_CLEARING_STATE);

        sw360Release.setDeclaredLicense(RELEASE_DECLEARED_LICENSE);
        sw360Release.setObservedLicense(RELEASE_OBSERVED_LICENSE);
        PackageURL packageURL = new PackageURL(PackageURL.StandardTypes.MAVEN, "org.group.id", name, RELEASE_VERSION1, null, null);
        sw360Release.setCoordinates(Collections.singletonMap(PackageURL.StandardTypes.MAVEN,
                packageURL.toString()));
        sw360Release.setReleaseTagUrl(RELEASE_RELEASE_TAG_URL);
        sw360Release.setSoftwareHeritageId(RELEASE_SOFTWAREHERITGAE_ID);
        sw360Release.setHashes(Collections.singleton(RELEASE_HASH1));
        sw360Release.setChangeStatus(RELEASE_CHANGESTATUS);
        sw360Release.setCopyrights(RELEASE_COPYRIGHT);
        sw360Release.setName(String.join("/", packageURL.getNamespace(), packageURL.getName()));

        return sw360Release;
    }

    private static void addSelfLink(SW360Release release) {
        release.getLinks().setSelf(new Self(RELEASE_HREF));
    }
}
