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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.packages.PackageClient;
import org.eclipse.sw360.datahandler.packages.PackageClients;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.packages.PackageSearchFilterRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360PackageService {

    @NonNull
    private final PackageTypeBridge packageTypeBridge;

    private PackageClient packageClient() {
        return PackageClients.get();
    }

    private org.eclipse.sw360.datahandler.services.users.User pojoUser(User sw360User) {
        return UserConverter.fromThrift(sw360User);
    }

    public Package createPackage(Package pkg, User sw360User) throws TException {
        AddDocumentRequestSummary documentRequestSummary = packageClient().createPackage(
                packageTypeBridge.toPojo(pkg), pojoUser(sw360User));
        if (documentRequestSummary == null) {
            return null;
        }
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            pkg.setId(documentRequestSummary.getId());
            pkg.setCreatedBy(sw360User.getEmail());
            return pkg;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE
                && SW360Constants.DUPLICATE_PACKAGE_BY_PURL.equals(documentRequestSummary.getMessage())) {
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

    public org.eclipse.sw360.datahandler.thrift.RequestStatus updatePackage(Package pkg, User sw360User) throws TException {
        RequestStatus requestStatus = packageClient().updatePackage(packageTypeBridge.toPojo(pkg), pojoUser(sw360User));

        if (requestStatus == null) {
            throw new RuntimeException("sw360 Package with id '" + pkg.getId() + " cannot be updated.");
        }
        if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new BadRequestClientException("Invalid Purl or linked release id.");
        } else if (requestStatus == RequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 package with same name and version '" + pkg.getName() + "' already exists.");
        } else if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new BadRequestClientException("Package name and version field cannot be empty or contain only whitespace character");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("sw360 Package with id '" + pkg.getId() + " cannot be updated.");
        }
        return packageTypeBridge.toThriftRequestStatus(requestStatus);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus deletePackage(String packageId, User sw360User) throws TException {
        RequestStatus status = packageClient().deletePackage(packageId, pojoUser(sw360User));
        return status != null ? packageTypeBridge.toThriftRequestStatus(status)
                : org.eclipse.sw360.datahandler.thrift.RequestStatus.FAILURE;
    }

    public Package getPackageForUserById(String id) throws TException {
        try {
            org.eclipse.sw360.datahandler.services.packages.Package pojo = packageClient().getPackageById(id);
            return packageTypeBridge.toThrift(pojo);
        } catch (SW360Exception e) {
            if (e.getErrorCode() != null && e.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Package does not exist! id=" + id);
            }
            throw e;
        }
    }

    public boolean validatePackageIds(Set<String> packageIds) throws TException {
        for (String id : packageIds) {
            if (null == getPackageForUserById(id)) {
                return false;
            }
        }
        return true;
    }

    public List<Package> getPackagesForUser() throws TException {
        return toThriftPackages(packageClient().getAllPackages());
    }

    public List<Package> searchPackage(String field, String searchQuery, boolean isExactMatch) throws TException {
        Set<String> values = CommonUtils.splitToSet(searchQuery);

        if (field.equals("name")) {
            if (isExactMatch) {
                values = values.stream().map(s -> "\"" + s + "\"")
                        .map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
            } else {
                values = values.stream().map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
            }
        }
        Map<String, Set<String>> queryMap = new HashMap<>();
        queryMap.put(field, values);

        PackageSearchFilterRequest request = new PackageSearchFilterRequest()
                .setText(searchQuery)
                .setSubQueryRestrictions(queryMap);
        return toThriftPackages(packageClient().searchByFilter(request));
    }

    public List<Package> searchPackageByName(String name) throws TException {
        return toThriftPackages(packageClient().searchByName(name));
    }

    public List<Package> searchByPackageManager(String pkgManager) throws TException {
        return toThriftPackages(packageClient().searchByPackageManager(pkgManager));
    }

    public List<Package> searchPackageByVersion(String version) throws TException {
        return toThriftPackages(packageClient().searchByVersion(version));
    }

    public List<Package> searchPackageByPurl(String purl) throws TException {
        return toThriftPackages(packageClient().searchByPurl(purl));
    }

    public int getTotalPackagesCounts() throws TException {
        return packageClient().getTotalPackagesCount();
    }

    public List<Package> refineSearch(Map<String, Set<String>> filterMap, User sw360User) throws TException {
        PackageSearchFilterRequest requestBody = new PackageSearchFilterRequest()
                .setSubQueryRestrictions(filterMap);
        return toThriftPackages(packageClient().refineSearch(requestBody, pojoUser(sw360User)));
    }

    public List<Package> getLinkedPackagesForRelease(String releaseId) throws TException {
        Set<org.eclipse.sw360.datahandler.services.packages.Package> packages =
                packageClient().getLinkedPackagesForRelease(releaseId);
        return toThriftPackages(packages != null ? List.copyOf(packages) : List.of());
    }

    public List<Package> getPackageWithReleaseByPackageIds(Set<String> packageIds) throws TException {
        return toThriftPackages(packageClient().getPackagesWithReleaseByIds(packageIds));
    }

    private List<Package> toThriftPackages(List<org.eclipse.sw360.datahandler.services.packages.Package> packages) {
        if (packages == null) {
            return List.of();
        }
        return packages.stream().map(packageTypeBridge::toThrift).collect(Collectors.toList());
    }
}
