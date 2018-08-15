/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.junit.Before;
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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ComponentImportUtilsTest extends ComponentAndAttachmentAwareDBTest {


    private final String fileName = "test-components.csv";
    private String attachmentsFilename= "test-attachments.csv";
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

        assertThat(componentClient.getComponentSummary(user), is(empty()));
        assertThat(componentClient.getReleaseSummary(user), is(empty()));

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient, attachmentClient, user);

        assertExpectedComponentsInDb();

        final String attachmentContentId = getCreatedAttachmentContentId();

        final AttachmentContent overwriter = new AttachmentContent().setId(OVERRIDING_ID).setOnlyRemote(true).setRemoteUrl(REMOTE_URL).setType(TYPE_ATTACHMENT);
        final AttachmentContent addition = new AttachmentContent().setId(ADDITIONAL_ID).setOnlyRemote(true).setRemoteUrl(REMOTE_URL).setType(TYPE_ATTACHMENT);

        attachmentContentRepository.add(overwriter);
        attachmentContentRepository.add(addition);

        assertThat(attachmentContentRepository.getAll(), Matchers.hasSize(3));
        FluentIterable<ComponentAttachmentCSVRecord> compAttachmentCSVRecords = getCompAttachmentCSVRecordsFromTestFile(attachmentsFilename);

        ComponentImportUtils.writeAttachmentsToDatabase(compAttachmentCSVRecords,user,componentClient,attachmentClient);

        try {
            attachmentClient.getAttachmentContent(attachmentContentId);
            fail("Expected exception not thrown");
        }catch (Exception e) {
            assertThat(e, is(instanceOf(SW360Exception.class)));
            assertThat(((SW360Exception)e).getWhy(), is("Cannot find "+ attachmentContentId + " in database."));
        }

        assertThat(attachmentContentRepository.getAll(), Matchers.hasSize(2) );
        final AttachmentContent attachmentContent = attachmentClient.getAttachmentContent(getCreatedAttachmentContentId());

        assertThat(attachmentContent, is(overwriter));


    }

    private String getCreatedAttachmentContentId() throws TException {
        List<Release> importedReleases = componentClient.getReleaseSummary(user);
        sortByField(importedReleases, Release._Fields.VERSION);
        sortByField(importedReleases, Release._Fields.NAME);
        final Release release = importedReleases.get(4);
        final Set<Attachment> attachments = release.getAttachments();
        assertThat(attachments.size(), is(1));
        final Attachment theAttachment = getFirst(attachments);
        return theAttachment.getAttachmentContentId();
    }

    @Test
    public void testImportTwiceWithOnlyAPart() throws Exception {
        FluentIterable<ComponentCSVRecord> compCSVRecords = getCompCSVRecordsFromTestFile(fileName);

        ComponentImportUtils.writeToDatabase(compCSVRecords.limit(1), componentClient, vendorClient, attachmentClient, user);

        assertThat(componentClient.getComponentSummary(user), hasSize(1));
        List<Release> releaseSummary = componentClient.getReleaseSummary(user);
        assertThat(releaseSummary, hasSize(1));

        assertThat(releaseSummary.get(0).getName(), is("7-Zip"));

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient, attachmentClient, user);

        assertExpectedComponentsInDb();
    }


    @Test
    public void testImportTwiceIsANoOp() throws Exception {
        FluentIterable<ComponentCSVRecord> compCSVRecords = getCompCSVRecordsFromTestFile(fileName);

        assertThat(componentClient.getComponentSummary(user), hasSize(0));
        assertThat(componentClient.getReleaseSummary(user), hasSize(0));
        assertThat(attachmentContentRepository.getAll(), Matchers.hasSize(0) );

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient, attachmentClient, user);
        assertThat(attachmentContentRepository.getAll(), Matchers.hasSize(1) );
        List<Component> componentSummaryAfterFirst = componentClient.getComponentSummary(user);
        List<Release> releaseSummaryAfterFirst = componentClient.getReleaseSummary(user);

        assertExpectedComponentsInDb();

        ComponentImportUtils.writeToDatabase(compCSVRecords, componentClient, vendorClient, attachmentClient, user);
        assertExpectedComponentsInDb();
        assertThat(attachmentContentRepository.getAll(), Matchers.hasSize(1) );
        assertThat(componentClient.getComponentSummary(user), is(componentSummaryAfterFirst));
        assertThat(componentClient.getReleaseSummary(user), is(releaseSummaryAfterFirst));

    }

    private void assertExpectedComponentsInDb() throws TException {
        List<Component> importedComponents = componentClient.getComponentSummary(user);
        List<Release> importedReleases = componentClient.getReleaseSummary(user);

        assertThat(importedComponents, hasSize(7)); // see the test file
        assertThat(importedReleases, hasSize(8)); // see the test file

        sortByField(importedComponents, Component._Fields.NAME);
        sortByField(importedReleases, Release._Fields.VERSION);
        sortByField(importedReleases, Release._Fields.NAME);

        Component component = importedComponents.get(0);
        assertThat(component.getName(), is("7-Zip"));

        component = componentClient.getComponentById(component.getId(), user);
        assertThat(component.getName(), is("7-Zip"));
        assertThat(component.getHomepage(), is("http://commons.apache.org/proper/commons-exec"));
        assertThat(component.getVendorNames(), is(emptyOrNullCollectionOf(String.class)));
        assertThat(component.getAttachments(), is(emptyOrNullCollectionOf(Attachment.class)));
        assertThat(component.getCreatedBy(), equalTo(user.getEmail()));
        assertThat(component.getReleases(), is(not(nullValue())));
        assertThat(getReleaseIds(component.getReleases()), containsInAnyOrder(importedReleases.get(0).getId(), importedReleases.get(1).getId()));

        final Release release = importedReleases.get(4);
        assertThat(release.getVersion(), is("1.2.11"));
        //This release has an download url so the import creates an attachmen
        final Set<Attachment> attachments = release.getAttachments();
        assertThat(attachments.size(), is(1));
        final Attachment theAttachment = getFirst(attachments);
        final String attachmentContentId = theAttachment.getAttachmentContentId();

        final AttachmentContent attachmentContent = attachmentClient.getAttachmentContent(attachmentContentId);

        assertThat(attachmentContent.isOnlyRemote(), is(true));

        assertThat(attachmentContent.getRemoteUrl(), is(REMOTE_URL));


    }

}