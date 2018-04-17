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

package org.eclipse.sw360.components.db;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseInstance;
import org.eclipse.sw360.datahandler.db.ComponentSearchHandler;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getComponentIds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

public class ComponentSearchHandlerTest {
    private static final String url = DatabaseSettings.COUCH_DB_URL;
    private static final String dbName = DatabaseSettings.COUCH_DB_DATABASE;

    private static final String email1 = "cedric.bodet@tngtech.com";
    private static final String email2 = "johannes.najjar@tngtech.com";

    private List<Component> components;

    private ComponentSearchHandler searchHandler;


    @Before
    public void setUp() throws Exception {
        assumeCanConnectTo(ThriftClients.BACKEND_URL + "/couchdblucene/");

        components = new ArrayList<>();
        Component component1 = new Component().setId("C1").setName("component1").setDescription("d1").setCreatedBy(email1);
        component1.addToLanguages("C");
        component1.addToCategories("library");
        component1.addToOperatingSystems("linux");
        component1.addToSoftwarePlatforms("boost");
        component1.addToReleaseIds("R1A");
        component1.addToReleaseIds("R1B");
        component1.addToVendorNames("V1");
        components.add(component1);
        Component component2 = new Component().setId("C2").setName("component2").setDescription("d2").setCreatedBy(email2);
        component2.addToLanguages("D");
        component2.addToLanguages("C");
        component2.addToCategories("test");
        component2.addToOperatingSystems("test");
        component2.addToSoftwarePlatforms("test");
        component2.addToReleaseIds("R2A");
        component2.addToReleaseIds("R2B");
        component2.addToReleaseIds("R2C");
        component1.addToVendorNames("V2");
        components.add(component2);
        Component component3 = new Component().setId("C3").setName("component3").setDescription("d3").setCreatedBy(email1);
        component3.addToSubscribers(email1);
        component3.addToLanguages("E");
        components.add(component3);

        // Create the database
        TestUtils.createDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);

        // Prepare the database
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);

        for (Component component : components) {
            databaseConnector.add(component);
        }

        // Prepare the handler
        searchHandler = new ComponentSearchHandler(DatabaseSettings.getConfiguredHttpClient(), dbName);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);
    }

    @Test
    public void testSearch() throws Exception {

        Map<String, Set<String>> searchRestrictions = new HashMap<>();

        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), is(getComponentIds(components)));
        searchRestrictions.put("languages", ImmutableSet.of("C"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1", "C2"));
        searchRestrictions.put("languages", ImmutableSet.of("D"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C2"));
        searchRestrictions.put("languages", ImmutableSet.of("C"));
        searchRestrictions.put("categories", ImmutableSet.of("library"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("categories");
        searchRestrictions.put("softwarePlatforms", ImmutableSet.of("boost"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("softwarePlatforms");
        searchRestrictions.put("operatingSystems", ImmutableSet.of("linux"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("operatingSystems");
        searchRestrictions.put("vendorNames", ImmutableSet.of("V1"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("vendorNmaes");
        searchRestrictions.put("vendorNames", ImmutableSet.of("V3"));
        assertThat(getComponentIds(searchHandler.search("comp", searchRestrictions)), is(empty()));
    }


}
