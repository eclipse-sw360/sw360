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
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedSW360Exception;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putAccessibleReleaseNamesInMap;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapSW360Exception;
import static org.eclipse.sw360.exporter.ReleaseExporter.*;

public class ReleaseHelper implements ExporterHelper<Release> {

    private static final Logger log = LogManager.getLogger(ReleaseHelper.class);

    private final ComponentService.Iface cClient;
    private final User user;
    private Map<Release, ReleaseClearingStatusData> releaseClearingStatusDataByRelease = null;
    private Map<String, Release> preloadedLinkedReleases = null;
    private Map<String, Component> preloadedComponents = null;

    /**
     * if a not empty map is assigned to this field, additional data has to be added
     * to each row
     */
    private final List<ReleaseClearingStatusData> releaseClearingStatuses;

    /**
     * Remember to preload the releases and set them via
     * {{@link #setPreloadedLinkedReleases(List, boolean)} so that we can also preload the
     * necessary components. If you miss that step, we will have to load each
     * component separately on demand which might take some additional time.
     *
     * @param cClient
     *            a {@link ComponentService.Iface} implementation
     * @throws SW360Exception
     */
    public ReleaseHelper(ComponentService.Iface cClient, User user) throws SW360Exception {
        this(cClient, user, null);
    }

    /**
     * If you do not want to get the additional data by setting
     * releaseClearingStatuses, then you probably want to use the alternative
     * constructor and read its instructions.
     *
     * @param cClient
     *            a {@link ComponentService.Iface} implementation
     * @param releaseClearingStatuses
     *            has to be given if additional data like component type and project
     *            origin should be included - may be <code>null</code> or an empty
     *            map otherwise
     * @throws SW360Exception
     */
    public ReleaseHelper(ComponentService.Iface cClient, User user,
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
        if (release.isSetPermissions() && release.getPermissions().get(RequestedAction.READ)) {
            for (Release._Fields renderedField : RELEASE_RENDERED_FIELDS) {
                addFieldValueToRow(row, renderedField, release, false);
            }
        } else {
            for (Release._Fields renderedField : RELEASE_RENDERED_FIELDS) {
                addInaccessibleFieldValueToRow(row, renderedField, release, false);
            }
        }
        return new SubTable(row);
    }

    public SubTable makeCustomRowsForProjectExport(Release release) throws SW360Exception {
        List<String> row = new ArrayList<>();
        if (release.isSetPermissions() && release.getPermissions().get(RequestedAction.READ)) {
            for (Release._Fields renderedField : RELEASE_RENDERED_FIELDS_PROJECTS) {
                addFieldValueToRow(row, renderedField, release, true);
            }
        } else {
            for (Release._Fields renderedField : RELEASE_RENDERED_FIELDS_PROJECTS) {
                addInaccessibleFieldValueToRow(row, renderedField, release, true);
            }
        }
        return new SubTable(row);
    }


    protected void addFieldValueToRow(List<String> row, Release._Fields field, Release release, boolean isForProjectExport) throws SW360Exception {
        switch (field) {
            case COMPONENT_ID:
                // first, add data for given field
                if (!isForProjectExport) {
                    row.add(release.getComponentId());
                }

                // second, add joined data, remark that headers have already been added
                // accordingly

                // add component type and categories in every case
                Component component = this.preloadedComponents.get(release.componentId);
                if (component == null) {
                    // maybe cache was not initialized properly, so try to load manually
                    try {
                        component = cClient.getComponentById(release.getComponentId(), user);
                    } catch (TException e) {
                        log.warn("No component found for id " + release.getComponentId()
                                + " which is set in release with id " + release.getId(), e);
                        component = null;
                    }
                }
                // check again and add value
                if (component == null) {
                    row.add("");
                } else {
                    row.add(ThriftEnumUtils.enumToString(component.getComponentType()));
                }

                // project origin and project mainline state only if wanted
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

            case VENDOR:
                addVendorToRow(release.getVendor(), row);
                break;
            case COTS_DETAILS:
                addCotsDetailsToRow(release.getCotsDetails(), row);
                break;
            case CLEARING_INFORMATION:
                addClearingInformationToRow(release.getClearingInformation(), row);
                break;
            case ECC_INFORMATION:
                addEccInformationToRow(release.getEccInformation(), row, isForProjectExport);
                break;
            case RELEASE_ID_TO_RELATIONSHIP:
                addReleaseIdToRelationShipToRow(release.getReleaseIdToRelationship(), row);
                break;
            case ATTACHMENTS:
                if(isForProjectExport) {
                	addSourceAttachmentsInformationToRow(release, row);
                }else {
                    String size = Integer.toString(release.isSetAttachments() ? release.getAttachments().size() : 0);
                    row.add(size);
                }
                break;
            default:
                Object fieldValue = release.getFieldValue(field);
                row.add(fieldValueAsString(fieldValue));
        }
    }    
    
