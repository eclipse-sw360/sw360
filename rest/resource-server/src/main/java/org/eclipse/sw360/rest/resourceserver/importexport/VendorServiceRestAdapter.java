/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.importexport;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.vendors.VendorConverter;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.datahandler.vendors.VendorClient;
import org.eclipse.sw360.datahandler.vendors.VendorClients;
import org.springframework.stereotype.Component;

/**
 * Thrift {@link VendorService.Iface} adapter for component CSV import — delegates to the vendors REST backend.
 */
@Component
public class VendorServiceRestAdapter implements VendorService.Iface {

    private VendorClient vendorClient() {
        return VendorClients.get();
    }

    @Override
    public org.eclipse.sw360.datahandler.thrift.vendors.Vendor getByID(String id) throws TException {
        Vendor vendor = vendorClient().getVendorById(id);
        return VendorConverter.toThrift(vendor);
    }

    @Override
    public List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> getAllVendors() throws TException {
        return vendorClient().getAllVendors().stream().map(VendorConverter::toThrift).toList();
    }

    @Override
    public Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> getAllVendorListPaginated(
            org.eclipse.sw360.datahandler.thrift.PaginationData pageData) throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public Set<String> getAllVendorNames() throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> searchVendors(
            String searchText, org.eclipse.sw360.datahandler.thrift.PaginationData pageData) throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public List<String> searchVendorIds(String searchText) throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary addVendor(
            org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendor) throws TException {
        return AddDocumentRequestSummaryConverter.toThrift(
                vendorClient().addVendor(VendorConverter.fromThrift(vendor)));
    }

    @Override
    public org.eclipse.sw360.datahandler.thrift.RequestStatus deleteVendor(String id, User user) throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public org.eclipse.sw360.datahandler.thrift.RequestStatus updateVendor(
            org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendor, User user) throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public ByteBuffer getVendorReportDataStream(List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> vendorList)
            throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }

    @Override
    public org.eclipse.sw360.datahandler.thrift.RequestStatus mergeVendors(
            String vendorTargetId, String vendorSourceId,
            org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendorSelection, User user) throws TException {
        throw new UnsupportedOperationException("Not used by component import");
    }
}
