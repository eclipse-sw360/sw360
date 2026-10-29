/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.EnumDisplayNames;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.components.ClearingInformation;
import org.eclipse.sw360.datahandler.services.components.COTSDetails;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.exporter.ComponentExportClient;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putAccessibleReleaseNamesInMap;
import static org.eclipse.sw360.exporter.ReleaseExporter.ECC_IGNORE_FIELDS;
import static org.eclipse.sw360.exporter.ReleaseExporter.HEADERS;
import static org.eclipse.sw360.exporter.ReleaseExporter.HEADERS_EXTENDED_BY_ADDITIONAL_DATA;
import static org.eclipse.sw360.exporter.ReleaseExporter.HEADERS_EXTENDED_BY_ADDITIONAL_DATA_PROJECT;
import static org.eclipse.sw360.exporter.ReleaseExporter.RELEASE_RENDERED_FIELDS;
import static org.eclipse.sw360.exporter.ReleaseExporter.RELEASE_RENDERED_FIELDS_PROJECTS;
import static org.eclipse.sw360.exporter.ReleaseExporter.VENDOR_IGNORED_FIELDS;

public class ReleaseHelper implements ExporterHelper<Release> {

    private static final Logger log = LogManager.getLogger(ReleaseHelper.class);

    private final ComponentExportClient cClient;
    private final User user;
    private Map<Release, ReleaseClearingStatusData> releaseClearingStatusDataByRelease = null;
    private Map<String, Release> preloadedLinkedReleases = null;
    private Map<String, Component> preloadedComponents = null;

    private final List<ReleaseClearingStatusData> releaseClearingStatuses;

    public ReleaseHelper(ComponentExportClient cClient, User user) throws SW360Exception {
        this(cClient, user, null);
    }

    public ReleaseHelper(ComponentExportClient cClient, User user,
                            List<ReleaseClearingStatusData> releaseClearingStatuses) throws SW360Exception {
        this.cClient = cClient;
        this.user = user;
        this.releaseClearingStatuses = releaseClearingStatuses;
        this.preloadedComponents = new HashMap<>();

        if (this.releaseClearingStatuses != null) {
            batchloadComponents(this.releaseClearingStatuses.stream().map(rcs -> rcs.getRelease().getComponentId())
                    .collect(Collectors.toSet()));
            this.releaseClearingStatusDataByRelease = releaseClearingStatuses.stream().collect(Collectors.toMap(ReleaseClearingStatusData::getRelease, rcs -> rcs));
        }
    }

    @Override
    public int getColumns() {
        return getHeaders().size();
    }

    @Override
    public List<String> getHeaders() {
        return addAdditionalData() ? HEADERS_EXTENDED_BY_ADDITIONAL_DATA : HEADERS;
    }

    public int getColumnsProjExport() {
        return getHeadersProjExport().size();
    }

    public List<String> getHeadersProjExport() {
        return addAdditionalData() ? HEADERS_EXTENDED_BY_ADDITIONAL_DATA_PROJECT : HEADERS;
    }

    @Override
    public SubTable makeRows(Release release) throws SW360Exception {
        List<String> row = new ArrayList<>();
        if (canRead(release)) {
            for (FieldAccessor<Release> renderedField : RELEASE_RENDERED_FIELDS) {
                addFieldValueToRow(row, renderedField, release, false);
            }
        } else {
            for (FieldAccessor<Release> renderedField : RELEASE_RENDERED_FIELDS) {
                addInaccessibleFieldValueToRow(row, renderedField, release, false);
            }
        }
        return new SubTable(row);
    }

    public SubTable makeCustomRowsForProjectExport(Release release) throws SW360Exception {
        List<String> row = new ArrayList<>();
        if (canRead(release)) {
            for (FieldAccessor<Release> renderedField : RELEASE_RENDERED_FIELDS_PROJECTS) {
                addFieldValueToRow(row, renderedField, release, true);
            }
        } else {
            for (FieldAccessor<Release> renderedField : RELEASE_RENDERED_FIELDS_PROJECTS) {
                addInaccessibleFieldValueToRow(row, renderedField, release, true);
            }
        }
        return new SubTable(row);
    }

    private static boolean canRead(Release release) {
        Map<RequestedAction, Boolean> permissions = release.getPermissions();
        return permissions != null && Boolean.TRUE.equals(permissions.get(RequestedAction.READ));
    }

    private void addFieldValueToRow(List<String> row, FieldAccessor<Release> field, Release release, boolean isForProjectExport) throws SW360Exception {
        switch (field.name()) {
            case "componentId":
                if (!isForProjectExport) {
                    row.add(release.getComponentId());
                }

                Component component = this.preloadedComponents.get(release.getComponentId());
                if (component == null) {
                    try {
                        component = cClient.getComponentById(release.getComponentId(), user);
                    } catch (TException e) {
                        log.warn("No component found for id " + release.getComponentId()
                                + " which is set in release with id " + release.getId(), e);
                        component = null;
                    }
                }
                if (component == null || component.getComponentType() == null) {
                    row.add("");
                } else {
                    row.add(EnumDisplayNames.toDisplayString(component.getComponentType()));
                }

                if (addAdditionalData()) {
                    if (releaseClearingStatusDataByRelease.containsKey(release)) {
                        ReleaseClearingStatusData releaseClearingData = releaseClearingStatusDataByRelease.get(release);
                        row.add(releaseClearingData.getProjectNames());
                        row.add(releaseClearingData.getMainlineStates());
                    } else {
                        row.add("");
                        row.add("");
                    }
                }
                if (!isForProjectExport) {
                    if (component == null) {
                        row.add("");
                    } else {
                        if (component.getCategories() == null) {
                            row.add("");
                        } else {
                            row.add(component.getCategories().toString());
                        }
                    }
                }

                break;

            case "vendor":
                addVendorToRow(release.getVendor(), row);
                break;
            case "cotsDetails":
                addNestedValues(row, release.getCotsDetails(), COTSDetails.class, Set.of());
                break;
            case "clearingInformation":
                addNestedValues(row, release.getClearingInformation(), ClearingInformation.class, Set.of());
                break;
            case "eccInformation":
                addNestedValues(row, release.getEccInformation(), EccInformation.class,
                        isForProjectExport ? ECC_IGNORE_FIELDS : Set.of());
                break;
            case "releaseIdToRelationship":
                addReleaseIdToRelationShipToRow(release.getReleaseIdToRelationship(), row);
                break;
            case "attachments":
                if (isForProjectExport) {
                    addSourceAttachmentsInformationToRow(release, row);
                } else {
                    String size = Integer.toString(release.getAttachments() != null ? release.getAttachments().size() : 0);
                    row.add(size);
                }
                break;
            default:
                Object fieldValue = field.get(release);
                row.add(fieldValueAsString(fieldValue));
        }
    }

