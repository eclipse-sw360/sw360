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

import java.util.List;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

/**
 * Client API for the vendors backend service.
 *
 * Types are service-api POJOs. See {@link VendorServiceRestClient} and {@link VendorClients}.
 */
public interface VendorClient {

    PaginatedResult<Vendor> getVendorsPage(PaginationData pageData);

    PaginatedResult<Vendor> searchVendors(String searchText, PaginationData pageData);

    Vendor getVendorById(String vendorId);

    List<Vendor> getAllVendors();

    AddDocumentRequestSummary addVendor(Vendor vendor);

    RequestStatus updateVendor(Vendor vendor, User user);

    RequestStatus deleteVendor(String vendorId, User user);

    byte[] exportVendorReport(List<Vendor> vendors);

    RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor vendorSelection, User user);
}
