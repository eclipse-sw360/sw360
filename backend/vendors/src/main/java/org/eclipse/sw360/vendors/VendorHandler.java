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

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertId;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertIdUnset;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.VendorSearchHandler;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

import com.ibm.cloud.cloudant.v1.Cloudant;

@Service
public class VendorHandler {

    private final VendorDatabaseHandler vendorDatabaseHandler;
    private final VendorSearchHandler vendorSearchHandler;

    public VendorHandler() throws IOException {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(
                DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        vendorSearchHandler = new VendorSearchHandler(
                DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }

    public VendorHandler(Cloudant client, String dbName) throws IOException {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(client, dbName);
        vendorDatabaseHandler = new VendorDatabaseHandler(databaseConnector);
        vendorSearchHandler = new VendorSearchHandler(
                client, dbName != null ? dbName : DatabaseSettings.COUCH_DB_DATABASE);
    }

    public Vendor getByID(String id) {
        try {
            assertNotEmpty(id);
            Vendor vendor = vendorDatabaseHandler.getByID(id);
            assertNotNull(vendor);
            return vendor;
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Vendor> getAllVendors() {
        return vendorDatabaseHandler.getAllVendors();
    }

    public Map<PaginationData, List<Vendor>> getAllVendorListPaginated(PaginationData pageData) {
        return vendorDatabaseHandler.getAllVendors(pageData);
    }

    public Set<String> getAllVendorNames() {
        HashSet<String> vendorNames = new HashSet<>();
        for (Vendor vendor : getAllVendors()) {
            vendorNames.add(vendor.getFullname());
            vendorNames.add(vendor.getShortname());
        }
        return vendorNames;
    }

    public Map<PaginationData, List<Vendor>> searchVendors(String searchText, PaginationData pageData) {
        return vendorSearchHandler.search(searchText, pageData);
    }

    public List<String> searchVendorIds(String searchText) {
        return vendorSearchHandler.searchIds(searchText);
    }

    public AddDocumentRequestSummary addVendor(Vendor vendor) {
        try {
            assertNotNull(vendor);
            assertIdUnset(vendor.getId());
            return vendorDatabaseHandler.addVendor(vendor);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus deleteVendor(String id, User user) {
        try {
            assertUser(user);
            assertId(id);
            return vendorDatabaseHandler.deleteVendor(id, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus updateVendor(Vendor vendor, User user) {
        try {
            assertUser(user);
            assertNotNull(vendor);
            assertId(vendor.getId());
            return vendorDatabaseHandler.updateVendor(vendor, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor mergeSelection, User user) {
        try {
            assertNotNull(mergeTargetId);
            assertNotNull(mergeSourceId);
            assertNotNull(mergeSelection);
            return vendorDatabaseHandler.mergeVendors(mergeTargetId, mergeSourceId, mergeSelection, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public byte[] getVendorReportDataStream(List<Vendor> vendorList) {
        return vendorDatabaseHandler.getVendorReportDataStream(vendorList);
    }
}
