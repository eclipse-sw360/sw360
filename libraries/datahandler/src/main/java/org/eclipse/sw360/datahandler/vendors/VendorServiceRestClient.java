/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.vendors;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link VendorClient}.
 *
 * Maps to {@code VendorController} under {@code /vendors/api/vendors}.
 */
public class VendorServiceRestClient implements VendorClient {

    private static final String BASE = "/vendors/api/vendors";

    private static final ParameterizedTypeReference<PaginatedResult<Vendor>> VENDOR_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Vendor>> VENDOR_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public VendorServiceRestClient(RestClient restClient) {
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
    public PaginatedResult<Vendor> getVendorsPage(PaginationData pageData) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/page")
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.displayStartOrZero())
                        .queryParam("rowsPerPage", pageData.rowsPerPageOrZero())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .retrieve()
                .body(VENDOR_PAGE));
    }

    @Override
    public PaginatedResult<Vendor> searchVendors(String searchText, PaginationData pageData) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/search")
                        .queryParam("searchText", searchText)
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.displayStartOrZero())
                        .queryParam("rowsPerPage", pageData.rowsPerPageOrZero())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .retrieve()
                .body(VENDOR_PAGE));
    }

    @Override
    public Vendor getVendorById(String vendorId) {
        return call(() -> restClient.get()
                .uri(BASE + "/{vendorId}", vendorId)
                .retrieve()
                .body(Vendor.class));
    }

    @Override
    public List<Vendor> getAllVendors() {
        List<Vendor> vendors = call(() -> restClient.get()
                .uri(BASE)
                .retrieve()
                .body(VENDOR_LIST));
        return vendors != null ? vendors : Collections.emptyList();
    }

    @Override
    public AddDocumentRequestSummary addVendor(Vendor vendor) {
        return call(() -> restClient.post()
                .uri(BASE)
                .body(vendor)
                .retrieve()
                .body(AddDocumentRequestSummary.class));
    }

    @Override
    public RequestStatus updateVendor(Vendor vendor, User user) {
        return call(() -> restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(vendor)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus deleteVendor(String vendorId, User user) {
        return call(() -> restClient.delete()
                .uri(BASE + "/{vendorId}", vendorId)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public byte[] exportVendorReport(List<Vendor> vendors) {
        byte[] data = call(() -> restClient.post()
                .uri(BASE + "/report")
                .body(vendors)
                .retrieve()
                .body(byte[].class));
        return data != null ? data : new byte[0];
    }

    @Override
    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor vendorSelection, User user) {
        return call(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/merge")
                        .queryParam("mergeTargetId", mergeTargetId)
                        .queryParam("mergeSourceId", mergeSourceId)
                        .build())
                .headers(h -> addUser(h, user))
                .body(vendorSelection)
                .retrieve()
                .body(RequestStatus.class));
    }
}
