/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cyclonedx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.CollectionUtils;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.*;
import org.cyclonedx.model.Component.Type;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.spdx.library.model.license.ListedLicenses;
import org.spdx.library.model.license.SpdxListedLicense;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CycloneDX BOM export implementation.
 * Supports both XML and JSON format of CycloneDX SBOM
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
public class CycloneDxBOMExporter {

    private static final Logger log = LogManager.getLogger(CycloneDxBOMExporter.class);
    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final PackageDatabaseHandler packageDatabaseHandler;
    private final User user;
    private final ListedLicenses listedLicenses;

    public CycloneDxBOMExporter(ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler, PackageDatabaseHandler packageDatabaseHandler, User user) {
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.packageDatabaseHandler = packageDatabaseHandler;
        this.user = user;
        this.listedLicenses = ListedLicenses.getListedLicenses();
    }

    private static Tool getTool() {
        Tool tool = new Tool();
        tool.setName(SW360Constants.TOOL_NAME);
        tool.setVendor(SW360Constants.TOOL_VENDOR);
        tool.setVersion(SW360Utils.getSW360Version());
        return tool;
    }

    public RequestSummary exportSbom(String projectId, String bomType, Boolean includeSubProjReleases, User user) {
        final RequestSummary summary = new RequestSummary(RequestStatus.SUCCESS);
        try {
            Project project = projectDatabaseHandler.getProjectById(projectId, user);
            Bom bom = new Bom();
            Set<String> linkedReleaseIds = Sets.newHashSet(CommonUtils.getNullToEmptyKeyset(project.getReleaseIdToUsage()));
            Set<String> linkedPackageIds = Sets.newHashSet(CommonUtils.nullToEmptySet(project.getPackageIds()));

            if (!SW360Utils.isUserAtleastDesiredRoleInPrimaryOrSecondaryGroup(user, SW360Constants.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE)) {
                log.warn("User does not have permission to export the SBOM: " + user.getEmail());
                summary.setRequestStatus(RequestStatus.ACCESS_DENIED);
                return summary;
            }

            if (includeSubProjReleases && project.getLinkedProjectsSize() > 0) {
                ProjectService.Iface client = new ThriftClients().makeProjectClient();
                Map<String, Set<String>> idsMap = SW360Utils.getLinkedReleaseIdsOfAllSubProjectsAsFlatList(project, Sets.newHashSet(), Sets.newHashSet(), Sets.newHashSet(), client, user);
                linkedReleaseIds.addAll(idsMap.get(SW360Constants.RELEASE_IDS));
                linkedPackageIds.addAll(idsMap.get(SW360Constants.PACKAGE_IDS));
            }

            if (PermissionUtils.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED) {
                List<Release> releaseList = componentDatabaseHandler.getAccessibleReleaseSummary(user);
                Set<String> releaseListIds = releaseList.stream().map(Release::getId).collect(Collectors.toSet());
                linkedReleaseIds = CollectionUtils.intersection(releaseListIds, linkedReleaseIds).stream().collect(Collectors.toSet());
            }

            if (SW360Constants.IS_PACKAGE_PORTLET_ENABLED && CommonUtils.isNotEmpty(linkedPackageIds)) {
                List<Package> packages = packageDatabaseHandler.getPackageByIds(linkedPackageIds);
                List<org.cyclonedx.model.Component> sbomComponents = getCycloneDxComponentsFromSw360Packages(packages);
                Set<String> releaseIds = packages.stream().filter(pkg -> CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())).map(Package::getReleaseId).collect(Collectors.toSet());
                if (linkedReleaseIds.removeAll(releaseIds) && CommonUtils.isNotEmpty(linkedReleaseIds)) {
                    List<Release> linkedReleases = componentDatabaseHandler.getReleasesByIds(linkedReleaseIds);
                    Set<String> componentIds = linkedReleases.stream().map(Release::getComponentId).filter(Objects::nonNull).collect(Collectors.toSet());
                    List<Component> components = componentDatabaseHandler.getComponentsByIds(componentIds);
                    sbomComponents.addAll(getCycloneDxComponentsFromSw360Releases(linkedReleases, components));
                }
                bom.setComponents(sbomComponents);
            } else if (CommonUtils.isNotEmpty(linkedReleaseIds)) {
                List<Release> linkedReleases = componentDatabaseHandler.getReleasesByIds(linkedReleaseIds);
                Set<String> componentIds = linkedReleases.stream().map(Release::getComponentId).filter(Objects::nonNull).collect(Collectors.toSet());
                List<Component> components = componentDatabaseHandler.getComponentsByIds(componentIds);
                List<org.cyclonedx.model.Component> sbomComponents = getCycloneDxComponentsFromSw360Releases(linkedReleases, components);
                bom.setComponents(sbomComponents);
            } else {
                log.warn("Cannot export SBOM for project without linked releases: " + projectId);
                summary.setRequestStatus(RequestStatus.FAILED_SANITY_CHECK);
                return summary;
            }

            org.cyclonedx.model.Component metadataComp = getMetadataComponent(project);
            Tool tool = getTool();
            Metadata metadata = new Metadata();
            metadata.setComponent(metadataComp);
            metadata.setTimestamp(new Date());
            metadata.setTools(Lists.newArrayList(tool));
            bom.setMetadata(metadata);

            if (SW360Constants.JSON_FILE_EXTENSION.equalsIgnoreCase(bomType)) {
                BomJsonGenerator jsonBom = new BomJsonGenerator(bom, Version.VERSION_16);
                summary.setMessage(jsonBom.toJsonString());
            } else {
                BomXmlGenerator xmlBom = new BomXmlGenerator(bom, Version.VERSION_16);
                summary.setMessage(xmlBom.toXmlString());
            }
            return summary;
        } catch (SW360Exception e) {
            log.error(String.format("An error occurred while fetching project: %s from db, for SBOM export!", projectId), e);
            summary.setMessage("An error occurred while fetching project from db, for SBOM export: " + e.getMessage());
        } catch (GeneratorException e) {
            log.error(String.format("An error occurred while exporting xml SBOM for project with id: %s", projectId), e);
            summary.setMessage("An error occurred while exporting xml SBOM for project: " + e.getMessage());
        } catch (Exception e) {
            log.error("An error occurred while exporting SBOM for project: " + projectId, e);
            summary.setMessage("An error occurred while exporting SBOM for project: " + e.getMessage());
        }
        summary.setRequestStatus(RequestStatus.FAILURE);
        return summary;
    }

    private org.cyclonedx.model.Component getMetadataComponent(Project project) {
        org.cyclonedx.model.Component component = new org.cyclonedx.model.Component();
        component.setAuthor(user.getEmail());
        component.setDescription(CommonUtils.nullToEmptyString(project.getDescription()));
        component.setName(project.getName());
        component.setVersion(CommonUtils.nullToEmptyString(project.getVersion()));
        component.setType(Type.APPLICATION);
        component.setGroup(CommonUtils.nullToEmptyString(project.getBusinessUnit()));
        return component;
    }

    private List<org.cyclonedx.model.Component> getCycloneDxComponentsFromSw360Releases(List<Release> releases, List<Component> components) {
        List<org.cyclonedx.model.Component> comps = Lists.newArrayList();
        Map<String, Component> compIdToComponentMap = components.stream().map(comp -> new AbstractMap.SimpleEntry<>(comp.getId(), comp)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldVal, newVal) -> newVal));
        for (Release release : releases) {
            Component sw360Comp = compIdToComponentMap.get(release.getComponentId());
            org.cyclonedx.model.Component comp = new org.cyclonedx.model.Component();
            comp.setName(release.getName());
            comp.setVersion(release.getVersion());
            comp.setDescription(CommonUtils.nullToEmptyString(sw360Comp.getDescription()));

            Set<String> purlSet = new TreeSet<>();
            if (!CommonUtils.isNullOrEmptyMap(release.getExternalIds())) {
                if (release.getExternalIds().containsKey(SW360Constants.PACKAGE_URL)) {
                    purlSet.addAll(getPurlFromSw360Document(release.getExternalIds(), SW360Constants.PACKAGE_URL));
                }
                if (release.getExternalIds().containsKey(SW360Constants.PURL_ID)) {
                    purlSet.addAll(getPurlFromSw360Document(release.getExternalIds(), SW360Constants.PURL_ID));
                }
            } else if (!CommonUtils.isNullOrEmptyMap(sw360Comp.getExternalIds())) {
                if (sw360Comp.getExternalIds().containsKey(SW360Constants.PACKAGE_URL)) {
                    purlSet.addAll(getPurlFromSw360Document(sw360Comp.getExternalIds(), SW360Constants.PACKAGE_URL));
                }
                if (sw360Comp.getExternalIds().containsKey(SW360Constants.PURL_ID)) {
                    purlSet.addAll(getPurlFromSw360Document(sw360Comp.getExternalIds(), SW360Constants.PURL_ID));
                }
            }
            if (CommonUtils.isNotEmpty(purlSet)) {
                comp.setPurl(String.join(", ", purlSet));
            }

            if (CommonUtils.isNotNullEmptyOrWhitespace(release.getCpeid())) {
                comp.setCpe(release.getCpeid());
            }

            comp.setType(sw360Comp.isSetCdxComponentType() ? getCdxComponentType(sw360Comp.getCdxComponentType()) : Type.LIBRARY);

            List<ExternalReference> extRefs = Lists.newArrayList();
            if (null != release.getRepository() && null != release.getRepository().getUrl()) {
                ExternalReference extRef = new ExternalReference();
                extRef.setType(org.cyclonedx.model.ExternalReference.Type.VCS);
                extRef.setUrl(release.getRepository().getUrl());
                extRefs.add(extRef);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Comp.getHomepage())) {
                ExternalReference extRef = new ExternalReference();
                extRef.setType(org.cyclonedx.model.ExternalReference.Type.WEBSITE);
                extRef.setUrl(sw360Comp.getHomepage());
                extRefs.add(extRef);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Comp.getMailinglist())) {
                ExternalReference extRef = new ExternalReference();
                extRef.setType(org.cyclonedx.model.ExternalReference.Type.MAILING_LIST);
                extRef.setUrl(sw360Comp.getMailinglist());
                extRefs.add(extRef);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Comp.getWiki())) {
                ExternalReference extRef = new ExternalReference();
                extRef.setType(org.cyclonedx.model.ExternalReference.Type.SUPPORT);
                extRef.setUrl(sw360Comp.getWiki());
                extRefs.add(extRef);
            }
            if (CommonUtils.isNotEmpty(extRefs)) {
                comp.setExternalReferences(extRefs);
            }

            Set<String> licenses = Sets.newHashSet();
            if (CommonUtils.isNotEmpty(release.getMainLicenseIds())) {
                licenses.addAll(release.getMainLicenseIds());
            }
            if (CommonUtils.isNotEmpty(release.getOtherLicenseIds())) {
                licenses.addAll(release.getOtherLicenseIds());
            }
            if (CommonUtils.isNotEmpty(sw360Comp.getMainLicenseIds())) {
                licenses.addAll(sw360Comp.getMainLicenseIds());
            }
            if (CommonUtils.isNotEmpty(licenses)) {
                comp.setLicenseChoice(getLicenseFromSw360Document(licenses));
            }

            comps.add(comp);
        }
        return comps;
    }

    private List<org.cyclonedx.model.Component> getCycloneDxComponentsFromSw360Packages(List<Package> packages) {
        List<org.cyclonedx.model.Component> comps = Lists.newArrayList();
        for (Package pckg : packages) {
            org.cyclonedx.model.Component comp = new org.cyclonedx.model.Component();
            try {
                PackageURL purl = new PackageURL(pckg.getPurl());
                comp.setName(purl.getName());
                comp.setPurl(purl);
            } catch (MalformedPackageURLException e) {
                log.error("Malformed PURL for component: " + pckg.getName(), e);
            }
            comp.setVersion(pckg.getVersion());
            if (CommonUtils.isNotEmpty(pckg.getLicenseIds())) {
                comp.setLicenseChoice(getLicenseFromSw360Document(pckg.getLicenseIds()));
            }
            comp.setType(pckg.getPackageType() != null ? getCdxComponentType(pckg.getPackageType()) : Type.LIBRARY);
            comp.setDescription(pckg.getDescription());
            List<ExternalReference> extRefs = Lists.newArrayList();
            if (CommonUtils.isNotNullEmptyOrWhitespace(pckg.getHomepageUrl())) {
                ExternalReference extRef = new ExternalReference();
                extRef.setType(org.cyclonedx.model.ExternalReference.Type.WEBSITE);
                extRef.setUrl(pckg.getHomepageUrl());
                extRefs.add(extRef);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(pckg.getVcs())) {
                ExternalReference extRef = new ExternalReference();
                extRef.setType(org.cyclonedx.model.ExternalReference.Type.VCS);
                extRef.setUrl(pckg.getVcs());
                extRefs.add(extRef);
            }
            if (CommonUtils.isNotEmpty(extRefs)) {
                comp.setExternalReferences(extRefs);
            }
            comps.add(comp);
        }
        return comps;
    }

    private String normalizeLicenseId(String licenseId) {
        if (CommonUtils.isNullEmptyOrWhitespace(licenseId)) {
            return licenseId;
        }
        try {
            // Check if the license ID is in the SPDX list and deprecated
            SpdxListedLicense spdxLicense = listedLicenses.getListedLicenseById(licenseId);
            if (spdxLicense != null && spdxLicense.isDeprecated()) {
                // Look for a replacement in the seeAlso field
                List<String> seeAlso = new ArrayList<>(spdxLicense.getSeeAlso());
                for (String url : seeAlso) {
                    if (url.contains("spdx.org/licenses/")) {
                        String replacementId = url.substring(url.lastIndexOf("/") + 1).replace(".html", "");
                        if (!replacementId.equals(licenseId) && listedLicenses.isSpdxListedLicenseId(replacementId)) {
                            log.info("Normalized deprecated license ID: " + licenseId + " to " + replacementId);
                            return replacementId;
                        }
                    }
                }
                log.warn("Deprecated license ID " + licenseId + " found, but no replacement available in seeAlso");
            }
            // If the license ID is valid and not deprecated, return it as-is
            if (listedLicenses.isSpdxListedLicenseId(licenseId)) {
                return licenseId;
            }
            // If the license ID is not in the SPDX list, return it unchanged
            return licenseId;
        } catch (Exception e) {
            log.warn("Failed to normalize license ID: " + licenseId, e);
            return licenseId;
        }
    }

    private boolean isValidSpdxLicenseId(String licenseId) {
        if (CommonUtils.isNullEmptyOrWhitespace(licenseId)) {
            return false;
        }
        // Use Spdx-Java-Library to validate the license ID against the official SPDX list
        return listedLicenses.isSpdxListedLicenseId(licenseId);
    }

    private LicenseChoice getLicenseFromSw360Document(Set<String> sw360Licenses) {
        LicenseChoice licenseChoice = new LicenseChoice();
        List<License> licenses = Lists.newArrayList();
        for (String lic : sw360Licenses) {
            String normalizedLic = normalizeLicenseId(lic);
            if (isValidSpdxLicenseId(normalizedLic)) {
                License license = new License();
                license.setId(normalizedLic);
                licenses.add(license);
            } else {
                log.warn("Skipping invalid SPDX license ID: " + lic);
            }
        }
        licenseChoice.setLicenses(licenses);
        return licenseChoice;
    }

    private org.cyclonedx.model.Component.Type getCdxComponentType(CycloneDxComponentType compType) {
        switch (compType) {
            case APPLICATION:
                return org.cyclonedx.model.Component.Type.APPLICATION;
            case CONTAINER:
                return org.cyclonedx.model.Component.Type.CONTAINER;
            case DEVICE:
                return org.cyclonedx.model.Component.Type.DEVICE;
            case FILE:
                return org.cyclonedx.model.Component.Type.FILE;
            case FIRMWARE:
                return org.cyclonedx.model.Component.Type.FIRMWARE;
            case FRAMEWORK:
                return org.cyclonedx.model.Component.Type.FRAMEWORK;
            case LIBRARY:
                return org.cyclonedx.model.Component.Type.LIBRARY;
            case OPERATING_SYSTEM:
                return org.cyclonedx.model.Component.Type.OPERATING_SYSTEM;
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getPurlFromSw360Document(Map<String, String> externalIds, String key) {
        String existingPurl = CommonUtils.nullToEmptyMap(externalIds).getOrDefault(key, "");
        Set<String> purlSet = Sets.newHashSet();
        if (CommonUtils.isNotNullEmptyOrWhitespace(existingPurl)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (existingPurl.equals(SW360Constants.NULL_STRING)) {
                    purlSet.add(SW360Constants.NULL_STRING);
                } else {
                    purlSet = mapper.readValue(existingPurl, Set.class);
                }
            } catch (IOException e) {
                purlSet.add(existingPurl);
            }
        }
        return purlSet;
    }
}
