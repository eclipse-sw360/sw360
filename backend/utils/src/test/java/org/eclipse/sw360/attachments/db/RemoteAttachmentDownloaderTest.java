/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.attachments.db;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.AttachmentContentRepository;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.eclipse.sw360.attachments.db.RemoteAttachmentDownloader.length;
import static org.eclipse.sw360.attachments.db.RemoteAttachmentDownloader.retrieveRemoteAttachments;
import static org.eclipse.sw360.datahandler.TestUtils.*;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

/**
 * @author daniele.fognini@tngtech.com
 */
public class RemoteAttachmentDownloaderTest {

    private static final String url = DatabaseSettingsTest.COUCH_DB_URL;
    private static final String dbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;

    private AttachmentConnector attachmentConnector;
    private AttachmentContentRepository repository;

    private List<String> garbage;
    private Duration downloadTimeout = Duration.durationOf(5, TimeUnit.SECONDS);

    private User dummyUser = new User().setEmail("dummy@some.domain");

    @BeforeClass
    public static void setUpClass() throws Exception {
        assertTestString(dbName);
        deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Before
    public void setUp() throws Exception {
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        attachmentConnector = new AttachmentConnector(DatabaseSettingsTest.getConfiguredClient(), dbName, downloadTimeout);
        repository = new AttachmentContentRepository(databaseConnector);

        garbage = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : garbage) {
            repository.remove(id);
        }
    }

    @Test
    public void testIntegration() throws Exception {
        AttachmentContent attachmentContent = saveRemoteAttachment(url);

        assertEquals(1, retrieveRemoteAttachments(DatabaseSettingsTest.getConfiguredClient(), dbName, downloadTimeout));

        assertTrue(hasLength(greaterThan(0L)).matches(
                attachmentConnector.getAttachmentStream(attachmentContent, dummyUser,
                        new Project()
                                .setVisbility(Visibility.ME_AND_MODERATORS)
                                .setCreatedBy(dummyUser.getEmail())
                                .setAttachments(Collections.singleton(
                                        new Attachment().setAttachmentContentId(attachmentContent.getId())
                                ))
                )
        ));

        assertEquals(0, retrieveRemoteAttachments(DatabaseSettingsTest.getConfiguredClient(), dbName, downloadTimeout));
    }

    @Test
    public void testWithBlackHole() throws Exception {
        assumeThat(getAvailableNetworkInterface(), isAvailable());

        saveRemoteAttachment("http://" + BLACK_HOLE_ADDRESS + "/filename");
        assertEquals(1, repository.getOnlyRemoteAttachments().size());

        ExecutorService executor = newSingleThreadExecutor();

        try {
            Future<Integer> future = executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return retrieveRemoteAttachments(DatabaseSettingsTest.getConfiguredClient(), dbName, downloadTimeout);
                }
            });

            try {
                Integer downloadedSuccessfully = future.get(1, TimeUnit.MINUTES);
                assertEquals(0, downloadedSuccessfully.intValue());
            } catch (TimeoutException e) {
                fail("retriever got stuck on a black hole");
                throw e; // unreachable
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }

        assertEquals(1, repository.getOnlyRemoteAttachments().size());
    }

    @Test
    public void testWithBrokenURL() throws Exception {
        AttachmentContent attachmentContent = saveRemoteAttachment(TestUtils.BLACK_HOLE_ADDRESS);
        AttachmentContent attachmentGood = saveRemoteAttachment(url);

        assertEquals(2, repository.getOnlyRemoteAttachments().size());
        assertEquals(1, retrieveRemoteAttachments(DatabaseSettingsTest.getConfiguredClient(), dbName, downloadTimeout));
        assertEquals(1, repository.getOnlyRemoteAttachments().size());

        assertTrue(hasLength(greaterThan(0L)).matches(
                attachmentConnector.getAttachmentStream(attachmentGood, dummyUser,
                        new Project()
                                .setVisbility(Visibility.ME_AND_MODERATORS)
                                .setCreatedBy(dummyUser.getEmail())
                                .setAttachments(Collections.singleton(
                                        new Attachment().setAttachmentContentId(attachmentGood.getId())
                                ))
                )
        ));

        assertEquals(1, repository.getOnlyRemoteAttachments().size());
        assertEquals(0, retrieveRemoteAttachments(DatabaseSettingsTest.getConfiguredClient(), dbName, downloadTimeout));
        assertEquals(1, repository.getOnlyRemoteAttachments().size());

        try {
            assertTrue(attachmentConnector.getAttachmentStream(attachmentContent, dummyUser,
                new Project()
                        .setVisbility(Visibility.ME_AND_MODERATORS)
                        .setCreatedBy(dummyUser.getEmail())
                        .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachmentContent.getId()))))
                .available() > 0L);
            fail("expected exception not thrown");
        } catch (SW360Exception e) {
            assertTrue(e.getWhy().contains(attachmentContent.getId()));
        }
    }

    private AttachmentContent saveRemoteAttachment(String remoteUrl) throws SW360Exception {
        AttachmentContent attachmentContent = new AttachmentContent()
                .setFilename("testfile")
                .setContentType("text")
                .setOnlyRemote(true)
                .setRemoteUrl(remoteUrl);

        repository.add(attachmentContent);

        garbage.add(attachmentContent.getId());

        return attachmentContent;
    }

    private static Matcher<InputStream> hasLength(final Matcher<Long> lengthMatcher) {
        return new TypeSafeMatcher<InputStream>() {
            @Override
            protected boolean matchesSafely(InputStream item) {
                try {
                    return lengthMatcher.matches(length(item));
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has Length: ");
                lengthMatcher.describeTo(description);
            }
        };
    }
}