    private void addSourceAttachmentsInformationToRow(Release release, List<String> row) throws SW360Exception{
        Set<Attachment> attachments = CommonUtils.nullToEmptySet(release.getAttachments());
        final Predicate<Attachment> isSourceAttachment = attachment -> AttachmentType.SOURCE.equals(attachment.getAttachmentType()) || AttachmentType.SOURCE_SELF.equals(attachment.getAttachmentType());
        String result = attachments.stream()
                .filter(isSourceAttachment)
                .map(Attachment::getFilename)
                .collect(Collectors.joining(", "));

        row.add(result);
    }

    private void addInaccessibleFieldValueToRow(List<String> row, Release._Fields field, Release release, boolean isForProjectExport) throws SW360Exception {
        switch (field) {
            case NAME:
                row.add(SW360Utils.INACCESSIBLE_RELEASE);
                break;
            case COMPONENT_ID:
                //component id
                row.add("");
                // component type
                row.add("");
                // project origin and project mainline state only if wanted
                if (addAdditionalData()) {
                    //Project names
                    row.add("");
                    // Mainline states
                    row.add("");
                }
                //component categories
                row.add("");
                break;

            case VENDOR:
                addVendorToRow(null, row);
                break;
            case COTS_DETAILS:
                addCotsDetailsToRow(null, row);
                break;
            case CLEARING_INFORMATION:
                addClearingInformationToRow(null, row);
                break;
            case ECC_INFORMATION:
                addEccInformationToRow(null, row, isForProjectExport);
                break;
            case RELEASE_ID_TO_RELATIONSHIP:
                addReleaseIdToRelationShipToRow(null, row);
                break;
            case ATTACHMENTS:
                String size = Integer.toString(0);
                row.add(size);
                break;
            default:
                row.add("");
        }
    }

    private void addVendorToRow(Vendor vendor, List<String> row) throws SW360Exception {
        try {
            Vendor.metaDataMap
                    .keySet()
                    .stream()
                    .filter(f -> !VENDOR_IGNORED_FIELDS.contains(f))
                    .forEach(f -> wrapSW360Exception(() -> {
                        if (vendor != null && vendor.isSet(f)) {
                            row.add(fieldValueAsString(vendor.getFieldValue(f)));
                        } else {
                            row.add("");
                        }
                    }));
        } catch (WrappedSW360Exception e) {
            throw e.getCause();
        }

    }

    private void addCotsDetailsToRow(COTSDetails cotsDetails, List<String> row) throws SW360Exception {
        try {
            COTSDetails.metaDataMap.keySet().forEach(f -> wrapSW360Exception(() -> {
                if (cotsDetails != null && cotsDetails.isSet(f)) {
                    row.add(fieldValueAsString(cotsDetails.getFieldValue(f)));
                } else {
                    row.add("");
                }
            }));
        } catch (WrappedSW360Exception e) {
            throw e.getCause();
        }
    }

    private void addClearingInformationToRow(ClearingInformation clearingInformation, List<String> row) throws SW360Exception {
        try {
            ClearingInformation.metaDataMap.keySet().forEach(f -> wrapSW360Exception(() -> {
                if (clearingInformation != null && clearingInformation.isSet(f)) {
                    row.add(fieldValueAsString(clearingInformation.getFieldValue(f)));
                } else {
                    row.add("");
                }
            }));
        } catch (WrappedSW360Exception e) {
            throw e.getCause();
        }
    }

    private void addEccInformationToRow(EccInformation eccInformation, List<String> row, boolean isForProjectExport) throws SW360Exception {
        try {
            EccInformation.metaDataMap.keySet().stream().filter(e -> (!isForProjectExport || !ReleaseExporter.ECC_IGNORE_FIELDS.contains(e))).forEach(f -> wrapSW360Exception(() -> {
                if (eccInformation != null && eccInformation.isSet(f)) {
                    row.add(fieldValueAsString(eccInformation.getFieldValue(f)));
                } else {
                    row.add("");
                }
            }));
        } catch (WrappedSW360Exception e) {
            throw e.getCause();
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

    public List<Release> getAllReleases(Set<String> ids) throws SW360Exception {
        List<Release> releasesByIdsForExport;
        try {
            releasesByIdsForExport = cClient.getReleasesWithAccessibilityByIdsForExport(nullToEmptySet(ids), user);
        } catch (TException e) {
            throw new SW360Exception("Error fetching release information");
        }
        return releasesByIdsForExport;
    }


    public List<Release> getReleases(Set<String> ids) throws SW360Exception {
        if (preloadedLinkedReleases != null){
            return getPreloadedReleases(ids);
        }
        List<Release> releasesByIdsForExport;
        try {
            releasesByIdsForExport = cClient.getReleasesWithAccessibilityByIdsForExport(nullToEmptySet(ids), user);
        } catch (TException e) {
            throw new SW360Exception("Error fetching release information");
        }

        // update preload cache so that it is available on next call to this method
        setPreloadedLinkedReleases(ThriftUtils.getIdMap(releasesByIdsForExport), false);

        return releasesByIdsForExport;
    }

    private List<Release> getPreloadedReleases(Set<String> ids) {
        return ids.stream().map(preloadedLinkedReleases::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
