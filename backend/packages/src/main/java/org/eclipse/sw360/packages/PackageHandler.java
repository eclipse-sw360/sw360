/*
* Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.packages;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertId;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.common.utils.converter.packages.PackageConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageSearchHandler;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

/**
* @author abdul.kapti@siemens-healthineers.com
*/
@Service
public class PackageHandler {

    private final PackageDatabaseHandler handler;
    private final PackageSearchHandler packageSearchHandler;

    public PackageHandler() throws IOException {
        handler = new PackageDatabaseHandler(
                DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_DATABASE,
                DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                DatabaseSettings.COUCH_DB_ATTACHMENTS);
        packageSearchHandler = new PackageSearchHandler(
                DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }

    public Package getPackageById(String packageId) {
        try {
            assertId(packageId);
            return PackageConverter.fromThrift(handler.getPackageById(packageId));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> getPackageWithReleaseByPackageIds(Set<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getPackageWithReleaseByPackageIds(ids).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> getPackageByIds(Set<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getPackageByIds(ids).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> getAllPackages() {
        return handler.getAllPackages().stream()
                .map(PackageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    public List<Package> getAllOrphanPackages() {
        return handler.getAllOrphanPackages().stream()
                .map(PackageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    public List<Package> searchPackages(String text, User user) {
        try {
            assertUser(user);
            assertNotEmpty(text, "package search text cannot be empty");
            return handler.searchPackages(packageSearchHandler, text).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchOrphanPackages(String text, User user) {
        try {
            assertUser(user);
            assertNotEmpty(text, "orphan package search text cannot be empty");
            return handler.searchOrphanPackages(packageSearchHandler, text).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public Set<Package> getPackagesByReleaseId(String id) {
        try {
            assertNotEmpty(id);
            return handler.getPackagesByReleaseId(id).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toSet());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public Set<Package> getPackagesByReleaseIds(Set<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getPackagesByReleaseIds(ids).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toSet());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public AddDocumentRequestSummary addPackage(Package pkg, User user) {
        try {
            assertNotNull(pkg);
            assertUser(user);
            return ThriftConverter.fromThriftAddDocumentRequestSummary(
                    handler.addPackage(PackageConverter.toThrift(pkg), user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updatePackage(Package pkg, User user) {
        try {
            assertNotNull(pkg);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(
                    handler.updatePackage(PackageConverter.toThrift(pkg), user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus deletePackage(String packageId, User user) {
        try {
            assertId(packageId);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.deletePackage(packageId, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public PaginatedResult<Package> getPackagesWithPagination(PaginationData pageData) {
        Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.packages.Package>> thriftResult =
                handler.getPackagesWithPagination(ThriftConverter.toThriftPaginationData(pageData));
        Map.Entry<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.packages.Package>> entry =
                thriftResult.entrySet().iterator().next();
        PaginationData paginationData = ThriftConverter.fromThriftPaginationData(entry.getKey());
        List<Package> packages = entry.getValue().stream()
                .map(PackageConverter::fromThrift)
                .collect(Collectors.toList());
        return new PaginatedResult<>(paginationData, packages);
    }

    public List<Package> searchPackagesWithFilter(String text, Map<String, Set<String>> subQueryRestrictions) {
        return handler.searchPackagesWithFilter(text, packageSearchHandler, subQueryRestrictions).stream()
                .map(PackageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    public int getTotalPackagesCount() {
        return handler.getTotalPackageCount();
    }

    public List<Package> searchByName(String name) {
        try {
            assertNotEmpty(name);
            return handler.searchByName(name).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchByPackageManager(String pkgManager) {
        try {
            assertNotEmpty(pkgManager);
            return handler.searchByPackageManager(pkgManager).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchByVersion(String version) {
        try {
            assertNotEmpty(version);
            return handler.searchByVersion(version).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchByPurl(String purl) {
        try {
            assertNotEmpty(purl);
            return handler.getPackageByPurl(purl).stream()
                    .map(PackageConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> refineSearchAccessiblePackages(String text, Map<String, Set<String>> subQueryRestrictions, User user) {
        return packageSearchHandler.searchAccessiblePackages(text, subQueryRestrictions, user).stream()
                .map(PackageConverter::fromThrift)
                .collect(Collectors.toList());
    }
}
