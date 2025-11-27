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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

/**
 * Small client for testing a service
 *
 * @author cedric.bodet@tngtech.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
@Ignore("Requires a running thrift server")
public class TestVendorClient {

    @Autowired
    @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE")
    DatabaseConnectorCloudant databaseConnector;

    @Autowired
    ThriftClients thriftClients;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void init() throws SW360Exception {
        databaseConnector.add(new Vendor().setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        databaseConnector.add(new Vendor().setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));
        databaseConnector.add(new Vendor().setShortname("Oracle").setFullname("Oracle Corporation Inc").setUrl("http://www.oracle.com"));
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Ignore("Requires a running thrift server")
    @Test
    public void testFetchingVendors() throws TException {
        VendorService.Iface client = thriftClients.makeVendorClient();

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