    private void addSourceAttachmentsInformationToRow(Release release, List<String> row) throws SW360Exception {
        Set<Attachment> attachments = CommonUtils.nullToEmptySet(release.getAttachments());
        final Predicate<Attachment> isSourceAttachment = attachment -> AttachmentType.SOURCE.equals(attachment.getAttachmentType()) || AttachmentType.SOURCE_SELF.equals(attachment.getAttachmentType());
        String result = attachments.stream()
                .filter(isSourceAttachment)
                .map(Attachment::getFilename)
                .collect(Collectors.joining(", "));

        row.add(result);
    }

    private void addInaccessibleFieldValueToRow(List<String> row, FieldAccessor<Release> field, Release release, boolean isForProjectExport) throws SW360Exception {
        switch (field.name()) {
            case "name":
                row.add(SW360Utils.INACCESSIBLE_RELEASE);
                break;
            case "componentId":
                row.add("");
                row.add("");
                if (addAdditionalData()) {
                    row.add("");
                    row.add("");
                }
                row.add("");
                break;

            case "vendor":
                addVendorToRow(null, row);
                break;
            case "cotsDetails":
                addNestedValues(row, null, COTSDetails.class, Set.of());
                break;
            case "clearingInformation":
                addNestedValues(row, null, ClearingInformation.class, Set.of());
                break;
            case "eccInformation":
                addNestedValues(row, null, EccInformation.class, isForProjectExport ? ECC_IGNORE_FIELDS : Set.of());
                break;
            case "releaseIdToRelationship":
                addReleaseIdToRelationShipToRow(null, row);
                break;
            case "attachments":
                row.add(Integer.toString(0));
                break;
            default:
                row.add("");
        }
    }

    private void addVendorToRow(Vendor vendor, List<String> row) throws SW360Exception {
        addNestedValues(row, vendor, Vendor.class, VENDOR_IGNORED_FIELDS);
    }

    private <T> void addNestedValues(List<String> row, T value, Class<T> type, Set<String> ignored) throws SW360Exception {
        for (FieldAccessor<T> field : PojoFields.of(type)) {
            if (ignored.contains(field.name())) {
                continue;
            }
            if (value != null && field.isSet(value)) {
                row.add(fieldValueAsString(field.get(value)));
            } else {
                row.add("");
            }
        }
    }

    private void addReleaseIdToRelationShipToRow(Map<String, ReleaseRelationship> releaseIdToRelationship, List<String> row) throws SW360Exception {
        if (releaseIdToRelationship != null) {
            row.add(fieldValueAsString(putAccessibleReleaseNamesInMap(releaseIdToRelationship, getReleases(releaseIdToRelationship
                    .keySet()), user, ReleaseRelationship.UNKNOWN)));
        } else {
            row.add("");
        }
    }

    private boolean addAdditionalData() {
        return !nullToEmptyList(releaseClearingStatuses).isEmpty();
    }

    public void setPreloadedLinkedReleases(Map<String, Release> preloadedLinkedReleases, boolean componentsNeeded)
            throws SW360Exception {
        this.preloadedLinkedReleases = preloadedLinkedReleases;

        if (componentsNeeded) {
            this.batchloadComponents(
                    preloadedLinkedReleases.values().stream().map(Release::getComponentId).collect(Collectors.toSet()));
        }
    }

    private void batchloadComponents(Set<String> cIds) throws SW360Exception {
        try {
            List<Component> componentsShort = cClient.getComponentsShort(cIds);

            this.preloadedComponents.putAll(ThriftUtils.getIdMap(componentsShort));
        } catch (TException e) {
            throw new SW360Exception("Could not get Components for ids [" + cIds + "] because of:\n" + e.getMessage());
        }
    }

    public List<Release> getReleases(Set<String> ids) throws SW360Exception {
        if (preloadedLinkedReleases != null) {
            return getPreloadedReleases(ids);
        }
        List<Release> releasesByIdsForExport;
        try {
            releasesByIdsForExport = cClient.getReleasesWithAccessibilityByIdsForExport(nullToEmptySet(ids), user);
        } catch (TException e) {
            throw new SW360Exception("Error fetching release information");
        }

        setPreloadedLinkedReleases(ThriftUtils.getIdMap(releasesByIdsForExport), false);

        return releasesByIdsForExport;
    }

    private List<Release> getPreloadedReleases(Set<String> ids) {
        return ids.stream().map(preloadedLinkedReleases::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
