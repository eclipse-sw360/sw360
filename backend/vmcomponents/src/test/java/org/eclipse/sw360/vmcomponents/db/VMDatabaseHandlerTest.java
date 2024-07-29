/*
 * SPDX-FileCopyrightText: © 2022 Siemens AG
 * SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.vmcomponents.db;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.junit.Assert.*;

/**
 * @author stefan.jaeger@evosoft.com
 */
@RunWith(MockitoJUnitRunner.class)
public class VMDatabaseHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_VM;

    private static VMPriority p1;
    private static VMPriority p2;
    private static VMPriority p3;

    private static VMAction a1;
    private static VMAction a2;
    private static VMAction a3;

    private static VMComponent c1;

    private static VMProcessReporting pr1;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private VMDatabaseHandler handler;

    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);

        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        // Prepare the database
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettingsTest.getConfiguredHttpClient(), dbName);

        // set up prios
        p1 = new VMPriority().setVmid("1").setShortText("one").setLongText("onelong");
        p2 = new VMPriority().setVmid("2").setShortText("two").setLongText("twolong");
        p3 = new VMPriority().setVmid("3").setShortText("three").setLongText("threelong");
        databaseConnector.add(p1);
        databaseConnector.add(p2);

        //set up actions
        a1 = new VMAction().setVmid("1").setText("one");
        a2 = new VMAction().setVmid("2").setText("two");
        a3 = new VMAction().setVmid("3").setText("three");
        databaseConnector.add(a1);
        databaseConnector.add(a2);

        //set up components
        c1 = new VMComponent()
                .setVmid("70")
                .setVendor("Apache Software Foundation")
                .setName("Tomcat")
                .setVersion("3.2.1")
                .setUrl("http://tomcat.apache.org/")
                .setSecurityUrl(null)
                .setEolReached(true)
                .setCpe("cpe:/a:apache:tomcat:3.2.1");
        c1.addToMinPatchLevels(new VMMinPatchLevel(p1.getVmid()).setVersion(null));
        c1.addToMinPatchLevels(new VMMinPatchLevel(p2.getVmid()).setVersion("6.0.36"));

        // set up process reporting
        pr1 = new VMProcessReporting(VMAction.class.getSimpleName(), SW360Utils.getCreatedOnTime());

        // Prepare the handler
        handler = new VMDatabaseHandler(DatabaseSettingsTest.getConfiguredHttpClient(), DatabaseSettingsTest.COUCH_DB_VM);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testAddPriority() throws Exception {
        RequestStatus requestStatus = handler.add(p3);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMPriority> prios = handler.getAll(VMPriority.class);
        assertEquals(3, prios.size());
        boolean found3 = false;
        for (VMPriority prio: prios) {
            if (prio.getVmid().equals("3")){
                found3 = true;
            }
        }
        assertTrue(found3);
    }

    @Test
    public void testUpdatePriority() throws Exception {
        p2.setLongText("updated");
        RequestStatus requestStatus = handler.update(p2);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMPriority> prios = handler.getAll(VMPriority.class);
        assertEquals(2, prios.size());
        boolean found2 = false;
        for (VMPriority prio: prios) {
            if (prio.getVmid().equals("2")){
                found2 = true;
                assertEquals("updated", prio.getLongText());
            }
        }
        assertTrue(found2);
    }

    @Test
    public void testDeletePriority() throws Exception {
        RequestStatus requestStatus = handler.delete(p2);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMPriority> prios = handler.getAll(VMPriority.class);
        assertEquals(1, prios.size());
        VMPriority prio1 = prios.get(0);
        assertEquals(p1.getVmid(), prio1.getVmid());
        assertEquals(p1.getLongText(), prio1.getLongText());

        requestStatus = handler.delete(p1);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        prios = handler.getAll(VMPriority.class);
        assertEquals(0, prios.size());

    }

    @Test
    public void testAddProcessReporting() throws Exception {
        RequestStatus requestStatus = handler.add(pr1);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMProcessReporting> processes = handler.getAll(VMProcessReporting.class);
        assertEquals(1, processes.size());
        assertEquals(VMAction.class.getSimpleName(), processes.get(0).getElementType());
    }

    @Test
    public void testAddAction() throws Exception {
        RequestStatus requestStatus = handler.add(a3);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMAction> actions = handler.getAll(VMAction.class);
        assertEquals(3, actions.size());
        boolean found3 = false;
        for (VMAction action: actions) {
            if (action.getVmid().equals("3")){
                found3 = true;
            }
        }
        assertTrue(found3);
    }

    @Test
    public void testUpdateAction() throws Exception {
        a2.setText("updated");
        RequestStatus requestStatus = handler.update(a2);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMAction> actions = handler.getAll(VMAction.class);
        assertEquals(2, actions.size());
        boolean found2 = false;
        for (VMAction action: actions) {
            if (action.getVmid().equals("2")){
                found2 = true;
                assertEquals("updated", action.getText());
            }
        }
        assertTrue(found2);
    }

    @Test
    public void testDeleteAction() throws Exception {
        RequestStatus requestStatus = handler.delete(a2);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMAction> actions = handler.getAll(VMAction.class);
        assertEquals(1, actions.size());
        VMAction action1 = actions.get(0);
        assertEquals("1", action1.getVmid());
        assertEquals("one", action1.getText());

        requestStatus = handler.delete(a1);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        actions = handler.getAll(VMAction.class);
        assertEquals(0, actions.size());

    }

    @Test
    public void testAddComponent() throws Exception {
        RequestStatus requestStatus = handler.add(c1);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMComponent> components = handler.getAll(VMComponent.class);
        assertEquals(1, components.size());
        VMComponent component = components.get(0);
        assertEquals("70", component.getVmid());
        assertEquals("Tomcat", component.getName());

        Set<VMMinPatchLevel> levels = c1.getMinPatchLevels();
        assertEquals(2, levels.size());
        for (VMMinPatchLevel level: levels) {
            switch (level.getPriority()){
                case "1": assertNull(level.getVersion()); break;
                case "2": assertEquals("6.0.36", level.getVersion()); break;
                default: fail("not expected: "+level.toString());
            }
        }
    }

    @Test
    public void testUpdateComponent() throws Exception {
        handler.add(c1);

        c1.setName("updated");
        RequestStatus requestStatus = handler.update(c1);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        List<VMComponent> components = handler.getAll(VMComponent.class);
        assertEquals(1, components.size());
        VMComponent component = components.get(0);
        assertEquals("70", component.getVmid());
        assertEquals("updated", component.getName());
    }

    @Test
    public void testDeleteComponent() throws Exception {
        handler.add(c1);
        List<VMComponent> components = handler.getAll(VMComponent.class);
        assertEquals(1, components.size());

        RequestStatus requestStatus = handler.delete(c1);
        assertEquals(RequestStatus.SUCCESS, requestStatus);

        components = handler.getAll(VMComponent.class);
        assertEquals(0, components.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByCreationDateException() throws Exception {
        handler.getByCreationDate(VMAction.class, "1");
    }

    @Test
    public void testGetByCreationDate() throws Exception {
        VMProcessReporting reporting = handler.getByCreationDate(VMProcessReporting.class, null);
        assertNull(reporting);

        String time = SW360Utils.getCreatedOnTime();
        reporting = handler.getByCreationDate(VMProcessReporting.class, time);
        assertNull(reporting);

        handler.add(new VMProcessReporting(VMAction.class.getSimpleName(), time));
        reporting = handler.getByCreationDate(VMProcessReporting.class, time);
        assertNotNull(reporting);
        assertEquals(time, reporting.getStartDate());
        assertNull(reporting.getEndDate());
    }

//    @Test
    public void testGetLastUpdated() throws InterruptedException {

        handler.add(new VMAction("lu1"));
        VMAction action = handler.getLastUpdated(VMAction.class);
        assertNull(action);

        action = handler.getByVmId(VMAction.class, "lu1");
        handler.update(action);
        action = handler.getLastUpdated(VMAction.class);
        assertNotNull(action);
        assertEquals("lu1", action.getVmid());
        assertNotNull(action.getLastUpdateDate());

        Thread.sleep(2000);

        action = new VMAction("lu2");
        handler.add(action);
        handler.update(action);
        action = handler.getLastUpdated(VMAction.class);
        assertNotNull(action);
        assertEquals("lu2", action.getVmid());
        assertNotNull(action.getLastUpdateDate());
    }

}
