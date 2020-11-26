/*
 * Copyright (c) Bosch Software Innovations GmbH 2018-2019.
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

import org.eclipse.sw360.antenna.sw360.client.rest.MultiStatusResponse;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360AttachmentAwareClient;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360ReleaseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResourceUtility;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.optionalFuture;

/**
 * Adapter implementation for the SW360 releases endpoint.
 */
class SW360ReleaseClientAdapterAsyncImpl implements SW360ReleaseClientAdapterAsync {
    private final SW360ReleaseClient releaseClient;
    private final SW360ComponentClientAdapterAsync sw360ComponentClientAdapter;

    public SW360ReleaseClientAdapterAsyncImpl(SW360ReleaseClient client,
                                              SW360ComponentClientAdapterAsync componentClientAdapter) {
        releaseClient = client;
        sw360ComponentClientAdapter = componentClientAdapter;
    }

    @Override
    public SW360ReleaseClient getReleaseClient() {
        return releaseClient;
    }

    public SW360ComponentClientAdapterAsync getComponentAdapter() {
        return sw360ComponentClientAdapter;
    }

    @Override
    public CompletableFuture<SW360Release> createRelease(SW360Release release) {
        return FutureUtils.wrapInFuture(() -> {
            if (release.getId() != null) {
                throw new IllegalArgumentException("Release already has the id " + release.getId());
            }
            return SW360ReleaseAdapterUtils.validateRelease(release);
        }, "Cannot create release for " + release.getName())
                .thenCompose(this::assignReleaseToComponent)
                .thenCompose(getReleaseClient()::createRelease);
    }

    /**
     * Obtains the component a new release belongs to and establishes the link.
     * It is also checked whether the release is not already present for this
     * component.
     *
     * @param release the release to be created
     * @return a future with the updated and validated release
     */
    private CompletableFuture<SW360Release> assignReleaseToComponent(SW360Release release) {
        final SW360Component componentFromRelease = SW360ComponentAdapterUtils.createFromRelease(release);
        return fetchComponentToAssign(componentFromRelease)
                .thenApply(cta -> {
                    if (containsRelease(cta, release)) {
                        throw new SW360ClientException("The release already exists in the found component");
                    }
                    release.setComponentId(cta.getId());
                    return release;
                });
    }

    /**
     * Obtains the component to assign to a release. The component is looked up
     * by the release name. If it does not exist yet, it is newly created.
     *
     * @param component the component created based on the release
     * @return the component to assign to a new release
     */
    private CompletableFuture<SW360Component> fetchComponentToAssign(SW360Component component) {
        return getComponentAdapter().getComponentByName(component.getName())
                .thenCompose(optExistingComponent ->
                        optExistingComponent.map(CompletableFuture::completedFuture)
                                .orElseGet(() -> getComponentAdapter().createComponent(component))
                );
    }

    /**
     * Checks whether a component has a release matching the given one.
     *
     * @param component the component in question
     * @param release   the release to check for
     * @return a flag whether this release exists
     */
    private static boolean containsRelease(SW360Component component, SW360Release release) {
        return component.getEmbedded().getReleases().stream()
                .map(SW360SparseRelease::getVersion)
                .anyMatch(release.getVersion()::equals);
    }

    @Override
    public CompletableFuture<AttachmentUploadResult<SW360Release>>
    uploadAttachments(AttachmentUploadRequest<SW360Release> uploadRequest) {
        return SW360AttachmentUtils.uploadAttachments(getReleaseClient(), uploadRequest,
                release -> release.getEmbedded().getAttachments());
    }

    @Override
    public CompletableFuture<Optional<SW360Release>> getReleaseById(String releaseId) {
        return optionalFuture(getReleaseClient().getRelease(releaseId));
    }

    @Override
    public CompletableFuture<Optional<SW360Release>> enrichSparseRelease(SW360SparseRelease sparseRelease) {
        return getReleaseById(sparseRelease.getReleaseId());
    }

    @Override
    public CompletableFuture<Optional<SW360SparseRelease>> getSparseReleaseByExternalIds(Map<String, ?> externalIds) {
        return getReleaseClient().getReleasesByExternalIds(externalIds)
                .thenApply(releases -> {
                    if (releases.isEmpty()) {
                        return Optional.empty();
                    } else if (releases.size() == 1) {
                        return Optional.of(releases.get(0));
                    } else {
                        throw new SW360ClientException("Multiple releases in SW360 matched by externalIDs: " +
                                externalIds);
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<SW360SparseRelease>> getSparseReleaseByNameAndVersion(String componentName,
                                                                                            String version) {
        return getComponentAdapter().getComponentByName(componentName)
                .thenApply(optComponent ->
                        optComponent.map(SW360Component::getEmbedded)
                                .map(SW360ComponentEmbedded::getReleases)
                                .flatMap(releases -> releases.stream()
                                        .filter(rel -> version.equals(rel.getVersion()))
                                        .findFirst()));

    }

    @Override
    public CompletableFuture<Optional<SW360Release>> getReleaseByVersion(SW360Component component, String releaseVersion) {
        if (component != null &&
                component.getEmbedded() != null &&
                component.getEmbedded().getReleases() != null) {

            List<SW360SparseRelease> releases = component.getEmbedded().getReleases();
            Optional<String> releaseId = releases.stream()
                    .filter(release -> release.getVersion().equals(releaseVersion))
                    .findFirst()
                    .flatMap(release -> SW360HalResourceUtility.getLastIndexOfSelfLink(release.getLinks()));
            if (releaseId.isPresent()) {
                return getReleaseById(releaseId.get());
            }
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<Path>> downloadAttachment(SW360Release release, SW360SparseAttachment attachment,
                                                                Path downloadPath) {
        return SW360AttachmentUtils.downloadAttachment(getReleaseClient(), release, attachment, downloadPath);
    }

    @Override
    public <T> CompletableFuture<T> processAttachment(SW360Release release, String attachmentId, SW360AttachmentAwareClient.AttachmentProcessor<? extends T> processor) {
        return FutureUtils.wrapInFuture(() -> release.getSelfLink().getHref(), "Release has no ID")
                .thenCompose(href -> getReleaseClient().processAttachment(href, attachmentId, processor));
    }

    @Override
    public CompletableFuture<SW360Release> deleteAttachments(SW360Release release, Collection<String> attachmentIds) {
        return attachmentIds.isEmpty() ? CompletableFuture.completedFuture(release) :
                getReleaseClient().deleteAttachments(release, attachmentIds);
    }

    @Override
    public CompletableFuture<SW360Release> updateRelease(SW360Release release) {
        return FutureUtils.wrapInFuture(() -> {
            if (release.getId() == null) {
                throw new IllegalArgumentException("Cannot update release without ID");
            }
            return SW360ReleaseAdapterUtils.validateRelease(release);
        }, "Cannot update release for " + release.getName())
                .thenCompose(getReleaseClient()::patchRelease);
    }

    @Override
    public CompletableFuture<MultiStatusResponse> deleteReleases(Collection<String> idsToDelete) {
        return SW360DeleteUtils.deleteEntities(getReleaseClient()::deleteReleases, idsToDelete);
    }

    @Override
    public CompletableFuture<Void> deleteRelease(String releaseId) {
        return SW360DeleteUtils.deleteEntity(getReleaseClient()::deleteReleases, releaseId, "release");
    }
}
