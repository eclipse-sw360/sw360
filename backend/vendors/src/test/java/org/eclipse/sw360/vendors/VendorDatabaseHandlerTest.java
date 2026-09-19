/*
 * Copyright Kavya Popat, 2026. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VendorDatabaseHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private VendorDatabaseHandler handler;
    private Vendor existingVendor;
    private final User user = new User().setEmail("merge-test@sw360.org").setUserGroup(UserGroup.ADMIN);

    @Before
    public void setUp() throws Exception {
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        existingVendor = new Vendor().setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org");
        databaseConnector.add(existingVendor);

        handler = new VendorDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testMergeVendors_selfMerge_returnsFailure() throws Exception {
        String vendorId = existingVendor.getId();

        RequestStatus status = handler.mergeVendors(vendorId, vendorId, new Vendor(), user);

        assertEquals(RequestStatus.FAILURE, status);

        // the guard fired before any fetch/permission/delete logic ran
        // the vendor must still exist, completely untouched
        Vendor stillExists = handler.getByID(vendorId);
        assertNotNull(stillExists);
        assertEquals("The Apache Software Foundation", stillExists.getFullname());
    }
}
