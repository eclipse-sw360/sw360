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
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class VendorHandlerTest {

    @Autowired
    private VendorHandler vendorHandler;

    @Autowired
    @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE")
    DatabaseConnectorCloudant databaseConnector;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    private List<Vendor> vendorList;

    @Before
    public void setUp() throws Exception {
        // Prepare the database
        vendorList = new ArrayList<>();
        vendorList.add(new Vendor().setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        vendorList.add(new Vendor().setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));

        for (Vendor vendor : vendorList) {
            databaseConnector.add(vendor);
        }
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testGetByID() throws Exception {
        for (Vendor vendor : vendorList) {
            String id = vendor.getId();

            Vendor actualVendor = vendorHandler.getByID(id);
            assertVendorEquals(vendor, actualVendor);
        }
    }

    @Test
    public void testGetAllVendors() throws Exception {
        List<Vendor> actualList = vendorHandler.getAllVendors();
        assertEquals(vendorList.size(), actualList.size());
    }

    @Test
    public void testAddVendor() throws Exception {
        Vendor oracle = new Vendor().setShortname("Oracle").setFullname("Oracle Corporation Inc").setUrl("http://www.oracle.com");
        AddDocumentRequestSummary summary = vendorHandler.addVendor(oracle);
        assertNotNull(summary.getId());
        assertEquals(vendorList.size() + 1, vendorHandler.getAllVendors().size());

        Vendor actual = vendorHandler.getByID(summary.getId());

        assertVendorEquals(oracle, actual);
    }

    private static void assertVendorEquals(Vendor vendor, Vendor actualVendor) {
        assertEquals(vendor.getShortname(), actualVendor.getShortname());
        assertEquals(vendor.getFullname(), actualVendor.getFullname());
        assertEquals(vendor.getUrl(), actualVendor.getUrl());
        assertEquals(vendor.getId(), actualVendor.getId());
    }

    @Test
    @Ignore
    public void testSearchVendors1() throws Exception {
        List<Vendor> vendors = vendorHandler.searchVendors("foundation");
        assertEquals(1, vendors.size());
        assertEquals(vendorList.get(1), vendors.get(0));
    }

    @Test
    @Ignore
    public void testSearchVendors2() throws Exception {
        List<Vendor> vendors = vendorHandler.searchVendors("http");
        assertEquals(0, vendors.size());
    }

    @Test
    @Ignore
    public void testSearchVendors3() throws Exception {
        List<Vendor> vendors = vendorHandler.searchVendors("soft");
        assertEquals(1, vendors.size()); // Software but not Microsoft
    }

    @Test
    @Ignore
    public void testSearchVendors4() throws Exception {
        List<Vendor> vendors = vendorHandler.searchVendors("m");
        assertEquals(1, vendors.size());
    }
}
