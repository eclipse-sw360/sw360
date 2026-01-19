/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vendors;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.VendorSearchHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;

import com.ibm.cloud.cloudant.v1.Cloudant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
public class VendorHandler implements VendorService.Iface {

    private final VendorDatabaseHandler vendorDatabaseHandler;
    private final VendorSearchHandler vendorSearchHandler;

    public VendorHandler() throws IOException {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        vendorSearchHandler = new VendorSearchHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);     // Remove release id from component
    }

    public VendorHandler(Cloudant client, String dbName) throws IOException {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(client, dbName);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        // Use the provided dbName for search handler to allow test databases to work correctly
        vendorSearchHandler = new VendorSearchHandler(client, dbName != null ? dbName : DatabaseSettings.COUCH_DB_DATABASE);     // Fallback to default DB name if null
    }

    @Override
    public Vendor getByID(String id) throws TException {
        assertNotEmpty(id);

        Vendor vendor = vendorDatabaseHandler.getByID(id);
        assertNotNull(vendor);

        return vendor;
    }

    @Override
    public List<Vendor> getAllVendors() throws TException {
        return vendorDatabaseHandler.getAllVendors();
    }

    @Override
    public Map<PaginationData, List<Vendor>> getAllVendorListPaginated(PaginationData pageData) throws TException {
        return vendorDatabaseHandler.getAllVendors(pageData);
    }

    @Override
    public Set<String> getAllVendorNames() throws TException {

        HashSet<String> vendorNames = new HashSet<>();
        for (Vendor vendor : getAllVendors()) {
            vendorNames.add(vendor.getFullname());
            vendorNames.add(vendor.getShortname());
        }
        return vendorNames;

    }

    @Override
    public Map<PaginationData, List<Vendor>> searchVendors(String searchText, PaginationData pageData) throws TException {
        return vendorSearchHandler.search(searchText, pageData);
    }

    @Override
    public List<String> searchVendorIds(String searchText) throws TException {
        return vendorSearchHandler.searchIds(searchText);
    }


    @Override
    public AddDocumentRequestSummary addVendor(Vendor vendor) throws TException {
        assertNotNull(vendor);
        assertIdUnset(vendor.getId());

        return vendorDatabaseHandler.addVendor(vendor);
    }

    @Override
    public RequestStatus deleteVendor(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return vendorDatabaseHandler.deleteVendor(id, user);
    }

    @Override
    public RequestStatus updateVendor(Vendor vendor, User user) throws TException {
        assertUser(user);
        assertNotNull(vendor);
        assertId(vendor.getId());

        return vendorDatabaseHandler.updateVendor(vendor, user);
    }

    @Override
    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor mergeSelection, User user) throws TException {
        assertNotNull(mergeTargetId);
        assertNotNull(mergeSourceId);
        assertNotNull(mergeSelection);

        return vendorDatabaseHandler.mergeVendors(mergeTargetId, mergeSourceId, mergeSelection, user);
    }

    @Override
    public ByteBuffer getVendorReportDataStream(List<Vendor> vendorList) throws TException {
        return vendorDatabaseHandler.getVendorReportDataStream(vendorList);
    }
}
