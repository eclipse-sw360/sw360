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

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VendorHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private VendorHandler vendorHandler;
    private VendorRepository vendorRepository;
    private List<Vendor> vendorList;
    private PaginationData pageData;

    @Before
    public void setUp() throws Exception {

        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(
                DatabaseSettingsTest.getConfiguredClient(), dbName);
        vendorList = new ArrayList<>();
        vendorList.add(new Vendor()
                .setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com")
                .setType("vendor"));
        vendorList.add(new Vendor()
                .setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org")
                .setType("vendor"));

        vendorRepository = new VendorRepository(databaseConnector);
        for (Vendor vendor : vendorList) {
            vendorRepository.add(vendor);
        }

        vendorHandler = new VendorHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
        pageData = new PaginationData();
        pageData.setSortColumnNumber(0);
        pageData.setDisplayStart(0);
        pageData.setRowsPerPage(10);
        pageData.setAscending(true);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testGetByID() throws Exception {
        for (Vendor vendor : vendorList) {
            Vendor actualVendor = vendorHandler.getByID(vendor.getId());
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
        Vendor oracle = new Vendor()
                .setShortname("Oracle")
                .setFullname("Oracle Corporation Inc")
                .setUrl("http://www.oracle.com");
        AddDocumentRequestSummary summary = vendorHandler.addVendor(oracle);
        assertNotNull(summary.getId());
        assertEquals(vendorList.size() + 1, vendorHandler.getAllVendors().size());

        Vendor actual = vendorHandler.getByID(summary.getId());

        assertVendorEquals(oracle, actual);
    }

    private static void assertVendorEquals(Vendor expected, Vendor actualVendor) {
        assertEquals(expected.getShortname(), actualVendor.getShortname());
        assertEquals(expected.getFullname(), actualVendor.getFullname());
        assertEquals(expected.getUrl(), actualVendor.getUrl());
        if (expected.getId() != null) {
            assertEquals(expected.getId(), actualVendor.getId());
        }
    }

    @Test
    public void testSearchVendors1() throws Exception {
        pageData.setSortColumnNumber(0);
        Map<PaginationData, List<Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("the", pageData);
        PaginationData pagination = paginatedVendors.keySet().iterator().next();
        List<Vendor> vendors = paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(1L, pagination.totalRowCountOrZero());
        assertEquals(vendorList.get(1).getFullname(), vendors.getFirst().getFullname());
    }

    @Test
    public void testSearchVendors2() throws Exception {
        pageData.setSortColumnNumber(0);
        Map<PaginationData, List<Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("xyz", pageData);
        PaginationData pagination = paginatedVendors.keySet().iterator().next();
        List<Vendor> vendors = paginatedVendors.values().iterator().next();
        assertEquals(0, vendors.size());
        assertEquals(0L, pagination.totalRowCountOrZero());
    }

    @Test
    public void testSearchVendors3() throws Exception {
        pageData.setSortColumnNumber(0);
        Map<PaginationData, List<Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("micro", pageData);
        PaginationData pagination = paginatedVendors.keySet().iterator().next();
        List<Vendor> vendors = paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(1L, pagination.totalRowCountOrZero());
        assertEquals(vendorList.get(0).getFullname(), vendors.getFirst().getFullname());
    }

    @Test
    public void testSearchVendors4() throws Exception {
        pageData.setSortColumnNumber(1);
        Map<PaginationData, List<Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("a", pageData);
        PaginationData pagination = paginatedVendors.keySet().iterator().next();
        List<Vendor> vendors = paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(1L, pagination.totalRowCountOrZero());
        assertEquals(vendorList.get(1).getShortname(), vendors.getFirst().getShortname());
    }

    @Test
    public void testSearchVendorsByScore() throws Exception {
        pageData.setSortColumnNumber(-2);
        Map<PaginationData, List<Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("Apache", pageData);
        List<Vendor> vendors = paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(vendorList.get(1).getFullname(), vendors.getFirst().getFullname());
    }
}
