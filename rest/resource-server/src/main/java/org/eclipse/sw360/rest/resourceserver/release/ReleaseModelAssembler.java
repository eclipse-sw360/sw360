/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.release;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.packages.PackageController;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReleaseModelAssembler {

    private static final ImmutableMap<Release._Fields, String> FIELDS_TO_BE_EMBEDDED = ImmutableMap.of(
            Release._Fields.MODERATORS, "sw360:moderators",
            Release._Fields.ATTACHMENTS, "sw360:attachments",
            Release._Fields.COTS_DETAILS, "sw360:cotsDetails",
            Release._Fields.RELEASE_ID_TO_RELATIONSHIP, "sw360:releaseIdToRelationship",
            Release._Fields.CLEARING_INFORMATION, "sw360:clearingInformation");

    private final RestControllerHelper restControllerHelper;
    private final SW360PackageService packageService;

    public HalResource<Release> toHalResource(Release release, boolean verbose) throws TException {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId())
                .withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);
        if (verbose) {
            if (release.getModerators() != null) {
                Set<String> moderators = release.getModerators();
                restControllerHelper.addEmbeddedModerators(halRelease, moderators);
                release.setModerators(null);
            }
            if (release.getAttachments() != null) {
                Set<Attachment> attachments = release.getAttachments();
                restControllerHelper.addEmbeddedAttachments(halRelease, attachments);
                release.setAttachments(null);
            }
            if (release.getVendor() != null) {
                Vendor vendor = release.getVendor();
                HalResource<Vendor> vendorHalResource = restControllerHelper.addEmbeddedVendor(vendor);
                halRelease.addEmbeddedResource("sw360:vendors", vendorHalResource);
                release.setVendor(null);
            }
            if (release.getMainLicenseIds() != null) {
                restControllerHelper.addEmbeddedLicenses(halRelease, release.getMainLicenseIds());
            }
            if (release.getOtherLicenseIds() != null) {
                restControllerHelper.addEmbeddedOtherLicenses(halRelease, release.getOtherLicenseIds());
            }
            Set<String> packageIds = release.getPackageIds();
            if (packageIds != null) {
                restControllerHelper.addEmbeddedPackages(halRelease, packageIds, packageService);
                release.setPackageIds(null);
            }
        }
        return halRelease;
    }

    public HalResource<Release> toDetailedHalResource(Release release) {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId())
                .withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);
        Set<String> packageIds = release.getPackageIds();
        if (packageIds != null) {
            for (String id : packageIds) {
                Link packageLink = linkTo(ReleaseController.class)
                        .slash("api" + PackageController.PACKAGES_URL + "/" + id)
                        .withRel("packages");
                halRelease.add(packageLink);
            }
        }
        release.setPackageIds(null);
        for (Entry<Release._Fields, String> field : FIELDS_TO_BE_EMBEDDED.entrySet()) {
            restControllerHelper.addEmbeddedFields(field.getValue(), release.getFieldValue(field.getKey()), halRelease);
        }
        release.unsetAttachments();
        return halRelease;
    }
}
