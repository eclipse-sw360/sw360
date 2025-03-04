/*
 * Copyright Siemens Healthineers GmBH, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.thrift.packages.*;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.helper.PackageHelper;
import java.util.*;
import java.util.stream.Collectors;
import static org.eclipse.sw360.datahandler.common.SW360Utils.displayNameFor;
import static org.eclipse.sw360.datahandler.thrift.packages.Package._Fields.*;
import static org.eclipse.sw360.exporter.ReleaseExporter.RELEASE_HEADERS_PROJECT_EXPORT;


public class PackageExporter extends ExcelExporter<Package, PackageHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put(Package._Fields.ID.getFieldName(), "package ID");
        nameToDisplayName.put(Package._Fields.NAME.getFieldName(), "package name");
        nameToDisplayName.put(Package._Fields.VERSION.getFieldName(), "package version");
        nameToDisplayName.put(Package._Fields.DESCRIPTION.getFieldName(), "package description");
        nameToDisplayName.put(Package._Fields.VENDOR.getFieldName(), "package vendor");
        nameToDisplayName.put(Package._Fields.LICENSE_IDS.getFieldName(), "license IDs");
        nameToDisplayName.put(Package._Fields.PACKAGE_TYPE.getFieldName(), "package type");
        nameToDisplayName.put(Package._Fields.PURL.getFieldName(), "purl");
        nameToDisplayName.put(Package._Fields.RELEASE.getFieldName(), "release");
    }


    public static final List<Package._Fields> PACKAGE_REQUIRED_FIELDS = ImmutableList.<Package._Fields>builder()
            .add(ID)
            .add(NAME)
            .add(VERSION)
            .add(PURL)
            .add(PACKAGE_TYPE)
            .add(RELEASE)
            .build();

    public static final List<Package._Fields> PACKAGE_RENDERED_FIELDS_PROJECTS = Package.metaDataMap.keySet()
            .stream()
            .filter(k -> PACKAGE_REQUIRED_FIELDS.contains(k))
            .collect(Collectors.toList());


    public PackageExporter(PackageService.Iface packageClient, User user, ComponentService.Iface cClient) {
        super(new PackageHelper(packageClient, cClient, user));
    }

    public static final List<String> PACKAGE_HEADERS_PROJECT_EXPORT = makePackageHeadersForProjectExport();

    private static List<String> makePackageHeadersForProjectExport() {
        List<String> headers = new ArrayList<>();
        for (Package._Fields field : PACKAGE_RENDERED_FIELDS_PROJECTS) {
            addToHeadersForProjectExport(headers, field);
        }
        return headers;
    }

    private static void addToHeadersForProjectExport(List<String> headers, Package._Fields field) {
        switch (field) {
            case RELEASE:
                headers.addAll(RELEASE_HEADERS_PROJECT_EXPORT);
                break;
            default:
                headers.add(displayNameFor(field.getFieldName(), nameToDisplayName));
        }
    }
}