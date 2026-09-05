/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.components.db;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.db.SvmConnector;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
// single-type imports win over the thrift.* on-demand import below: the handler now
// returns the service-api status types, while the moderator stubs stay thrift.
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.ReleaseImmutableField;
import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.datahandler.services.components.ClearingState;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.common.utils.converter.components.ComponentConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.services.components.ECCStatus;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.common.utils.converter.vendors.VendorConverter;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.junit.Assume.assumeTrue;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class ComponentDatabaseHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;
    private static final String changeLogsDbName = DatabaseSettingsTest.COUCH_DB_CHANGELOGS;

    private static final String email1 = "cedric.bodet@tngtech.com";
    private static final String email2 = "johannes.najjar@tngtech.com";

    private static final String category = "Mobile";

    private static final User user1 = new User().setEmail(email1).setDepartment("AB CD EF").setId("481489458");
    private static final User user2 = new User().setEmail(email2).setDepartment("AB CD EF").setId("4786487647680");

    private List<Component> components;
    private Map<String, Component>  componentMap;
    private List<Release> releases;
    private Map<String, Vendor> vendors;
    private ComponentDatabaseHandler handler;
    private ProjectDatabaseHandler projectHandler;

    private int nextReleaseVersion = 0;


    @Mock
    ComponentModerator moderator;
    @Mock
    ReleaseModerator releaseModerator;
    @Mock
    ProjectModerator projectModerator;

    @Mock
    SvmConnector svmConnector;

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
        component1.setReleaseIds(new HashSet<>(Arrays.asList("R1A", "R1B")));
        component1.setCategories(new HashSet<>(Collections.singleton(category)));
        components.add(component1);
        Component component2 = new Component().setId("C2").setName("component2").setDescription("d2").setCreatedBy(email2).setMainLicenseIds(new HashSet<>(Arrays.asList("lic2"))).setCreatedOn("2017-07-21");
        component2.setReleaseIds(new HashSet<>(Arrays.asList("R2A", "R2B", "R2C")));
        component2.setCategories(new HashSet<>(Collections.singleton(category)));
        components.add(component2);
        Component component3 = new Component().setId("C3").setName("component3").setDescription("d3").setCreatedBy(email1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic3"))).setCreatedOn("2017-07-22");
        component3.setSubscribers(new HashSet<>(Collections.singleton(email1)));
        component3.setLanguages(new HashSet<>(Collections.singleton("E")));
        component3.setCategories(new HashSet<>(Collections.singleton(category)));
        components.add(component3);

        releases = new ArrayList<>();
        Release release1a = new Release().setId("R1A").setComponentId("C1").setName("component1").setVersion("releaseA").setCreatedBy(email1).setVendorId("V1");
        releases.add(release1a);
        Release release1b = new Release().setId("R1B").setComponentId("C1").setName("component1").setVersion("releaseB").setCreatedBy(email2).setVendorId("V2");
        release1b.setEccInformation(new EccInformation().setAl("AL"));
        release1b.setSubscribers(new HashSet<>(Collections.singleton(email1)));
        releases.add(release1b);
        Release release2a = new Release().setId("R2A").setComponentId("C2").setName("component2").setVersion("releaseA").setCreatedBy(email1).setVendorId("V3");
        releases.add(release2a);
        Release release2b = new Release().setId("R2B").setComponentId("C2").setName("component2").setVersion("releaseB").setCreatedBy(email2).setVendorId("V1");
        releases.add(release2b);
        release2b.setSubscribers(new HashSet<>(Collections.singleton(email2)));
        Release release2c = new Release().setId("R2C").setComponentId("C2").setName("component2").setVersion("releaseC").setCreatedBy(email1).setVendorId("V2");
        releases.add(release2c);

        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        // Prepare the database
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);

        for (Vendor vendor : vendors.values()) {
            databaseConnector.add(vendor);
        }
        for (Component component : components) {
            component.setType(SW360Constants.TYPE_COMPONENT);
            databaseConnector.add(component);
        }
        for (Release release : releases) {
            release.setType(SW360Constants.TYPE_RELEASE);
            databaseConnector.add(release);
        }

        componentMap = components.stream().collect(Collectors.toMap(Component::getId, c -> c));

        // Prepare the handler
        handler = new ComponentDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName, moderator, releaseModerator, projectModerator);
        handler.setSvmConnector(svmConnector);
        projectHandler = new ProjectDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, attachmentsDbName);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testUpdateReleasesWithSvmTrackingFeedback() throws Exception {
        Project project = new Project().setId("P1").setName("Project").setCreatedBy(email1);
        project.setExternalIds(new HashMap<>(ImmutableMap.of(SW360Constants.SVM_COMPONENT_ID_KEY, "unchanged")));
        projectHandler.addProject(project, user1);

        when(svmConnector.fetchComponentMappings())
                .thenReturn(ImmutableMap.of("R1A", ImmutableMap.of(SW360Constants.SVM_COMPONENT_ID_KEY, 123),
                        "R2B", ImmutableMap.of(SW360Constants.SVM_COMPONENT_ID_KEY, 456),
                        project.getId(), ImmutableMap.of(SW360Constants.SVM_COMPONENT_ID_KEY, 789)));
        RequestStatus requestStatus = handler.updateReleasesWithSvmTrackingFeedback();

        assertEquals(RequestStatus.SUCCESS, requestStatus);
        Release r1A = handler.getRelease("R1A", user1);
        assertEquals("123", r1A.getExternalIds().get(SW360Constants.SVM_COMPONENT_ID));
        Release r2B = handler.getRelease("R2B", user1);
        assertEquals("456", r2B.getExternalIds().get(SW360Constants.SVM_COMPONENT_ID));
        Project p1 = projectHandler.getProjectById(project.getId(), user1);
        assertEquals(project.getExternalIds(), p1.getExternalIds());
    }

    @Test
    public void testGetComponentByReleaseId() throws Exception {
        Component component = new Component().setId("Linking").setName("Linking").setDescription("d1").setCreatedBy(email1);
        component.setCategories(new HashSet<>(Collections.singleton(category)));
        final HashMap<String, ReleaseRelationship> releaseLink = new HashMap<>();

        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            releaseLink.put("R1A", ReleaseRelationship.CONTAINED);
            releaseLink.put("R2A", ReleaseRelationship.CONTAINED);
        }

        Release release = new Release().setId("LinkingRelease").setComponentId("Linking").setName("Linking").setVersion("1.0")
                .setCreatedBy(email1).setVendorId("V1").setReleaseIdToRelationship(releaseLink);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        Set<Component> usingComponents;
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            usingComponents = handler.getUsingComponents("R1A");
        } else {
            usingComponents = handler.getUsingComponents(ImmutableSet.of("LinkingRelease"));
        }

        assertTrue(containsInAnyOrder("Linking").matches(componentIds(usingComponents)));
    }


    @Test
    public void testGetComponentByReleaseIds() throws Exception {
        Component component = new Component().setId("Linking").setName("Linking").setDescription("d1").setCreatedBy(email1);
        component.setCategories(new HashSet<>(Collections.singleton(category)));
        final HashMap<String, ReleaseRelationship> releaseLink = new HashMap<>();

        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            releaseLink.put("R1A", ReleaseRelationship.CONTAINED);
            releaseLink.put("R2A", ReleaseRelationship.CONTAINED);
        }

        Release release = new Release().setId("LinkingRelease").setComponentId("Linking").setName("Linking").setVersion("1.0")
                .setCreatedBy(email1).setVendorId("V1").setReleaseIdToRelationship(releaseLink);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        Set<Component> usingComponents;
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            usingComponents = handler.getUsingComponents(ImmutableSet.of("R1A", "R2A"));
        } else {
            usingComponents = handler.getUsingComponents(ImmutableSet.of("LinkingRelease"));
        }

        assertTrue(containsInAnyOrder("Linking").matches(componentIds(usingComponents)));
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

        assertNotNull(component.getId());
        assertNotNull(component.getName());
        assertNotNull(component.getMainLicenseIds());
        assertNotNull(component.getPermissions());
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
        Set<String> componentIds = componentIds(recentComponents);
        assertTrue(containsInAnyOrder("C3", "C2", "C1").matches(componentIds));
    }

    @Test
    public void testGetRecentComponents2() throws Exception {
        List<Component> recentComponents = handler.getRecentComponentsSummary(2, user1);
        Set<String> componentIds = componentIds(recentComponents);
        assertEquals(2, recentComponents.size());
        assertTrue(containsInAnyOrder("C3", "C2").matches(componentIds));
    }

    @Test
    public void testGetRecentReleases() throws Exception {
        List<Release> recentReleases = handler.getRecentReleases();
        Iterable<String> relaseIds = collectReleaseIds(recentReleases);

        assertTrue(containsInAnyOrder("R1A", "R1B", "R2A", "R2B", "R2C").matches(relaseIds));
    }


    @Test
    public void testGetReleasesFromVendorId() throws Exception {
        List<Release> v1 = handler.getReleasesFromVendorId("V1", user1);   //user is just needed for permissions
        List<Release> v2 = handler.getReleasesFromVendorId("V2", user1);
        List<Release> v3 = handler.getReleasesFromVendorId("V3", user1);

        assertTrue(containsInAnyOrder("R1A", "R2B").matches(collectReleaseIds(v1)));
        assertTrue(containsInAnyOrder("R1B", "R2C").matches(collectReleaseIds(v2)));
        assertTrue(containsInAnyOrder("R2A").matches(collectReleaseIds(v3)));

    }

    @Test
    public void testSearchReleaseByNamePrefix() throws Exception {
        List<Release> releases = handler.searchReleaseByNamePrefix("component1");
        assertTrue(containsInAnyOrder("R1A", "R1B").matches(collectReleaseIds(releases)));
    }

    @Test
    public void testSearchReleaseByNamePrefix2() throws Exception {
        List<Release> releases = handler.searchReleaseByNamePrefix("compo");
        assertTrue(containsInAnyOrder("R1A", "R1B", "R2A", "R2B", "R2C").matches(collectReleaseIds(releases)));
    }

    @Test
    public void testGetSummaryForExport() throws Exception {
        List<Component> summaryForExport = handler.getSummaryForExport();
        // C4 should NOT be in the results
        assertTrue(containsInAnyOrder("C1", "C2", "C3").matches(componentIds(summaryForExport)));
    }

    @Test
    public void testGetSubscribedComponents() throws Exception {
        List<Component> user1components = handler.getSubscribedComponents(email1);
        List<Component> user2components = handler.getSubscribedComponents(email2);

        assertTrue(contains("C3").matches(componentIds(user1components)));
        assertTrue(user2components.isEmpty());

        handler.subscribeComponent("C1", user2);

        List<Component> user2components2 = handler.getSubscribedComponents(email2);
        assertTrue(contains("C1").matches(componentIds(user2components2)));

        handler.unsubscribeComponent("C1", user2);
        List<Component> user2components3 = handler.getSubscribedComponents(email2);
        assertTrue(user2components3.isEmpty());
    }

    @Test
    public void testGetSubscribedReleases() throws Exception {
        List<Release> user1releases = handler.getSubscribedReleases(email1);

        handler.subscribeRelease("R1A", user2);
        List<Release> user2releases = handler.getSubscribedReleases(email2);

        assertTrue(contains("R1B").matches(collectReleaseIds(user1releases)));
        assertTrue(contains("R1A", "R2B").matches(collectReleaseIds(user2releases)));

        handler.unsubscribeRelease("R1A", user2);
        assertTrue(contains("R2B").matches(collectReleaseIds(handler.getSubscribedReleases(email2))));

    }

    @Test
    public void testGetLinkedReleases() throws Exception {
        assumeTrue("Not running since Releases cannot be interlinked",
                SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);

        final Map<String, ReleaseRelationship> relations = new HashMap<>();
        relations.put("R1A", ReleaseRelationship.REFERRED);

        final Release r1A = handler.getRelease("R1A", user1);
        r1A.setReleaseIdToRelationship(ImmutableMap.of("R1B", ReleaseRelationship.CONTAINED,
                                                       "R2A", ReleaseRelationship.REFERRED
            ));

        handler.updateRelease(r1A, user1, ReleaseImmutableField.DEFAULT);

        final Release r1B = handler.getRelease("R1B", user2);
        r1B.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.REFERRED));
        handler.updateRelease(r1B,user2, ReleaseImmutableField.DEFAULT);

        final Release r2A = handler.getRelease("R2A", user1);
        r2A.setReleaseIdToRelationship(ImmutableMap.of("R2B", ReleaseRelationship.CONTAINED));
        handler.updateRelease(r2A, user1, ReleaseImmutableField.DEFAULT);

        final Release r2B = handler.getRelease("R2B", user2);
        r2B.setReleaseIdToRelationship(ImmutableMap.of("R1B", ReleaseRelationship.CONTAINED,
                "R1A", ReleaseRelationship.REFERRED));

        handler.updateRelease(r2B, user2, ReleaseImmutableField.DEFAULT);

        // we wrap the potentially infinite loop in an executor
        final ExecutorService service = Executors.newSingleThreadExecutor();

        final Future<List<ReleaseLink>> completionFuture = service.submit(() -> handler.getLinkedReleases(relations));

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        final List<ReleaseLink> linkedReleases = completionFuture.get();

        ReleaseLink releaseLinkR1A = createReleaseLinkTo(r1A)
                .setReleaseRelationship(org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.REFERRED)
                .setNodeId("R1A")
                .setClearingState(org.eclipse.sw360.datahandler.thrift.components.ClearingState.NEW_CLEARING);

        stripRandomPartsOfNodeIds(linkedReleases);

        assertTrue(contains(releaseLinkR1A).matches(linkedReleases));
    }

    private void stripRandomPartsOfNodeIds(List<ReleaseLink> linkedReleases) {
        linkedReleases.forEach(rl -> rl.setNodeId(rl.getNodeId().split("_")[0]));
    }

    private ReleaseLink createReleaseLinkTo(Release release) {
        org.eclipse.sw360.datahandler.services.vendors.Vendor vendor =
                VendorConverter.fromThrift(vendors.get(release.getVendorId()));
        release.setVendor(vendor);
        Component component = componentMap.get(release.getComponentId());
        String fullname = SW360Utils.getReleaseFullname(
                vendor.getShortname(), release.getName(), release.getVersion());
        return new ReleaseLink(release.getId(),
                vendor.getShortname(),
                component.getName(),
                release.getVersion(),
                fullname, !nullToEmptyMap(release.getReleaseIdToRelationship()).isEmpty());
    }

    @Test
    public void testGetLinkedReleases2() throws Exception {
        assumeTrue("Not running since Releases cannot be interlinked",
                SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);

        final Map<String, ReleaseRelationship> relations = new HashMap<>();
        relations.put("R1A", ReleaseRelationship.REFERRED);

        final Release r1A = handler.getRelease("R1A", user1);
        r1A.setReleaseIdToRelationship(ImmutableMap.of("R1B", ReleaseRelationship.CONTAINED,
                "R2A", ReleaseRelationship.REFERRED
        ));

        handler.updateRelease(r1A, user1, ReleaseImmutableField.DEFAULT);

        final Release r1B = handler.getRelease("R1B", user2);
        r1B.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.CONTAINED));
        handler.updateRelease(r1B,user2, ReleaseImmutableField.DEFAULT);

        final Release r2A = handler.getRelease("R2A", user1);
        handler.updateRelease(r2A, user1, ReleaseImmutableField.DEFAULT);

        // we wrap the potentially infinite loop in an executor
        final ExecutorService service = Executors.newSingleThreadExecutor();

        final Future<List<ReleaseLink>> completionFuture = service.submit(() -> handler.getLinkedReleases(relations));

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        final List<ReleaseLink> linkedReleases = completionFuture.get();

        ReleaseLink releaseLinkR1A = createReleaseLinkTo(r1A)
                .setReleaseRelationship(org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.REFERRED)
                .setNodeId("R1A")
                .setClearingState(org.eclipse.sw360.datahandler.thrift.components.ClearingState.NEW_CLEARING);

        stripRandomPartsOfNodeIds(linkedReleases);
        assertTrue(contains(releaseLinkR1A).matches(linkedReleases));
    }

    @Test
    public void testGetReleases() throws Exception {
        Set<String> releaseIds = collectReleaseIds(this.releases);
        List<Release> releases = handler.getReleases(releaseIds);

        assertEquals(releaseIds, collectReleaseIds(releases));
    }

    @Test
    public void testGetReleasesWithPermissions() throws Exception {
        Set<String> expectedReleaseIds = collectReleaseIds(this.releases);
        List<Release> releases = handler.getReleasesWithPermissions(expectedReleaseIds, user1);
        assertEquals(expectedReleaseIds, collectReleaseIds(releases));

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

        assertTrue(permissionsOfOwnRelease.get(RequestedAction.READ));
        assertTrue(permissionsOfOwnRelease.get(RequestedAction.ATTACHMENTS));
        assertTrue(permissionsOfOwnRelease.get(RequestedAction.WRITE));
        assertTrue(permissionsOfOwnRelease.get(RequestedAction.CLEARING));
        assertTrue(permissionsOfOwnRelease.get(RequestedAction.DELETE));
        assertTrue(permissionsOfOwnRelease.get(RequestedAction.USERS));

        Map<RequestedAction, Boolean> permissionsOfForeignRelease = releaseB.getPermissions();
        assertTrue(permissionsOfForeignRelease.get(RequestedAction.READ));
        assertFalse(permissionsOfForeignRelease.get(RequestedAction.ATTACHMENTS));
        assertFalse(permissionsOfForeignRelease.get(RequestedAction.WRITE));
        assertFalse(permissionsOfForeignRelease.get(RequestedAction.CLEARING));
        assertFalse(permissionsOfForeignRelease.get(RequestedAction.DELETE));
        assertFalse(permissionsOfForeignRelease.get(RequestedAction.USERS));
    }

    @Test
    public void testDeleteComponentWithUnusedRelease() throws Exception {
        Component component = new Component().setId("Del").setName("delete").setDescription("d1").setCreatedBy(email1);
        component.setCategories(new HashSet<>(Collections.singleton(category)));
        Release release = new Release().setId("DelR").setComponentId("Del").setName("delete Release").setVersion("1.0").setCreatedBy(email1).setVendorId("V1").setClearingState(ClearingState.NEW_CLEARING);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        {
            Component del = handler.getComponent("Del", user1);
            assertEquals("delete", del.getName());
            Release delR = handler.getRelease("DelR", user1);
            assertEquals("delete Release", delR.getName());
        }

        RequestStatus status = handler.deleteComponent("Del", user1);

        assertEquals(RequestStatus.SUCCESS, status);
    }

    @Test
    public void testDontDeleteComponentWithReleaseInUse() throws Exception {
        Component component = new Component().setId("Del").setName("delete").setDescription("d1").setCreatedBy(email1);
        component.setCategories(new HashSet<>(Collections.singleton(category)));
        Release release = new Release().setId("DelR").setComponentId("Del").setName("delete Release").setVersion("1.0").setCreatedBy(email1).setVendorId("V1").setClearingState(ClearingState.NEW_CLEARING);

        handler.addComponent(component, email1);
        handler.addRelease(release, user1);

        // Make release "DelR" in use by linking it from another release
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            final Release r1A = handler.getRelease("R1A", user1);
            r1A.setReleaseIdToRelationship(ImmutableMap.of("DelR", ReleaseRelationship.CONTAINED));
            handler.updateRelease(r1A, user1, ReleaseImmutableField.DEFAULT);
        } else {
            Project project = new Project().setReleaseIdToUsage(
                    ImmutableMap.of("DelR", new ProjectReleaseRelationship().setReleaseRelation(
                            org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.CONTAINED))
            ).setName("Project").setCreatedBy(email1);
            projectHandler.addProject(project, user1);
        }

        RequestStatus status = handler.deleteComponent("Del", user1);

        assertEquals(RequestStatus.IN_USE, status);

        // Verify component and release still exist
        Component del = handler.getComponent("Del", user1);
        assertEquals("delete", del.getName());
        Release delR = handler.getRelease("DelR", user1);
        assertEquals("delete Release", delR.getName());
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
        assertEquals(2, actual.getReleases() == null ? 0 : actual.getReleases().size());
        assertEquals(0, actual.getReleaseIds() == null ? 0 : actual.getReleaseIds().size());

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
        assertEquals(1, actual.getSubscribers() == null ? 0 : actual.getSubscribers().size());
        assertTrue(actual.getSubscribers().contains(email1));
    }

    @Test
    public void testAddComponent() throws Exception {
        Component expected = new Component().setName("NEW_CLEARING");
        expected.setCategories(new HashSet<>(Collections.singleton(category)));
        Release release = new Release().setName("REL").setVersion("VER");
        expected.setReleases(Collections.singletonList(release));

        String id = handler.addComponent(expected, "new@mail.com").getId();
        assertNotNull(id);

        Component actual = handler.getComponent(id, user1);
        // Check that object was added correctly
        assertEquals(expected.getName(), actual.getName());
        assertEquals("new@mail.com", actual.getCreatedBy());
        assertEquals(0, actual.getReleases() == null ? 0 : actual.getReleases().size()); // Releases are not included!

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
            component.setCategories(new HashSet<>(Collections.singleton(category)));
            handler.addComponent(component, email1);
        }

        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue(component.getMainLicenseIds().isEmpty());
        }

        String id = addRelease(componentId, ImmutableSet.of("14", "15"));
        String id1 = addRelease(componentId, ImmutableSet.of("14", "13"));

        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue(containsInAnyOrder("13", "14", "15").matches(component.getMainLicenseIds()));
        }

        assertEquals(RequestStatus.SUCCESS, handler.deleteRelease(id, user1));

        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue(containsInAnyOrder("13", "14").matches(component.getMainLicenseIds()));
        }

        assertEquals(RequestStatus.SUCCESS, handler.deleteRelease(id1, user1));
        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue(component.getMainLicenseIds().isEmpty());
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
            component.setCategories(new HashSet<>(Collections.singleton(category)));
            handler.addComponent(component, email1);
        }

        {
            Component component = handler.getComponent(componentId, user1);
            assertNull("Check that languages are not initialized", component.getLanguages());
            assertNull("Check that operating systems are not initialized", component.getOperatingSystems());
            assertNull("Check that vendor names are not initialized", component.getVendorNames());
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
            assertTrue(containsInAnyOrder("C", "C++").matches(component.getLanguages()));
            assertTrue(containsInAnyOrder("Linux Ubuntu", "Linux Mint").matches(component.getOperatingSystems()));
            assertTrue(containsInAnyOrder(vendors.get("V1").getShortname()).matches(component.getVendorNames()));
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
            assertTrue(containsInAnyOrder("C", "C++", "C#").matches(component.getLanguages()));
            assertTrue(containsInAnyOrder("Linux Ubuntu", "Linux Mint", "Linux Debian").matches(component.getOperatingSystems()));
            assertTrue(containsInAnyOrder(vendors.get("V1").getShortname(), vendors.get("V2").getShortname()).matches(component.getVendorNames()));
        }

        handler.deleteRelease(id, user1);

        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue(containsInAnyOrder("C++", "C#").matches(component.getLanguages()));
            assertTrue(containsInAnyOrder("Linux Mint", "Linux Debian").matches(component.getOperatingSystems()));
            assertTrue(containsInAnyOrder(vendors.get("V2").getShortname()).matches(component.getVendorNames()));
        }

        handler.deleteRelease(id2, user1);

        {
            Component component = handler.getComponent(componentId, user1);
            assertTrue(component.getLanguages().isEmpty());
            assertTrue(component.getOperatingSystems().isEmpty());
            assertTrue(component.getVendorNames().isEmpty());
        }
    }

    @Test
    public void testUpdateComponent() throws Exception {
        // Make some changes in the component
        Component expected = components.get(0);
        expected.setReleases(null);
        expected.setName("UPDATE");

        RequestStatus status = handler.updateComponent(expected, user1);
        Component actual = handler.getComponent("C1", user1);

        assertEquals(RequestStatus.SUCCESS, status);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(email1, actual.getCreatedBy());

        // Check releases
        assertEquals(2, actual.getReleases() == null ? 0 : actual.getReleases().size());
        assertEquals(0, actual.getReleaseIds() == null ? 0 : actual.getReleaseIds().size());

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
        assertEquals(RequestStatus.DUPLICATE, status);
    }

    @Test
    public void testUpdateInconsistentComponent() throws Exception {
        // Make some changes in the component
        Component expected = components.get(0);
        expected.setReleases(null);
        expected.setReleaseIds(null);

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
        assertEquals(RequestStatus.SUCCESS, status);
        Component actual = handler.getComponent("C1", user1);

        //Other asserts have been dealt with in testUpdateComponent

        // Check releases
        assertEquals(2, actual.getReleases() == null ? 0 : actual.getReleases().size());
        assertEquals(0, actual.getReleaseIds() == null ? 0 : actual.getReleaseIds().size());
    }


    @Test
    public void testUpdateComponentSentToModeration() throws Exception {
        // Make some changes in the component
        Component component = components.get(0);
        String expected = component.getName();
        component.setName("UPDATE");

        when(moderator.updateComponent(any(org.eclipse.sw360.datahandler.thrift.components.Component.class), eq(user2))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateComponent(component, user2);
        Component actual = handler.getComponent("C1", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(component.getId(), actual.getId());
        assertEquals(expected, actual.getName());
        verify(moderator).updateComponent(any(org.eclipse.sw360.datahandler.thrift.components.Component.class), eq(user2));
    }

    @Test
    public void testForceUpdateComponent() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        // Make some changes in the component
        Component component = components.get(0);
        String expected = "UPDATE";
        component.setName(expected);

        lenient().when(moderator.updateComponent(ComponentConverter.toThrift(component), user2)).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateComponent(component, user2, true);
        Component actual = handler.getComponent("C1", user1);

        assertEquals(RequestStatus.SUCCESS, status);

        assertEquals(component.getId(), actual.getId());
        assertEquals(expected, actual.getName());
        verify(moderator, never()).updateComponent(ComponentConverter.toThrift(component), user2);
    }

    @Test
    public void testUpdateRelease() throws Exception {
        Release expected = releases.get(1);
        expected.setName("UPDATED");

        RequestStatus status = handler.updateRelease(expected, user2, ReleaseImmutableField.DEFAULT);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SUCCESS, status);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getComponentId(), actual.getComponentId());
        assertEquals(email2, actual.getCreatedBy());

        // Check releases
        assertEquals(1, actual.getSubscribers() == null ? 0 : actual.getSubscribers().size());
        assertTrue(actual.getSubscribers().contains(email1));
    }

    @Test
    public void testUpdateReleaseDuplicate() throws Exception {
        // given:
        Release release = releases.get(0);
        release.setVersion("releaseB");

        // when:
        RequestStatus status = handler.updateRelease(release, user2, ReleaseImmutableField.DEFAULT);

        // then:
        assertEquals(RequestStatus.DUPLICATE, status);
    }

    @Test
    public void testUpdateSentToModeration() throws Exception {
        Release release = releases.get(1);
        String expected = release.getName();
        release.setName("UPDATED");

        when(releaseModerator.updateRelease(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateRelease(release, user1, ReleaseImmutableField.DEFAULT);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        assertEquals(expected, actual.getName());
        verify(releaseModerator).updateRelease(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1));
    }

    @Test
    public void testForceUpdateRelease() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        Release release = releases.get(1);
        String expected = "UPDATED";
        release.setName(expected);

        lenient().when(releaseModerator.updateRelease(ReleaseConverter.toThrift(release), user1)).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateRelease(release, user1, ReleaseImmutableField.DEFAULT, true);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SUCCESS, status);
        assertEquals(expected, actual.getName());
        verify(releaseModerator, never()).updateRelease(ReleaseConverter.toThrift(release), user1);
    }

    @Test
    public void testEccUpdateSentToEccModeration() throws Exception {
        Release release = releases.get(1);
        String expected = release.getEccInformation().getAl();
        release.getEccInformation().setAl("UPDATED");

        when(releaseModerator.updateReleaseEccInfo(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateRelease(release, user1, ReleaseImmutableField.DEFAULT);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        assertEquals(expected, actual.getEccInformation().getAl());
        verify(releaseModerator).updateReleaseEccInfo(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1));
    }

    @Test
    public void testForceEccUpdate() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        Release release = releases.get(1);
        String expected = "UPDATED";
        release.getEccInformation().setAl(expected);

        lenient().when(releaseModerator.updateReleaseEccInfo(ReleaseConverter.toThrift(release), user1)).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.updateRelease(release, user1, ReleaseImmutableField.DEFAULT, true);
        Release actual = handler.getRelease("R1B", user1);

        assertEquals(RequestStatus.SUCCESS, status);
        assertEquals(expected, actual.getEccInformation().getAl());
        verify(releaseModerator, never()).updateReleaseEccInfo(ReleaseConverter.toThrift(release), user1);
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
        when(moderator.deleteComponent(any(org.eclipse.sw360.datahandler.thrift.components.Component.class), eq(user2))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteComponent("C3", user2);
        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        List<Component> componentSummary = handler.getComponentSummary(user1);
        assertEquals(3, componentSummary.size());
        assertTrue("Component NOT deleted", componentsContain(componentSummary, "C1"));
        verify(moderator).deleteComponent(any(org.eclipse.sw360.datahandler.thrift.components.Component.class), eq(user2));
    }

    @Test
    public void testForceDeleteComponent() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        lenient().when(moderator.deleteComponent(any(org.eclipse.sw360.datahandler.thrift.components.Component.class), eq(user2))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteComponent("C3", user2, true);
        assertEquals(RequestStatus.SUCCESS, status);
        List<Component> componentSummary = handler.getComponentSummary(user1);
        assertEquals(2, componentSummary.size());
        assertFalse("Component deleted", componentsContain(componentSummary, "C3"));
        verify(moderator, never()).deleteComponent(any(org.eclipse.sw360.datahandler.thrift.components.Component.class), eq(user2));
    }

    @Test
    public void testDontDeleteUsedComponent() throws Exception {
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            final Release r1A = handler.getRelease("R1A", user1);
            r1A.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.CONTAINED));
            handler.updateRelease(r1A, user1, ReleaseImmutableField.DEFAULT);
        } else {
            Project project = new Project().setReleaseIdToUsage(
                    ImmutableMap.of("R2A", new ProjectReleaseRelationship().setReleaseRelation(
                            org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.CONTAINED))
            ).setName("Project").setCreatedBy(email1);
            projectHandler.addProject(project, user1);
        }

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
        assertEquals(1, component.getReleases() == null ? 0 : component.getReleases().size());
        assertFalse("Release deleted", releasesContain(component.getReleases(), "R1B"));
    }

    @Test
    public void testDontDeleteUsedRelease() throws Exception {

        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            final Release r1A = handler.getRelease("R1A", user1);
            r1A.setReleaseIdToRelationship(ImmutableMap.of("R2A", ReleaseRelationship.CONTAINED));
            handler.updateRelease(r1A, user1, ReleaseImmutableField.DEFAULT);
        } else {
            Project project = new Project().setReleaseIdToUsage(
                    ImmutableMap.of("R2A", new ProjectReleaseRelationship().setReleaseRelation(
                            org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.CONTAINED))
            ).setName("Project").setCreatedBy(email1);
            projectHandler.addProject(project, user1);
        }

        RequestStatus status = handler.deleteRelease("R2A", user1);
        assertEquals(RequestStatus.IN_USE, status);
        List<Release> releaseSummary = handler.getReleaseSummary();
        assertEquals(5, releaseSummary.size());
        assertTrue("Release not deleted", releasesContain(releaseSummary, "R2A"));
    }

    @Test
    public void testDeleteReleaseNotModerator() throws Exception {
        when(releaseModerator.deleteRelease(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteRelease("R1B", user1);
        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);
        List<Release> releaseSummary = handler.getReleaseSummary();
        assertEquals(5, releaseSummary.size());
        assertTrue("Component NOT deleted", releasesContain(releaseSummary, "R1B"));
        verify(releaseModerator).deleteRelease(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1));
    }

    @Test
    public void testForceDeleteRelease() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        lenient().when(releaseModerator.deleteRelease(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1))).thenReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteRelease("R1B", user1, true);
        assertEquals(RequestStatus.SUCCESS, status);
        List<Release> releaseSummary = handler.getReleaseSummary();
        assertEquals(4, releaseSummary.size());
        assertFalse("Release deleted", releasesContain(releaseSummary, "R1B"));
        verify(releaseModerator, never()).deleteRelease(any(org.eclipse.sw360.datahandler.thrift.components.Release.class), eq(user1));
    }


    private static Set<String> componentIds(Collection<Component> components) {
        return components.stream().map(Component::getId).collect(Collectors.toSet());
    }

    private static Set<String> collectReleaseIds(Collection<Release> releases) {
        return releases.stream().map(Release::getId).collect(Collectors.toSet());
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
        tmp.setId(null);
        tmp.setRevision(null);
        handler.addComponent(tmp, email1).getId();

        final Map<String, List<String>> duplicateComponents = handler.getDuplicateComponents();

        assertTrue(duplicateComponents.isEmpty());
    }


    @Test
    public void testDuplicateReleaseNotAdded() throws Exception {

        String originalReleaseId = "R1A";
        final Release tmp = handler.getRelease(originalReleaseId, user1);
        tmp.setId(null);
        tmp.setRevision(null);
        AddDocumentRequestSummary summary = handler.addRelease(tmp, user1);

        final Map<String, List<String>> duplicateReleases = handler.getDuplicateReleases();

        assertEquals(AddDocumentRequestStatus.DUPLICATE, summary.getRequestStatus());
        assertTrue(duplicateReleases.isEmpty());
    }

    @Test
    public void testDuplicateCheckDoesntMatchByPrefix() throws Exception {

        String originalReleaseId = "R1A";
        final Release tmp = handler.getRelease(originalReleaseId, user1);
        tmp.setId(null);
        tmp.setRevision(null);
        tmp.setName(tmp.getName().substring(0, 4));
        String newReleaseId = handler.addRelease(tmp, user1).getId();

        assertFalse(CommonUtils.isNullEmptyOrWhitespace(newReleaseId));
    }

    @Test
    public void testHasChangesInEccFields() throws Exception {
        Release original = handler.getRelease("R1A", user1);
        original.getEccInformation().setEccStatus(ECCStatus.APPROVED).setAssessorDepartment("XYZ").setAssessorContactPerson("asessor@example.com");
        assertFalse(handler.hasChangesInEccFields(original, original));

        Release changedStatus = handler.getRelease("R1A", user1);
        changedStatus.getEccInformation().setEccStatus(ECCStatus.IN_PROGRESS);
        assertTrue(handler.hasChangesInEccFields(changedStatus, original));

        Release changedCrypto = handler.getRelease("R1A", user1);
        changedCrypto.getEccInformation().setContainsCryptography(true);
        assertTrue(handler.hasChangesInEccFields(changedCrypto, original));

        Release changedAl = handler.getRelease("R1A", user1);
        changedAl.getEccInformation().setAl("string value");
        assertTrue(handler.hasChangesInEccFields(changedAl, original));

        Release changed = handler.getRelease("R1A", user1);
        changed.getEccInformation().setEccStatus(ECCStatus.APPROVED).setAssessorDepartment("XYZ").setAssessorContactPerson("");
        assertFalse(handler.hasChangesInEccFields(changed, original));
    }
}
