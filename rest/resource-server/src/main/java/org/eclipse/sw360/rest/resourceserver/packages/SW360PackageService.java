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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.packages.PackageSearchFilterRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360PackageService {

    private static final String PACKAGES_URI = "/packages/api/packages";

    @NonNull
    private final RestClient restClient;

    @NonNull
    private final PackageTypeBridge packageTypeBridge;

    private void addUserHeaders(RestClient.RequestHeadersSpec<?> spec, User user) {
        spec.header("X-User-Email", user.getEmail())
            .header("X-User-Department", user.getDepartment())
            .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    public Package createPackage(Package pkg, User sw360User) throws TException {
        var request = restClient.post().uri(PACKAGES_URI).body(packageTypeBridge.toPojo(pkg));
        addUserHeaders(request, sw360User);
        AddDocumentRequestSummary documentRequestSummary = request
                .retrieve()
                .body(AddDocumentRequestSummary.class);
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
        var request = restClient.put().uri(PACKAGES_URI).body(packageTypeBridge.toPojo(pkg));
        addUserHeaders(request, sw360User);
        RequestStatus requestStatus = request
                .retrieve()
                .body(RequestStatus.class);

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
        var request = restClient.delete().uri(PACKAGES_URI + "/" + packageId);
        addUserHeaders(request, sw360User);
        RequestStatus status = request
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? packageTypeBridge.toThriftRequestStatus(status)
                : org.eclipse.sw360.datahandler.thrift.RequestStatus.FAILURE;
    }

    public Package getPackageForUserById(String id) throws TException {
        try {
            org.eclipse.sw360.datahandler.services.packages.Package pojo = restClient.get()
                    .uri(PACKAGES_URI + "/" + id)
                    .retrieve()
                    .body(org.eclipse.sw360.datahandler.services.packages.Package.class);
            return packageTypeBridge.toThrift(pojo);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Package does not exist! id=" + id);
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
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.get()
                .uri(PACKAGES_URI)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
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
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.post()
                .uri(PACKAGES_URI + "/search/filter")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    public List<Package> searchPackageByName(String name) throws TException {
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.get()
                .uri(uriBuilder -> uriBuilder.path(PACKAGES_URI + "/search/name").queryParam("name", name).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    public List<Package> searchByPackageManager(String pkgManager) throws TException {
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.get()
                .uri(uriBuilder -> uriBuilder.path(PACKAGES_URI + "/search/manager").queryParam("pkgManager", pkgManager).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    public List<Package> searchPackageByVersion(String version) throws TException {
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.get()
                .uri(uriBuilder -> uriBuilder.path(PACKAGES_URI + "/search/version").queryParam("version", version).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    public List<Package> searchPackageByPurl(String purl) throws TException {
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.get()
                .uri(uriBuilder -> uriBuilder.path(PACKAGES_URI + "/search/purl").queryParam("purl", purl).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    public int getTotalPackagesCounts() throws TException {
        Integer count = restClient.get()
                .uri(PACKAGES_URI + "/count")
                .retrieve()
                .body(Integer.class);
        return count != null ? count : 0;
    }

    public List<Package> refineSearch(Map<String, Set<String>> filterMap, User sw360User) throws TException {
        PackageSearchFilterRequest requestBody = new PackageSearchFilterRequest()
                .setSubQueryRestrictions(filterMap);
        var request = restClient.post().uri(PACKAGES_URI + "/refine-search").body(requestBody);
        addUserHeaders(request, sw360User);
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = request
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    public List<Package> getLinkedPackagesForRelease(String releaseId) throws TException {
        Set<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.get()
                .uri(PACKAGES_URI + "/by-release/" + releaseId)
                .retrieve()
                .body(new ParameterizedTypeReference<Set<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages != null ? List.copyOf(packages) : List.of());
    }

    public List<Package> getPackageWithReleaseByPackageIds(Set<String> packageIds) throws TException {
        List<org.eclipse.sw360.datahandler.services.packages.Package> packages = restClient.post()
                .uri(PACKAGES_URI + "/with-release")
                .body(packageIds)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.packages.Package>>() {});
        return toThriftPackages(packages);
    }

    private List<Package> toThriftPackages(List<org.eclipse.sw360.datahandler.services.packages.Package> packages) {
        if (packages == null) {
            return List.of();
        }
        return packages.stream().map(packageTypeBridge::toThrift).collect(Collectors.toList());
    }
}
