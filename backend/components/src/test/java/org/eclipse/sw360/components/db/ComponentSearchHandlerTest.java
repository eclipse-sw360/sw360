/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.db;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.db.ComponentSearchHandler;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

public class ComponentSearchHandlerTest {
    private static final String url = DatabaseSettingsTest.COUCH_DB_URL;
    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private static final String email1 = "cedric.bodet@tngtech.com";
    private static final String email2 = "johannes.najjar@tngtech.com";

    private List<Component> components;

    private ComponentSearchHandler searchHandler;

    @Before
    public void setUp() throws Exception {
        assumeCanConnectTo(ThriftClients.BACKEND_URL + "/couchdblucene/");

        components = new ArrayList<>();
        Component component1 = new Component().setId("C1").setName("component1").setDescription("d1").setCreatedBy(email1);
        component1.setLanguages(new HashSet<>(Collections.singleton("C")));
        component1.setCategories(new HashSet<>(Collections.singleton("library")));
        component1.setOperatingSystems(new HashSet<>(Collections.singleton("linux")));
        component1.setSoftwarePlatforms(new HashSet<>(Collections.singleton("boost")));
        component1.setReleaseIds(new HashSet<>(Arrays.asList("R1A", "R1B")));
        component1.setVendorNames(new HashSet<>(Collections.singleton("V1")));
        components.add(component1);
        Component component2 = new Component().setId("C2").setName("component2").setDescription("d2").setCreatedBy(email2);
        component2.setLanguages(new HashSet<>(Arrays.asList("D", "C")));
        component2.setCategories(new HashSet<>(Collections.singleton("test")));
        component2.setOperatingSystems(new HashSet<>(Collections.singleton("test")));
        component2.setSoftwarePlatforms(new HashSet<>(Collections.singleton("test")));
        component2.setReleaseIds(new HashSet<>(Arrays.asList("R2A", "R2B", "R2C")));
        component2.setVendorNames(new HashSet<>(Collections.singleton("V2")));
        components.add(component2);
        Component component3 = new Component().setId("C3").setName("component3").setDescription("d3").setCreatedBy(email1);
        component3.setSubscribers(new HashSet<>(Collections.singleton(email1)));
        component3.setLanguages(new HashSet<>(Collections.singleton("E")));
        components.add(component3);

        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);

        for (Component component : components) {
            component.setType(SW360Constants.TYPE_COMPONENT);
            databaseConnector.add(component);
        }

        searchHandler = new ComponentSearchHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testSearch() throws Exception {

        Map<String, Set<String>> searchRestrictions = new HashMap<>();

        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), is(componentIds(components)));
        searchRestrictions.put("languages", ImmutableSet.of("C"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1", "C2"));
        searchRestrictions.put("languages", ImmutableSet.of("D"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C2"));
        searchRestrictions.put("languages", ImmutableSet.of("C"));
        searchRestrictions.put("categories", ImmutableSet.of("library"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("categories");
        searchRestrictions.put("softwarePlatforms", ImmutableSet.of("boost"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("softwarePlatforms");
        searchRestrictions.put("operatingSystems", ImmutableSet.of("linux"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("operatingSystems");
        searchRestrictions.put("vendorNames", ImmutableSet.of("V1"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), containsInAnyOrder("C1"));
        searchRestrictions.remove("vendorNmaes");
        searchRestrictions.put("vendorNames", ImmutableSet.of("V3"));
        assertThat(componentIds(searchHandler.search("comp", searchRestrictions)), is(empty()));
    }

    private static Set<String> componentIds(Collection<Component> components) {
        return components.stream().map(Component::getId).collect(Collectors.toSet());
    }
}
