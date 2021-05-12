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
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.VendorSearchHandler;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
public class VendorHandler implements VendorService.Iface {

    private final VendorDatabaseHandler vendorDatabaseHandler;
    private final VendorSearchHandler vendorSearchHandler;

    public VendorHandler() throws IOException {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
        DatabaseConnector databaseConnectorNative = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        vendorSearchHandler = new VendorSearchHandler(databaseConnectorNative, DatabaseSettings.getConfiguredClient());     // Remove release id from component
    }

    public VendorHandler(Supplier<CloudantClient> httpClient,Supplier<HttpClient> clientlient, String dbName) throws IOException {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(httpClient, dbName);
        DatabaseConnector databaseConnectorNative = new DatabaseConnector(clientlient, DatabaseSettings.COUCH_DB_DATABASE);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        vendorSearchHandler = new VendorSearchHandler(databaseConnectorNative, httpClient);     // Remove release id from component
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
    public Set<String> getAllVendorNames() throws TException {

        HashSet<String> vendorNames = new HashSet<>();
        for (Vendor vendor : getAllVendors()) {
            vendorNames.add(vendor.getFullname());
            vendorNames.add(vendor.getShortname());
        }
        return vendorNames;

    }

    @Override
    public List<Vendor> searchVendors(String searchText) throws TException {
        return vendorSearchHandler.search(searchText);
    }

    @Override
    public List<String> searchVendorIds(String searchText) throws TException {
        return vendorSearchHandler.searchIds(searchText);
    }


    @Override
    public String addVendor(Vendor vendor) throws TException {
        assertNotNull(vendor);
        assertIdUnset(vendor.getId());

        vendorDatabaseHandler.addVendor(vendor);

        return vendor.getId();
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

        return vendorDatabaseHandler.updateVendor(vendor, user);
    }

    @Override
    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor mergeSelection, User user) throws TException {
        assertNotNull(mergeTargetId);
        assertNotNull(mergeSourceId);
        assertNotNull(mergeSelection);
        
        return vendorDatabaseHandler.mergeVendors(mergeTargetId, mergeSourceId, mergeSelection, user);
    }
}
