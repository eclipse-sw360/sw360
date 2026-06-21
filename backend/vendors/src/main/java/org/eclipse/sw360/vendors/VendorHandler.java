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

import com.ibm.cloud.cloudant.v1.Cloudant;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
import org.springframework.stereotype.Service;

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
            org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendor = vendorDatabaseHandler.getByID(id);
            assertNotNull(vendor);
            return ThriftConverter.fromThriftVendor(vendor);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public List<Vendor> getAllVendors() {
        try {
            return vendorDatabaseHandler.getAllVendors().stream()
                    .map(ThriftConverter::fromThriftVendor)
                    .collect(Collectors.toList());
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public Map<PaginationData, List<Vendor>> getAllVendorListPaginated(PaginationData pageData) {
        try {
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> thriftResult =
                    vendorDatabaseHandler.getAllVendors(ThriftConverter.toThriftPaginationData(pageData));
            return convertPaginatedResult(thriftResult);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
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
        Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> thriftResult =
                vendorSearchHandler.search(searchText, ThriftConverter.toThriftPaginationData(pageData));
        return convertPaginatedResult(thriftResult);
    }

    public List<String> searchVendorIds(String searchText) {
        return vendorSearchHandler.searchIds(searchText);
    }

    public AddDocumentRequestSummary addVendor(Vendor vendor) {
        try {
            assertNotNull(vendor);
            assertIdUnset(vendor.getId());
            return ThriftConverter.fromThriftAddDocumentRequestSummary(
                    vendorDatabaseHandler.addVendor(ThriftConverter.toThriftVendor(vendor)));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus deleteVendor(String id, User user) {
        try {
            assertUser(user);
            assertId(id);
            return ThriftConverter.fromThriftRequestStatus(vendorDatabaseHandler.deleteVendor(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus updateVendor(Vendor vendor, User user) {
        try {
            assertUser(user);
            assertNotNull(vendor);
            assertId(vendor.getId());
            return ThriftConverter.fromThriftRequestStatus(
                    vendorDatabaseHandler.updateVendor(ThriftConverter.toThriftVendor(vendor), user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor mergeSelection, User user) {
        try {
            assertNotNull(mergeTargetId);
            assertNotNull(mergeSourceId);
            assertNotNull(mergeSelection);
            return ThriftConverter.fromThriftRequestStatus(vendorDatabaseHandler.mergeVendors(
                    mergeTargetId, mergeSourceId, ThriftConverter.toThriftVendor(mergeSelection), user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public byte[] getVendorReportDataStream(List<Vendor> vendorList) {
        try {
            List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> thriftVendors = vendorList.stream()
                    .map(ThriftConverter::toThriftVendor)
                    .collect(Collectors.toList());
            return vendorDatabaseHandler.getVendorReportDataStream(thriftVendors).array();
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    private static Map<PaginationData, List<Vendor>> convertPaginatedResult(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> thriftResult) {
        Map.Entry<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> entry =
                thriftResult.entrySet().iterator().next();
        PaginationData paginationData = ThriftConverter.fromThriftPaginationData(entry.getKey());
        List<Vendor> vendors = entry.getValue().stream()
                .map(ThriftConverter::fromThriftVendor)
                .collect(Collectors.toList());
        return Map.of(paginationData, vendors);
    }
}
