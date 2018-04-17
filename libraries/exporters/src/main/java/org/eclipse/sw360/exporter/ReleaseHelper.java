/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedSW360Exception;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putReleaseNamesInMap;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapSW360Exception;
import static org.eclipse.sw360.exporter.ReleaseExporter.*;

class ReleaseHelper implements ExporterHelper<Release> {

    private static final Logger log = Logger.getLogger(ReleaseHelper.class);

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
    protected ReleaseHelper(ComponentService.Iface cClient, User user) throws SW360Exception {
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
    protected ReleaseHelper(ComponentService.Iface cClient, User user,
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

    @Override
    public SubTable makeRows(Release release) throws SW360Exception {
        List<String> row = new ArrayList<>();
        for (Release._Fields renderedField : RELEASE_RENDERED_FIELDS) {
            addFieldValueToRow(row, renderedField, release);
        }
        return new SubTable(row);
    }

    private void addFieldValueToRow(List<String> row, Release._Fields field, Release release) throws SW360Exception {
        switch (field) {
            case COMPONENT_ID:
                // first, add data for given field
                row.add(release.getComponentId());

                // second, add joined data, remark that headers have already been added
                // accordingly

                // add component type in every case
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

                // and project origin only if wanted
                if (addAdditionalData()) {
                    if (releaseClearingStatusDataByRelease.containsKey(release)) {
                        row.add(releaseClearingStatusDataByRelease.get(release).getProjectNames());
                    } else {
                        row.add("");
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
                addEccInformationToRow(release.getEccInformation(), row);
                break;
            case RELEASE_ID_TO_RELATIONSHIP:
                addReleaseIdToRelationShipToRow(release.getReleaseIdToRelationship(), row);
                break;
            case ATTACHMENTS:
                String size = Integer.toString(release.isSetAttachments() ? release.getAttachments().size() : 0);
                row.add(size);
                break;
            default:
                Object fieldValue = release.getFieldValue(field);
                row.add(fieldValueAsString(fieldValue));
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

    private void addEccInformationToRow(EccInformation eccInformation, List<String> row) throws SW360Exception {
        try {
            EccInformation.metaDataMap.keySet().forEach(f -> wrapSW360Exception(() -> {
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
            row.add(fieldValueAsString(putReleaseNamesInMap(releaseIdToRelationship, getReleases(releaseIdToRelationship
                    .keySet()))));
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

    List<Release> getReleases(Set<String> ids) throws SW360Exception {
        if (preloadedLinkedReleases != null){
            return getPreloadedReleases(ids);
        }
        List<Release> releasesByIdsForExport;
        try {
            releasesByIdsForExport = cClient.getReleasesByIdsForExport(nullToEmptySet(ids));
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
