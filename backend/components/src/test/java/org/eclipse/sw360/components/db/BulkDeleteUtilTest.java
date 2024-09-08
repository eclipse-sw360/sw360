/*
 * Copyright (C) TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.components.db;

import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.BulkDeleteUtil;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BulkDeleteUtilTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;
    private static final String changeLogsDbName = DatabaseSettingsTest.COUCH_DB_CHANGELOGS;

    //User infomation
    private static final String USER_EMAIL1 = "hoge@piyo.co.jp";
    private static final User user1 = new User().setEmail(USER_EMAIL1).setDepartment("Department1").setId("admin12345").setUserGroup(UserGroup.ADMIN);

    private static final String category = "Mobile";

    //Release IDs
    private static final String RELEASE_ID_A1 = "dr_r_a1"; 
    private static final String RELEASE_ID_B1 = "dr_r_b1";
    private static final String RELEASE_ID_C1 = "dr_r_c1";
    private static final String RELEASE_ID_C2 = "dr_r_c2";
    private static final String RELEASE_ID_D1 = "dr_r_d1";
    private static final String RELEASE_ID_E1 = "dr_r_e1";
    private static final String RELEASE_ID_F1 = "dr_r_f1";
    private static final String RELEASE_ID_G1 = "dr_r_g1";
    private static final String RELEASE_ID_H1 = "dr_r_h1";
    
    //Component IDs
    private static final String COMPONENT_ID_A = "dr_c_a"; 
    private static final String COMPONENT_ID_B = "dr_c_b";
    private static final String COMPONENT_ID_C = "dr_c_c";
    private static final String COMPONENT_ID_D = "dr_c_d";
    private static final String COMPONENT_ID_E = "dr_c_e";
    private static final String COMPONENT_ID_F = "dr_c_f";
    private static final String COMPONENT_ID_G = "dr_c_g";
    private static final String COMPONENT_ID_H= "dr_c_h";
    
    //Project IDs
    private static final String PROJECT_ID_A= "dr_p_a";
   
    //Variables for tree data creation
    private int treeNodeCreateReleaseCounter;
    private int treeNodeMaxLink;
    private int treeNodeDepth;

    //Variables for time log
    private DateFormat timeLogDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
    private BufferedWriter timeLogWriter = null;
    private long timeLogLastTime = 0;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Map<String, Vendor> vendors;
    private ComponentDatabaseHandler handler;
    private DatabaseConnector databaseConnector;
    private DatabaseConnector changeLogsDatabaseConnector;
    private BulkDeleteUtil bulkDeleteUtil;

    private int nextReleaseVersion = 0;

    @Mock
    ComponentModerator moderator;
    @Mock
    ReleaseModerator releaseModerator;
    @Mock
    ProjectModerator projectModerator;
    
    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);
        assertTestString(changeLogsDbName);
        assertTestString(attachmentsDbName);

        // Set up vendors
        vendors = new HashMap<>();
        vendors.put("V1", new Vendor().setId("V1").setShortname("Microsoft").setFullname("Microsoft Corporation").setUrl("http://www.microsoft.com"));
        vendors.put("V2", new Vendor().setId("V2").setShortname("Apache").setFullname("The Apache Software Foundation").setUrl("http://www.apache.org"));
        vendors.put("V3", new Vendor().setId("V3").setShortname("Oracle").setFullname("Oracle Corporation Inc").setUrl("http://www.oracle.com"));

        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), changeLogsDbName);

        // Prepare the database
        databaseConnector = new DatabaseConnector(DatabaseSettingsTest.getConfiguredHttpClient(), dbName);
        changeLogsDatabaseConnector = new DatabaseConnector(DatabaseSettingsTest.getConfiguredHttpClient(), changeLogsDbName);
        
        // Prepare vendors
        for (Vendor vendor : vendors.values()) {
            databaseConnector.add(vendor);
        }
        
        // Prepare the handler
        handler = new ComponentDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName, moderator, releaseModerator, projectModerator);
        
        // Prepare the utility object
        bulkDeleteUtil = handler.getBulkDeleteUtil();
        
        treeNodeCreateReleaseCounter = 0;
    }

    @After
    public void tearDown() throws Exception {
        bulkDeleteUtil.unsetInspector();
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), changeLogsDbName);
    }
    
    @Test
    public void testGetAllLinkedReleaseMap() throws Exception {
        if (!isFeatureEnable()) {
            System.out.println("BulkReleaseDeletion is disabled. these test is Skipped.");
            return;
        }
        
        createTestRecords001();
        
        Map<String, Release> outMap = new HashMap<String, Release>();
        bulkDeleteUtil.getAllLinkedReleaseMap(RELEASE_ID_A1, outMap);

        assertEquals(5, outMap.size());
        assertTrue(outMap.containsKey(RELEASE_ID_A1));
        assertTrue(outMap.containsKey(RELEASE_ID_B1));
        assertTrue(outMap.containsKey(RELEASE_ID_C1));
        assertTrue(outMap.containsKey(RELEASE_ID_D1));
        assertTrue(outMap.containsKey(RELEASE_ID_F1));
    }
    
    @Test
    public void testGetExternalLinkMap() throws Exception {

        createTestRecords002();
        
        Map<String, Release> linkedReleaseMap = new HashMap<String, Release>();
        Map<String, Boolean> outExternalFlagMap = new HashMap<String, Boolean>();
        Map<String, List<String>> outReferencingReleaseIdsMap = new HashMap<String, List<String>>();
        
        bulkDeleteUtil.getAllLinkedReleaseMap(RELEASE_ID_A1, linkedReleaseMap);
        assertEquals(7, linkedReleaseMap.size());
        
        //Check ExternalFlagMap
        bulkDeleteUtil.getExternalLinkMap(linkedReleaseMap.keySet(), outExternalFlagMap, outReferencingReleaseIdsMap);
        assertEquals(7, outExternalFlagMap.size());
        assertFalse(outExternalFlagMap.get(RELEASE_ID_A1));
        assertFalse(outExternalFlagMap.get(RELEASE_ID_B1));
        assertTrue(outExternalFlagMap.get(RELEASE_ID_C1));
        assertTrue(outExternalFlagMap.get(RELEASE_ID_D1));
        assertFalse(outExternalFlagMap.get(RELEASE_ID_F1));
        assertTrue(outExternalFlagMap.get(RELEASE_ID_G1));
        assertTrue(outExternalFlagMap.get(RELEASE_ID_H1));
        assertFalse(outExternalFlagMap.containsKey(RELEASE_ID_E1));
        
        //Check ReferencingReleaseIdsMap
        assertEquals(7, outReferencingReleaseIdsMap.size());
        List<String> referencingReleaseIdList = null; 
        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_A1);
        assertEquals(0, referencingReleaseIdList.size());
        
        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_B1);
        assertEquals(1, referencingReleaseIdList.size());
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_A1));
        
        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_C1);
        assertEquals(1, referencingReleaseIdList.size());
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_A1));
        
        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_D1);
        assertEquals(2, referencingReleaseIdList.size());
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_A1));        
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_E1));
        
        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_F1);
        assertEquals(1, referencingReleaseIdList.size());
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_B1));

        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_G1);
        assertEquals(1, referencingReleaseIdList.size());
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_C1));

        referencingReleaseIdList = outReferencingReleaseIdsMap.get(RELEASE_ID_H1);
        assertEquals(1, referencingReleaseIdList.size());
        assertTrue(referencingReleaseIdList.contains(RELEASE_ID_D1));
    }

    @Test
    public void testDeleteBulkRelease001() throws Exception {
        if (!isFeatureEnable()) {
            System.out.println("BulkReleaseDeletion is disabled. these test is Skipped.");
            return;
        }

        createTestRecords001();
        
        bulkDeleteUtil.setInspector(new BulkDeleteUtil.BulkDeleteUtilInspector() {
            
            @Override
            public void checkVariables(Map<String, Release> releaseMap, Map<String, Component> componentMap,
                    Map<String, Boolean> externalLinkMap, Map<String, List<String>> referencingReleaseIdsMap) {}

            @Override
            public void checkLoopState(int loopCount, Map<String, Release> releaseMap,
                    Map<String, Component> componentMap, Map<String, BulkOperationResultState> resultStateMap) {}
            
            @Override
            public void checkLeafReleaseIdsInLoop(int loopCount, Set<String> leafReleaseIds) {
                switch(loopCount) {
                case 0: 
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_F1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 1:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertEquals(2, leafReleaseIds.size());
                    break;            		
                case 2:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_B1));
                    assertEquals(1, leafReleaseIds.size());
                    break;            		
                case 3:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_A1));
                    assertEquals(1, leafReleaseIds.size());
                    break;            		
                }
            }

            @Override
            public void checkUpdatedReferencingReleaseListInLoop(int loopCount, List<Release> updatedReferencingReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    assertEquals(2, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(1, relationMap.size());
                            assertTrue(relationMap.containsKey(RELEASE_ID_B1));
                        } else if (RELEASE_ID_B1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(0, relationMap.size());
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;           		
                case 2:
                    assertEquals(1, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(0, relationMap.size());
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;
                case 3:
                    assertEquals(0, updatedReferencingReleaseList.size());
                    break;            		
                }
            }

            @Override
            public void checkUpdatedComponentListInLoop(int loopCount, List<Component> updatedComponentList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_C.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(1, releaseIds.size());
                            assertTrue(releaseIds.contains(RELEASE_ID_C2));
                        } else if (COMPONENT_ID_D.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(2, updatedComponentList.size());
                    break;            		
                case 2:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_B.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(1, updatedComponentList.size());
                    break;
                case 3:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_A.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(1, updatedComponentList.size());
                }
            }

            @Override
            public void checkDeletedReleaseListInLoop(int loopCount, List<Release> deletedReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_C1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else if (RELEASE_ID_D1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(2, deletedReleaseList.size());
                    break;            		
                case 2:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_B1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(1, deletedReleaseList.size());
                    break;            		
                case 3:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(1, deletedReleaseList.size());
                    break;            		
                }
            }
        });

        assertTrue(releaseExists(RELEASE_ID_A1));
        assertTrue(releaseExists(RELEASE_ID_B1));
        assertTrue(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertTrue(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertTrue(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertTrue(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));
        
        BulkOperationNode level1Component = bulkDeleteUtil.deleteBulkRelease(RELEASE_ID_A1, user1, false);

        assertFalse(releaseExists(RELEASE_ID_A1));
        assertFalse(releaseExists(RELEASE_ID_B1));
        assertFalse(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertFalse(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertFalse(componentExists(COMPONENT_ID_A));
        assertFalse(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertFalse(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));

        //Check BulkOperationNodes 
        //Object[0] : NodeType, Object[1] : ResultState
        Map<String, Object[]> expectedResults = new HashMap<String, Object[]>();
        expectedResults.put(RELEASE_ID_A1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_B1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_C1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_D1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_F1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_A, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(COMPONENT_ID_B, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(COMPONENT_ID_C, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_D, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(COMPONENT_ID_F, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        checkBulkOperationNode(level1Component, expectedResults);

    }
    
    private void checkBulkOperationNode(BulkOperationNode node, Map<String, Object[]> expectedResults ) {
        String message = String.format("checkBulkOperationNode node id=%s", node.id);
        Object [] expectedResult = expectedResults.get(node.getId());
        assertEquals(message, expectedResult[0],  node.getType());
        assertEquals(message, expectedResult[1], node.getState());
        for (BulkOperationNode childNode :  node.getChildList()) {
            checkBulkOperationNode(childNode, expectedResults);
        }
    }

    //@Test
    public void testDeleteBulkRelease002() throws Exception {
        if (!isFeatureEnable()) {
            System.out.println("BulkReleaseDeletion is disabled. these test is Skipped.");
            return;
        }
        
        startTimeLog("testDeleteBulkRelease002.log");
        try {
            addTimeLog("test data creation start.");
            
            List<String> releaseIdList = new ArrayList<String>();
            List<String> componentIdList = new ArrayList<String>();
            
            createTestRecords002(2, 2, releaseIdList, componentIdList);
            String rootReleaseId = releaseIdList.get(0);
            assertEquals(7, releaseIdList.size());
            assertEquals(4, componentIdList.size());
            
            List<String> otherReleaseIdList = new ArrayList<String>();
            List<String> otherComponentIdList = new ArrayList<String>();
            for (int i=0; i<10; i++) {
                createTestRecords002(2, 2, otherReleaseIdList, otherComponentIdList);            
            }
            assertEquals(70, otherReleaseIdList.size());
            assertEquals(40, otherComponentIdList.size());
            
            addTimeLog("test data creation end.");
            addTimeLog("deleteBulkRelease start.");
            
            BulkOperationNode level1Component = bulkDeleteUtil.deleteBulkRelease(rootReleaseId, user1, false);
            assertNotNull(level1Component);
            
            addTimeLog("deleteBulkRelease end.");
            
            addTimeLog("check records start.");
            
            //Records to be deleted
            for (String releaseId : releaseIdList) {
                assertFalse(this.releaseExists(releaseId));
            }
            for (String componentId : componentIdList) {
                assertFalse(this.componentExists(componentId));
            }
            //Records to be undeleted
            for (String releaseId : otherReleaseIdList) {
                assertTrue(this.releaseExists(releaseId));
            }
            for (String componentId : otherComponentIdList) {
                assertTrue(this.componentExists(componentId));
            }
            
            addTimeLog("check records end.");
            
        } finally {
            stopTimeLog();
        }
    }
    
    @Test
    public void testDeleteBulkRelease_ConflictError001() throws Exception {
        if (!isFeatureEnable()) {
            System.out.println("BulkReleaseDeletion is disabled. these test is Skipped.");
            return;
        }
        
        createTestRecords001();
        
        bulkDeleteUtil.setInspector(new BulkDeleteUtil.BulkDeleteUtilInspector() {
            
            @Override
            public void checkVariables(Map<String, Release> releaseMap, Map<String, Component> componentMap,
                    Map<String, Boolean> externalLinkMap, Map<String, List<String>> referencingReleaseIdsMap) {}

            @Override
            public void checkLoopState(int loopCount, Map<String, Release> releaseMap,
                    Map<String, Component> componentMap, Map<String, BulkOperationResultState> resultStateMap) {}
            
            @Override
            public void checkLeafReleaseIdsInLoop(int loopCount, Set<String> leafReleaseIds) {
                switch(loopCount) {
                case 0: 
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_F1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 1:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertEquals(2, leafReleaseIds.size());
                    break;            		
                case 2:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_B1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertEquals(2, leafReleaseIds.size());
                    break;            		
                case 3:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertEquals(1, leafReleaseIds.size());
                    break;            		
                }
            }

            @Override
            public void checkUpdatedReferencingReleaseListInLoop(int loopCount, List<Release> updatedReferencingReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    assertEquals(2, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(1, relationMap.size());
                            assertTrue(relationMap.containsKey(RELEASE_ID_B1));
                        } else if (RELEASE_ID_B1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(0, relationMap.size());
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;           		
                case 2:
                    assertEquals(1, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(1, relationMap.size());
                            assertTrue(relationMap.containsKey(RELEASE_ID_C1));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;
                case 3:
                    fail("unexpected call");
                    break;            		
                }
            }

            @Override
            public void checkUpdatedComponentListInLoop(int loopCount, List<Component> updatedComponentList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_C.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(1, releaseIds.size());
                            assertTrue(releaseIds.contains(RELEASE_ID_C2));
                        } else if (COMPONENT_ID_D.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(2, updatedComponentList.size());
                    break;            		
                case 2:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_B.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(1, updatedComponentList.size());
                    break;
                case 3:
                    fail("unexpected call");
                    break;
                }
            }

            @Override
            public void checkDeletedReleaseListInLoop(int loopCount, List<Release> deletedReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_C1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                            
                            //couse RELEASE_ID_C1 confliction
                            Release conflectedRelease = release.deepCopy();
                            conflectedRelease.addToLanguages("java");
                            databaseConnector.update(conflectedRelease);
                            
                        } else if (RELEASE_ID_D1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(2, deletedReleaseList.size());
                    break;            		
                case 2:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_B1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(1, deletedReleaseList.size());
                    break;            		
                case 3:
                    fail("unexpected call");
                    break;            		
                }
            }
        });

        assertTrue(releaseExists(RELEASE_ID_A1));
        assertTrue(releaseExists(RELEASE_ID_B1));
        assertTrue(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertTrue(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertTrue(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertTrue(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));
        
        BulkOperationNode level1Component = bulkDeleteUtil.deleteBulkRelease(RELEASE_ID_A1, user1, false);

        assertTrue(releaseExists(RELEASE_ID_A1)); //Remained
        assertFalse(releaseExists(RELEASE_ID_B1));
        assertTrue(releaseExists(RELEASE_ID_C1)); //Conflicted
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertFalse(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertFalse(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertFalse(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));

        //Check BulkOperationNodes 
        //Object[0] : NodeType, Object[1] : ResultState
        Map<String, Object[]> expectedResults = new HashMap<String, Object[]>();
        expectedResults.put(RELEASE_ID_A1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_B1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_C1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.CONFLICTED});
        expectedResults.put(RELEASE_ID_D1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_F1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_A, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_B, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(COMPONENT_ID_C, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_D, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(COMPONENT_ID_F, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        checkBulkOperationNode(level1Component, expectedResults);
        
        //check links of remained releases
        Release a1= databaseConnector.get(Release.class, RELEASE_ID_A1);
        Map<String, ReleaseRelationship> relationShip_a1 = a1.getReleaseIdToRelationship();
        assertTrue(relationShip_a1.containsKey(RELEASE_ID_C1));
        assertEquals(1, relationShip_a1.size());
        
        Release c1= databaseConnector.get(Release.class, RELEASE_ID_C1);
        Map<String, ReleaseRelationship> relationShip_c1 = c1.getReleaseIdToRelationship();
        assertTrue(CommonUtils.isNullOrEmptyMap(relationShip_c1));
    }

    @Test
    public void testDeleteBulkRelease_ConflictError002() throws Exception {
        if (!isFeatureEnable()) {
            System.out.println("BulkReleaseDeletion is disabled. these test is Skipped.");
            return;
        }
        
        createTestRecords001();
        
        bulkDeleteUtil.setInspector(new BulkDeleteUtil.BulkDeleteUtilInspector() {
            
            @Override
            public void checkVariables(Map<String, Release> releaseMap, Map<String, Component> componentMap,
                    Map<String, Boolean> externalLinkMap, Map<String, List<String>> referencingReleaseIdsMap) {}

            @Override
            public void checkLoopState(int loopCount, Map<String, Release> releaseMap,
                    Map<String, Component> componentMap, Map<String, BulkOperationResultState> resultStateMap) {}
            
            @Override
            public void checkLeafReleaseIdsInLoop(int loopCount, Set<String> leafReleaseIds) {
                switch(loopCount) {
                case 0: 
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_F1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 1:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 2:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertEquals(1, leafReleaseIds.size());
                    break;
                default:
                    fail("unexpected call");
                    break;            		
                }
            }

            @Override
            public void checkUpdatedReferencingReleaseListInLoop(int loopCount, List<Release> updatedReferencingReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    assertEquals(2, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(1, relationMap.size());
                            assertTrue(relationMap.containsKey(RELEASE_ID_B1));
                        } else if (RELEASE_ID_B1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(0, relationMap.size());
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;           		
                default:
                    fail("unexpected call");
                    break;            		
                }
            }

            @Override
            public void checkUpdatedComponentListInLoop(int loopCount, List<Component> updatedComponentList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_C.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(1, releaseIds.size());
                            assertTrue(releaseIds.contains(RELEASE_ID_C2));
                        } else if (COMPONENT_ID_D.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                            
                            //couse COMPONENT_ID_D confliction
                            Component conflectedComponent = component.deepCopy();
                            conflectedComponent.setDescription("new component");
                            databaseConnector.update(conflectedComponent);
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(2, updatedComponentList.size());
                    break;            		
                default:
                    fail("unexpected call");
                    break;
                }
            }

            @Override
            public void checkDeletedReleaseListInLoop(int loopCount, List<Release> deletedReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_C1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(1, deletedReleaseList.size());
                    break;            		
                default:
                    fail("unexpected call");
                    break;            		
                }
            }
        });

        assertTrue(releaseExists(RELEASE_ID_A1));
        assertTrue(releaseExists(RELEASE_ID_B1));
        assertTrue(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertTrue(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertTrue(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertTrue(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));
        
        BulkOperationNode level1Component = bulkDeleteUtil.deleteBulkRelease(RELEASE_ID_A1, user1, false);

        assertTrue(releaseExists(RELEASE_ID_A1));
        assertTrue(releaseExists(RELEASE_ID_B1));
        assertFalse(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertTrue(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertTrue(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertTrue(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));

        //Check BulkOperationNodes 
        //Object[0] : NodeType, Object[1] : ResultState
        Map<String, Object[]> expectedResults = new HashMap<String, Object[]>();
        expectedResults.put(RELEASE_ID_A1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_B1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_C1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_D1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_F1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_A, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_B, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_C, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_D, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_F, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        checkBulkOperationNode(level1Component, expectedResults);
        
        //check links of remained releases
        Release a1= databaseConnector.get(Release.class, RELEASE_ID_A1);
        Map<String, ReleaseRelationship> relationShip_a1 = a1.getReleaseIdToRelationship();
        assertTrue(relationShip_a1.containsKey(RELEASE_ID_B1));
        assertEquals(1, relationShip_a1.size());

        Release b1= databaseConnector.get(Release.class, RELEASE_ID_B1);
        Map<String, ReleaseRelationship> relationShip_b1 = b1.getReleaseIdToRelationship();
        assertTrue(relationShip_b1.containsKey(RELEASE_ID_D1));
        assertEquals(1, relationShip_b1.size());
        
        Release d1= databaseConnector.get(Release.class, RELEASE_ID_D1);
        Map<String, ReleaseRelationship> relationShip_d1 = d1.getReleaseIdToRelationship();
        assertTrue(CommonUtils.isNullOrEmptyMap(relationShip_d1));
    }

    @Test
    public void testDeleteBulkRelease_ConflictError003() throws Exception {
        if (!isFeatureEnable()) {
            System.out.println("BulkReleaseDeletion is disabled. these test is Skipped.");
            return;
        }
        
        createTestRecords001();
        
        bulkDeleteUtil.setInspector(new BulkDeleteUtil.BulkDeleteUtilInspector() {
            
            @Override
            public void checkVariables(Map<String, Release> releaseMap, Map<String, Component> componentMap,
                    Map<String, Boolean> externalLinkMap, Map<String, List<String>> referencingReleaseIdsMap) {}

            @Override
            public void checkLoopState(int loopCount, Map<String, Release> releaseMap,
                    Map<String, Component> componentMap, Map<String, BulkOperationResultState> resultStateMap) {}
            
            @Override
            public void checkLeafReleaseIdsInLoop(int loopCount, Set<String> leafReleaseIds) {
                switch(loopCount) {
                case 0: 
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_F1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 1:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_D1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 2:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_B1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                case 3:
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_B1));
                    assertTrue(leafReleaseIds.contains(RELEASE_ID_C1));
                    assertEquals(2, leafReleaseIds.size());
                    break;
                default:
                    fail("unexpected call");
                    break;            		
                }
            }

            @Override
            public void checkUpdatedReferencingReleaseListInLoop(int loopCount, List<Release> updatedReferencingReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    assertEquals(2, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(1, relationMap.size());
                            assertTrue(relationMap.containsKey(RELEASE_ID_B1));
                            
                            //couse RELEASE_ID_A1 confliction
                            Release conflectedRelease = databaseConnector.get(Release.class, RELEASE_ID_A1);
                            conflectedRelease.addToLanguages("java");
                            databaseConnector.update(conflectedRelease);
                            
                        } else if (RELEASE_ID_B1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(0, relationMap.size());
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;           		
                case 2:
                    assertEquals(1, updatedReferencingReleaseList.size());
                    for (Release release : updatedReferencingReleaseList) {
                        if (RELEASE_ID_A1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertEquals(1, relationMap.size());
                            assertTrue(relationMap.containsKey(RELEASE_ID_C1));
                            
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    break;
                default:
                    fail("unexpected call");
                    break;            		
                }
            }

            @Override
            public void checkUpdatedComponentListInLoop(int loopCount, List<Component> updatedComponentList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Component component : updatedComponentList) {
                        if (COMPONENT_ID_D.equals(component.getId())) {
                            Set<String> releaseIds = component.getReleaseIds();
                            assertEquals(0, releaseIds.size());
                        } else {
                            fail(String.format("Unexpected component id=%s", component.getId()));
                        }
                    }
                    assertEquals(1, updatedComponentList.size());
                    break;            		
                case 2:
                    assertEquals(0, updatedComponentList.size());
                    break;          
                default:
                    fail("unexpected call");
                    break;
                }
            }

            @Override
            public void checkDeletedReleaseListInLoop(int loopCount, List<Release> deletedReleaseList) {
                switch(loopCount) {
                case 0:
                    //nothing to do
                    break;
                case 1:
                    for (Release release : deletedReleaseList) {
                        if (RELEASE_ID_D1.equals(release.getId())) {
                            Map<String, ReleaseRelationship> relationMap = release.getReleaseIdToRelationship();
                            assertTrue(CommonUtils.isNullOrEmptyMap(relationMap));
                        } else {
                            fail(String.format("Unexpected release id=%s", release.getId()));
                        }
                    }
                    assertEquals(1, deletedReleaseList.size());
                    break;
                case 2:
                    assertEquals(0, deletedReleaseList.size());
                    break;
                default:
                    fail("unexpected call");
                    break;            		
                }
            }
        });

        assertTrue(releaseExists(RELEASE_ID_A1));
        assertTrue(releaseExists(RELEASE_ID_B1));
        assertTrue(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertTrue(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertTrue(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertTrue(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));
        
        BulkOperationNode level1Component = bulkDeleteUtil.deleteBulkRelease(RELEASE_ID_A1, user1, false);

        assertTrue(releaseExists(RELEASE_ID_A1));
        assertTrue(releaseExists(RELEASE_ID_B1));
        assertTrue(releaseExists(RELEASE_ID_C1));
        assertTrue(releaseExists(RELEASE_ID_C2));
        assertFalse(releaseExists(RELEASE_ID_D1));
        assertTrue(releaseExists(RELEASE_ID_E1));
        assertTrue(releaseExists(RELEASE_ID_F1));

        assertTrue(componentExists(COMPONENT_ID_A));
        assertTrue(componentExists(COMPONENT_ID_B));
        assertTrue(componentExists(COMPONENT_ID_C));
        assertFalse(componentExists(COMPONENT_ID_D));
        assertTrue(componentExists(COMPONENT_ID_E));
        assertTrue(componentExists(COMPONENT_ID_F));

        //Check BulkOperationNodes 
        //Object[0] : NodeType, Object[1] : ResultState
        Map<String, Object[]> expectedResults = new HashMap<String, Object[]>();
        expectedResults.put(RELEASE_ID_A1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_B1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_C1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.FAILED});
        expectedResults.put(RELEASE_ID_D1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(RELEASE_ID_F1, new Object[]{BulkOperationNodeType.RELEASE, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_A, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_B, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_C, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        expectedResults.put(COMPONENT_ID_D, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.SUCCEEDED});
        expectedResults.put(COMPONENT_ID_F, new Object[]{BulkOperationNodeType.COMPONENT, BulkOperationResultState.EXCLUDED});
        checkBulkOperationNode(level1Component, expectedResults);
        
        //check links of remained releases
        Release a1= databaseConnector.get(Release.class, RELEASE_ID_A1);
        Map<String, ReleaseRelationship> relationShip_a1 = a1.getReleaseIdToRelationship();
        assertTrue(relationShip_a1.containsKey(RELEASE_ID_B1));
        assertTrue(relationShip_a1.containsKey(RELEASE_ID_C1));
        assertEquals(2, relationShip_a1.size());
        
        Release b1= databaseConnector.get(Release.class, RELEASE_ID_B1);
        Map<String, ReleaseRelationship> relationShip_b1 = b1.getReleaseIdToRelationship();
        assertTrue(CommonUtils.isNullOrEmptyMap(relationShip_b1));
        
        Release c1= databaseConnector.get(Release.class, RELEASE_ID_C1);
        Map<String, ReleaseRelationship> relationShip_c1 = c1.getReleaseIdToRelationship();
        assertTrue(CommonUtils.isNullOrEmptyMap(relationShip_c1));
    }
    
    private void createTestRecords001() {
        
        List<Component> components = new ArrayList<Component>();
        Component component_dr_A = new Component().setId(COMPONENT_ID_A).setName("DR_A").setDescription("DR Component A").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_A.addToReleaseIds(RELEASE_ID_A1);
        component_dr_A.addToCategories(category);
        components.add(component_dr_A);
        
        Component component_dr_B = new Component().setId(COMPONENT_ID_B).setName("DR_B").setDescription("DR Component B").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_B.addToReleaseIds(RELEASE_ID_B1);
        component_dr_B.addToCategories(category);
        components.add(component_dr_B);
 
        Component component_dr_C = new Component().setId(COMPONENT_ID_C).setName("DR_C").setDescription("DR Component C").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_C.addToReleaseIds(RELEASE_ID_C1);
        component_dr_C.addToReleaseIds(RELEASE_ID_C2);
        component_dr_C.addToCategories(category);
        components.add(component_dr_C);
 
        Component component_dr_D = new Component().setId(COMPONENT_ID_D).setName("DR_D").setDescription("DR Component D").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_D.addToReleaseIds(RELEASE_ID_D1);
        component_dr_D.addToCategories(category);
        components.add(component_dr_D);
 
        Component component_dr_E = new Component().setId(COMPONENT_ID_E).setName("DR_E").setDescription("DR Component E").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_E.addToReleaseIds(RELEASE_ID_E1);
        component_dr_E.addToCategories(category);
        components.add(component_dr_E);
 
        Component component_dr_F = new Component().setId(COMPONENT_ID_F).setName("DR_F").setDescription("DR Component F").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_F.addToReleaseIds(RELEASE_ID_F1);
        component_dr_F.addToCategories(category);
        components.add(component_dr_F);
 
        List<Release> releases = new ArrayList<Release>();
        Release release_dr_A1 = new Release().setId(RELEASE_ID_A1).setComponentId(component_dr_A.getId()).setName(component_dr_A.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_A1.putToReleaseIdToRelationship(RELEASE_ID_B1, ReleaseRelationship.CONTAINED);
        release_dr_A1.putToReleaseIdToRelationship(RELEASE_ID_C1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_A1);
        
        Release release_dr_B1 = new Release().setId(RELEASE_ID_B1).setComponentId(component_dr_B.getId()).setName(component_dr_B.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_B1.putToReleaseIdToRelationship(RELEASE_ID_D1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_B1);
 
        Release release_dr_C1 = new Release().setId(RELEASE_ID_C1).setComponentId(component_dr_C.getId()).setName(component_dr_C.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_C1.putToReleaseIdToRelationship(RELEASE_ID_F1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_C1);

        Release release_dr_C2 = new Release().setId(RELEASE_ID_C2).setComponentId(component_dr_C.getId()).setName(component_dr_C.getName()).setVersion("2.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        releases.add(release_dr_C2);
        
        Release release_dr_D1 = new Release().setId(RELEASE_ID_D1).setComponentId(component_dr_D.getId()).setName(component_dr_D.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        releases.add(release_dr_D1);
 
        Release release_dr_E1 = new Release().setId(RELEASE_ID_E1).setComponentId(component_dr_E.getId()).setName(component_dr_E.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_E1.putToReleaseIdToRelationship(RELEASE_ID_F1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_E1);
 
        Release release_dr_F1 = new Release().setId(RELEASE_ID_F1).setComponentId(component_dr_F.getId()).setName(component_dr_F.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        releases.add(release_dr_F1);
        
        for (Component component : components) {
            databaseConnector.add(component);
        }
        for (Release release : releases) {
            databaseConnector.add(release);
        }
    }
    

    private void createTestRecords002() {
        
        List<Component> components = new ArrayList<Component>();
        Component component_dr_A = new Component().setId(COMPONENT_ID_A).setName("DR_A").setDescription("DR Component A").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_A.addToReleaseIds(RELEASE_ID_A1);
        component_dr_A.addToCategories(category);
        components.add(component_dr_A);
        
        Component component_dr_B = new Component().setId(COMPONENT_ID_B).setName("DR_B").setDescription("DR Component B").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_B.addToReleaseIds(RELEASE_ID_B1);
        component_dr_B.addToCategories(category);
        components.add(component_dr_B);
 
        Component component_dr_C = new Component().setId(COMPONENT_ID_C).setName("DR_C").setDescription("DR Component C").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_C.addToReleaseIds(RELEASE_ID_C1);
        component_dr_C.addToCategories(category);
        components.add(component_dr_C);
 
        Component component_dr_D = new Component().setId(COMPONENT_ID_D).setName("DR_D").setDescription("DR Component D").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_D.addToReleaseIds(RELEASE_ID_D1);
        component_dr_D.addToCategories(category);
        components.add(component_dr_D);
 
        Component component_dr_E = new Component().setId(COMPONENT_ID_E).setName("DR_E").setDescription("DR Component E").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_E.addToReleaseIds(RELEASE_ID_E1);
        component_dr_E.addToCategories(category);
        components.add(component_dr_E);
 
        Component component_dr_F = new Component().setId(COMPONENT_ID_F).setName("DR_F").setDescription("DR Component F").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_F.addToReleaseIds(RELEASE_ID_F1);
        component_dr_F.addToCategories(category);
        components.add(component_dr_F);

        Component component_dr_G = new Component().setId(COMPONENT_ID_G).setName("DR_G").setDescription("DR Component G").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_G.addToReleaseIds(RELEASE_ID_G1);
        component_dr_G.addToCategories(category);
        components.add(component_dr_G);

        Component component_dr_H = new Component().setId(COMPONENT_ID_H).setName("DR_H").setDescription("DR Component H").setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component_dr_H.addToReleaseIds(RELEASE_ID_H1);
        component_dr_H.addToCategories(category);
        components.add(component_dr_H);

        
        List<Release> releases = new ArrayList<Release>();
        Release release_dr_A1 = new Release().setId(RELEASE_ID_A1).setComponentId(component_dr_A.getId()).setName(component_dr_A.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_A1.putToReleaseIdToRelationship(RELEASE_ID_B1, ReleaseRelationship.CONTAINED);
        release_dr_A1.putToReleaseIdToRelationship(RELEASE_ID_C1, ReleaseRelationship.CONTAINED);
        release_dr_A1.putToReleaseIdToRelationship(RELEASE_ID_D1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_A1);
        
        Release release_dr_B1 = new Release().setId(RELEASE_ID_B1).setComponentId(component_dr_B.getId()).setName(component_dr_B.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_B1.putToReleaseIdToRelationship(RELEASE_ID_F1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_B1);
 
        Release release_dr_C1 = new Release().setId(RELEASE_ID_C1).setComponentId(component_dr_C.getId()).setName(component_dr_C.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_C1.putToReleaseIdToRelationship(RELEASE_ID_G1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_C1);

        Release release_dr_D1 = new Release().setId(RELEASE_ID_D1).setComponentId(component_dr_D.getId()).setName(component_dr_D.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_D1.putToReleaseIdToRelationship(RELEASE_ID_H1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_D1);
 
        Release release_dr_E1 = new Release().setId(RELEASE_ID_E1).setComponentId(component_dr_E.getId()).setName(component_dr_E.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        release_dr_E1.putToReleaseIdToRelationship(RELEASE_ID_D1, ReleaseRelationship.CONTAINED);
        releases.add(release_dr_E1);
 
        Release release_dr_F1 = new Release().setId(RELEASE_ID_F1).setComponentId(component_dr_F.getId()).setName(component_dr_F.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        releases.add(release_dr_F1);
        
        Release release_dr_G1 = new Release().setId(RELEASE_ID_G1).setComponentId(component_dr_G.getId()).setName(component_dr_G.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        releases.add(release_dr_G1);

        Release release_dr_H1 = new Release().setId(RELEASE_ID_H1).setComponentId(component_dr_H.getId()).setName(component_dr_H.getName()).setVersion("1.00").setCreatedBy(USER_EMAIL1).setVendorId("V1");
        releases.add(release_dr_H1);
        
        List<Project>projects = new ArrayList<Project>();
        Project project_A = new Project().setId(PROJECT_ID_A).setName("PROJECT_A").setVisbility(Visibility.EVERYONE);
        project_A.putToReleaseIdToUsage(RELEASE_ID_C1, new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        projects.add(project_A);
        
        for (Component component : components) {
            databaseConnector.add(component);
        }
        for (Release release : releases) {
            databaseConnector.add(release);
        }
        for (Project project : projects) {
            databaseConnector.add(project);
        }
    }

    private void createTestRecords002(int maxLink, int depth, List<String> releaseIdList, List<String> componentIdList) {
        //create root node
        String componentId = String.format("dr_%08x", treeNodeCreateReleaseCounter);
        treeNodeCreateReleaseCounter++;
        String version = String.format("%04x", 0);
        String releaseId = String.format("%s_%s", componentId, version);

        Component rootComponent = new Component().setId(componentId).setName(componentId).setDescription(componentId).setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        rootComponent.addToReleaseIds(releaseId);
        rootComponent.addToCategories(category);
        databaseConnector.add(rootComponent);
        componentIdList.add(componentId);
        
        Release rootRelease = new Release().setId(releaseId).setComponentId(componentId).setName(releaseId).setVersion(version).setCreatedBy(USER_EMAIL1).setVendorId("V1");
        databaseConnector.add(rootRelease);
        releaseIdList.add(releaseId);
        
        //create tree nodes
        treeNodeMaxLink = maxLink;
        treeNodeDepth = depth;
        createReleaseTree(releaseId, 0, releaseIdList, componentIdList);
    }
    
    private void createReleaseTree(String parentId, int level, List<String> outReleaseIdList, List< String> outComponentIdList) {
        //create a compoent
        String componentId = String.format("dr_%08x", treeNodeCreateReleaseCounter);
        treeNodeCreateReleaseCounter++;
        Component component = new Component().setId(componentId).setName(componentId).setDescription(componentId).setCreatedBy(USER_EMAIL1).setMainLicenseIds(new HashSet<>(Arrays.asList("lic1"))).setCreatedOn("2022-07-20");
        component.addToCategories(category);
        databaseConnector.add(component);
        assertFalse(outComponentIdList.contains(componentId));
        outComponentIdList.add(componentId);
        
        //create releases
        for (int i = 0; i < treeNodeMaxLink; i++) {
            //add release
            String version = String.format("%04x", i);
            String releaseId = String.format("%s_%s", componentId, version);
            Release release = new Release().setId(releaseId).setComponentId(componentId).setName(releaseId).setVersion(version).setCreatedBy(USER_EMAIL1).setVendorId("V1");
            databaseConnector.add(release);
            assertFalse(outReleaseIdList.contains(releaseId));
            outReleaseIdList.add(releaseId);
 
            //update compoennt
            Component updatedComponent = databaseConnector.get(Component.class, componentId);
            updatedComponent.addToReleaseIds(releaseId);
            databaseConnector.update(updatedComponent);
 
            //update parent release
            if (parentId != null) {
                Release parentRelease = databaseConnector.get(Release.class, parentId);
                assertNotNull(parentRelease);
                parentRelease.putToReleaseIdToRelationship(releaseId, ReleaseRelationship.CONTAINED);
                databaseConnector.update(parentRelease);
            }
            
            //create child releases
            if (level + 1 < treeNodeDepth) {
                createReleaseTree(releaseId, level + 1, outReleaseIdList, outComponentIdList);
            }
        }
    }

    private boolean releaseExists(String releaseId) {
        Release release = databaseConnector.get(Release.class, releaseId);
        return release != null;
    }

    private boolean componentExists(String componentId) throws SW360Exception {
        Component component = databaseConnector.get(Component.class, componentId);
        return component != null;
    }
    
    private boolean isFeatureEnable() {
        if (!BackendUtils.IS_BULK_RELEASE_DELETING_ENABLED) {
            return false;
        }
        if (!PermissionUtils.IS_ADMIN_PRIVATE_ACCESS_ENABLED) {
            return false;
        }
        return true;
    }
    
    public boolean startTimeLog(String filePath) {
        try {
            timeLogWriter = new BufferedWriter(new FileWriter(filePath));
            timeLogWriter.write("\"Type\",\"Message\",\"Time\",\"Interval[s]\"");
            timeLogWriter.newLine();
            timeLogWriter.flush();
            timeLogLastTime = System.nanoTime();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean stopTimeLog() {
        try {
            if (timeLogWriter != null){
                timeLogWriter.close();
            }
            timeLogLastTime = 0;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addTimeLog(String message) {
        try {
            if(timeLogWriter != null) {
                long currentTime = System.nanoTime();
                double interval = (double)(currentTime - timeLogLastTime) / 1000000000;
                timeLogLastTime = currentTime;
                String dateStr = timeLogDateFormat.format(new Date());
                timeLogWriter.write(String.format("\"[TIME]\",\"%s\",\"%s\",\"%.3f\"", message, dateStr, interval));
                timeLogWriter.newLine();
                timeLogWriter.flush();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
