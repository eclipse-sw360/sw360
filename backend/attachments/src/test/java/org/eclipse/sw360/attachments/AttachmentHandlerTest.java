/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.attachments;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class AttachmentHandlerTest {

    @Autowired
    @Qualifier("COUCH_DB_DATABASE")
    private String dbName;

    @Autowired
    @Qualifier("COUCH_DB_ATTACHMENTS")
    private String attachmentsDbName;

    @Autowired
    @Qualifier("LUCENE_SEARCH_LIMIT")
    private int luceneSearchLimit;

    @Autowired
    private Cloudant client;

    @Autowired
    private AttachmentHandler handler;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void setUp() throws Exception {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(client, attachmentsDbName, luceneSearchLimit);
        databaseConnector.add(new AttachmentContent().setId("A1").setFilename("a.txt").setContentType("text"));
        databaseConnector.add(new AttachmentContent().setId("A2").setFilename("b.jpg").setContentType("image"));
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testGetAttachmentContent() throws Exception {
        AttachmentContent attachment = handler.getAttachmentContent("A1");
        assertEquals("A1", attachment.id);
        assertEquals("a.txt", attachment.filename);
        assertEquals("text", attachment.contentType);
    }

    @Test
    public void testVacuum_OnlyAdminCanRun() throws Exception {
        final RequestSummary requestSummary = handler.vacuumAttachmentDB(new User("a", "a").setUserGroup(UserGroup.USER), ImmutableSet.of("A1", "A2"));
        assertEquals(RequestStatus.FAILURE, requestSummary.requestStatus);
    }

    @Test
    public void testVacuum_AllIdsUsedIsNoop() throws Exception {
        final RequestSummary requestSummary = handler.vacuumAttachmentDB(new User("a", "a").setUserGroup(UserGroup.ADMIN), ImmutableSet.of("A1", "A2"));
        assertEquals(RequestStatus.SUCCESS, requestSummary.requestStatus);
        assertEquals(2, requestSummary.totalElements);
        assertEquals(0, requestSummary.totalAffectedElements);

        final AttachmentContent a1 = handler.getAttachmentContent("A1");
        assertNotNull(a1);
        final AttachmentContent a2 = handler.getAttachmentContent("A2");
        assertNotNull(a2);
    }


    @Test
    public void testVacuum_UnusedIdIsDeleted() throws Exception {
        final RequestSummary requestSummary = handler.vacuumAttachmentDB(new User("a", "a").setUserGroup(UserGroup.ADMIN), ImmutableSet.of("A1"));
        assertEquals(RequestStatus.SUCCESS, requestSummary.requestStatus);
        assertEquals(2, requestSummary.totalElements);
        assertEquals(1, requestSummary.totalAffectedElements);

        final AttachmentContent a1 = handler.getAttachmentContent("A1");
        assertNotNull(a1);
        Assert.assertThrows(SW360Exception.class, () -> handler.getAttachmentContent("A2"));
    }

    @Test
    public void testMakeAndGetAttachmentUsage() throws Exception {
        AttachmentUsage usage1 = new AttachmentUsage();
        usage1.setOwner(Source.projectId("r1"));
        usage1.setUsedBy(Source.projectId("p1"));
        usage1.setAttachmentContentId("a1");
        usage1 = handler.makeAttachmentUsage(usage1);

        AttachmentUsage usage2 = new AttachmentUsage();
        usage2.setOwner(Source.projectId("r2"));
        usage2.setUsedBy(Source.projectId("p1"));
        usage2.setAttachmentContentId("a2");
        usage2 = handler.makeAttachmentUsage(usage2);

        assertEquals(usage1, handler.getAttachmentUsage(usage1.getId()));
        assertEquals(usage2, handler.getAttachmentUsage(usage2.getId()));
    }

    @Test
    public void testUpdateAttachmentUsage() throws Exception {
        AttachmentUsage usage1 = new AttachmentUsage();
        usage1.setOwner(Source.projectId("r1"));
        usage1.setUsedBy(Source.projectId("p1"));
        usage1.setAttachmentContentId("a1");
        handler.makeAttachmentUsage(usage1);

        usage1.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l1", "l2"))));
        handler.updateAttachmentUsage(usage1);

        assertEquals(usage1, handler.getAttachmentUsage(usage1.getId()));
    }

    @Test(expected = SW360Exception.class)
    public void testDeleteAttachmentUsage() throws Exception {
        AttachmentUsage usage1 = new AttachmentUsage();
        usage1.setOwner(Source.projectId("r1"));
        usage1.setUsedBy(Source.projectId("p1"));
        usage1.setAttachmentContentId("a1");
        handler.makeAttachmentUsage(usage1);

        Assert.assertTrue(usage1.isSetId());
        handler.deleteAttachmentUsage(usage1);
        Assert.assertNull(handler.getAttachmentUsage(usage1.getId()));
    }

    @Test
    public void testBulkDeleteAttachmentUsage() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3));

        Assert.assertTrue(usage1.isSetId());
        Assert.assertTrue(usage2.isSetId());
        Assert.assertTrue(usage3.isSetId());

        handler.deleteAttachmentUsages(Lists.newArrayList(usage1, usage3));
        assertTrue(Matchers.containsInAnyOrder(usage2).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
    }

    @Test
    public void testGetAttachmentUsages() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage4, usage5)
                .matches(handler.getAttachmentUsages(Source.releaseId("r1"), "a11", null)));
    }

    @Test
    public void testGetAttachmentUsagesWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage5)
                .matches(handler.getAttachmentUsages(Source.releaseId("r1"), "a11", UsageData.licenseInfo(new LicenseInfoUsage()))));
    }

    @Test
    public void testGetAttachmentsUsages() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2, usage4, usage5)
                .matches(handler.getAttachmentsUsages(Source.releaseId("r1"), ImmutableSet.of("a11", "a12"), null)));
        assertTrue(Matchers.empty()
                .matches(handler.getAttachmentsUsages(Source.releaseId("r1"), Collections.emptySet(), null)));
    }

    @Test
    public void testGetAttachmentsUsagesWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a13", UsageData.manuallySet(new ManuallySetUsage()));
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2, usage5)
                .matches(handler.getAttachmentsUsages(Source.releaseId("r1"), ImmutableSet.of("a11", "a12", "a13"), UsageData.licenseInfo(new LicenseInfoUsage()))));
    }

    @Test
    public void testGetUsedAttachments() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2, usage3).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
    }

    @Test
    public void testGetUsedAttachmentsWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2)
                .matches(handler.getUsedAttachments(Source.projectId("p1"), UsageData.licenseInfo(new LicenseInfoUsage()))));
    }

    @Test
    public void testReplacementOfUsageWithoutEmptyUsageData() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        usage1.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l1", "l2"))));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        usage2.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p2", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p3", "r1", "a11");
        usage5.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l3"))));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        AttachmentUsage usage6 = createUsage("p1", "r19", "a91");
        usage6.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l9"))));
        handler.replaceAttachmentUsages(Source.projectId("p1"), Lists.newArrayList(usage6));

        assertTrue(Matchers.containsInAnyOrder(usage3, usage6).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage4).matches(handler.getUsedAttachments(Source.projectId("p2"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage5).matches(handler.getUsedAttachments(Source.projectId("p3"), null)));
    }

    @Test
    public void testReplacementOfUsageWithEmptyUsageData() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        usage1.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l1", "l2"))));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        usage2.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p2", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p3", "r1", "a11");
        usage5.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l3"))));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        AttachmentUsage usage6 = createUsage("p1", "r8", "a81");
        AttachmentUsage usage7 = createUsage("p1", "r9", "a91");

        handler.replaceAttachmentUsages(Source.projectId("p1"), Lists.newArrayList(usage6, usage7));

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2, usage6, usage7).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage4).matches(handler.getUsedAttachments(Source.projectId("p2"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage5).matches(handler.getUsedAttachments(Source.projectId("p3"), null)));
    }

    @Test
    public void testReplacingWithEmptyUsagesListDoesNothing() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        usage1.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l1", "l2"))));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        usage2.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p2", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p3", "r1", "a11");
        usage5.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l3"))));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        handler.replaceAttachmentUsages(Source.projectId("p1"), Lists.newArrayList());

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2, usage3).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage4).matches(handler.getUsedAttachments(Source.projectId("p2"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage5).matches(handler.getUsedAttachments(Source.projectId("p3"), null)));
    }

    @Test
    public void testDeleteAttachmentUsagesByUsageDataTypeWithNonEmptyType() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        usage1.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l1", "l2"))));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        usage2.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p2", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p3", "r1", "a11");
        usage5.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l3"))));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        handler.deleteAttachmentUsagesByUsageDataType(Source.projectId("p1"), UsageData.licenseInfo(new LicenseInfoUsage(Collections.emptySet())));

        assertTrue(Matchers.containsInAnyOrder(usage3).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage4).matches(handler.getUsedAttachments(Source.projectId("p2"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage5).matches(handler.getUsedAttachments(Source.projectId("p3"), null)));
    }

    @Test
    public void testDeleteAttachmentUsagesByUsageDataTypeWithEmptyType() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        usage1.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l1", "l2"))));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        usage2.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p2", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p3", "r1", "a11");
        usage5.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet("l3"))));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        handler.deleteAttachmentUsagesByUsageDataType(Source.projectId("p1"), null);

        assertTrue(Matchers.containsInAnyOrder(usage1, usage2).matches(handler.getUsedAttachments(Source.projectId("p1"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage4).matches(handler.getUsedAttachments(Source.projectId("p2"), null)));
        assertTrue(Matchers.containsInAnyOrder(usage5).matches(handler.getUsedAttachments(Source.projectId("p3"), null)));
    }

    @Test
    public void testAttachmentUsageCount() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        AttachmentUsage usage6 = createUsage("p5", "r3", "a31");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5, usage6));

        Map<Source, Set<String>> queryFor = ImmutableMap.of(
            Source.releaseId("r1"), ImmutableSet.of("a11", "a12"),
            Source.releaseId("r3"), ImmutableSet.of("a31")
        );

        Map<Map<Source, String>, Integer> counts = handler.getAttachmentUsageCount(queryFor, null);

        Map<Map<Source, String>, Integer> expected = ImmutableMap.of(ImmutableMap.of(Source.releaseId("r1"), "a11"), 3,
                ImmutableMap.of(Source.releaseId("r1"), "a12"), 1, ImmutableMap.of(Source.releaseId("r3"), "a31"), 1);

        for (Entry<Map<Source, String>, Integer> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), counts.get(entry.getKey()));
        }
    }

    @Test
    public void testAttachmentUsageCountWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        AttachmentUsage usage6 = createUsage("p5", "r3", "a31");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5, usage6));

        Map<Source, Set<String>> queryFor = ImmutableMap.of(Source.releaseId("r1"), ImmutableSet.of("a11", "a12"), Source.releaseId("r3"),
                ImmutableSet.of("a31"));

        Map<Map<Source, String>, Integer> counts = handler.getAttachmentUsageCount(queryFor, UsageData.licenseInfo(new LicenseInfoUsage()));

        Map<Map<Source, String>, Integer> expected = ImmutableMap.of(ImmutableMap.of(Source.releaseId("r1"), "a11"), 2);

        for (Entry<Map<Source, String>, Integer> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), counts.get(entry.getKey()));
        }
    }

    private AttachmentUsage createUsage(String usedBy, String owner, String attachmentId) {
        AttachmentUsage usage = new AttachmentUsage();
        usage.setUsedBy(Source.projectId(usedBy));
        usage.setOwner(Source.releaseId(owner));
        usage.setAttachmentContentId(attachmentId);
        return usage;
    }

    private AttachmentUsage createUsage(String usedBy, String owner, String attachmentId, UsageData usageData) {
        AttachmentUsage usage = createUsage(usedBy, owner, attachmentId);
        usage.setUsageData(usageData);
        return usage;
    }
}
