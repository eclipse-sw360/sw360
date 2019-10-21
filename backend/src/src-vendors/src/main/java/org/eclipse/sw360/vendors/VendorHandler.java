/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.vendors;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.VendorSearchHandler;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
public class VendorHandler implements VendorService.Iface {

    private final VendorDatabaseHandler vendorDatabaseHandler;
    private final VendorSearchHandler vendorSearchHandler;

    public VendorHandler() throws IOException {
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        vendorSearchHandler = new VendorSearchHandler(databaseConnector);     // Remove release id from component
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
