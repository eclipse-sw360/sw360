/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.packages;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.packages.PackageSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360PackageService {
    @NonNull
    private final RestControllerHelper<Package> rch;

    public Package createPackage(Package pkg, User sw360User) throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        AddDocumentRequestSummary documentRequestSummary = sw360PackageClient.addPackage(pkg, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            pkg.setId(documentRequestSummary.getId());
            pkg.setCreatedBy(sw360User.getEmail());
            return pkg;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE
                && documentRequestSummary.getMessage().equals(SW360Constants.DUPLICATE_PACKAGE_BY_PURL) ) {
            throw new DataIntegrityViolationException("sw360 package with same purl '" + pkg.getPurl() + "' already exists.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 package with same name and version '" + pkg.getName() + "' already exists.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
            throw new BadRequestClientException("Dependent document Id/ids not valid.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.NAMINGERROR) {
            throw new BadRequestClientException("Package name field cannot be empty or contain only whitespace character");
        }
        return null;
    }

    public RequestStatus updatePackage(Package pkg, User sw360User) throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        rch.checkForCyclicOrInvalidDependencies(sw360PackageClient, pkg, sw360User);

        RequestStatus requestStatus;
        requestStatus = sw360PackageClient.updatePackage(pkg, sw360User);

        if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new BadRequestClientException("Invalid Purl or linked release id.");
        } else if (requestStatus == RequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 package with same name and version '" + pkg.getName() + "' already exists.");
        } else if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new BadRequestClientException("Package name and version field cannot be empty or contain only whitespace character");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("sw360 Package with id '" + pkg.getId() + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus deletePackage(String packageId, User sw360User) throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.deletePackage(packageId, sw360User);
    }

    public PackageService.Iface getThriftPackageClient() throws TTransportException {
        return ThriftClients.makePackageClient();
    }

    public Package getPackageForUserById(String id) throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        try {
            return sw360PackageClient.getPackageById(id);
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Package does not exist! id=" + id);
            } else {
                throw sw360Exp;
            }
        }
    }

    public boolean validatePackageIds(Set<String> packageIds) throws TException {
        for (String id: packageIds) {
            if (null == getPackageForUserById(id)) {
                return false;
            }
        }
        return true;
    }

    public List<Package> getPackagesForUser() throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.getAllPackages();
    }

    public List<Package> searchPackageByName(String name) throws TException {
        final PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.searchByName(name);
    }

    public List<Package> searchByPackageManager(String pkgManager) throws TException {
        final PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.searchByPackageManager(pkgManager);
    }

    public List<Package> searchPackageByVersion(String version) throws TException {
        final PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.searchByVersion(version);
    }

    public List<Package> searchPackageByPurl(String purl) throws TException {
        final PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.searchByPurl(purl);
    }

    public int getTotalPackagesCounts() throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        return sw360PackageClient.getTotalPackagesCount();
    }


    public Map<PaginationData, List<Package>> refineSearch(Map<String, Set<String>> filterMap, User sw360User, Pageable pageable) throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        PaginationData pageData = pageableToPaginationData(pageable, PackageSortColumn.BY_NAME, true);
        return sw360PackageClient.refineSearchAccessiblePackages(filterMap, sw360User, pageData);
    }

    public Map<PaginationData, List<Package>> searchFilteredPackages(String searchText, User sw360User, Pageable pageable) throws TException {
        PackageService.Iface sw360PackageClient = getThriftPackageClient();
        PaginationData pageData = pageableToPaginationData(pageable, PackageSortColumn.BY_NAME, true);
        return sw360PackageClient.searchFilteredPackages(searchText, sw360User, pageData);
    }

    private static PaginationData pageableToPaginationData(@NotNull Pageable pageable,
                                                            PackageSortColumn defaultColumn,
                                                            Boolean defaultAscending) {
        PackageSortColumn column = PackageSortColumn.BY_CREATEDON;
        boolean ascending = false;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            column = switch (property) {
                case "createdOn" -> PackageSortColumn.BY_CREATEDON;
                case "name" -> PackageSortColumn.BY_NAME;
                case "version" -> PackageSortColumn.BY_VERSION;
                case "packageManager" -> PackageSortColumn.BY_PACKAGE_MANAGER;
                case "score" -> PackageSortColumn.BY_SCORE;
                default -> column;
            };
            ascending = order.isAscending();
        } else {
            if (defaultColumn != null) {
                column = defaultColumn;
                if (defaultAscending != null) {
                    ascending = defaultAscending;
                }
            }
        }
        return new PaginationData().setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize())
                .setSortColumnNumber(column.getValue())
                .setAscending(ascending);
    }
}
