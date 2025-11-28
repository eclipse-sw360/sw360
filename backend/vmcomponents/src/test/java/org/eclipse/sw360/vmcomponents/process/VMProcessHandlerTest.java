/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.process;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.eclipse.sw360.vmcomponents.AbstractJSONMockTest;

import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.vmcomponents.common.VMResult;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.handler.SVMSyncHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * @author stefan.jaeger@evosoft.com
 */
@RunWith(MockitoJUnitRunner.class)
public class VMProcessHandlerTest extends AbstractJSONMockTest {
    private static final Logger log = getLogger(VMProcessHandlerTest.class);

    private final String URL_ACTIONS = "http://localhost:8090/portal/api/v1/public/actions";
    private final String URL_COMPONENTS = "http://localhost:8090/portal/api/v1/public/components";
    private final String URL_PRIORITIES = "http://localhost:8090/portal/api/v1/public/priorities";
    private final String URL_VULNERABILITIES = "http://localhost:8090/portal/api/v1/public/notifications";
    private final String actionId = "5";
    private final String actionText = "Install New Package";
    private final String componentId = "70";
    private final String componentName = "Tomcat";
    private final List<String> componentIds;
    private final String priorityId = "2";
    private final String priorityShort = "major";
    private final String vulnerabilityId = "22955";
    private final String vulnerabilityTitle = "Red Hat RHEL 5, 6 - java-1.6.0-openjdk Multiple Vulnerabilities - RHSA-2014:0097-1";

    private long timeout = 2000;

    @Autowired
    private VMDatabaseHandler handler;

    public VMProcessHandlerTest() {
        componentIds = new ArrayList<>();
        componentIds.addAll(Arrays.asList("1,2,3,5,6,7,10,70,20172,20173".split(",")));
    }

    @Before
    public void setUp() throws TException, IOException {
        assumeCanConnectTo(URL_ACTIONS);

        // mock preparation
        staticJSONResponse("/portal/api/v1/public/actions/5", "{\"text\": \"Install New Package\"}");
        staticJSONResponse("/portal/api/v1/public/components/70", new File("src/test/resources/components_70.json"));
        staticJSONResponse("/portal/api/v1/public/priorities/2", new File("src/test/resources/priorities_2.json"));
        staticJSONResponse("/portal/api/v1/public/notifications/22955", new File("src/test/resources/notifications_22955.json"));
    }

    @After
    public void tearDown() throws Exception {
//        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testGetMasterDataAction() throws Exception {
        handler.add(new VMAction(actionId));

        VMAction action = handler.getAll(VMAction.class).get(0);
        assertNotNull(action);
        assertNotNull(action.getId());
        assertEquals(actionId, action.getVmid());
        assertNull(action.getText());

        List<String> ids = Arrays.asList(action.getId());
        VMProcessHandler.getMasterData(VMAction.class, ids, URL_ACTIONS, false);

        Thread.sleep(timeout);

        action = handler.getAll(VMAction.class).get(0);
        assertNotNull(action);
        assertNotNull(action.getId());
        assertEquals(actionId, action.getVmid());
        assertEquals(actionText, action.getText());
    }

//    @Test
    public void testGetMasterDataComponent() throws Exception {
        handler.add(new VMComponent(SW360Utils.getCreatedOnTime(), componentId));

        VMComponent component = handler.getAll(VMComponent.class).get(0);
        assertNotNull(component);
        assertNotNull(component.getId());
        assertEquals(componentId, component.getVmid());
        assertNull(component.getName());

        VMProcessHandler.getMasterData(VMComponent.class, component.getId(), URL_COMPONENTS, false);

        Thread.sleep(timeout);

        component = handler.getAll(VMComponent.class).get(0);
        assertNotNull(component);
        assertNotNull(component.getId());
        assertEquals(componentId, component.getVmid());
        assertEquals(componentName, component.getName());
    }

