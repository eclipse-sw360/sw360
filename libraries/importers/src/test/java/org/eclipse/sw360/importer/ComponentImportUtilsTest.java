/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.importer;

import com.google.common.collect.FluentIterable;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.apache.thrift.TException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.emptyOrNullCollectionOf;
import static org.eclipse.sw360.datahandler.TestUtils.sortByField;
import static org.eclipse.sw360.datahandler.common.CommonUtils.getFirst;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_ATTACHMENT;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getReleaseIds;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

/**
 * @author daniele.fognini@tngtech.com
 */
@Ignore("This test class is ignored for now")
public class ComponentImportUtilsTest extends ComponentAndAttachmentAwareDBTest {


    private final String fileName = "test-components.csv";
    private String attachmentsFilename = "test-attachments.csv";
    private final String REMOTE_URL = "http://www.testurl.com";
    private final String OVERRIDING_ID = "OVERRIDING_ID";
    private final String ADDITIONAL_ID = "ADDITIONAL_ID";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testImportOnEmptyDb() throws Exception {
        FluentIterable<ComponentCSVRecord> compCSVRecords = getCompCSVRecordsFromTestFile(fileName);

        Assert.assertTrue(componentClient.getComponentSummary(user).isEmpty());
        Assert.assertTrue(componentClient.getReleaseSummary(user).isEmpty());

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient,
                attachmentClient, user);

        assertExpectedComponentsInDb();

        final String attachmentContentId = getCreatedAttachmentContentId();

        final AttachmentContent overwriter = new AttachmentContent().setId(OVERRIDING_ID)
                .setOnlyRemote(true).setRemoteUrl(REMOTE_URL).setType(TYPE_ATTACHMENT)
                .setFilename(fileName).setContentType("text/plain");
        final AttachmentContent addition = new AttachmentContent().setId(ADDITIONAL_ID)
                .setOnlyRemote(true).setRemoteUrl(REMOTE_URL).setType(TYPE_ATTACHMENT)
                .setFilename(fileName).setContentType("text/plain");

        attachmentContentRepository.add(overwriter);
        attachmentContentRepository.add(addition);

        Assert.assertTrue(Matchers.hasSize(3).matches(attachmentContentRepository.getAll()));
        FluentIterable<ComponentAttachmentCSVRecord> compAttachmentCSVRecords =
                getCompAttachmentCSVRecordsFromTestFile(attachmentsFilename);

        ComponentImportUtils.writeAttachmentsToDatabase(compAttachmentCSVRecords, user,
                componentClient, attachmentClient);

        try {
            attachmentClient.getAttachmentContent(attachmentContentId);
            fail("Expected exception not thrown");
        } catch (Exception e) {
            Assert.assertTrue(instanceOf(SW360Exception.class).matches(e));
            Assert.assertEquals("Cannot find " + attachmentContentId + " in database.",
                    ((SW360Exception) e).getWhy());
        }

        Assert.assertTrue(Matchers.hasSize(2).matches(attachmentContentRepository.getAll()));
        final AttachmentContent attachmentContent =
                attachmentClient.getAttachmentContent(getCreatedAttachmentContentId());
        attachmentContent.setOnlyRemote(true);

