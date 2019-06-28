/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.exporter.helper.ReleaseHelper;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Utils.displayNameFor;
import static org.eclipse.sw360.datahandler.thrift.components.Release._Fields.*;


public class ReleaseExporter extends ExcelExporter<Release, ReleaseHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put(Release._Fields.ID.getFieldName(), "release ID");
        nameToDisplayName.put(Release._Fields.CPEID.getFieldName(), "CPE ID");
        nameToDisplayName.put(Release._Fields.COMPONENT_ID.getFieldName(), "component ID");
        nameToDisplayName.put(Release._Fields.RELEASE_DATE.getFieldName(), "release date");
        nameToDisplayName.put(Release._Fields.EXTERNAL_IDS.getFieldName(), "external IDs");
        nameToDisplayName.put(Release._Fields.CREATED_ON.getFieldName(), "created on");
        nameToDisplayName.put(Release._Fields.CREATED_BY.getFieldName(), "created by");
        nameToDisplayName.put(Release._Fields.MAINLINE_STATE.getFieldName(), "mainline state");
        nameToDisplayName.put(Release._Fields.CLEARING_STATE.getFieldName(), "clearing state");
        nameToDisplayName.put(Release._Fields.FOSSOLOGY_ID.getFieldName(), "fossology id");
        nameToDisplayName.put(Release._Fields.CLEARING_TEAM_TO_FOSSOLOGY_STATUS.getFieldName(), "clearing team with FOSSology status");
        nameToDisplayName.put(Release._Fields.ATTACHMENT_IN_FOSSOLOGY.getFieldName(), "attachment in FOSSology");
        nameToDisplayName.put(Release._Fields.CLEARING_INFORMATION.getFieldName(), "clearing information");
        nameToDisplayName.put(Release._Fields.ECC_INFORMATION.getFieldName(), "ECC information");
        nameToDisplayName.put(Release._Fields.COTS_DETAILS.getFieldName(), "COTS details");
        nameToDisplayName.put(Release._Fields.MAIN_LICENSE_IDS.getFieldName(), "main license IDs");
        nameToDisplayName.put(Release._Fields.DOWNLOADURL.getFieldName(), "downloadurl");
        nameToDisplayName.put(Release._Fields.RELEASE_ID_TO_RELATIONSHIP.getFieldName(), "releases with relationship");
        nameToDisplayName.put(Release._Fields.OPERATING_SYSTEMS.getFieldName(), "operating systems");
    }

    public static final List<Release._Fields> RELEASE_IGNORED_FIELDS = ImmutableList.<Release._Fields>builder()
            .add(REVISION)
            .add(DOCUMENT_STATE)
            .add(PERMISSIONS)
            .add(VENDOR_ID)
            .build();

    public static final List<Vendor._Fields> VENDOR_IGNORED_FIELDS = ImmutableList.<Vendor._Fields>builder()
            .add(Vendor._Fields.PERMISSIONS)
            .add(Vendor._Fields.REVISION)
            .add(Vendor._Fields.ID)
            .add(Vendor._Fields.TYPE)
            .build();

    public static final List<Release._Fields> RELEASE_RENDERED_FIELDS = Release.metaDataMap.keySet()
            .stream()
            .filter(k -> !RELEASE_IGNORED_FIELDS.contains(k))
            .collect(Collectors.toList());

    public static final List<String> HEADERS = makeHeaders();

    public static final List<String> HEADERS_EXTENDED_BY_ADDITIONAL_DATA = makeHeadersForExtendedExport();

    public ReleaseExporter(ComponentService.Iface cClient, List<Release> releases, User user,
            List<ReleaseClearingStatusData> releaseClearingStatuses) throws SW360Exception {
        super(new ReleaseHelper(cClient, user, releaseClearingStatuses));
        preloadLinkedReleasesFor(releases);
    }

    private static List<String> makeHeaders() {
        List<String> headers = new ArrayList<>();
        for (Release._Fields field : RELEASE_RENDERED_FIELDS) {
            addToHeaders(headers, field);
        }
        return headers;
    }

    private static List<String> makeHeadersForExtendedExport() {
        List<String> additionalHeaders = new ArrayList<>();
        additionalHeaders.add(displayNameFor("project origin", nameToDisplayName));
        additionalHeaders.add(displayNameFor("project mainline state", nameToDisplayName));

        List<String> completeHeaders = new ArrayList<>();
        completeHeaders.addAll(makeHeaders());
        completeHeaders.addAll(
                // add after component type which is directly after component_id
                completeHeaders.indexOf(displayNameFor(COMPONENT_ID.getFieldName(), nameToDisplayName)) + 2,
                additionalHeaders);

        return completeHeaders;
    }

    private static void addToHeaders(List<String> headers, Release._Fields field) {
        switch (field) {
            case COMPONENT_ID:
                headers.add(displayNameFor(field.getFieldName(), nameToDisplayName));
                headers.add(displayNameFor("component type", nameToDisplayName));
                headers.add(displayNameFor("categories", nameToDisplayName));
                break;
            case VENDOR:
                Vendor.metaDataMap.keySet().stream()
                        .filter(f -> ! VENDOR_IGNORED_FIELDS.contains(f))
                        .forEach(f -> headers.add("vendor " + f.getFieldName()));
                break;
            case COTS_DETAILS:
                COTSDetails.metaDataMap.keySet()
                        .forEach(f -> headers.add("COTS details: " + f.getFieldName()));
                break;
            case CLEARING_INFORMATION:
                ClearingInformation.metaDataMap.keySet()
                        .forEach(f -> headers.add("clearing information: " + f.getFieldName()));
                break;
            case ECC_INFORMATION:
                EccInformation.metaDataMap.keySet()
                        .forEach(f -> headers.add("ECC information: " + f.getFieldName()));
                break;
            default:
                headers.add(displayNameFor(field.getFieldName(), nameToDisplayName));
        }
    }

    private void preloadLinkedReleasesFor(List<Release> releases) throws SW360Exception {
        Set<String> linkedReleaseIds = releases
                .stream()
                .map(Release::getReleaseIdToRelationship)
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Map<String, Release> releasesById = ThriftUtils.getIdMap(helper.getReleases(linkedReleaseIds));
        helper.setPreloadedLinkedReleases(releasesById, true);
    }
}
