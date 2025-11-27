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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageSearchHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of the Thrift service
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
@Component
public class PackageHandler implements PackageService.Iface {

    @Autowired
    private PackageDatabaseHandler handler;
    @Autowired
    private PackageSearchHandler packageSearchHandler;

    @Override
    public Package getPackageById(String packageId) throws SW360Exception {
        assertId(packageId);
        return handler.getPackageById(packageId);
    }

    @Override
    public List<Package> getPackageWithReleaseByPackageIds(Set<String> ids) throws SW360Exception {
        assertNotEmpty(ids);
        return handler.getPackageWithReleaseByPackageIds(ids);
    }

    @Override
    public List<Package> getPackageByIds(Set<String> ids) throws SW360Exception {
        assertNotEmpty(ids);
        return handler.getPackageByIds(ids);
    }

    @Override
    public List<Package> getAllPackages() throws TException {
        return handler.getAllPackages();
    }

    @Override
    public List<Package> getAllOrphanPackages() throws TException {
        return handler.getAllOrphanPackages();
    }

    @Override
    public List<Package> searchPackages(String text, User user) throws TException {
        assertUser(user);
        assertNotEmpty(text, "package search text cannot be empty");
        return handler.searchPackages(packageSearchHandler, text);
    }

    @Override
    public List<Package> searchOrphanPackages(String text, User user) throws TException {
        assertUser(user);
        assertNotEmpty(text, "orphan package search text cannot be empty");
        return handler.searchOrphanPackages(packageSearchHandler, text);
    }

    @Override
    public Set<Package> getPackagesByReleaseId(String id) throws TException {
        assertNotEmpty(id);
        return handler.getPackagesByReleaseId(id);
    }

    @Override
    public Set<Package> getPackagesByReleaseIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getPackagesByReleaseIds(ids);
    }

    @Override
    public AddDocumentRequestSummary addPackage(Package pkg, User user) throws TException {
        assertNotNull(pkg);
        assertUser(user);
        return handler.addPackage(pkg, user);
    }

    @Override
    public RequestStatus updatePackage(Package pkg, User user) throws SW360Exception {
        assertNotNull(pkg);
        assertUser(user);
        return handler.updatePackage(pkg, user);
    }

    @Override
    public RequestStatus deletePackage(String packageId, User user) throws TException {
        assertId(packageId);
        assertUser(user);
        return handler.deletePackage(packageId, user);
    }

    @Override
    public Map<PaginationData, List<Package>> getPackagesWithPagination(PaginationData pageData) throws TException {
        return handler.getPackagesWithPagination(pageData);
    }

    @Override
    public List<Package> searchPackagesWithFilter(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return handler.searchPackagesWithFilter(text, packageSearchHandler, subQueryRestrictions);
    }

    @Override
    public int getTotalPackagesCount() {
        return handler.getTotalPackageCount();
    }

    @Override
    public List<Package> searchByName(String name) throws TException {
        assertNotEmpty(name);

        return handler.searchByName(name);
    }

    @Override
    public List<Package> searchByPackageManager(String pkgManager) throws TException {
        assertNotEmpty(pkgManager);

        return handler.searchByPackageManager(pkgManager);
    }

    @Override
    public List<Package> searchByVersion(String version) throws TException {
        assertNotEmpty(version);

        return handler.searchByVersion(version);
    }

    @Override
    public List<Package> searchByPurl(String purl) throws TException {
        assertNotEmpty(purl);

        return handler.getPackageByPurl(purl);
    }

    @Override
    public List<Package> refineSearchAccessiblePackages(String text, Map<String,Set<String>> subQueryRestrictions, User user) throws TException {
        return packageSearchHandler.searchAccessiblePackages(text, subQueryRestrictions, user);
    }
}
