/*
 * Copyright Siemens Healthineers GmBH, 2025. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.utils.SubTable;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.eclipse.sw360.exporter.PackageExporter.*;
import static org.eclipse.sw360.exporter.ProjectExporter.HEADERS;
import static org.eclipse.sw360.exporter.ReleaseExporter.*;

public class PackageHelper implements ExporterHelper<Package> {
    private static final Logger log = LogManager.getLogger(PackageHelper.class);
    private final PackageService.Iface packageClient;
    private final ComponentService.Iface cClient;
    private final User user;
    public static final Set<String> RELEASES_LINKED_TO_PACKAGES = new HashSet<>();

    public PackageHelper(PackageService.Iface packageClient, ComponentService.Iface cClient, User user) {
        this.packageClient = packageClient;
        this.cClient = cClient;
        this.user = user;
    }

    public int getColumns() {
        return getHeaders().size();
    }

    public int getColumnsProjExport() {
        return getHeadersProjExport().size();
    }

    public List<String> getHeadersProjExport() {
        return PACKAGE_HEADERS_PROJECT_EXPORT;
    }

    @Override
    public List<String> getHeaders() {
        return HEADERS;
    }

    @Override
    public SubTable makeRows(Package document) throws SW360Exception {
        List<String> row = new ArrayList<>();
        for(Package._Fields renderedField : PACKAGE_RENDERED_FIELDS_PROJECTS) {
            addFieldValueToRow(row, renderedField, document);
        }
        return new SubTable(row);
    }


    private void addFieldValueToRow(List<String> row, Package._Fields field, Package document) throws SW360Exception {
        switch (field) {
            case ID:
                row.add(document.getId());
                break;
            case NAME:
                row.add(document.getName());
                break;
            case VERSION:
                row.add(document.getVersion());
                break;
            case PURL:
                row.add(document.getPurl());
                break;
            case PACKAGE_TYPE:
                row.add(ThriftEnumUtils.enumToString(document.getPackageType()));
                break;
            case RELEASE:
                try {
                    ReleaseHelper releaseHelper = new ReleaseHelper(cClient, user);
                    if (document.getReleaseId() == null || document.getReleaseId().isEmpty()) {
                        // Ensure row is filled for each header of the release
                        for (int i = 0; i < RELEASE_HEADERS_PROJECT_EXPORT.size(); i++) {
                            row.add("");
                        }
                        break;
                    }else{
                        Release release = cClient.getReleaseById(document.getReleaseId(), user);
                        RELEASES_LINKED_TO_PACKAGES.add(release.getId());
                        if (release.isSetPermissions() && release.getPermissions().get(RequestedAction.READ)) {
                            for (Release._Fields renderedField : RELEASE_RENDERED_FIELDS_PROJECTS) {
                                releaseHelper.addFieldValueToRow(row, renderedField, release, true);
                            }
                        }
                    }
                }catch (TException e) {
                    log.info("Failed to fetch release for ID for package: " + document.getName(), e);
                }
                break;
            default:
                Object fieldValue = document.getFieldValue(field);
                row.add(fieldValueAsString(fieldValue));
        }
    }

    public List<Package> getPackages(Set<String> packageIds) throws SW360Exception {
        try {
            return packageClient.getPackageByIds(packageIds);
        } catch (TException e) {
            log.error("Could not fetch packages", e);
            return new ArrayList<>();
        }
    }
}