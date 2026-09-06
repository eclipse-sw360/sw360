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

import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageSearchHandler;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
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
            return handler.getPackageById(packageId);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> getPackageWithReleaseByPackageIds(Set<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getPackageWithReleaseByPackageIds(ids);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> getPackageByIds(Set<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getPackageByIds(ids);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> getAllPackages() {
        return handler.getAllPackages();
    }

    public List<Package> getAllOrphanPackages() {
        return handler.getAllOrphanPackages();
    }

    public List<Package> searchPackages(String text, User user) {
        try {
            assertUser(user);
            assertNotEmpty(text, "package search text cannot be empty");
            return handler.searchPackages(packageSearchHandler, text);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchOrphanPackages(String text, User user) {
        try {
            assertUser(user);
            assertNotEmpty(text, "orphan package search text cannot be empty");
            return handler.searchOrphanPackages(packageSearchHandler, text);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public Set<Package> getPackagesByReleaseId(String id) {
        try {
            assertNotEmpty(id);
            return handler.getPackagesByReleaseId(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public Set<Package> getPackagesByReleaseIds(Set<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getPackagesByReleaseIds(ids);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public AddDocumentRequestSummary addPackage(Package pkg, User user) {
        try {
            assertNotNull(pkg);
            assertUser(user);
            return handler.addPackage(pkg, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus updatePackage(Package pkg, User user) {
        try {
            assertNotNull(pkg);
            assertUser(user);
            return handler.updatePackage(pkg, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus deletePackage(String packageId, User user) {
        try {
            assertId(packageId);
            assertUser(user);
            return handler.deletePackage(packageId, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public PaginatedResult<Package> getPackagesWithPagination(PaginationData pageData) {
        Map<PaginationData, List<Package>> result = handler.getPackagesWithPagination(pageData);
        Map.Entry<PaginationData, List<Package>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    public List<Package> searchPackagesWithFilter(String text, Map<String, Set<String>> subQueryRestrictions) {
        return handler.searchPackagesWithFilter(text, packageSearchHandler, subQueryRestrictions);
    }

    public int getTotalPackagesCount() {
        return handler.getTotalPackageCount();
    }

    public List<Package> searchByName(String name) {
        try {
            assertNotEmpty(name);
            return handler.searchByName(name);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchByPackageManager(String pkgManager) {
        try {
            assertNotEmpty(pkgManager);
            return handler.searchByPackageManager(pkgManager);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchByVersion(String version) {
        try {
            assertNotEmpty(version);
            return handler.searchByVersion(version);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> searchByPurl(String purl) {
        try {
            assertNotEmpty(purl);
            return handler.getPackageByPurl(purl);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Package> refineSearchAccessiblePackages(String text, Map<String, Set<String>> subQueryRestrictions, User user) {
        return packageSearchHandler.searchAccessiblePackages(text, subQueryRestrictions, user);
    }
}
