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
    private List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> vendorList;
    private PaginationData pageData;

    @Before
    public void setUp() throws Exception {

        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(
                DatabaseSettingsTest.getConfiguredClient(), dbName);
        vendorList = new ArrayList<>();
        vendorList.add(new org.eclipse.sw360.datahandler.thrift.vendors.Vendor()
                .setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        vendorList.add(new org.eclipse.sw360.datahandler.thrift.vendors.Vendor()
                .setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));

        for (org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendor : vendorList) {
            databaseConnector.add(vendor);
        }

        vendorHandler = new VendorHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
        vendorRepository = new VendorRepository(databaseConnector);
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
        for (org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendor : vendorList) {
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

    private static void assertVendorEquals(
            org.eclipse.sw360.datahandler.thrift.vendors.Vendor expected, Vendor actualVendor) {
        assertEquals(expected.getShortname(), actualVendor.getShortname());
        assertEquals(expected.getFullname(), actualVendor.getFullname());
        assertEquals(expected.getUrl(), actualVendor.getUrl());
        assertEquals(expected.getId(), actualVendor.getId());
    }

    @Test
    public void testSearchVendors1() throws Exception {
        pageData.setSortColumnNumber(0);
        Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("the", toThriftPaginationData(pageData));
        org.eclipse.sw360.datahandler.thrift.PaginationData pagination =
                paginatedVendors.keySet().iterator().next();
        List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> vendors =
                paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(1, pagination.getTotalRowCount());
        assertEquals(vendorList.get(1).getFullname(), vendors.getFirst().getFullname());
    }

    @Test
    public void testSearchVendors2() throws Exception {
        pageData.setSortColumnNumber(0);
        Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("xyz", toThriftPaginationData(pageData));
        org.eclipse.sw360.datahandler.thrift.PaginationData pagination =
                paginatedVendors.keySet().iterator().next();
        List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> vendors =
                paginatedVendors.values().iterator().next();
        assertEquals(0, vendors.size());
        assertEquals(0, pagination.getTotalRowCount());
    }

    @Test
    public void testSearchVendors3() throws Exception {
        pageData.setSortColumnNumber(0);
        Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("micro", toThriftPaginationData(pageData));
        org.eclipse.sw360.datahandler.thrift.PaginationData pagination =
                paginatedVendors.keySet().iterator().next();
        List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> vendors =
                paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(1, pagination.getTotalRowCount());
        assertEquals(vendorList.get(0).getFullname(), vendors.getFirst().getFullname());
    }

    @Test
    public void testSearchVendors4() throws Exception {
        pageData.setSortColumnNumber(1);
        Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>> paginatedVendors =
                vendorRepository.searchVendorsWithPagination("a", toThriftPaginationData(pageData));
        org.eclipse.sw360.datahandler.thrift.PaginationData pagination =
                paginatedVendors.keySet().iterator().next();
        List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor> vendors =
                paginatedVendors.values().iterator().next();
        assertEquals(1, vendors.size());
        assertEquals(1, pagination.getTotalRowCount());
        assertEquals(vendorList.get(1).getShortname(), vendors.getFirst().getShortname());
    }

    private static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPaginationData(PaginationData pageData) {
        return new org.eclipse.sw360.datahandler.thrift.PaginationData()
                .setSortColumnNumber(pageData.getSortColumnNumber())
                .setDisplayStart(pageData.getDisplayStart())
                .setRowsPerPage(pageData.getRowsPerPage())
                .setAscending(pageData.getAscending());
    }
}
