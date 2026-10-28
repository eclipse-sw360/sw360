/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.components.ClearingInformation;
import org.eclipse.sw360.datahandler.services.components.COTSDetails;
import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.exporter.helper.ReleaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Utils.displayNameFor;

public class ReleaseExporter extends ExcelExporter<Release, ReleaseHelper> {

    private static final Map<String, String> nameToDisplayName;

    public static final Set<String> VENDOR_IGNORED_FIELDS = Set.of("permissions", "revision", "id", "type");

    public static final Set<String> ECC_IGNORE_FIELDS = Set.of(
            "materialIndexNumber", "assessorContactPerson", "eccComment");

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put("id", "release ID");
        nameToDisplayName.put("cpeid", "CPE ID");
        nameToDisplayName.put("componentId", "component ID");
        nameToDisplayName.put("releaseDate", "release date");
        nameToDisplayName.put("externalIds", "release: external IDs (SVM-ID)");
        nameToDisplayName.put("createdOn", "created on");
        nameToDisplayName.put("createdBy", "created by");
        nameToDisplayName.put("mainlineState", "mainline state");
        nameToDisplayName.put("clearingState", "clearing state");
        nameToDisplayName.put("externalToolProcesses", "external tool process");
        nameToDisplayName.put("clearingInformation", "clearing information");
        nameToDisplayName.put("eccInformation", "ECC information");
        nameToDisplayName.put("cotsDetails", "COTS details");
        nameToDisplayName.put("mainLicenseIds", "main license IDs");
        nameToDisplayName.put("otherLicenseIds", "other license IDs");
        nameToDisplayName.put("sourceCodeDownloadurl", "Source Code Downloadurl");
        nameToDisplayName.put("binaryDownloadurl", "Binary Downloadurl");
        nameToDisplayName.put("releaseIdToRelationship", "releases with relationship");
        nameToDisplayName.put("operatingSystems", "operating systems");
    }

    public static final List<FieldAccessor<Release>> RELEASE_RENDERED_FIELDS = ExporterFields.except(
            Release.class, "revision", "documentState", "permissions", "vendorId");

    public static final List<FieldAccessor<Release>> RELEASE_RENDERED_FIELDS_PROJECTS = ExporterFields.only(
            Release.class,
            "id",
            "componentId",
            "name",
            "version",
            "mainLicenseIds",
            "otherLicenseIds",
            "sourceCodeDownloadurl",
            "clearingState",
            "eccInformation",
            "vendor",
            "externalIds",
            "attachments");

    public static final List<String> HEADERS = makeHeaders();

    public static final List<String> HEADERS_EXTENDED_BY_ADDITIONAL_DATA = makeHeadersForExtendedExport();

    public static final List<String> RELEASE_HEADERS_PROJECT_EXPORT = makeReleaseHeadersForProjectExport();

    public static final List<String> HEADERS_EXTENDED_BY_ADDITIONAL_DATA_PROJECT = makeHeadersForExtendedExportProject();

    private static List<String> makeReleaseHeadersForProjectExport() {
        List<String> headers = new ArrayList<>();
        for (FieldAccessor<Release> field : RELEASE_RENDERED_FIELDS_PROJECTS) {
            addToHeadersForProjectExport(headers, field);
        }
        return headers;
    }

    private static void addToHeadersForProjectExport(List<String> headers, FieldAccessor<Release> field) {
        switch (field.name()) {
        case "componentId":
            headers.add(displayNameFor("component type", nameToDisplayName));
            break;
        case "vendor":
            addNestedHeaders(headers, "vendor ", Vendor.class, VENDOR_IGNORED_FIELDS);
            break;
        case "cotsDetails":
            addNestedHeaders(headers, "COTS details: ", COTSDetails.class, Set.of());
            break;
        case "clearingInformation":
            addNestedHeaders(headers, "clearing information: ", ClearingInformation.class, Set.of());
            break;
        case "eccInformation":
            addNestedHeaders(headers, "ECC information: ", EccInformation.class, ECC_IGNORE_FIELDS);
            break;
        case "attachments":
            headers.add("SRC/SRS Attachments");
            break;
        default:
            headers.add(displayNameFor(field.name(), nameToDisplayName));
        }
    }

    private static List<String> makeHeadersForExtendedExportProject() {
        List<String> additionalHeaders = new ArrayList<>();
        additionalHeaders.add(displayNameFor("project origin", nameToDisplayName));
        additionalHeaders.add(displayNameFor("project mainline state", nameToDisplayName));

        List<String> completeHeaders = new ArrayList<>();
        completeHeaders.addAll(makeHeadersForExtendedExport());
        completeHeaders.addAll(
                completeHeaders.indexOf(displayNameFor("componentId", nameToDisplayName)) + 2,
                additionalHeaders);

        return completeHeaders;
    }

    public ReleaseExporter(ComponentExportClient cClient, List<Release> releases, User user,
            List<ReleaseClearingStatusData> releaseClearingStatuses) throws SW360Exception {
        super(new ReleaseHelper(cClient, user, releaseClearingStatuses));
        preloadLinkedReleasesFor(releases);
    }

    private static List<String> makeHeaders() {
        List<String> headers = new ArrayList<>();
        for (FieldAccessor<Release> field : RELEASE_RENDERED_FIELDS) {
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
                completeHeaders.indexOf(displayNameFor("componentId", nameToDisplayName)) + 2,
                additionalHeaders);

        return completeHeaders;
    }

    private static void addToHeaders(List<String> headers, FieldAccessor<Release> field) {
        switch (field.name()) {
            case "componentId":
                headers.add(displayNameFor(field.name(), nameToDisplayName));
                headers.add(displayNameFor("component type", nameToDisplayName));
                headers.add(displayNameFor("categories", nameToDisplayName));
                break;
            case "vendor":
                addNestedHeaders(headers, "vendor ", Vendor.class, VENDOR_IGNORED_FIELDS);
                break;
            case "cotsDetails":
                addNestedHeaders(headers, "COTS details: ", COTSDetails.class, Set.of());
                break;
            case "clearingInformation":
                addNestedHeaders(headers, "clearing information: ", ClearingInformation.class, Set.of());
                break;
            case "eccInformation":
                addNestedHeaders(headers, "ECC information: ", EccInformation.class, Set.of());
                break;
            default:
                headers.add(displayNameFor(field.name(), nameToDisplayName));
        }
    }

    private static void addNestedHeaders(List<String> headers, String prefix, Class<?> type, Set<String> ignored) {
        for (FieldAccessor<?> field : PojoFields.of(type)) {
            if (!ignored.contains(field.name())) {
                headers.add(prefix + field.name());
            }
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
