/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vendors;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;

import java.util.List;

/**
 * Small client for testing a service
 *
 * @author cedric.bodet@tngtech.com
 */
public class TestVendorClient {
    private static final Logger log = LogManager.getLogger(TestVendorClient.class);

    @SuppressWarnings("unused")
    public static void InitDatabase() throws SW360Exception {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);

        databaseConnector.add(new Vendor().setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        databaseConnector.add(new Vendor().setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));
        databaseConnector.add(new Vendor().setShortname("Oracle").setFullname("Oracle Corporation Inc").setUrl("http://www.oracle.com"));
    }

    public static void main(String[] args) throws TException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/vendorservice/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        VendorService.Iface client = new VendorService.Client(protocol);
        PaginationData pageData = new PaginationData();
        pageData.setAscending(true);
        pageData.setRowsPerPage(10);
        pageData.setDisplayStart(0);
        pageData.setSortColumnNumber(0);

        List<Vendor> vendors = client.getAllVendors();

        reportFindings(vendors);

        log.info("Now looking for matches starting with 'm' from vendor service");

        reportFindings(client.searchVendors("m", pageData).values().iterator().next());
    }

    private static void reportFindings(List<Vendor> vendors) {
        log.info("Fetched {} from vendor service", vendors.size());
        for (Vendor vendor : vendors) {
            log.info("{}: {} ({})", vendor.getId(), vendor.getFullname(), vendor.getShortname());
        }
    }

}
