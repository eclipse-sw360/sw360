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
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.*;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class AttachmentHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;

    private AttachmentHandler handler;


    @Before
    public void setUp() throws Exception {
        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);

        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        databaseConnector.add(new AttachmentContent().setId("A1").setFilename("a.txt").setContentType("text"));
        databaseConnector.add(new AttachmentContent().setId("A2").setFilename("b.jpg").setContentType("image"));

        handler = new AttachmentHandler(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE, dbName);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the database
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);
    }

    @Test
    public void testGetAttachmentContent() throws Exception {
        AttachmentContent attachment = handler.getAttachmentContent("A1");
        assertThat("A1", equalTo(attachment.id));
        assertThat("a.txt", equalTo(attachment.filename));
        assertThat("text", equalTo(attachment.contentType));
    }

    @Test
    public void testVacuum_OnlyAdminCanRun() throws Exception {
        final RequestSummary requestSummary = handler.vacuumAttachmentDB(new User("a", "a").setUserGroup(UserGroup.USER), ImmutableSet.of("A1", "A2"));
        assertThat(requestSummary.requestStatus, is(RequestStatus.FAILURE));
    }

    @Test
    public void testVacuum_AllIdsUsedIsNoop() throws Exception {
        final RequestSummary requestSummary = handler.vacuumAttachmentDB(new User("a", "a").setUserGroup(UserGroup.ADMIN), ImmutableSet.of("A1", "A2"));
        assertThat(requestSummary.requestStatus, is(RequestStatus.SUCCESS));
        assertThat(requestSummary.totalElements, is(2));
        assertThat(requestSummary.totalAffectedElements, is(0));

        final AttachmentContent a1 = handler.getAttachmentContent("A1");
        assertThat(a1, is(notNullValue()));
        final AttachmentContent a2 = handler.getAttachmentContent("A2");
        assertThat(a2, is(notNullValue()));
    }


    @Test
    public void testVacuum_UnusedIdIsDeleted() throws Exception {
        final RequestSummary requestSummary = handler.vacuumAttachmentDB(new User("a", "a").setUserGroup(UserGroup.ADMIN), ImmutableSet.of("A1"));
        assertThat(requestSummary.requestStatus, is(RequestStatus.SUCCESS));
        assertThat(requestSummary.totalElements, is(2));
        assertThat(requestSummary.totalAffectedElements, is(1));

        final AttachmentContent a1 = handler.getAttachmentContent("A1");
        assertThat(a1, is(notNullValue()));
        final AttachmentContent a2 = handler.getAttachmentContent("A2");
        assertThat(a2, is(nullValue()));

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

        assertThat(handler.getAttachmentUsage(usage1.getId()), is(usage1));
        assertThat(handler.getAttachmentUsage(usage2.getId()), is(usage2));
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

        assertThat(handler.getAttachmentUsage(usage1.getId()), is(usage1));
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
        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage2));
    }

    @Test
    public void testGetAttachmentUsages() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertThat(handler.getAttachmentUsages(Source.releaseId("r1"), "a11", null),
                containsInAnyOrder(usage1, usage4, usage5));
    }

    @Test
    public void testGetAttachmentUsagesWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertThat(handler.getAttachmentUsages(Source.releaseId("r1"), "a11", UsageData.licenseInfo(new LicenseInfoUsage())),
                containsInAnyOrder(usage1, usage5));
    }

    @Test
    public void testGetAttachmentsUsages() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertThat(handler.getAttachmentsUsages(Source.releaseId("r1"), ImmutableSet.of("a11", "a12"), null),
                containsInAnyOrder(usage1, usage2, usage4, usage5));
        assertThat(handler.getAttachmentsUsages(Source.releaseId("r1"), Collections.emptySet(), null),
                empty());
    }

    @Test
    public void testGetAttachmentsUsagesWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage3 = createUsage("p2", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a13", UsageData.manuallySet(new ManuallySetUsage()));
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertThat(handler.getAttachmentsUsages(Source.releaseId("r1"), ImmutableSet.of("a11", "a12", "a13"), UsageData.licenseInfo(new LicenseInfoUsage())),
                containsInAnyOrder(usage1, usage2, usage5));
    }

    @Test
    public void testGetUsedAttachments() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11");
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12");
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11");
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage1, usage2, usage3));
    }

    @Test
    public void testGetUsedAttachmentsWithFilter() throws Exception {
        AttachmentUsage usage1 = createUsage("p1", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage2 = createUsage("p1", "r1", "a12", UsageData.licenseInfo(new LicenseInfoUsage()));
        AttachmentUsage usage3 = createUsage("p1", "r2", "a21");
        AttachmentUsage usage4 = createUsage("p3", "r1", "a11");
        AttachmentUsage usage5 = createUsage("p4", "r1", "a11", UsageData.licenseInfo(new LicenseInfoUsage()));
        handler.makeAttachmentUsages(Lists.newArrayList(usage1, usage2, usage3, usage4, usage5));

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), UsageData.licenseInfo(new LicenseInfoUsage())),
                containsInAnyOrder(usage1, usage2));
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

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage3, usage6));
        assertThat(handler.getUsedAttachments(Source.projectId("p2"), null), containsInAnyOrder(usage4));
        assertThat(handler.getUsedAttachments(Source.projectId("p3"), null), containsInAnyOrder(usage5));
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

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage1, usage2, usage6, usage7));
        assertThat(handler.getUsedAttachments(Source.projectId("p2"), null), containsInAnyOrder(usage4));
        assertThat(handler.getUsedAttachments(Source.projectId("p3"), null), containsInAnyOrder(usage5));
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

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage1, usage2, usage3));
        assertThat(handler.getUsedAttachments(Source.projectId("p2"), null), containsInAnyOrder(usage4));
        assertThat(handler.getUsedAttachments(Source.projectId("p3"), null), containsInAnyOrder(usage5));
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

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage3));
        assertThat(handler.getUsedAttachments(Source.projectId("p2"), null), containsInAnyOrder(usage4));
        assertThat(handler.getUsedAttachments(Source.projectId("p3"), null), containsInAnyOrder(usage5));
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

        assertThat(handler.getUsedAttachments(Source.projectId("p1"), null), containsInAnyOrder(usage1, usage2));
        assertThat(handler.getUsedAttachments(Source.projectId("p2"), null), containsInAnyOrder(usage4));
        assertThat(handler.getUsedAttachments(Source.projectId("p3"), null), containsInAnyOrder(usage5));
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
            assertThat(counts.get(entry.getKey()), is(entry.getValue()));
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
            assertThat(counts.get(entry.getKey()), is(entry.getValue()));
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
