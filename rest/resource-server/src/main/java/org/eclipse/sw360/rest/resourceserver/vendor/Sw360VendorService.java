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

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorSortColumn;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class Sw360VendorService {
    public Map<PaginationData, List<Vendor>> getVendors(Pageable pageable) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            PaginationData pageData = pageableToPaginationData(pageable,
                    VendorSortColumn.BY_FULLNAME, true);
            return sw360VendorClient.getAllVendorListPaginated(pageData);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<PaginationData, List<Vendor>> searchVendors(String searchText, Pageable pageable) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            PaginationData pageData = pageableToPaginationData(pageable,
                    VendorSortColumn.BY_FULLNAME, true);
            return sw360VendorClient.searchVendors(searchText, pageData);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Vendor getVendorById(String vendorId) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            return sw360VendorClient.getByID(vendorId);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Vendor getVendorByFullName(String fullName) {
        try {
            for (Vendor vendor : getAllVendorList()) {
                if(fullName.equals(vendor.getFullname())) {
                    return vendor;
                }
            }
            return null;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Vendor createVendor(Vendor vendor) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            if (CommonUtils.isNullEmptyOrWhitespace(vendor.getFullname()) || CommonUtils.isNullEmptyOrWhitespace(vendor.getShortname())
                    || CommonUtils.isNullEmptyOrWhitespace(vendor.getUrl())) {
                throw new BadRequestClientException("A Vendor cannot have null or empty 'Full Name' or 'Short Name' or 'URL'!");
            }
            AddDocumentRequestSummary summary = sw360VendorClient.addVendor(vendor);
            if (AddDocumentRequestStatus.SUCCESS.equals(summary.getRequestStatus())) {
                vendor.setId(summary.getId());
                return vendor;
            } else if (AddDocumentRequestStatus.DUPLICATE.equals(summary.getRequestStatus())) {
                throw new DataIntegrityViolationException("A Vendor with same full name '" + vendor.getFullname() + "' and URL already exists!");
            } else if (AddDocumentRequestStatus.FAILURE.equals(summary.getRequestStatus())) {
                throw new BadRequestClientException(summary.getMessage());
            }
            return null;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestStatus vendorUpdate(Vendor vendor, User sw360User, String id) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            Vendor existingVendor = sw360VendorClient.getByID(id);
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
            RequestStatus requestStatus = sw360VendorClient.updateVendor(existingVendor, sw360User);
            return requestStatus;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestStatus deleteVendorByid(String vendorId, User sw360User) throws TException {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            ComponentService.Iface componentClient = getThriftComponentClient();
            List<Release> releases = componentClient.getReleasesFromVendorId(vendorId, sw360User);

            if (releases.stream().anyMatch(release -> !release.getPermissions().get(RequestedAction.WRITE))) {
                throw new AccessDeniedException("You do not have permission to delete vendor with id " + vendorId);
            }

            for (Release release : releases) {
                if (release.isSetVendorId()) release.unsetVendorId();
                if (release.isSetVendor()) release.unsetVendor();
                RequestStatus status = componentClient.updateRelease(release, sw360User);
                if (status != RequestStatus.SUCCESS) return status;
            }

            return sw360VendorClient.deleteVendor(vendorId, sw360User);
        } catch (TException e) {
            throw new TException(e);
        }
    }
    public void deleteAllVendors(User sw360User) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            List<Vendor> vendors = getAllVendorList();
            for (Vendor vendor : vendors) {
                sw360VendorClient.deleteVendor(vendor.getId(), sw360User);
            }
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    private VendorService.Iface getThriftVendorClient() throws TTransportException {
        return new ThriftClients().makeVendorClient();
    }

    public ComponentService.Iface getThriftComponentClient() throws TTransportException {
        return new ThriftClients().makeComponentClient();
    }

    public ByteBuffer exportExcel() throws TException {
        List<Vendor> vendors = getAllVendorList();
        VendorService.Iface sw360VendorClient = getThriftVendorClient();
        return sw360VendorClient.getVendorReportDataStream(vendors);
    }

    private List<Vendor> getAllVendorList() throws TException {
        VendorService.Iface sw360VendorClient = getThriftVendorClient();
        return sw360VendorClient.getAllVendors();
    }

    public Set<Release> getAllReleaseList(String vendorId) throws TException {
        ComponentService.Iface componentsClient = getThriftComponentClient();
        Set<Release> releases = componentsClient.getReleasesByVendorId(vendorId);
        return releases;
    }

    public RequestStatus mergeVendors(String vendorTargetId, String vendorSourceId, Vendor vendorSelection, User user) throws TException, ResourceClassNotFoundException {
        VendorService.Iface sw360VendorClient = getThriftVendorClient();
        RequestStatus requestStatus;
        requestStatus =  sw360VendorClient.mergeVendors(vendorTargetId, vendorSourceId, vendorSelection, user);

        if (requestStatus == RequestStatus.IN_USE) {
            throw new BadRequestClientException("Vendor used as source or target has an open MR");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new ResourceClassNotFoundException("Internal server error while merging the vendors");
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new AccessDeniedException("Access denied");
        }

        return requestStatus;
    }

    /**
     * Converts a Pageable object to a PaginationData object.
     *
     * @param pageable the Pageable object to convert
     * @return a PaginationData object representing the pagination information
     */
    private static PaginationData pageableToPaginationData(
            @NotNull Pageable pageable, VendorSortColumn defaultSort, Boolean defaultAscending
    ) {
        VendorSortColumn column = VendorSortColumn.BY_FULLNAME;
        boolean ascending = true;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            column = switch (property) {
                case "fullname" -> VendorSortColumn.BY_FULLNAME;
                case "shortname" -> VendorSortColumn.BY_SHORTNAME;
                default -> column; // Default to BY_NAME if no match
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
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.getValue()).setAscending(ascending);
    }
}
