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

import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ComponentSummary extends DocumentSummary<Component> {

    private final ReleaseRepository releaseRepository;
    private final VendorRepository vendorRepository;

    public ComponentSummary() {
        // Create summary without database connection
        this(null, null);
    }

    public ComponentSummary(ReleaseRepository releaseRepository, VendorRepository vendorRepository) {
        this.releaseRepository = releaseRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    protected Component summary(SummaryType type, Component document) {

        Component copy = new Component();
        if (type == SummaryType.EXPORT_SUMMARY) {
            List<Release> releases = releaseRepository.getReleasesFromComponentId(document.getId());
            return makeExportSummary(document, releases);
        } else if (type == SummaryType.DETAILED_EXPORT_SUMMARY) {
            List<Release> releases = releaseRepository.getReleasesFromComponentId(document.getId());

            final Map<String, Vendor> vendorsById = vendorRepository.getAllIdMap();

            for (Release release : releases) {
                if (release.getVendor() == null) {
                    String vendorId = release.getVendorId();
                    if (vendorId != null && !vendorId.isEmpty()) {
                        release.setVendor(vendorsById.get(vendorId));
                    }
                }
            }

            return makeDetailedExportSummary(document, releases);
        } else if (type == SummaryType.HOME) {
            copy.setId(document.getId());
            copy.setDescription(document.getDescription());
        }

        copy.setId(document.getId());
        copy.setName(document.getName());
        copy.setVendorNames(document.getVendorNames());
        copy.setComponentType(document.getComponentType());
        copy.setCategories(document.getCategories());

        if (type == SummaryType.SUMMARY) {
            setSummaryFields(document, copy);
        }

        return copy;
    }

    protected static void setSummaryFields(Component document, Component copy) {
        copy.setId(document.getId());
        copy.setRevision(document.getRevision());
        copy.setType(document.getType());
        copy.setName(document.getName());
        copy.setDescription(document.getDescription());
        copy.setAttachments(document.getAttachments());
        copy.setCreatedOn(document.getCreatedOn());
        copy.setComponentType(document.getComponentType());
        copy.setCreatedBy(document.getCreatedBy());
        copy.setSubscribers(document.getSubscribers());
        copy.setModerators(document.getModerators());
        copy.setComponentOwner(document.getComponentOwner());
        copy.setOwnerAccountingUnit(document.getOwnerAccountingUnit());
        copy.setOwnerGroup(document.getOwnerGroup());
        copy.setOwnerCountry(document.getOwnerCountry());
        copy.setRoles(document.getRoles());
        copy.setVisbility(document.getVisbility());
        copy.setBusinessUnit(document.getBusinessUnit());
        copy.setCdxComponentType(document.getCdxComponentType());
        copy.setExternalIds(document.getExternalIds());
        copy.setAdditionalData(document.getAdditionalData());
        copy.setReleases(document.getReleases());
        copy.setReleaseIds(document.getReleaseIds());
        copy.setMainLicenseIds(document.getMainLicenseIds());
        copy.setDefaultVendor(document.getDefaultVendor());
        copy.setDefaultVendorId(document.getDefaultVendorId());
        copy.setCategories(document.getCategories());
        copy.setLanguages(document.getLanguages());
        copy.setSoftwarePlatforms(document.getSoftwarePlatforms());
        copy.setOperatingSystems(document.getOperatingSystems());
        copy.setVendorNames(document.getVendorNames());
        copy.setHomepage(document.getHomepage());
        copy.setMailinglist(document.getMailinglist());
        copy.setWiki(document.getWiki());
        copy.setBlog(document.getBlog());
        copy.setWikipedia(document.getWikipedia());
        copy.setOpenHub(document.getOpenHub());
        copy.setVcs(document.getVcs());
        copy.setDocumentState(document.getDocumentState());
        copy.setPermissions(document.getPermissions());
        copy.setModifiedBy(document.getModifiedBy());
        copy.setModifiedOn(document.getModifiedOn());
    }

    private Component makeDetailedExportSummary(Component document, List<Release> releases) {
        document.setReleases(releases);
        return document;
    }

    private Component makeExportSummary(Component document, List<Release> releases) {

        if (releaseRepository == null) {
            throw new IllegalStateException("Cannot make export summary without database connection!");
        }

        Component copy = new Component();

        copy.setId(document.getId());
        copy.setName(document.getName());
        copy.setLanguages(document.getLanguages());
        copy.setOperatingSystems(document.getOperatingSystems());
        copy.setSoftwarePlatforms(document.getSoftwarePlatforms());
        copy.setCreatedBy(document.getCreatedBy());
        copy.setCreatedOn(document.getCreatedOn());
        copy.setVendorNames(document.getVendorNames());
        copy.setMainLicenseIds(document.getMainLicenseIds());
        copy.setComponentType(document.getComponentType());
        copy.setDefaultVendorId(document.getDefaultVendorId());
        copy.setVcs(document.getVcs());
        copy.setHomepage(document.getHomepage());
        copy.setExternalIds(document.getExternalIds());

        List<Release> exportReleases = new ArrayList<>();
        for (Release release : releases) {
            Release exportRelease = new Release();
            exportRelease.setName(release.getName());
            exportRelease.setVersion(release.getVersion());
            exportRelease.setComponentId("");
            exportReleases.add(exportRelease);
        }
        copy.setReleases(exportReleases);

        return copy;
    }
}
