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
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.After;
import org.junit.Before;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for LicenseHandler tests. Extracted from LicenseHandlerTest
 * so we don't have to copy-paste the setup code every time we add a new test file.
 */
public abstract class AbstractLicenseHandlerTest {

    protected static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    protected LicenseHandler handler;
    protected User user;

    protected Map<String, License> licenses;
    protected Map<String, Obligation> obligs;

    // same helper that was in LicenseHandlerTest
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
        // fresh db for each test
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        createTestEntries();
        handler = new LicenseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
        user = new User().setEmail("test@siemens.com").setDepartment("CT BE OP SWI OSS").setUserGroup(UserGroup.ADMIN);
    }

    @After
    public void tearDown() throws Exception {
        // clean up after each test
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    // creates 2 licenses and 5 obligations, same data as LicenseHandlerTest
    protected void createTestEntries() throws SW360Exception {
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

        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);

        for (Obligation oblig : obligs.values()) {
            db.add(oblig);
        }

        for (License license : licenses.values()) {
            db.add(license);
        }
    }
}