    @Test
    public void testGetMasterDataPriority() throws Exception {
        handler.add(new VMPriority(priorityId));

        VMPriority prio = handler.getAll(VMPriority.class).get(0);
        assertNotNull(prio);
        assertNotNull(prio.getId());
        assertEquals(priorityId, prio.getVmid());
        assertNull(prio.getShortText());
        log.debug(prio.toString());

        List<String> ids = Arrays.asList(prio.getId());
        VMProcessHandler.getMasterData(VMPriority.class, ids, URL_PRIORITIES, false);

        Thread.sleep(timeout);

        prio = handler.getAll(VMPriority.class).get(0);
        assertNotNull(prio);
        assertNotNull(prio.getId());
        assertEquals(priorityId, prio.getVmid());
        assertEquals(priorityShort, prio.getShortText());
    }

    @Test
    public void testGetMasterDataVulnerability() throws Exception {
        handler.add(new Vulnerability(vulnerabilityId));

        Vulnerability vul = handler.getAll(Vulnerability.class).get(0);
        assertNotNull(vul);
        assertNotNull(vul.getId());
        assertEquals(vulnerabilityId, vul.getExternalId());
        assertNull(vul.getTitle());
        log.debug(vul.toString());

        List<String> ids = Arrays.asList(vul.getId());
        VMProcessHandler.getMasterData(Vulnerability.class, ids, URL_VULNERABILITIES, false);

        Thread.sleep(timeout);

        vul = handler.getAll(Vulnerability.class).get(0);
        assertNotNull(vul);
        assertNotNull(vul.getId());
        assertEquals(vulnerabilityId, vul.getExternalId());
        assertEquals(vulnerabilityTitle, vul.getTitle());
    }

    @Test
    public void testGetMasterDataComponents() throws Exception {
        assertEquals(0, handler.getAll(VMComponent.class).size());

        for (String vmid:componentIds) {
            handler.add(new VMComponent(SW360Utils.getCreatedOnTime(), vmid));
        }
        List<VMComponent> components = handler.getAll(VMComponent.class);
        assertEquals(componentIds.size(), components.size());
        List<String> cIds = new ArrayList<>(components.size());
        for (VMComponent comp:components) {
            cIds.add(comp.getId());
        }

        VMProcessHandler.getMasterData(VMComponent.class, cIds, URL_COMPONENTS, false);

        Thread.sleep(timeout*2);

        components = handler.getAll(VMComponent.class);
        assertEquals(componentIds.size(), components.size());
        for (VMComponent comp:components) {
            assertNotNull(comp.getId());
            if(comp.getVmid().equals(componentId)){
                assertNotNull(comp.getName());
                assertEquals(componentName, comp.getName());
            } else {
                assertNull(comp.getName());
            }
        }
    }


    @Test
    public void testGetMasterData100Components() throws Exception {
        componentIds.clear();
        timeout = timeout * 2;
        String comp100 = "";
        for (int i = 0; i < 100; i++) {
            comp100 += i + ",";
        }
        comp100.substring(0,comp100.length());
        componentIds.addAll(Arrays.asList(comp100.split(",")));

        testGetMasterDataComponents();
    }


//    @Test
    public void testSVM() throws Exception {
        String urlSVMComponents = "http://localhost:8090/portal/api/v1/public/components";
        String urlSVMActions = "http://localhost:8090/portal/api/v1/public/actions";
        String urlSVMPriorities = "http://localhost:8090/portal/api/v1/public/priorities";

        testSVMperType(urlSVMActions, VMAction.class);
        testSVMperType(urlSVMPriorities, VMPriority.class);
        testSVMperType(urlSVMComponents, VMComponent.class);

        Thread.sleep(5*60*1000);

        Set<String> all = handler.getAllIds(VMAction.class);
        assertTrue(all.size() == 9);

        all = handler.getAllIds(VMPriority.class);
        assertTrue(all.size() == 5);

        all = handler.getAllIds(VMComponent.class);
        assertTrue(all.size() > 15000);

    }

    private <T extends TBase> void testSVMperType(String url, Class type) throws Exception {


        SVMSyncHandler<T> ssh = new SVMSyncHandler<T>(type);
        VMResult<String> vmResult = ssh.getSMVElementIds(url);
        log.info(vmResult.elements.size()+" vmids received. start storing elements...");
        VMProcessHandler.storeElements(type, vmResult.elements, url, true);
        log.info("Storing and getting master data triggered. waiting for completion...");
    }
}
