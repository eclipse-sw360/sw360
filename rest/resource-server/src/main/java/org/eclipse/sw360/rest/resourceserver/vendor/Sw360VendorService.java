/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Vendor.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.vendor;

import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.services.vendors.VendorSortColumn;

import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Sw360VendorService {

    private static final String VENDORS_URI = "/vendors/api/vendors";

    private final RestClient restClient;

    @lombok.NonNull
    private final org.eclipse.sw360.rest.resourceserver.component.ComponentServiceRestAdapter componentServiceRestAdapter;

    private void addUserHeaders(RestClient.RequestHeadersSpec<?> spec, User user) {
        spec.header("X-User-Email", user.getEmail())
            .header("X-User-Department", user.getDepartment())
            .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    public Map<PaginationData, List<Vendor>> getVendors(Pageable pageable) {
        PaginationData pageData = pageableToPaginationData(pageable, VendorSortColumn.BY_FULLNAME, true);
        PaginatedResult<Vendor> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(VENDORS_URI + "/page")
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.getDisplayStart())
                        .queryParam("rowsPerPage", pageData.getRowsPerPage())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<PaginatedResult<Vendor>>() {});
        return toPaginatedMap(result);
    }

    public Map<PaginationData, List<Vendor>> searchVendors(String searchText, Pageable pageable) {
        PaginationData pageData = pageableToPaginationData(pageable, VendorSortColumn.BY_SCORE, true);
        PaginatedResult<Vendor> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(VENDORS_URI + "/search")
                        .queryParam("searchText", searchText)
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.getDisplayStart())
                        .queryParam("rowsPerPage", pageData.getRowsPerPage())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<PaginatedResult<Vendor>>() {});
        return toPaginatedMap(result);
    }

    public Vendor getVendorById(String vendorId) {
        return restClient.get()
                .uri(VENDORS_URI + "/" + vendorId)
                .retrieve()
                .body(Vendor.class);
    }

    public org.eclipse.sw360.datahandler.thrift.vendors.Vendor getThriftVendorById(String vendorId) {
        return toThriftVendor(getVendorById(vendorId));
    }

    public static org.eclipse.sw360.datahandler.thrift.vendors.Vendor toThriftVendor(Vendor pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vendors.Vendor thrift =
                new org.eclipse.sw360.datahandler.thrift.vendors.Vendor();
        thrift.setId(pojo.getId());
        thrift.setRevision(pojo.getRevision());
        thrift.setType(pojo.getType());
        thrift.setShortname(pojo.getShortname());
        thrift.setFullname(pojo.getFullname());
        thrift.setUrl(pojo.getUrl());
        return thrift;
    }

    public Vendor getVendorByFullName(String fullName) {
        for (Vendor vendor : getAllVendorList()) {
            if (fullName.equals(vendor.getFullname())) {
                return vendor;
            }
        }
        return null;
    }

    public Vendor createVendor(Vendor vendor) {
        if (CommonUtils.isNullEmptyOrWhitespace(vendor.getFullname())
                || CommonUtils.isNullEmptyOrWhitespace(vendor.getShortname())
                || CommonUtils.isNullEmptyOrWhitespace(vendor.getUrl())) {
            throw new BadRequestClientException(
                    "A Vendor cannot have null or empty 'Full Name' or 'Short Name' or 'URL'!");
        }
        AddDocumentRequestSummary summary = restClient.post()
                .uri(VENDORS_URI)
                .body(vendor)
                .retrieve()
                .body(AddDocumentRequestSummary.class);
        if (summary == null) {
            return null;
        }
        if (AddDocumentRequestStatus.SUCCESS.equals(summary.getRequestStatus())) {
            vendor.setId(summary.getId());
            return vendor;
        } else if (AddDocumentRequestStatus.DUPLICATE.equals(summary.getRequestStatus())) {
            throw new DataIntegrityViolationException(
                    "A Vendor with same full name '" + vendor.getFullname() + "' and URL already exists!");
        } else if (AddDocumentRequestStatus.FAILURE.equals(summary.getRequestStatus())) {
            throw new BadRequestClientException(summary.getMessage());
        }
        return null;
    }

    public RequestStatus vendorUpdate(Vendor vendor, User sw360User, String id) {
        Vendor existingVendor = getVendorById(id);
        if (existingVendor != null) {
            if (vendor.getShortname() != null) {
                existingVendor.setShortname(vendor.getShortname());
            }
            if (vendor.getFullname() != null) {
                existingVendor.setFullname(vendor.getFullname());
            }
            if (vendor.getUrl() != null) {
                existingVendor.setUrl(vendor.getUrl());
            }
        }
        var req = restClient.put()
                .uri(VENDORS_URI)
                .body(existingVendor);
        addUserHeaders(req, sw360User);
        return req.retrieve().body(RequestStatus.class);
    }

    public RequestStatus deleteVendorByid(String vendorId, User sw360User) throws TException {
        try {
            ComponentService.Iface componentClient = getThriftComponentClient();
            List<Release> releases = componentClient.getReleasesFromVendorId(vendorId, sw360User);

            if (releases.stream().anyMatch(release -> !release.getPermissions().get(RequestedAction.WRITE))) {
                throw new AccessDeniedException("You do not have permission to delete vendor with id " + vendorId);
            }

            for (Release release : releases) {
                if (release.isSetVendorId()) {
                    release.unsetVendorId();
                }
                if (release.isSetVendor()) {
                    release.unsetVendor();
                }
                org.eclipse.sw360.datahandler.thrift.RequestStatus status =
                        componentClient.updateRelease(release, sw360User);
                if (status != org.eclipse.sw360.datahandler.thrift.RequestStatus.SUCCESS) {
                    return RequestStatus.valueOf(status.name());
                }
            }

            var req = restClient.delete().uri(VENDORS_URI + "/" + vendorId);
            addUserHeaders(req, sw360User);
            return req.retrieve().body(RequestStatus.class);
        } catch (TException e) {
            throw new TException(e);
        }
    }

    public void deleteAllVendors(User sw360User) {
        List<Vendor> vendors = getAllVendorList();
        for (Vendor vendor : vendors) {
            try {
                deleteVendorByid(vendor.getId(), sw360User);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ComponentService.Iface getThriftComponentClient() {
        return componentServiceRestAdapter;
    }

    public ByteBuffer exportExcel() throws TException {
        List<Vendor> vendors = getAllVendorList();
        byte[] data = restClient.post()
                .uri(VENDORS_URI + "/report")
                .body(vendors)
                .retrieve()
                .body(byte[].class);
        return data != null ? ByteBuffer.wrap(data) : ByteBuffer.allocate(0);
    }

    private List<Vendor> getAllVendorList() {
        List<Vendor> vendors = restClient.get()
                .uri(VENDORS_URI)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Vendor>>() {});
        return vendors != null ? vendors : Collections.emptyList();
    }

    public Set<Release> getAllReleaseList(String vendorId) throws TException {
        ComponentService.Iface componentsClient = getThriftComponentClient();
        return componentsClient.getReleasesByVendorId(vendorId);
    }

    public RequestStatus mergeVendors(
            String vendorTargetId, String vendorSourceId, Vendor vendorSelection, User user)
            throws TException, ResourceClassNotFoundException {
        var req = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(VENDORS_URI + "/merge")
                        .queryParam("mergeTargetId", vendorTargetId)
                        .queryParam("mergeSourceId", vendorSourceId)
                        .build())
                .body(vendorSelection);
        addUserHeaders(req, user);
        RequestStatus requestStatus = req.retrieve().body(RequestStatus.class);

        if (requestStatus == RequestStatus.IN_USE) {
            throw new BadRequestClientException("Vendor used as source or target has an open MR");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new ResourceClassNotFoundException("Internal server error while merging the vendors");
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new AccessDeniedException("Access denied");
        }

        return requestStatus;
    }

    private static Map<PaginationData, List<Vendor>> toPaginatedMap(PaginatedResult<Vendor> result) {
        if (result == null) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(result.getPaginationData(), result.getData());
    }

    private static PaginationData pageableToPaginationData(
            @NotNull Pageable pageable, VendorSortColumn defaultSort, Boolean defaultAscending) {
        VendorSortColumn column = VendorSortColumn.BY_FULLNAME;
        boolean ascending = true;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            column = switch (property) {
                case "fullname" -> VendorSortColumn.BY_FULLNAME;
                case "shortname" -> VendorSortColumn.BY_SHORTNAME;
                case "score" -> VendorSortColumn.BY_SCORE;
                default -> column;
            };
            ascending = order.isAscending();
        } else {
            if (defaultSort != null) {
                column = defaultSort;
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
