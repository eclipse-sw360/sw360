/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class AttachmentConnectorTest {

    @Autowired
    Cloudant client;

    @Autowired
    AttachmentConnector attachmentConnector;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void setUp() throws Exception {
        attachmentConnector.setDownloadTimeout(durationOf(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testDeleteAttachmentsDifference() throws Exception {
        Attachment a1 = mock(Attachment.class);
        when(a1.getAttachmentContentId()).thenReturn("a1cid");
        Assert.assertNull(a1.getSha1());
        Assert.assertEquals(false, a1.isSetSha1());

        Attachment a2 = mock(Attachment.class);
        when(a2.getAttachmentContentId()).thenReturn("a2cid");
        Assert.assertNull(a2.getSha1());
        Assert.assertEquals(false, a2.isSetSha1());

        Set<Attachment> before = new HashSet<>();
        before.add(a1);
        before.add(a2);

        Attachment a3 = mock(Attachment.class);
        when(a3.getAttachmentContentId()).thenReturn("a1cid");
        Assert.assertNull(a3.getSha1());
        Assert.assertEquals(false, a3.isSetSha1());

        Set<Attachment> after = new HashSet<>();
        after.add(a3);

        Set<String> deletedIds = new HashSet<>();
        deletedIds.add("a2cid");

        attachmentConnector.deleteAttachmentDifference(before, after);
    }

    @Test
    public void testDeleteAttachmentsDifferenceOnlyNonAcceptedIsDeleted() throws Exception {
        Attachment a1 = mock(Attachment.class);
        when(a1.getAttachmentContentId()).thenReturn("a1");

        Attachment a2 = mock(Attachment.class);
        when(a2.getAttachmentContentId()).thenReturn("a2");
        when(a2.getCheckStatus()).thenReturn(CheckStatus.REJECTED);

        Attachment a3 = mock(Attachment.class);
        lenient().when(a3.getAttachmentContentId()).thenReturn("a3");
        when(a3.getCheckStatus()).thenReturn(CheckStatus.ACCEPTED);

        Set<Attachment> before = new HashSet<>();
        before.add(a1);
        before.add(a2);
        before.add(a3);

        Set<Attachment> after = new HashSet<>();
        after.add(a1);

        Set<String> expectedIdsToDelete = new HashSet<>();
        expectedIdsToDelete.add("a2");

        attachmentConnector.deleteAttachmentDifference(before, after);
    }
}
