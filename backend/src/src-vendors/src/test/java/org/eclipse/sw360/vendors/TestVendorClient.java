/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.vendors;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Small client for testing a service
 *
 * @author cedric.bodet@tngtech.com
 */
public class TestVendorClient {

    @SuppressWarnings("unused")
    public static void InitDatabase() throws MalformedURLException {
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);

        databaseConnector.add(new Vendor().setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        databaseConnector.add(new Vendor().setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));
        databaseConnector.add(new Vendor().setShortname("Oracle").setFullname("Oracle Corporation Inc").setUrl("http://www.oracle.com"));
    }

    public static void main(String[] args) throws TException, IOException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/vendorservice/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        VendorService.Iface client = new VendorService.Client(protocol);

        List<Vendor> vendors = client.getAllVendors();

        reportFindings(vendors);

        System.out.println("Now looking for matches starting with 'm' from vendor service");

        reportFindings(client.searchVendors("m"));
    }

    private static void reportFindings(List<Vendor> vendors) {
        System.out.println("Fetched " + vendors.size() + " from vendor service");
        for (Vendor vendor : vendors) {
            System.out.println(vendor.getId() + ": " + vendor.getFullname() + " (" + vendor.getShortname() + ")");
        }
    }

}
