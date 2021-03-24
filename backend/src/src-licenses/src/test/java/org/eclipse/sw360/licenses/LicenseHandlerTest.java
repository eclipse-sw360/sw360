/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test class for the license service
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private LicenseHandler handler;
    private User user;

    private Map<String, License> licenses;
    private Map<String, Obligation> obligs;

    public static Obligation getById(String id, Collection<Obligation> obligs) {
        if (id != null && obligs != null) {
            for (Obligation oblig : obligs) {
                if (id.equals(oblig.getId()))
                    return oblig;
            }
        }
        return null;
    }

    @Before
    public void setUp() throws Exception {
        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredHttpClient(), dbName);

        // Create all test entries
        createTestEntries();

        // Create the handler
        handler = new LicenseHandler(DatabaseSettingsTest.getConfiguredHttpClient(), dbName);

        // Create the user
        user = new User().setEmail("test@siemens.com").setDepartment("CT BE OP SWI OSS").setUserGroup(UserGroup.ADMIN);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the database
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredHttpClient(), dbName);
    }

    @Test
    public void testGetLicenseSummary() throws Exception {
        List<License> summary = handler.getLicenseSummary();

        assertEquals(summary.size(), licenses.size());

        for (License license : summary) {
            // Only those two fields should be present
            assertNotNull(license.id);
            assertNotNull(license.fullname);
            // The rest should not be set
            assertFalse(license.isSetObligationDatabaseIds());
            assertFalse(license.isSetObligations());
            assertFalse(license.isSetReviewdate());
        }
    }

    @Test
    public void testGetLicense() throws Exception {
        License expLicense = licenses.get("Apache-1.1");
        License actLicense = handler.getByID(expLicense.getId(), user.getDepartment());

        assertEquals(expLicense.getId(), actLicense.getId());
        assertEquals(expLicense.getFullname(), actLicense.getFullname());
        assertEquals(expLicense.getObligationDatabaseIdsSize(), actLicense.getObligationsSize());

        // Check obligations
        for (String id : expLicense.getObligationDatabaseIds()) {
            Obligation actTodo = getById(id, actLicense.getObligations());
            Obligation expTodo = getById(id, obligs.values());

            // Now check equals
            assertEquals(expTodo.getId(), actTodo.getId());
            assertEquals(expTodo.getText(), actTodo.getText());
            assertEquals(expTodo.getRevision(), actTodo.getRevision());
        }
    }

    @Test(expected = SW360Exception.class)
    public void testGetLicense2() throws Exception {
        // Test non existing ID
        License license = handler.getByID("zzPdtr", user.getDepartment());
        assertNull(license);
    }

    @Test(expected = SW360Exception.class)
    public void testGetLicense3() throws Exception {
        // Test existing ID, but that is not a license
        License license = handler.getByID(obligs.values().iterator().next().getId(), user.getDepartment());
        assertNull(license);
    }


    @Test
    public void testAddLicense() throws Exception {
        License license = new License();
        license.setShortname("GPL+3.0");
        license.setFullname("The GPL Software License, Version 3.0");

        RequestStatus status = handler.updateLicense(license, user, user);
        assertEquals(RequestStatus.SUCCESS, status);

        License licenseActual = handler.getByID(license.getShortname(), user.getDepartment());
        assertEquals("GPL+3.0", licenseActual.getId());
        assertEquals("GPL+3.0", licenseActual.getShortname());
        assertEquals("The GPL Software License, Version 3.0", licenseActual.getFullname());
    }

    @Test
    public void testUpdateLicense() throws Exception {
        License license = licenses.get("Apache-1.1");
        license.setFullname("Fullname of license changed");
        RequestStatus status = handler.updateLicense(license, user, user);
        assertEquals(RequestStatus.SUCCESS, status);

        License licenseActual = handler.getByID(license.getShortname(), user.getDepartment());
        assertEquals("Fullname of license changed", licenseActual.getFullname());
    }

    @Test(expected = SW360Exception.class)
    public void testAddLicenseNotValid() throws Exception {
        License invalidLicense = new License();
        invalidLicense.setShortname("Invalid ! License Id ???");
        invalidLicense.setFullname("This is a invalid license");
        handler.updateLicense(invalidLicense, user, user);
    }

    @Test(expected = SW360Exception.class)
    public void testUpdateLicenseIdNotMatch() throws Exception {
        License invalidLicense = licenses.get("Apache-1.1");
        invalidLicense.setShortname("Apache-2.0");
        handler.updateLicense(invalidLicense, user, user);
    }

    public void createTestEntries() throws MalformedURLException {
        // List of test objects
        licenses = new HashMap<>();
        obligs = new HashMap<>();

        License license1 = new License();
        license1.setShortname("Apache-1.1");
        license1.setId("Apache-1.1");
        license1.setFullname("The Apache Software License, Version 1.1");
        license1.setLicenseType(new LicenseType().setLicenseTypeId(3).setType("Red - copyleft effect"));
        license1.setReviewdate("10.10.2010");
        license1.addToObligationDatabaseIds("T1");
        license1.addToObligationDatabaseIds("T2");
        license1.addToObligationDatabaseIds("T5");
        licenses.put(license1.id, license1);

        License license2 = new License();
        license2.setShortname("Apache-2.0");
        license2.setId("Apache-2.0");
        license2.setFullname("The Apache Software License, Version 2.0");
        license2.setReviewdate("12.12.2012");
        license2.addToObligationDatabaseIds("T3");
        license2.addToObligationDatabaseIds("T4");
        licenses.put(license2.id, license2);

        Obligation oblig1 = new Obligation().setId("T1").setText("You must include the acknowledgement as part of the documentation for the end user. An example looks as following:  This product includes software developed by the Apache Software Foundation (http://www.apache.org/).");
        Obligation oblig2 = new Obligation().setId("T2").setText("You must not names listed in in the license at paragraph 4 (for example Apache and Apache Software Foundation) neither in the documentation nor for ads or marketing.");
        Obligation oblig3 = new Obligation().setId("T3").setText("Then you must add the following sentence in the header of any modified/added file: 'Code modifications by Siemens AG are under Siemens license conditions'");
        Obligation oblig4 = new Obligation().setId("T4").setText("You must include a prominent notice in the header of all modified files in the following form: © Siemens AG, [year]");
        Obligation oblig5 = new Obligation().setId("T5").setText("With the Apache License 2.0,no copyleft effect for proprietary code exists. For proprietary Siemens modifications you can choose the license (meaning applying the Apache 2.0 license or any other license)");

        obligs.put("T1", oblig1);
        obligs.put("T2", oblig2);
        obligs.put("T3", oblig3);
        obligs.put("T4", oblig4);
        obligs.put("T5", oblig5);

        DatabaseConnector db = new DatabaseConnector(DatabaseSettingsTest.getConfiguredHttpClient(), dbName);

        // Add obligations to database
        for (Obligation oblig : obligs.values()) {
            db.add(oblig);
        }

        // Finally, add the licenses to the database
        for (License license : licenses.values()) {
            db.add(license);
        }

    }
}