        Assert.assertEquals(overwriter, attachmentContent);
    }

    private String getCreatedAttachmentContentId() throws TException {
        List<Release> importedReleases = componentClient.getReleaseSummary(user);
        sortByField(importedReleases, Release._Fields.VERSION);
        sortByField(importedReleases, Release._Fields.NAME);
        final Release release = importedReleases.get(4);
        final Set<Attachment> attachments = release.getAttachments();
        Assert.assertEquals(1, attachments.size());
        final Attachment theAttachment = getFirst(attachments);
        return theAttachment.getAttachmentContentId();
    }

    @Test
    public void testImportTwiceWithOnlyAPart() throws Exception {
        FluentIterable<ComponentCSVRecord> compCSVRecords = getCompCSVRecordsFromTestFile(fileName);

        ComponentImportUtils.writeToDatabase(compCSVRecords.limit(1), componentClient, vendorClient,
                attachmentClient, user);

        Assert.assertEquals(1, componentClient.getComponentSummary(user).size());
        List<Release> releaseSummary = componentClient.getReleaseSummary(user);
        Assert.assertEquals(1, releaseSummary.size());

        Assert.assertEquals("7-Zip", releaseSummary.getFirst().getName());

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient,
                attachmentClient, user);

        assertExpectedComponentsInDb();
    }


    @Test
    public void testImportTwiceIsANoOp() throws Exception {
        FluentIterable<ComponentCSVRecord> compCSVRecords = getCompCSVRecordsFromTestFile(fileName);

        Assert.assertEquals(0, componentClient.getComponentSummary(user).size());
        Assert.assertEquals(0, componentClient.getReleaseSummary(user).size());
        Assert.assertTrue(Matchers.hasSize(0).matches(attachmentContentRepository.getAll()));

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient,
                attachmentClient, user);
        Assert.assertTrue(Matchers.hasSize(1).matches(attachmentContentRepository.getAll()));
        List<Component> componentSummaryAfterFirst = componentClient.getComponentSummary(user);
        List<Release> releaseSummaryAfterFirst = componentClient.getReleaseSummary(user);

        assertExpectedComponentsInDb();

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient,
                attachmentClient, user);
        assertExpectedComponentsInDb();
        Assert.assertTrue(Matchers.hasSize(1).matches(attachmentContentRepository.getAll()));
        Assert.assertEquals(componentSummaryAfterFirst, componentClient.getComponentSummary(user));
        Assert.assertEquals(releaseSummaryAfterFirst, componentClient.getReleaseSummary(user));

    }

    private void assertExpectedComponentsInDb() throws TException {
        List<Component> importedComponents = componentClient.getComponentSummary(user);
        List<Release> importedReleases = componentClient.getReleaseSummary(user);

        Assert.assertEquals(7, importedComponents.size()); // see the test file
        Assert.assertEquals(8, importedReleases.size()); // see the test file

        sortByField(importedComponents, Component._Fields.NAME);
        sortByField(importedReleases, Release._Fields.VERSION);
        sortByField(importedReleases, Release._Fields.NAME);

        Component component = importedComponents.get(0);
        Assert.assertEquals("7-Zip", component.getName());

        component = componentClient.getComponentById(component.getId(), user);
        Assert.assertEquals("7-Zip", component.getName());
        Assert.assertEquals("http://commons.apache.org/proper/commons-exec", component.getHomepage());
        Assert.assertEquals(emptyOrNullCollectionOf(String.class), component.getVendorNames());
        Assert.assertEquals(emptyOrNullCollectionOf(Attachment.class), component.getAttachments());
        Assert.assertEquals(user.getEmail(), component.getCreatedBy());
        Assert.assertEquals(not(nullValue()), component.getReleases());
        Assert.assertTrue(containsInAnyOrder(
                importedReleases.get(0).getId(), importedReleases.get(1).getId())
                .matches(getReleaseIds(component.getReleases())));

        final Release release = importedReleases.get(4);
        Assert.assertEquals("1.2.11", release.getVersion());
        // This release has an download url so the import creates an attachmen
        final Set<Attachment> attachments = release.getAttachments();
        Assert.assertEquals(1, attachments.size());
        final Attachment theAttachment = getFirst(attachments);
        final String attachmentContentId = theAttachment.getAttachmentContentId();

        final AttachmentContent attachmentContent =
                attachmentClient.getAttachmentContent(attachmentContentId);

        Assert.assertTrue(attachmentContent.isOnlyRemote());

        Assert.assertEquals(REMOTE_URL, attachmentContent.getRemoteUrl());
    }

}
