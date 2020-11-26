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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResourceUtility;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SW360Release extends SW360HalResource<SW360ReleaseLinkObjects, SW360ReleaseEmbedded> {

    private static final String OVERRIDDEN_LICENSES_KEY = "overridden_license";
    private static final String DECLARED_LICENSE_KEY = "declared_license";
    private static final String OBSERVED_LICENSES_KEY = "observed_license";
    private static final String RELEASE_TAG_URL_KEY = "release_tag";
    private static final String SOFTWARE_HERITAGE_ID_KEY = "swh";
    private static final String HASHES_PREFIX = "hash_";
    private static final String CHANGESTATUS_KEY = "change_status";
    private static final String COPYRIGHTS_KEY = "copyrights";
    private static final String CLEARINGSTATE_KEY = "clearingState";
    private static final String HOMEPAGE_KEY = "homepage";

    @JsonIgnore
    private boolean isProprietary;
    private String name;
    private String version;
    private String createdOn;
    private String cpeId;
    private String downloadurl;
    private SW360ClearingState sw360ClearingState;
    private final Map<String, String> externalIds = new HashMap<>();
    @JsonSerialize
    private final Map<String, String> additionalData = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getComponentId() {
        return Optional.ofNullable(getLinks())
                .map(SW360ReleaseLinkObjects::getSelfComponent)
                .flatMap(SW360HalResourceUtility::getLastIndexOfSelfLink)
                .orElse(null);
    }

    public SW360Release setComponentId(String componentId) {
        getLinks().setSelfComponent(new Self(componentId));
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return name;
    }

    public SW360Release setName(String name) {
        this.name = name;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getVersion() {
        return version;
    }

    public SW360Release setVersion(String version) {
        this.version = version;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCreatedOn() {
        return createdOn;
    }

    public SW360Release setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCpeId() {
        return cpeId;
    }

    public SW360Release setCpeId(String cpeId) {
        this.cpeId = cpeId;
        return this;
    }

    @JsonIgnore
    public boolean isSetMainLicenseIds() {
        return !getEmbedded().getLicenses().isEmpty();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> getMainLicenseIds() {
        return Optional.ofNullable(getEmbedded().getLicenses())
                .map(lics -> lics
                        .stream()
                        .filter(Objects::nonNull)
                        .map(SW360SparseLicense::getShortName)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        }

    public SW360Release setMainLicenseIds(Set<String> mainLicenseIds) {
        if (!mainLicenseIds.isEmpty()) {
            List<SW360SparseLicense> licenses = mainLicenseIds.stream()
                    .map(licenseId -> new SW360SparseLicense()
                            .setShortName(licenseId))
                    .collect(Collectors.toList());
            getEmbedded().setLicenses(licenses);
        }
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDownloadurl() {
        return downloadurl;
    }

    public void setDownloadurl(String downloadurl) {
        this.downloadurl = downloadurl;
    }

    @JsonIgnore
    public Map<String, String> getCoordinates() {
        return externalIds.entrySet().stream()
                .filter(e -> e.getValue().startsWith("pkg:"))
                .filter(e -> isValidPkgUrl(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public SW360Release setCoordinates(Map<String, String> coordinates) {
        coordinates.entrySet().stream()
                .filter(entry -> isValidPkgUrl(entry.getValue()))
                .forEach(entry -> externalIds.put(entry.getKey(), entry.getValue()));
        return this;
    }

    private static boolean isValidPkgUrl (String purl) {
        try {
            new PackageURL(purl);
            return true;
        } catch (MalformedPackageURLException e) {
            return false;
        }
    }

    @JsonIgnore
    public String getOverriddenLicense() {
        return additionalData.get(OVERRIDDEN_LICENSES_KEY);
    }

    public SW360Release setOverriddenLicense(String overriddenLicense) {
        additionalData.put(OVERRIDDEN_LICENSES_KEY, overriddenLicense);
        return this;
    }

    @JsonIgnore
    public String getDeclaredLicense() {
        return additionalData.get(DECLARED_LICENSE_KEY);
    }

    public SW360Release setDeclaredLicense(String declaredLicense) {
        additionalData.put(DECLARED_LICENSE_KEY, declaredLicense);
        return this;
    }

    @JsonIgnore
    public String getObservedLicense() {
        return additionalData.get(OBSERVED_LICENSES_KEY);
    }

    public SW360Release setObservedLicense(String observedLicense) {
        additionalData.put(OBSERVED_LICENSES_KEY, observedLicense);
        return this;
    }

    @JsonIgnore
    public String getReleaseTagUrl() {
        return externalIds.get(RELEASE_TAG_URL_KEY);
    }

    public SW360Release setReleaseTagUrl(String releaseTagUrl) {
        externalIds.put(RELEASE_TAG_URL_KEY, releaseTagUrl);
        return this;
    }

    @JsonIgnore
    public String getSoftwareHeritageId() {
        return externalIds.get(SOFTWARE_HERITAGE_ID_KEY);
    }

    public SW360Release setSoftwareHeritageId(String softwareHeritageId) {
        externalIds.put(SOFTWARE_HERITAGE_ID_KEY, softwareHeritageId);
        return this;
    }

    @JsonIgnore
    public Set<String> getHashes() {
        return externalIds.entrySet().stream()
                .filter(e -> e.getKey().startsWith(HASHES_PREFIX))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    private void dropAllHashes() {
        externalIds.keySet().stream()
                .filter(s -> s.startsWith(HASHES_PREFIX))
                .forEach(externalIds::remove);
    }

    public SW360Release setHashes(Set<String> hashes) {
        dropAllHashes();

        int i = 1;
        for (String hash : hashes) {
            if (hash != null && !hash.isEmpty()) {
                externalIds.put(HASHES_PREFIX + i, hash);
                i++;
            }
        }
        return this;
    }

    @JsonIgnore
    public String getClearingState() {
        return additionalData.get(CLEARINGSTATE_KEY);
    }

    public SW360Release setClearingState(String clearingState) {
        additionalData.put(CLEARINGSTATE_KEY, clearingState);
        return this;
    }

    @JsonProperty("clearingState")
    public SW360ClearingState getSw360ClearingState() {
        return sw360ClearingState;
    }

    public void setSw360ClearingState(SW360ClearingState sw360ClearingState) {
        this.sw360ClearingState = sw360ClearingState;
    }

    @JsonIgnore
    public String getChangeStatus() {
        return additionalData.get(CHANGESTATUS_KEY);
    }


    public SW360Release setChangeStatus(String changeStatus) {
        additionalData.put(CHANGESTATUS_KEY, changeStatus);
        return this;
    }

    @JsonIgnore
    public String getCopyrights() {
        return additionalData.get(COPYRIGHTS_KEY);
    }

    public SW360Release setCopyrights(String copyrights) {
        additionalData.put(COPYRIGHTS_KEY, copyrights);
        return this;
    }

    @JsonIgnore
    public boolean isProprietary() {
        return isProprietary;
    }

    @JsonIgnore
    public SW360Release setProprietary(boolean proprietary) {
        isProprietary = proprietary;
        return this;
    }

    @JsonIgnore
    public String getHomepageUrl() {
        return additionalData.get(HOMEPAGE_KEY);
    }

    public SW360Release setHomepageUrl(String homepageUrl) {
        additionalData.put(HOMEPAGE_KEY, homepageUrl);
        return this;
    }


    public Map<String,String> getExternalIds() {
        return new HashMap<>(externalIds);
    }

    public SW360Release setExternalIds(Map<String, String> externalIds) {
        this.externalIds.putAll(externalIds);
        return this;
    }

    public SW360Release setAdditionalData(Map<String, String> additionalData) {
        this.additionalData.putAll(additionalData);
        return this;
    }

    public boolean shareIdentifier(SW360Release releaseCompare) {
        return this.name.equals(Optional.of(releaseCompare.getName()).orElse(""))
                && this.version.equals(Optional.of(releaseCompare.getVersion()).orElse(""));
    }

    public SW360Release mergeWith(SW360Release releaseWithPrecedence) {
        name = getDominantGetterFromVariableMergeOrNull(releaseWithPrecedence, SW360Release::getName);
        version = getDominantGetterFromVariableMergeOrNull(releaseWithPrecedence, SW360Release::getVersion);
        cpeId = getDominantGetterFromVariableMergeOrNull(releaseWithPrecedence, SW360Release::getCpeId);
        downloadurl = getDominantGetterFromVariableMergeOrNull(releaseWithPrecedence, SW360Release::getDownloadurl);
        if (releaseWithPrecedence.isSetMainLicenseIds()) {
            setMainLicenseIds(releaseWithPrecedence.getMainLicenseIds());
        }
        Self releaseIdWithPrecedence = releaseWithPrecedence.getLinks().getSelf();
        if (releaseIdWithPrecedence != null && !releaseIdWithPrecedence.getHref().isEmpty()) {
            getLinks().setSelf(releaseIdWithPrecedence);
        }
        Self componentIdWithPrecedence = releaseWithPrecedence.getLinks().getSelfComponent();
        if (componentIdWithPrecedence != null && !componentIdWithPrecedence.getHref().isEmpty()) {
            getLinks().setSelfComponent(componentIdWithPrecedence);
        }
        final Set<SW360SparseAttachment> releaseWithPrecedenceAttachments = releaseWithPrecedence.getEmbedded().getAttachments();
        if (!releaseWithPrecedenceAttachments.isEmpty()) {
            if (getEmbedded().getAttachments().isEmpty()) {
                getEmbedded().setAttachments(releaseWithPrecedenceAttachments);
            } else {
                getEmbedded().setAttachments(mergeAttachments(getEmbedded().getAttachments(), releaseWithPrecedenceAttachments));
            }
        }
        externalIds.putAll(releaseWithPrecedence.externalIds);
        additionalData.putAll(releaseWithPrecedence.additionalData);

        return this;
    }

    private Set<SW360SparseAttachment> mergeAttachments(Set<SW360SparseAttachment> attachments, Set<SW360SparseAttachment> releaseWithPrecedenceAttachments) {
        attachments.addAll(releaseWithPrecedenceAttachments);
        return attachments;
    }

    private <T> T getDominantGetterFromVariableMergeOrNull(SW360Release release, Function<SW360Release, T> getter) {
        return Optional.ofNullable(getter.apply(release))
                .orElse(getter.apply(this));
    }

    @Override
    public SW360ReleaseLinkObjects createEmptyLinks() {
        return new SW360ReleaseLinkObjects();
    }

    @Override
    public SW360ReleaseEmbedded createEmptyEmbedded() {
        return new SW360ReleaseEmbedded();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360Release) || !super.equals(o)) return false;
        SW360Release release = (SW360Release) o;
        return Objects.equals(name, release.name) &&
                Objects.equals(version, release.version) &&
                Objects.equals(cpeId, release.cpeId) &&
                Objects.equals(downloadurl, release.downloadurl) &&
                Objects.equals(externalIds, release.externalIds) &&
                Objects.equals(additionalData, release.additionalData) &&
                Objects.equals(createdOn, release.createdOn) &&
                Objects.equals(sw360ClearingState, release.sw360ClearingState) &&
                isProprietary == release.isProprietary;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, version, cpeId, downloadurl, externalIds, additionalData,
                isProprietary, createdOn, sw360ClearingState);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360Release;
    }
}
