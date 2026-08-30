package org.eclipse.sw360.rest.resourceserver.release;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
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

import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_PACKAGE_PORTLET_ENABLED;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class ReleaseModelAssembler {

    private static final ImmutableMap<Release._Fields,String> mapOfFieldsTobeEmbedded = ImmutableMap.of(
            Release._Fields.MODERATORS, "sw360:moderators",
            Release._Fields.ATTACHMENTS, "sw360:attachments",
            Release._Fields.COTS_DETAILS, "sw360:cotsDetails",
            Release._Fields.RELEASE_ID_TO_RELATIONSHIP,"sw360:releaseIdToRelationship",
            Release._Fields.CLEARING_INFORMATION, "sw360:clearingInformation");

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final SW360PackageService packageService;

    ReleaseModelAssembler(@NonNull RestControllerHelper restControllerHelper, @NonNull SW360PackageService packageService) {
        this.restControllerHelper = restControllerHelper;
        this.packageService = packageService;
    }

    HalResource<Release> createHalReleaseResource(Release release, boolean verbose) throws TException {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId()).withRel("component");
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

    @NonNull HalResource<Release> createHalReleaseResourceWithAllDetails(Release release) {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId())
                .withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);
        if (SW360Utils.readConfig(IS_PACKAGE_PORTLET_ENABLED, true) && release.getPackageIds() != null) {
            for (String id : release.getPackageIds()) {
                Link packageLink = linkTo(ReleaseController.class)
                        .slash("api" + PackageController.PACKAGES_URL + "/" + id).withRel("packages");
                halRelease.add(packageLink);
            }
            release.setPackageIds(null);
        }
        for (Map.Entry<Release._Fields, String> field : mapOfFieldsTobeEmbedded.entrySet()) {
            restControllerHelper.addEmbeddedFields(field.getValue(), release.getFieldValue(field.getKey()), halRelease);
        }
        // Do not add attachment as it is an embedded field
        release.unsetAttachments();
        return halRelease;
    }

}
