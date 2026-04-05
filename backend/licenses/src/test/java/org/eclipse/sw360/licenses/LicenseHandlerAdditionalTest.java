/*
 * Copyright Siemens AG, 2013-2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class for additional LicenseHandler methods that have no test coverage.
 * 
 * This test file addresses issue #3736: Missing test coverage for LicenseHandler business logic methods
 * 
 * Methods tested:
 * - deleteLicense (positive + negative)
 * - addLicenseType (positive + negative)
 * - deleteLicenseType (positive)
 * - getLicenseSummaryForExport
 * - getLicenseTypes
 * - testDeleteNonExistentLicense
 * - testDeleteNonExistentLicenseType
 */
public class LicenseHandlerAdditionalTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private LicenseHandler handler;
    private User adminUser;
    private User clearingAdminUser;
    private User nonAdminUser;
    private User sw360AdminUser;
    private Map<String, License> licenses;

    @Before
    public void setUp() throws Exception {
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        createTestEntries();
        handler = new LicenseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
        
        adminUser = new User()
                .setEmail("admin@sw360.org")
                .setDepartment("CT BE OP SWI OSS")
                .setUserGroup(UserGroup.ADMIN);
        
        clearingAdminUser = new User()
                .setEmail("clearingadmin@sw360.org")
                .setDepartment("CT BE OP SWI OSS")
                .setUserGroup(UserGroup.CLEARING_ADMIN);
        
        nonAdminUser = new User()
                .setEmail("user@sw360.org")
                .setDepartment("CT BE OP SWI OSS")
                .setUserGroup(UserGroup.USER);
        
        sw360AdminUser = new User()
                .setEmail("sw360admin@sw360.org")
                .setDepartment("CT BE OP SWI OSS")
                .setUserGroup(UserGroup.SW360_ADMIN);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }
    
    private void createTestEntries() throws Exception {
        licenses = new HashMap<>();
        
        License license1 = new License();
        license1.setShortname("TestLicense-1");
        license1.setId("TestLicense-1");
        license1.setFullname("Test License 1");
        licenses.put(license1.id, license1);
        
        License license2 = new License();
        license2.setShortname("TestLicense-2");
        license2.setId("TestLicense-2");
        license2.setFullname("Test License 2");
        licenses.put(license2.id, license2);
        
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        
        for (License license : licenses.values()) {
            db.add(license);
        }
    }

    @Test
    public void testDeleteLicense() throws Exception {
        License license = new License();
        String licenseShortname = "TestLicense-Delete-" + System.currentTimeMillis();
        license.setShortname(licenseShortname);
        license.setFullname("Test License for Deletion");
        
        handler.updateLicense(license, adminUser, adminUser);
        
        License created = handler.getByID(licenseShortname, adminUser.getDepartment());
        assertNotNull("License should be created", created);
        
        RequestStatus deleteStatus = handler.deleteLicense(created.getId(), adminUser);
        assertEquals("License deletion should succeed", RequestStatus.SUCCESS, deleteStatus);
        
        assertThrows(SW360Exception.class, () -> handler.getByID(created.getId(), adminUser.getDepartment()));
    }

    @Test
    public void testDeleteLicenseNonAdmin() throws Exception {
        License license = new License();
        String licenseShortname = "TestLicense-DeleteNonAdmin-" + System.currentTimeMillis();
        license.setShortname(licenseShortname);
        license.setFullname("Test License for Deletion by Non-Admin");
        
        handler.updateLicense(license, adminUser, adminUser);
        
        License created = handler.getByID(licenseShortname, adminUser.getDepartment());
        assertNotNull("License should be created", created);
        
        RequestStatus deleteStatus = handler.deleteLicense(created.getId(), nonAdminUser);
        assertEquals("Non-admin should not be able to delete license", RequestStatus.FAILURE, deleteStatus);
    }

    @Test
    public void testAddLicenseType() throws Exception {
        LicenseType licenseType = new LicenseType();
        String typeName = "Test License Type " + System.currentTimeMillis();
        licenseType.setLicenseType(typeName);
        
        RequestStatus addStatus = handler.addLicenseType(licenseType, adminUser);
        assertEquals("License type addition should succeed", RequestStatus.SUCCESS, addStatus);
        
        List<LicenseType> types = handler.getLicenseTypes();
        boolean found = false;
        for (LicenseType type : types) {
            if (typeName.equals(type.getLicenseType())) {
                found = true;
                break;
            }
        }
        assertTrue("Added license type should be found in list", found);
    }

    @Test
    public void testAddLicenseTypeNonAdmin() throws Exception {
        LicenseType licenseType = new LicenseType();
        String typeName = "Test License Type NonAdmin " + System.currentTimeMillis();
        licenseType.setLicenseType(typeName);
        
        RequestStatus addStatus = handler.addLicenseType(licenseType, nonAdminUser);
        assertEquals("Non-admin should not be able to add license type", RequestStatus.ACCESS_DENIED, addStatus);
    }

    @Test
    public void testDeleteLicenseType() throws Exception {
        LicenseType licenseType = new LicenseType();
        String typeName = "Test License Type to Delete " + System.currentTimeMillis();
        licenseType.setLicenseType(typeName);
        
        handler.addLicenseType(licenseType, adminUser);
        
        List<LicenseType> types = handler.getLicenseTypes();
        Integer typeId = null;
        for (LicenseType type : types) {
            if (typeName.equals(type.getLicenseType())) {
                typeId = type.getLicenseTypeId();
                break;
            }
        }
        assertNotNull("License type should be created", typeId);
        
        RequestStatus deleteStatus = handler.deleteLicenseType(typeId.toString(), sw360AdminUser);
        assertEquals("License type deletion should succeed", RequestStatus.SUCCESS, deleteStatus);
        
        List<LicenseType> typesAfterDelete = handler.getLicenseTypes();
        boolean found = false;
        for (LicenseType type : typesAfterDelete) {
            if (typeName.equals(type.getLicenseType())) {
                found = true;
                break;
            }
        }
        assertFalse("License type should be deleted", found);
    }

    @Test
    public void testGetLicenseSummaryForExport() throws Exception {
        License license1 = new License();
        license1.setShortname("ExportTest-1-" + System.currentTimeMillis());
        license1.setFullname("Export Test License 1");
        
        License license2 = new License();
        license2.setShortname("ExportTest-2-" + System.currentTimeMillis());
        license2.setFullname("Export Test License 2");
        
        handler.updateLicense(license1, adminUser, adminUser);
        handler.updateLicense(license2, adminUser, adminUser);
        
        License created1 = handler.getByID(license1.getShortname(), adminUser.getDepartment());
        License created2 = handler.getByID(license2.getShortname(), adminUser.getDepartment());
        
        List<License> summary = handler.getLicenseSummary();
        assertNotNull("License summary should not be null", summary);
        assertTrue("License summary should contain at least the created licenses", summary.size() >= 2);
        
        boolean found1 = false, found2 = false;
        for (License l : summary) {
            if (license1.getShortname().equals(l.getShortname())) found1 = true;
            if (license2.getShortname().equals(l.getShortname())) found2 = true;
        }
        assertTrue("Created license 1 should be in summary", found1);
        assertTrue("Created license 2 should be in summary", found2);
    }

    @Test
    public void testGetLicenseTypes() throws Exception {
        List<LicenseType> types = handler.getLicenseTypes();
        assertNotNull("License types list should not be null", types);
        
        for (LicenseType type : types) {
            assertNotNull("License type ID should not be null", type.getLicenseTypeId());
            assertNotNull("License type name should not be null", type.getLicenseType());
        }
    }

    @Test(expected = org.eclipse.sw360.datahandler.thrift.SW360Exception.class)
    public void testDeleteNonExistentLicense() throws Exception {
        handler.deleteLicense("non_existent_id_12345", adminUser);
    }

    @Test
    public void testDeleteNonExistentLicenseType() throws Exception {
        RequestStatus status = handler.deleteLicenseType("99999", adminUser);
        assertEquals("Deleting non-existent license type should return INVALID_INPUT", RequestStatus.INVALID_INPUT, status);
    }
}
