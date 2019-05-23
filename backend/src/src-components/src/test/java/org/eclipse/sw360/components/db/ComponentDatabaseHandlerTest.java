/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.components.db;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.*;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getComponentIds;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getReleaseIds;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printFullname;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.ensureEccInformationIsSet;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentDatabaseHandlerTest {

    private static final String dbName = DatabaseSettings.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettings.COUCH_DB_ATTACHMENTS;

    private static final String email1 = "cedric.bodet@tngtech.com";
    private static final String email2 = "johannes.najjar@tngtech.com";

    private static final User user1 = new User().setEmail(email1).setDepartment("AB CD EF").setId("481489458");
    private static final User user2 = new User().setEmail(email2).setDepartment("AB CD EF").setId("4786487647680");

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private List<Component> components;
    private Map<String, Component>  componentMap;
    private List<Release> releases;
    private Map<String, Vendor> vendors;
    private ComponentDatabaseHandler handler;

    private int nextReleaseVersion = 0;


    @Mock
    ComponentModerator moderator;
    @Mock
    ReleaseModerator releaseModerator;

    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);
        assertTestString(attachmentsDbName);

        // Set up vendors
        vendors = new HashMap<>();
        vendors.put("V1", new Vendor().setId("V1").setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        vendors.put("V2", new Vendor().setId("V2").setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));
        vendors.put("V3", new Vendor().setId("V3").setShortname("Oracle").setFullname("Oracle Corporation Inc").setUrl("http://www.oracle.com"));


        components = new ArrayList<>();
        Component component1 = new Component().setId("C1").setName("component1").setDescription("d1").setCreatedBy(email1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2017-07-20");
        component1.addToReleaseIds("R1A");
        component1.addToReleaseIds("R1B");
        components.add(component1);
        Component component2 = new Component().setId("C2").setName("component2").setDescription("d2").setCreatedBy(email2).setMainLicenseIds(new HashSet<>(Arrays.asList("lic2"))).setCreatedOn("2017-07-21");
        component2.addToReleaseIds("R2A");
        component2.addToReleaseIds("R2B");
        component2.addToReleaseIds("R2C");
        components.add(component2);
        Component component3 = new Component().setId("C3").setName("component3").setDescription("d3").setCreatedBy(email1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic3"))).setCreatedOn("2017-07-22");
        component3.addToSubscribers(email1);
        component3.addToLanguages("E");
        components.add(component3);

        releases = new ArrayList<>();
        Release release1a = new Release().setId("R1A").setComponentId("C1").setName("component1").setVersion("releaseA").setCreatedBy(email1).setVendorId("V1");
        releases.add(release1a);
        Release release1b = new Release().setId("R1B").setComponentId("C1").setName("component1").setVersion("releaseB").setCreatedBy(email2).setVendorId("V2");
        release1b.setEccInformation(new EccInformation().setAL("AL"));
        release1b.addToSubscribers(email1);
        releases.add(release1b);
        Release release2a = new Release().setId("R2A").setComponentId("C2").setName("component2").setVersion("releaseA").setCreatedBy(email1).setVendorId("V3");
        releases.add(release2a);
        Release release2b = new Release().setId("R2B").setComponentId("C2").setName("component2").setVersion("releaseB").setCreatedBy(email2).setVendorId("V1");
        releases.add(release2b);
        release2b.addToSubscribers(email2);
        Release release2c = new Release().setId("R2C").setComponentId("C2").setName("component2").setVersion("releaseC").setCreatedBy(email1).setVendorId("V2");
        releases.add(release2c);

        // Create the database
        TestUtils.createDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);

        // Prepare the database
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);

        for (Vendor vendor : vendors.values()) {
            databaseConnector.add(vendor);
        }
        for (Component component : components) {
            databaseConnector.add(component);
        }
        for (Release release : releases) {
            databaseConnector.add(release);
        }

        componentMap= ThriftUtils.getIdMap(components);

        // Prepare the handler
        handler = new ComponentDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), dbName, attachmentsDbName, moderator, releaseModerator);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);
    }

    @Test
    public void testGetComponentByReleaseId() throws Exception {
        Component component = new Component().setId("Linking").setName("Linking").setDescription("d1").setCreatedBy(email1);

        final HashMap<String, ReleaseRelationship> releaseLink = new HashMap<>();

        releaseLink.put("R1A", ReleaseRelationship.CONTAINED);
        releaseLink.put("R2A", ReleaseRelationship.CONTAINED);

        Release release = new Release().setId("LinkingRelease").setComponentId("Linking").setName("Linking").setVersion("1.0")
                .setCreatedBy(email1).setVendorId("V1").setReleaseIdToRelationship(releaseLink);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        final Set<Component> usingComponents = handler.getUsingComponents("R1A");

        assertThat(getComponentIds(usingComponents), containsInAnyOrder("Linking"));
    }


    @Test
    public void testGetComponentByReleaseIds() throws Exception {
        Component component = new Component().setId("Linking").setName("Linking").setDescription("d1").setCreatedBy(email1);

        final HashMap<String, ReleaseRelationship> releaseLink = new HashMap<>();

        releaseLink.put("R1A", ReleaseRelationship.CONTAINED);
        releaseLink.put("R2A", ReleaseRelationship.CONTAINED);

        Release release = new Release().setId("LinkingRelease").setComponentId("Linking").setName("Linking").setVersion("1.0")
                .setCreatedBy(email1).setVendorId("V1").setReleaseIdToRelationship(releaseLink);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        final Set<Component> usingComponents = handler.getUsingComponents(ImmutableSet.of("R1A", "R2A"));

        assertThat(getComponentIds(usingComponents), containsInAnyOrder("Linking"));
    }

    @Test
    public void testGetComponentSummary() throws Exception {
        List<Component> summary = handler.getComponentSummary(user1);

        assertEquals(3, summary.size());
        assertTrue(componentsContain(summary, "C1"));
        assertTrue(componentsContain(summary, "C2"));
        assertTrue(componentsContain(summary, "C3"));
    }

    @Test
    public void testGetComponentSummarySingleItem() throws Exception {
        List<Component> summary = handler.getComponentSummary(user1);

        Component component = getComponent(summary, "C1");
        assertNotNull(component);
        assertEquals("C1", component.getId());
        assertEquals("component1", component.getName());

        for (Component._Fields field : Component._Fields.values()) {
            boolean isSet = component.isSet(field);
            switch (field) {
                // Fields that are defined
                case ID:
                case NAME:
                case MAIN_LICENSE_IDS:
                case PERMISSIONS:
                    assertTrue(field.getFieldName(), isSet);
                    break;
                // Fields that may or may not be defined
                case CATEGORIES:
                case TYPE:
                    break;
                // Fields that are not defined
                default:
                    break;
            }
        }
    }


    @Test
    public void testGetReleaseSummary() throws Exception {
        List<Release> summary = handler.getReleaseSummary();

        assertEquals(5, summary.size());
    }

    @Test
    public void testComponentSummary() throws Exception {
        List<Component> summary = handler.getComponentSummary(user1);
        assertEquals(3, summary.size());
    }

    @Test
    public void testGetRecentComponents() throws Exception {
        List<Component> recentComponents = handler.getRecentComponentsSummary(5, user1);
        Set<String> componentIds = getComponentIds(recentComponents);
        assertThat(componentIds, containsInAnyOrder("C3", "C2", "C1"));
    }

    @Test
    public void testGetRecentComponents2() throws Exception {
        List<Component> recentComponents = handler.getRecentComponentsSummary(2, user1);
        Set<String> componentIds = getComponentIds(recentComponents);
        assertEquals(2, recentComponents.size());
        assertThat(componentIds, containsInAnyOrder("C3", "C2"));
    }

    @Test
    public void testGetRecentReleases() throws Exception {
        List<Release> recentReleases = handler.getRecentReleases();
        Iterable<String> relaseIds = getReleaseIds(recentReleases);

        assertThat(relaseIds, containsInAnyOrder("R1A", "R1B", "R2A", "R2B", "R2C"));
    }


    @Test
    public void testGetReleasesFromVendorId() throws Exception {
        List<Release> v1 = handler.getReleasesFromVendorId("V1", user1);   //user is just needed for permissions
        List<Release> v2 = handler.getReleasesFromVendorId("V2", user1);
        List<Release> v3 = handler.getReleasesFromVendorId("V3", user1);

        assertThat(getReleaseIds(v1), containsInAnyOrder("R1A", "R2B"));
        assertThat(getReleaseIds(v2), containsInAnyOrder("R1B", "R2C"));
        assertThat(getReleaseIds(v3), containsInAnyOrder("R2A"));

    }

    @Test
    public void testSearchReleaseByNamePrefix() throws Exception {
        List<Release> releases = handler.searchReleaseByNamePrefix("component1");
        assertThat(getReleaseIds(releases), containsInAnyOrder("R1A", "R1B"));
    }

    @Test
    public void testSearchReleaseByNamePrefix2() throws Exception {
        List<Release> releases = handler.searchReleaseByNamePrefix("compo");
        assertThat(getReleaseIds(releases), containsInAnyOrder("R1A", "R1B", "R2A", "R2B", "R2C"));
    }

    @Test
    public void testGetSummaryForExport() throws Exception {
        List<Component> summaryForExport = handler.getSummaryForExport();
        // C4 should NOT be in the results
        assertThat(getComponentIds(summaryForExport), containsInAnyOrder("C1", "C2", "C3"));
    }

    @Test
    public void testGetSubscribedComponents() throws Exception {
        List<Component> user1components = handler.getSubscribedComponents(email1);
        List<Component> user2components = handler.getSubscribedComponents(email2);

        assertThat(getComponentIds(user1components), contains("C3"));
        assertThat(user2components, is(empty()));

        handler.subscribeComponent("C1", user2);

        List<Component> user2components2 = handler.getSubscribedComponents(email2);
        assertThat(getComponentIds(user2components2), contains("C1"));

        handler.unsubscribeComponent("C1", user2);
        List<Component> user2components3 = handler.getSubscribedComponents(email2);
        assertThat(user2components3, is(empty()));
    }

    @Test
    public void testGetSubscribedReleases() throws Exception {
        List<Release> user1releases = handler.getSubscribedReleases(email1);

        handler.subscribeRelease("R1A", user2);
        List<Release> user2releases = handler.getSubscribedReleases(email2);

        assertThat(getReleaseIds(user1releases), contains("R1B"));
        assertThat(getReleaseIds(user2releases), contains("R1A", "R2B"));

        handler.unsubscribeRelease("R1A", user2);
        assertThat(getReleaseIds(handler.getSubscribedReleases(email2)), contains("R2B"));

    }

    @Test
    public void testGetLinkedReleases() throws Exception {

        final Map<String, ReleaseRelationship> relations = new HashMap<>();
        relations.put("R1A", ReleaseRelationship.REFERRED);

        final Release r1A = handler.getRelease("R1A", user1);
        r1A.setReleaseIdToRelationship(ImmutableMap.of("R1B", ReleaseRelationship.CONTAINED,
                                                       "R2A", ReleaseRelationship.REFERRED
            ));

        handler.updateRelease(r1A, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);

        final Release r1B = handler.getRelease("R1B", user2);
        r1B.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.REFERRED));
        handler.updateRelease(r1B,user2, ThriftUtils.IMMUTABLE_OF_RELEASE);

        final Release r2A = handler.getRelease("R2A", user1);
        r2A.setReleaseIdToRelationship(ImmutableMap.of("R2B", ReleaseRelationship.CONTAINED));
        handler.updateRelease(r2A, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);

        final Release r2B = handler.getRelease("R2B", user2);
        r2B.setReleaseIdToRelationship(ImmutableMap.of("R1B", ReleaseRelationship.CONTAINED,
                "R1A", ReleaseRelationship.REFERRED));

        handler.updateRelease(r2B, user2, ThriftUtils.IMMUTABLE_OF_RELEASE);

        // we wrap the potentially infinite loop in an executor
        final ExecutorService service = Executors.newSingleThreadExecutor();

        final Future<List<ReleaseLink>> completionFuture = service.submit(() -> handler.getLinkedReleases(relations));

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        final List<ReleaseLink> linkedReleases = completionFuture.get();

        ReleaseLink releaseLinkR1A = createReleaseLinkTo(r1A)
                .setReleaseRelationship(ReleaseRelationship.REFERRED)
                .setNodeId("R1A");

        stripRandomPartsOfNodeIds(linkedReleases);

        assertThat(linkedReleases, contains(releaseLinkR1A));
    }

    private void stripRandomPartsOfNodeIds(List<ReleaseLink> linkedReleases) {
        linkedReleases.forEach(rl -> rl.setNodeId(rl.getNodeId().split("_")[0]));
    }

    @NotNull
    private ReleaseLink createReleaseLinkTo(Release release) {
        release.setVendor(vendors.get(release.getVendorId()));
        return new ReleaseLink(release.getId(),
                vendors.get(release.getVendorId()).getShortname(),
                componentMap.get(release.getComponentId()).getName(),
                release.getVersion(),
                printFullname(release), !nullToEmptyMap(release.getReleaseIdToRelationship()).isEmpty());
    }

    @Test
    public void testGetLinkedReleases2() throws Exception {

        final Map<String, ReleaseRelationship> relations = new HashMap<>();
        relations.put("R1A", ReleaseRelationship.REFERRED);

        final Release r1A = handler.getRelease("R1A", user1);
        r1A.setReleaseIdToRelationship(ImmutableMap.of("R1B", ReleaseRelationship.CONTAINED,
                "R2A", ReleaseRelationship.REFERRED
        ));

        handler.updateRelease(r1A, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);

        final Release r1B = handler.getRelease("R1B", user2);
        r1B.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.CONTAINED));
        handler.updateRelease(r1B,user2, ThriftUtils.IMMUTABLE_OF_RELEASE);

        final Release r2A = handler.getRelease("R2A", user1);
        handler.updateRelease(r2A, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);

        // we wrap the potentially infinite loop in an executor
        final ExecutorService service = Executors.newSingleThreadExecutor();

        final Future<List<ReleaseLink>> completionFuture = service.submit(() -> handler.getLinkedReleases(relations));

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        final List<ReleaseLink> linkedReleases = completionFuture.get();

        ReleaseLink releaseLinkR1A = createReleaseLinkTo(r1A)
                .setReleaseRelationship(ReleaseRelationship.REFERRED)
                .setNodeId("R1A");

        stripRandomPartsOfNodeIds(linkedReleases);
        assertThat(linkedReleases, contains(releaseLinkR1A));
    }

    @Test
    public void testGetReleases() throws Exception {
        Set<String> releaseIds = getReleaseIds(this.releases);
        List<Release> releases = handler.getReleases(releaseIds);

        assertThat(getReleaseIds(releases), is(releaseIds));
    }

    @Test
    public void testGetReleasesWithPermissions() throws Exception {
        Set<String> releaseIds = getReleaseIds(this.releases);
        List<Release> releases = handler.getReleasesWithPermissions(releaseIds, user1);
        assertThat(getReleaseIds(releases), is(releaseIds));

        Release releaseA = null;
        Release releaseB = null;


        for (Release release : releases) {
            if (release.getId().equals("R1A")) {
                releaseA = release;
            }

            if (release.getId().equals("R1B")) {
                releaseB = release;
            }
        }

        if (releaseA == null) releaseA = new Release();
        if (releaseB == null) releaseB = new Release();

        Map<RequestedAction, Boolean> permissionsOfOwnRelease = releaseA.getPermissions();

        assertThat(permissionsOfOwnRelease.get(RequestedAction.READ), is(true));
        assertThat(permissionsOfOwnRelease.get(RequestedAction.ATTACHMENTS), is(true));
        assertThat(permissionsOfOwnRelease.get(RequestedAction.WRITE), is(true));
        assertThat(permissionsOfOwnRelease.get(RequestedAction.CLEARING), is(true));
        assertThat(permissionsOfOwnRelease.get(RequestedAction.DELETE), is(true));
        assertThat(permissionsOfOwnRelease.get(RequestedAction.USERS), is(true));

        Map<RequestedAction, Boolean> permissionsOfForeignRelease = releaseB.getPermissions();
        assertThat(permissionsOfForeignRelease.get(RequestedAction.READ), is(true));
        assertThat(permissionsOfForeignRelease.get(RequestedAction.ATTACHMENTS), is(false));
        assertThat(permissionsOfForeignRelease.get(RequestedAction.WRITE), is(false));
        assertThat(permissionsOfForeignRelease.get(RequestedAction.CLEARING), is(false));
        assertThat(permissionsOfForeignRelease.get(RequestedAction.DELETE), is(false));
        assertThat(permissionsOfForeignRelease.get(RequestedAction.USERS), is(false));
    }

    @Test
    public void testDontDeleteComponentWithReleaseContained() throws Exception {
        Component component = new Component().setId("Del").setName("delete").setDescription("d1").setCreatedBy(email1);
        Release release = new Release().setId("DelR").setComponentId("Del").setName("delete Release").setVersion("1.0").setCreatedBy(email1).setVendorId("V1").setClearingState(ClearingState.NEW_CLEARING);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        {
            Component del = handler.getComponent("Del", user1);
            assertThat(del.getName(), is("delete"));
            Release delR = handler.getRelease("DelR", user1);
            assertThat(delR.getName(), is("delete Release"));
        }

        RequestStatus status = handler.deleteComponent("Del", user1);

        assertThat(status, is(RequestStatus.IN_USE));

        {
            Component del = handler.getComponent("Del", user1);
            assertThat(del.getName(), is("delete"));
            Release delR = handler.getRelease("DelR", user1);
            assertThat(delR.getName(), is("delete Release"));
        }
    }

    @Test
    public void testGetMyComponents() throws Exception {
        List<Component> user2components = handler.getMyComponents(email2);
        List<Component> user1components = handler.getMyComponents(email1);

        assertTrue(componentsContain(user1components, "C1"));
        assertTrue(componentsContain(user1components, "C3"));

        assertTrue(componentsContain(user2components, "C2"));
        assertFalse(componentsContain(user2components, "C3"));
    }

    @Ignore ("This functionality is deactivated due to performance problems. See commit ff0d8f7.")
    @Test
    public void testGetMyComponentsReferencedByRelease() throws Exception {
        List<Component> user1components = handler.getMyComponents(email1);
        List<Component> user2components = handler.getMyComponents(email2);

        assertEquals(3, user1components.size());
        assertEquals(2, user2components.size());

        assertTrue(componentsContain(user1components, "C2"));

        assertTrue(componentsContain(user2components, "C1"));
        assertFalse(componentsContain(user2components, "C3"));
    }

    @Test
    public void testGetComponent() throws Exception {
        Component actual = handler.getComponent("C1", user1);
        Component expected = components.get(0);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(email1, actual.getCreatedBy());

        // Check releases
        assertEquals(2, actual.getReleasesSize());
        assertEquals(0, actual.getReleaseIdsSize());

        assertTrue(releasesContain(actual.getReleases(), "R1A"));
        assertTrue(releasesContain(actual.getReleases(), "R1B"));
        assertFalse(releasesContain(actual.getReleases(), "R2A"));
        assertFalse(releasesContain(actual.getReleases(), "R2B"));
        assertFalse(releasesContain(actual.getReleases(), "R2C"));
    }

    @Test
    public void testGetRelease() throws Exception {
        Release actual = handler.getRelease("R1B", user1);
        Release expected = releases.get(1);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getComponentId(), actual.getComponentId());
        assertEquals(email2, actual.getCreatedBy());

        // Check releases
        assertEquals(1, actual.getSubscribersSize());
        assertTrue(actual.getSubscribers().contains(email1));
    }

    @Test
    public void testAddComponent() throws Exception {
        Component expected = new Component().setName("NEW_CLEARING");
        Release release = new Release().setName("REL").setVersion("VER");
        expected.addToReleases(release);

        String id = handler.addComponent(expected, "new@mail.com").getId();
        assertNotNull(id);

        Component actual = handler.getComponent(id, user1);
        // Check that object was added correctly
        assertEquals(expected.getName(), actual.getName());
        assertEquals("new@mail.com", actual.getCreatedBy());
        assertEquals(0, actual.getReleasesSize()); // Releases are not included!

    }

    @Test
    public void testAddRelease() throws Exception {
        Release expected = new Release().setName("REL").setVersion("VER");
        expected.setComponentId("C1");

        String id = handler.addRelease(expected, user1).getId();
        assertNotNull(id);

        Release actual = handler.getRelease(id, user1);
        // Check that object was added correctly
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());

        // Check that the component was also updated
        Component component = handler.getComponent("C1", user1);
        assertTrue(releasesContain(component.getReleases(), id));
    }


    @Test
    public void testAddReleaseUpdatesMainLicenseIds() throws Exception {
        String componentId = "C4";

        {
            Component component = new Component().setId(componentId).setName("component4").setDescription("d4").setCreatedBy(email1);
            handler.addComponent(component, email1);
        }

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.getMainLicenseIds(), is(empty()));
        }

        String id = addRelease(componentId, ImmutableSet.of("14", "15"));
        String id1 = addRelease(componentId, ImmutableSet.of("14", "13"));

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.getMainLicenseIds(), containsInAnyOrder("13", "14", "15"));
        }

        assertThat(handler.deleteRelease(id, user1), is(RequestStatus.SUCCESS));

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.getMainLicenseIds(), containsInAnyOrder("13", "14"));
        }

        assertThat(handler.deleteRelease(id1, user1), is(RequestStatus.SUCCESS));
        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.getMainLicenseIds(), is(empty()));
        }
    }

    private String addRelease(String componentId, Set<String> licenseIds) throws SW360Exception {
        Release release = new Release()
                .setName("REL")
                .setVersion(nextReleaseVersion+"")
                .setMainLicenseIds(licenseIds)
                .setComponentId(componentId);
        nextReleaseVersion++;
        String id = handler.addRelease(release, user1).getId();
        assertNotNull(id);
        return id;
    }

    @Test
    public void testAddReleaseUpdatesProgrammingLanguagesOperatingSystemsAndVendorNames() throws Exception {
        String componentId = "C4";

        {
            Component component = new Component().setId(componentId).setName("component4").setDescription("d4").setCreatedBy(email1);
            handler.addComponent(component, email1);
        }

        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue("Check that languages are not initialized", component.languages == null);
            assertTrue("Check that operating systems are not initialized", component.operatingSystems == null);
            assertTrue("Check that vendor names are not initialized", component.vendorNames == null);
        }

        Set<String> os = new HashSet<>();
        os.add("Linux Ubuntu");
        os.add("Linux Mint");

        Set<String> lang = new HashSet<>();
        lang.add("C");
        lang.add("C++");

        Release release = new Release().setName("REL").setVersion("VER").setOperatingSystems(os).setLanguages(lang).setVendorId("V1");
        release.setComponentId(componentId);

        String id = handler.addRelease(release, user1).getId();
        assertNotNull(id);

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.languages, containsInAnyOrder("C", "C++"));
            assertThat(component.operatingSystems, containsInAnyOrder("Linux Ubuntu", "Linux Mint"));
            assertThat(component.vendorNames, containsInAnyOrder(vendors.get("V1").getShortname()));
        }
        Set<String> os2 = new HashSet<>();
        os2.add("Linux Debian");
        os2.add("Linux Mint");

        Set<String> lang2 = new HashSet<>();
        lang2.add("C#");
        lang2.add("C++");


        Release release2 = new Release().setName("REL2").setVersion("VER2").setOperatingSystems(os2).setLanguages(lang2).setVendorId("V2");
        release2.setComponentId(componentId);

        String id2 = handler.addRelease(release2, user1).getId();

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.languages, containsInAnyOrder("C", "C++", "C#"));
            assertThat(component.operatingSystems, containsInAnyOrder("Linux Ubuntu", "Linux Mint", "Linux Debian"));
            assertThat(component.vendorNames, containsInAnyOrder(vendors.get("V1").getShortname(), vendors.get("V2").getShortname()));
        }

        handler.deleteRelease(id, user1);

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.languages, containsInAnyOrder("C++", "C#"));
            assertThat(component.operatingSystems, containsInAnyOrder("Linux Mint", "Linux Debian"));
            assertThat(component.vendorNames, containsInAnyOrder(vendors.get("V2").getShortname()));
        }

        handler.deleteRelease(id2, user1);

        {
            Component component = handler.getComponent(componentId, user1);
            assertThat(component.languages, is(empty()));
            assertThat(component.operatingSystems, is(empty()));
            assertThat(component.vendorNames, is(empty()));
        }
    }

    @Test
    public void testUpdateComponent() throws Exception {
        // Make some changes in the component
        Component expected = components.get(0);
        expected.unsetReleases();
        expected.setName("UPDATE");

        RequestStatus status = handler.updateComponent(expected, user1);
        Component actual = handler.getComponent("C1", user1);

        assertEquals(RequestStatus.SUCCESS, status);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(email1, actual.getCreatedBy());

        // Check releases
        assertEquals(2, actual.getReleasesSize());
        assertEquals(0, actual.getReleaseIdsSize());

        assertTrue(releasesContain(actual.getReleases(), "R1A"));
        assertTrue(releasesContain(actual.getReleases(), "R1B"));
        assertFalse(releasesContain(actual.getReleases(), "R2A"));
        assertFalse(releasesContain(actual.getReleases(), "R2B"));
        assertFalse(releasesContain(actual.getReleases(), "R2C"));
    }

    @Test
    public void testUpdateComponentDuplicate() throws Exception {
        // given:
        Component component = components.get(0);
        component.setName("component2");

        // when:
        RequestStatus status = handler.updateComponent(component, user1);

        // then:
        assertThat(status, is(RequestStatus.DUPLICATE));
    }

    @Test
    public void testUpdateInconsistentComponent() throws Exception {
        // Make some changes in the component
        Component expected = components.get(0);
        expected.unsetReleases();
        expected.unsetReleaseIds();

        List<Release> tmpReleases = new ArrayList<>();
        tmpReleases.add(releases.get(0));
        tmpReleases.add(releases.get(1));

        Set<String> tmpReleaseIds = new HashSet<>();
        tmpReleaseIds.add(tmpReleases.get(0).getId());
        tmpReleaseIds.add(tmpReleases.get(1).getId());

        expected.setName("UPDATE");
        expected.setReleaseIds(tmpReleaseIds);
        expected.setReleases(tmpReleases);

        RequestStatus status = handler.updateComponent(expected, user1);
        assertThat(status, is(RequestStatus.SUCCESS));
        Component actual = handler.getComponent("C1", user1);

        //Other asserts have been dealt with in testUpdateComponent

        // Check releases
        assertEquals(2, actual.getReleasesSize());
        assertEquals(0, actual.getReleaseIdsSize());
    }


    @Test
    public void testUpdateComponentSentToModeration() throws Exception {
        // Make some changes in the component
        Component component = components.get(0);
        String expected = component.getName();
        component.setName("UPDATE");

        when(moderator.updateComponent(component, user2)).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateComponent(component, user2);
        Component actual = handler.getComponent("C1", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(component.getId(), actual.getId());
        assertEquals(expected, actual.getName());
        verify(moderator).updateComponent(component, user2);
    }

    @Test
    public void testUpdateRelease() throws Exception {
        Release expected = releases.get(1);
        expected.setName("UPDATED");

        RequestStatus status = handler.updateRelease(expected, user2, ThriftUtils.IMMUTABLE_OF_RELEASE);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SUCCESS, status);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getComponentId(), actual.getComponentId());
        assertEquals(email2, actual.getCreatedBy());

        // Check releases
        assertEquals(1, actual.getSubscribersSize());
        assertTrue(actual.getSubscribers().contains(email1));
    }

    @Test
    public void testUpdateReleaseDuplicate() throws Exception {
        // given:
        Release release = releases.get(0);
        release.setVersion("releaseB");

        // when:
        RequestStatus status = handler.updateRelease(release, user2, ThriftUtils.IMMUTABLE_OF_RELEASE);

        // then:
        assertThat(status, is(RequestStatus.DUPLICATE));
    }

    @Test
    public void testUpdateSentToModeration() throws Exception {
        Release release = releases.get(1);
        String expected = release.getName();
        release.setName("UPDATED");

        when(releaseModerator.updateRelease(release, user1)).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateRelease(release, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        assertEquals(expected, actual.getName());
        verify(releaseModerator).updateRelease(release, user1);
    }

    @Test
    public void testEccUpdateSentToEccModeration() throws Exception {
        Release release = releases.get(1);
        String expected = release.getEccInformation().getAL();
        release.getEccInformation().setAL("UPDATED");

        when(releaseModerator.updateReleaseEccInfo(release, user1)).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateRelease(release, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        assertEquals(expected, actual.getEccInformation().getAL());
        verify(releaseModerator).updateReleaseEccInfo(release, user1);
    }
    @Test
    public void testDeleteComponent() throws Exception {
        RequestStatus status = handler.deleteComponent("C3", user1);
        assertEquals(RequestStatus.SUCCESS, status);
        List<Component> componentSummary = handler.getComponentSummary(user1);
        assertEquals(2, componentSummary.size());
        assertFalse("Component deleted", componentsContain(componentSummary, "C3"));
    }

    @Test
    public void testDeleteComponentNotModerator() throws Exception {
        when(moderator.deleteComponent(any(Component.class), eq(user2))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteComponent("C3", user2);
        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        List<Component> componentSummary = handler.getComponentSummary(user1);
        assertEquals(3, componentSummary.size());
        assertTrue("Component NOT deleted", componentsContain(componentSummary, "C1"));
        verify(moderator).deleteComponent(any(Component.class), eq(user2));
    }

    @Test
    public void testDontDeleteUsedComponent() throws Exception {
        final Release r1A = handler.getRelease("R1A", user1);
        r1A.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.CONTAINED));
        handler.updateRelease(r1A, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);

        RequestStatus status = handler.deleteComponent("C2", user1);
        assertEquals(RequestStatus.IN_USE, status);
        List<Component> componentSummary = handler.getComponentSummary(user1);
        assertEquals(3, componentSummary.size());
        assertTrue("Component not deleted", componentsContain(componentSummary, "C2"));
    }


    @Test
    public void testDeleteRelease() throws Exception {

        RequestStatus status = handler.deleteRelease("R1B", user2);
        assertEquals(RequestStatus.SUCCESS, status);
        List<Release> releaseSummary = handler.getReleaseSummary();
        assertEquals(4, releaseSummary.size());
        assertFalse("Component deleted", releasesContain(releaseSummary, "R1B"));

        // Check deletion in component
        Component component = handler.getComponent("C1", user1);
        assertEquals(1, component.getReleasesSize());
        assertFalse("Release deleted", releasesContain(component.getReleases(), "R1B"));
    }

    @Test
    public void testDontDeleteUsedRelease() throws Exception {

        final Release r1A = handler.getRelease("R1A", user1);
        r1A.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.CONTAINED));
        handler.updateRelease(r1A, user1, ThriftUtils.IMMUTABLE_OF_RELEASE);

        RequestStatus status = handler.deleteRelease("R2A", user1);
        assertEquals(RequestStatus.IN_USE, status);
        List<Release> releaseSummary = handler.getReleaseSummary();
        assertEquals(5, releaseSummary.size());
        assertTrue("Release not deleted", releasesContain(releaseSummary, "R2A"));
    }

    @Test
    public void testDeleteReleaseNotModerator() throws Exception {
        when(releaseModerator.deleteRelease(any(Release.class), eq(user1))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteRelease("R1B", user1);
        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        List<Release> releaseSummary = handler.getReleaseSummary();
        assertEquals(5, releaseSummary.size());
        assertTrue("Component NOT deleted", releasesContain(releaseSummary, "R1B"));
        verify(releaseModerator).deleteRelease(any(Release.class), eq(user1));
    }

    private static boolean componentsContain(Collection<Component> components, @NotNull String id) {
        for (Component component : components) {
            if (id.equals(component.getId()))
                return true;
        }
        return false;
    }

    private static Component getComponent(Collection<Component> components, @NotNull String id) {
        for (Component component : components) {
            if (id.equals(component.getId()))
                return component;
        }
        return null;
    }

    private static boolean releasesContain(Collection<Release> releases, @NotNull String id) {
        for (Release release : releases) {
            if (id.equals(release.getId()))
                return true;
        }
        return false;
    }


    @Test
    public void testDuplicateComponentNotAdded() throws Exception {
        String originalComponentId = "C3";
        final Component tmp = handler.getComponent(originalComponentId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        handler.addComponent(tmp, email1).getId();

        final Map<String, List<String>> duplicateComponents = handler.getDuplicateComponents();

        assertThat(duplicateComponents.size(), is(0));
    }


    @Test
    public void testDuplicateReleaseNotAdded() throws Exception {

        String originalReleaseId = "R1A";
        final Release tmp = handler.getRelease(originalReleaseId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        String newReleaseId = handler.addRelease(tmp, user1).getId();

        final Map<String, List<String>> duplicateReleases = handler.getDuplicateReleases();

        assertThat(newReleaseId, isEmptyOrNullString());
        assertThat(duplicateReleases.size(), is(0));
    }

    @Test
    public void testDuplicateCheckDoesntMatchByPrefix() throws Exception {

        String originalReleaseId = "R1A";
        final Release tmp = handler.getRelease(originalReleaseId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        tmp.setName(tmp.getName().substring(0, 4));
        String newReleaseId = handler.addRelease(tmp, user1).getId();

        assertThat(newReleaseId, not(isEmptyOrNullString()));
    }

    @Test
    public void testHasChangesInEccFields() throws Exception {
        Release original = handler.getRelease("R1A", user1);
        original.getEccInformation().setEccStatus(ECCStatus.APPROVED).setAssessorDepartment("XYZ").setAssessorContactPerson("asessor@example.com");
        assertThat(handler.hasChangesInEccFields(original, original), is(false));
        ComponentDatabaseHandler.ECC_FIELDS.forEach(
                f -> {
                    Release changed;
                    try {
                        changed = ensureEccInformationIsSet(handler.getRelease("R1A", user1));
                    } catch (SW360Exception e) {
                        throw new RuntimeException(e);
                    }
                    switch(f) {
                        case ECC_STATUS:
                            changed.getEccInformation().setFieldValue(f, ECCStatus.IN_PROGRESS);
                            break;
                        default:
                            changed.getEccInformation().setFieldValue(f, "string value");
                    }
                    assertThat("Field " + f + " did not trigger ecc change flag", handler.hasChangesInEccFields(changed, original), is(true));
                }
        );

        Release changed = handler.getRelease("R1A", user1);
        changed.getEccInformation().setEccStatus(ECCStatus.APPROVED).setAssessorDepartment("XYZ").setAssessorContactPerson("");
        assertThat(handler.hasChangesInEccFields(changed, original), is(false));

    }
}
