/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import com.google.common.base.Strings;

import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ReleaseSummary extends DocumentSummary<Release> {

    private final VendorRepository vendorRepository;

    public ReleaseSummary() {
        this(null);
    }

    public ReleaseSummary(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public List<Release> makeSummary(SummaryType type, Collection<Release> fullDocuments) {
        if (fullDocuments == null) return Collections.emptyList();

        Set<String> vendorIds = fullDocuments
                .stream()
                .filter(Objects::nonNull)
                .map(Release::getVendorId)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Map<String, Vendor> vendorById = vendorRepository.getIdMap(vendorIds);

        List<Release> documents = new ArrayList<>(fullDocuments.size());
        for (Release fullDocument : fullDocuments) {
            if (fullDocument == null) continue;
            Release document = summary(type, fullDocument, vendorById::get);
            if (document != null) documents.add(document);
        }
        return documents;
    }

    @Override
    protected Release summary(SummaryType type, Release document) {
        return summary(type, document, vendorRepository::get);
    }

    protected Release summary(SummaryType type, Release document, Function<String, Vendor> vendorProvider) {
        Release copy = new Release();
        if (type == SummaryType.DETAILED_EXPORT_SUMMARY) {
            setDetailedExportSummaryFields(document, copy);
        } else {
            setShortSummaryFields(document, copy);
            if (type != SummaryType.SHORT) {
                setAdditionalFieldsForSummariesOtherThanShortAndDetailedExport(document, copy);
            }
        }
        if (!Strings.isNullOrEmpty(document.getVendorId())) {
            Vendor vendor = vendorProvider.apply(document.getVendorId());
            copy.setVendor(vendor);
        }
        return copy;
    }

    /**
     * Explicit copy of all Release fields except revision, documentState,
     * permissions, and vendorId (semantic equivalent of ReleaseExporter.RELEASE_RENDERED_FIELDS).
     */
    private void setDetailedExportSummaryFields(Release document, Release copy) {
        copy.setId(document.getId());
        copy.setType(document.getType());
        copy.setCpeid(document.getCpeid());
        copy.setName(document.getName());
        copy.setVersion(document.getVersion());
        copy.setComponentId(document.getComponentId());
        copy.setReleaseDate(document.getReleaseDate());
        copy.setComponentType(document.getComponentType());
        copy.setExternalIds(document.getExternalIds());
        copy.setAdditionalData(document.getAdditionalData());
        copy.setSourceCodeDownloadurl(document.getSourceCodeDownloadurl());
        copy.setBinaryDownloadurl(document.getBinaryDownloadurl());
        copy.setAttachments(document.getAttachments());
        copy.setCreatedOn(document.getCreatedOn());
        copy.setRepository(document.getRepository());
        copy.setMainlineState(document.getMainlineState());
        copy.setClearingState(document.getClearingState());
        copy.setExternalToolProcesses(document.getExternalToolProcesses());
        copy.setCreatedBy(document.getCreatedBy());
        copy.setCreatorDepartment(document.getCreatorDepartment());
        copy.setProjectMainlineState(document.getProjectMainlineState());
        copy.setContributors(document.getContributors());
        copy.setModerators(document.getModerators());
        copy.setSubscribers(document.getSubscribers());
        copy.setRoles(document.getRoles());
        copy.setMainLicenseIds(document.getMainLicenseIds());
        copy.setOtherLicenseIds(document.getOtherLicenseIds());
        copy.setVendor(document.getVendor());
        copy.setClearingInformation(document.getClearingInformation());
        copy.setLanguages(document.getLanguages());
        copy.setOperatingSystems(document.getOperatingSystems());
        copy.setCotsDetails(document.getCotsDetails());
        copy.setEccInformation(document.getEccInformation());
        copy.setSoftwarePlatforms(document.getSoftwarePlatforms());
        copy.setReleaseIdToRelationship(document.getReleaseIdToRelationship());
        copy.setPackageIds(document.getPackageIds());
        copy.setSpdxId(document.getSpdxId());
        copy.setModifiedBy(document.getModifiedBy());
        copy.setModifiedOn(document.getModifiedOn());
    }

    private void setShortSummaryFields(Release document, Release copy) {
        copy.setId(document.getId());
        copy.setRevision(document.getRevision());
        copy.setName(document.getName());
        copy.setVersion(document.getVersion());
        copy.setComponentId(document.getComponentId());
        copy.setExternalToolProcesses(document.getExternalToolProcesses());
        copy.setClearingState(document.getClearingState());
        copy.setMainlineState(document.getMainlineState());
        copy.setCpeid(document.getCpeid());
        copy.setReleaseDate(document.getReleaseDate());
        copy.setSourceCodeDownloadurl(document.getSourceCodeDownloadurl());
        copy.setBinaryDownloadurl(document.getBinaryDownloadurl());
        copy.setPackageIds(document.getPackageIds());
    }

    private void setAdditionalFieldsForSummariesOtherThanShortAndDetailedExport(Release document, Release copy) {
        copy.setCreatedBy(document.getCreatedBy());
        copy.setMainlineState(document.getMainlineState());
        copy.setClearingState(document.getClearingState());
        copy.setLanguages(document.getLanguages());
        copy.setOperatingSystems(document.getOperatingSystems());
        copy.setAttachments(document.getAttachments());
        copy.setMainLicenseIds(document.getMainLicenseIds());
        copy.setEccInformation(document.getEccInformation());
    }
}
