/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.packages;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.datahandler.services.packages.PackageSearchFilterRequest;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link PackageClient}.
 *
 * Maps to {@code PackageController} under {@code /packages/api/packages}.
 */
public class PackageServiceRestClient implements PackageClient {

    private static final String BASE = "/packages/api/packages";

    private static final ParameterizedTypeReference<List<Package>> PACKAGE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<Package>> PACKAGE_SET =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public PackageServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public AddDocumentRequestSummary createPackage(Package pkg, User user) {
        return call(() -> restClient.post()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(pkg)
                .retrieve()
                .body(AddDocumentRequestSummary.class));
    }

    @Override
    public RequestStatus updatePackage(Package pkg, User user) {
        return call(() -> restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(pkg)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus deletePackage(String packageId, User user) {
        return call(() -> restClient.delete()
                .uri(BASE + "/{packageId}", packageId)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public Package getPackageById(String id) {
        return call(() -> restClient.get()
                .uri(BASE + "/{id}", id)
                .retrieve()
                .body(Package.class));
    }

    @Override
    public List<Package> getAllPackages() {
        List<Package> packages = call(() -> restClient.get()
                .uri(BASE)
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public List<Package> searchByFilter(PackageSearchFilterRequest request) {
        List<Package> packages = call(() -> restClient.post()
                .uri(BASE + "/search/filter")
                .body(request)
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public List<Package> searchByName(String name) {
        List<Package> packages = call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/search/name").queryParam("name", name).build())
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public List<Package> searchByPackageManager(String pkgManager) {
        List<Package> packages = call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/search/manager").queryParam("pkgManager", pkgManager)
                        .build())
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public List<Package> searchByVersion(String version) {
        List<Package> packages = call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/search/version").queryParam("version", version).build())
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public List<Package> searchByPurl(String purl) {
        List<Package> packages = call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/search/purl").queryParam("purl", purl).build())
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public int getTotalPackagesCount() {
        Integer count = call(() -> restClient.get()
                .uri(BASE + "/count")
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public List<Package> refineSearch(PackageSearchFilterRequest request, User user) {
        List<Package> packages = call(() -> restClient.post()
                .uri(BASE + "/refine-search")
                .headers(h -> addUser(h, user))
                .body(request)
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }

    @Override
    public Set<Package> getLinkedPackagesForRelease(String releaseId) {
        Set<Package> packages = call(() -> restClient.get()
                .uri(BASE + "/by-release/{releaseId}", releaseId)
                .retrieve()
                .body(PACKAGE_SET));
        return packages != null ? packages : Collections.emptySet();
    }

    @Override
    public List<Package> getPackagesWithReleaseByIds(Set<String> packageIds) {
        List<Package> packages = call(() -> restClient.post()
                .uri(BASE + "/with-release")
                .body(packageIds)
                .retrieve()
                .body(PACKAGE_LIST));
        return packages != null ? packages : Collections.emptyList();
    }
}
