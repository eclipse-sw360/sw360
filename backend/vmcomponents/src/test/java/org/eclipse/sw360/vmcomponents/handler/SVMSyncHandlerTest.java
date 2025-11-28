/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.handler;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;
import org.eclipse.sw360.vmcomponents.AbstractJSONMockTest;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.vmcomponents.common.SVMMapper;
import org.eclipse.sw360.vmcomponents.common.VMResult;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * @author stefan.jaeger@evosoft.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class SVMSyncHandlerTest extends AbstractJSONMockTest {

    private final String URL_ACTIONS = "http://localhost:8090/portal/api/v1/public/actions";
    private final String URL_VULNERABILITY = "http://localhost:8090/portal/api/v1/public/notifications";

    private SVMSyncHandler<VMAction> svmActionHandler = null;
    private SVMSyncHandler<VMComponent> svmComponentHandler = null;
    private SVMSyncHandler<VMPriority> svmPriorityHandler = null;
    private SVMSyncHandler<Vulnerability> svmVulHandler = null;
    private SVMSyncHandler<Release> releaseHandler = null;

    @Autowired
    private VMDatabaseHandler handler;
    @Autowired
    private ComponentDatabaseHandler compDBHandler;
    @Autowired
    private VendorRepository vendorRepository;
    private User user;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void setUp() throws TException, IOException {
        assumeCanConnectTo(URL_ACTIONS);

        svmActionHandler = new SVMSyncHandler<>(VMAction.class);
        svmComponentHandler = new SVMSyncHandler<>(VMComponent.class);
        svmPriorityHandler = new SVMSyncHandler<>(VMPriority.class);
        svmVulHandler = new SVMSyncHandler<>(Vulnerability.class);
        releaseHandler = new SVMSyncHandler<>(Release.class);
        user = new User().setEmail("me");

        // mock preparation
        staticJSONResponse("/portal/api/v1/public/actions", "[1,2,3,4,5,6,7,8,9]");
        staticJSONResponse("/portal/api/v1/public/notifications/22955", new File("src/test/resources/notifications_22955.json"));
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

//    @Test
    public void create5000Matches() throws SW360Exception, MalformedURLException {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "droelf");
        component.setName("droelf");
        component.setVendor("droelf");
        component.setVersion("1.0");
        component.setCpe("droelf");
        handler.add(component);
        Component relComponent = new Component("droelf");
        compDBHandler.addComponent(relComponent, "me");
        Release release = new Release("droelf", "1.0", relComponent.getId());
        release.setCpeid("droelf");
        compDBHandler.addRelease(release, user);

        VMMatchState[] states = {VMMatchState.ACCEPTED, VMMatchState.DECLINED, VMMatchState.MATCHING_LEVEL_1, VMMatchState.MATCHING_LEVEL_2, VMMatchState.MATCHING_LEVEL_3};
        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.CPE);

        for (int i=0; i<5000; i++){
            VMMatch match = new VMMatch(component.getId(), release.getId(), types, states[i%states.length]);
            SVMMapper.updateMatch(match, component, release, () -> relComponent);
            handler.add(match);
        }
    }

    @Test
    public void testGetSMVElementIds() throws Exception {
        assertEquals(0, handler.getAllIds(VMAction.class).size());
        VMResult smvElementIds = svmActionHandler.getSMVElementIds(URL_ACTIONS);
        assertEquals(0, handler.getAllIds(VMAction.class).size());
        assertEquals(9, smvElementIds.elements.size());
        assertEquals(RequestStatus.SUCCESS, smvElementIds.requestSummary.requestStatus);
    }

    @Test
    public void testDeleteMissingElements() throws SW360Exception {
        List<String> ids = new ArrayList<>(3);
        ids.add("1"); ids.add("2"); ids.add("3");
        svmActionHandler.storeNewElement(ids.get(0));
        svmActionHandler.storeNewElement(ids.get(1));
        svmActionHandler.storeNewElement(ids.get(2));
        List<VMAction> actions = handler.getAll(VMAction.class);
        assertNotNull(actions);
        assertEquals(ids.size(), actions.size());

        ids.remove(1);
        ids.add("4");
        String deletedId = handler.getByVmId(VMAction.class, "2").getId();
        VMResult<String> result = svmActionHandler.deleteMissingElements(ids);
        assertEquals(RequestStatus.SUCCESS, result.requestSummary.requestStatus);
        assertEquals(1, result.elements.size());
        assertEquals(deletedId, result.elements.get(0));

        actions = handler.getAll(VMAction.class);
        assertNotNull(actions);
        assertEquals(2, actions.size());
        ids.clear();
        ids.add(actions.get(0).getVmid());
        ids.add(actions.get(1).getVmid());
        assertTrue(ids.contains("1"));
        assertTrue(ids.contains("3"));

    }

    @Test
    public void testStoreNewElement() throws Exception {
        VMResult vmResult = svmPriorityHandler.storeNewElement("1");
        assertEquals(RequestStatus.SUCCESS, vmResult.requestSummary.requestStatus);
        List<VMPriority> all = handler.getAll(VMPriority.class);
        assertEquals(1, all.size());
        VMPriority priority = all.iterator().next();
        assertEquals("1", priority.getVmid());
        assertTrue(!StringUtils.isEmpty(priority.getId()));
        assertNull(priority.getLongText());
    }

    @Test
    public void testGetSMVElementMasterDataById() throws Exception {
        handler.add(new VMAction("5").setText("action"));
        handler.add(new VMPriority("2").setShortText("prio").setLongText("priority"));
        handler.add(new Vulnerability("22955"));
        Vulnerability vul = handler.getByVmId(Vulnerability.class, "22955");
        assertNull(vul.getTitle());
        VMResult<Vulnerability> result = svmVulHandler.getSMVElementMasterDataById(vul.getId(), URL_VULNERABILITY);
        assertEquals(RequestStatus.SUCCESS, result.requestSummary.requestStatus);
        vul = result.elements.get(0);
        assertEquals("action", vul.getAction());
        assertEquals("priority", vul.getPriorityText());
        assertNotNull(vul.getTitle());
        vul = handler.getAll(Vulnerability.class).get(0);
        assertNotNull(vul);
        assertNull(vul.getTitle());
    }

    @Test
    public void testSyncDatabaseNew() throws Exception {
        assertEquals(0, handler.getAll(VMComponent.class).size());
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "70");
        VMResult result = svmComponentHandler.syncDatabase(component);
        assertEquals(RequestStatus.SUCCESS, result.requestSummary.requestStatus);
        List<VMComponent> all = handler.getAll(VMComponent.class);
        assertEquals(1, all.size());
        component = all.get(0);
        assertEquals("70", component.getVmid());
        assertNull(component.getName());
        assertNotNull(component.getId());
    }

    @Test
    public void testSyncDatabaseUpdate() throws Exception {
        assertEquals(0, handler.getAll(VMComponent.class).size());
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "70");
        handler.add(component);
        List<VMComponent> all = handler.getAll(VMComponent.class);
        assertEquals(1, all.size());
        component = all.get(0);
        assertEquals("70", component.getVmid());
        assertNull(component.getName());
        assertNotNull(component.getId());
        assertNull(component.getCpe());

        component.setCpe("cpe");
        VMResult result = svmComponentHandler.syncDatabase(component);
        assertEquals(RequestStatus.SUCCESS, result.requestSummary.requestStatus);
        all = handler.getAll(VMComponent.class);
        assertEquals(1, all.size());
        component = all.get(0);
        assertEquals("70", component.getVmid());
        assertNull(component.getName());
        assertNotNull(component.getId());
        assertEquals("cpe", component.getCpe());
    }

    @Test
    public void testFindMatchByComponentCPE() throws Exception {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "droelf");
        VMResult matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        handler.add(component);
        assertNotNull(component.getId());
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        component.setCpe("cpe");
        handler.update(component);
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        Component relComponent = new Component("droelf");
        compDBHandler.addComponent(relComponent, "me");
        assertNotNull(relComponent.getId());
        Release release = new Release("droelf", "1.0", relComponent.getId());
        compDBHandler.addRelease(release, user);
        assertNotNull(release.getId());
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        release = new Release("oelf", "2.0", relComponent.getId());
        release.setCpeid("cpe");
        compDBHandler.addRelease(release, user);
        assertNotNull(release.getId());

        List<VMMatch> matches = handler.getAll(VMMatch.class);
        assertEquals(0, matches.size());
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(1, matchResult.requestSummary.getTotalAffectedElements());
        component = handler.getById(VMComponent.class, component.getId());

        matches = handler.getAll(VMMatch.class);
        assertEquals(1, matches.size());
        VMMatch match = matches.get(0);
        assertEquals(component.getId(), match.getVmComponentId());
        assertEquals(release.getId(), match.getReleaseId());
        assertEquals(1, match.getMatchTypes().size());
        assertEquals(VMMatchType.CPE, match.getMatchTypes().iterator().next());
        assertEquals(VMMatchState.ACCEPTED, match.getState());
    }


    @Test
    public void testFindMatchByComponentText() throws Exception {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "droe");
        VMResult matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        handler.add(component);
        assertNotNull(component.getId());
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        component.setCpe("cpe_nix_match");
        component.setName("droe");
        component.setVersion("1");
        component.setVendor("doelf");
        handler.update(component);
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        Component relComponent = new Component("droelf");
        compDBHandler.addComponent(relComponent, "me");
        assertNotNull(relComponent.getId());
        Vendor vendor = new Vendor("doe","doelf","http://doelf.com");
        vendorRepository.add(vendor);
        Release release = new Release("droelf", "1.0", relComponent.getId());
        release.setVendorId(vendor.getId());
        release.setVendor(vendor);
        compDBHandler.addRelease(release, user);
        assertNotNull(release.getId());
        List<VMMatch> matches = handler.getAll(VMMatch.class);
        assertEquals(0, matches.size());
        matchResult = svmComponentHandler.findMatchByComponent(component.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(1, matchResult.requestSummary.getTotalAffectedElements());
        component = handler.getById(VMComponent.class, component.getId());

        matches = handler.getAll(VMMatch.class);
        assertEquals(1, matches.size());
        VMMatch match = matches.get(0);
        assertEquals(component.getId(), match.getVmComponentId());
        assertEquals(release.getId(), match.getReleaseId());
        assertEquals(3, match.getMatchTypes().size());
        assertTrue(match.getMatchTypes().contains(VMMatchType.NAME_CR));
        assertTrue(match.getMatchTypes().contains(VMMatchType.VENDOR_CR));
        assertTrue(match.getMatchTypes().contains(VMMatchType.VERSION_CR));
        assertEquals(VMMatchState.MATCHING_LEVEL_3, match.getState());
    }


    @Test
    public void testFindMatchByReleaseText() throws Exception {
        Component relComponent = new Component("droe");
        compDBHandler.addComponent(relComponent, "me");
        assertNotNull(relComponent.getId());
        Vendor vendor = new Vendor("doe","doelf","http://doelf.com");
        vendorRepository.add(vendor);
        Release release = new Release("droelf", "1", relComponent.getId());
        VMResult matchResult = releaseHandler.findMatchByRelease(release.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        release.setVendorId(vendor.getId());
        release.setVendor(vendor);
        compDBHandler.addRelease(release, user);
        assertNotNull(release.getId());

        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "droe");
        handler.add(component);
        assertNotNull(component.getId());

        matchResult = releaseHandler.findMatchByRelease(release.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(0, matchResult.requestSummary.getTotalAffectedElements());

        component.setCpe("cpe_nix_match");
        component.setName("droelf");
        component.setVersion("1.0");
        component.setVendor("doe");
        handler.update(component);

        List<VMMatch> matches = handler.getAll(VMMatch.class);
        assertEquals(0, matches.size());
        matchResult = releaseHandler.findMatchByRelease(release.getId());
        assertEquals(RequestStatus.SUCCESS, matchResult.requestSummary.requestStatus);
        assertEquals(1, matchResult.requestSummary.getTotalAffectedElements());
        component = handler.getById(VMComponent.class, component.getId());

        matches = handler.getAll(VMMatch.class);
        assertEquals(1, matches.size());
        VMMatch match = matches.get(0);
        assertEquals(component.getId(), match.getVmComponentId());
        assertEquals(release.getId(), match.getReleaseId());
        assertEquals(3, match.getMatchTypes().size());
        assertTrue(match.getMatchTypes().contains(VMMatchType.NAME_RC));
        assertTrue(match.getMatchTypes().contains(VMMatchType.VENDOR_RC));
        assertTrue(match.getMatchTypes().contains(VMMatchType.VERSION_RC));
        assertEquals(VMMatchState.MATCHING_LEVEL_3, match.getState());
    }
}
