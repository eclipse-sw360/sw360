/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentConnectorTest {

    @Mock
    DatabaseConnector connector;

    AttachmentConnector attachmentConnector;

    @Before
    public void setUp() throws Exception {
        attachmentConnector = new AttachmentConnector(connector, durationOf(5, TimeUnit.SECONDS));
    }

    @Test
    public void testDeleteAttachmentsDifference() throws Exception {
        Attachment a1 = mock(Attachment.class);
        when(a1.getAttachmentContentId()).thenReturn("a1cid");
        when(a1.getSha1()).thenReturn(null);
        when(a1.isSetSha1()).thenReturn(false);
        Attachment a2 = mock(Attachment.class);
        when(a2.getAttachmentContentId()).thenReturn("a2cid");
        when(a2.getSha1()).thenReturn(null);
        when(a2.isSetSha1()).thenReturn(false);

        Set<Attachment> before = new HashSet<>();
        before.add(a1);
        before.add(a2);

        Attachment a3 = mock(Attachment.class);
        when(a3.getAttachmentContentId()).thenReturn("a1cid");
        when(a3.getSha1()).thenReturn("sha1");
        when(a3.isSetSha1()).thenReturn(true);

        Set<Attachment> after = new HashSet<>();
        after.add(a3);

        Set<String> deletedIds = new HashSet<>();
        deletedIds.add("a2cid");

        attachmentConnector.deleteAttachmentDifference(before, after);
        verify(connector).deleteIds(deletedIds, AttachmentContent.class);
    }

    @Test
    public void testDeleteAttachmentsDifferenceOnlyNonAcceptedIsDeleted() throws Exception {
        Attachment a1 = mock(Attachment.class);
        when(a1.getAttachmentContentId()).thenReturn("a1");

        Attachment a2 = mock(Attachment.class);
        when(a2.getAttachmentContentId()).thenReturn("a2");
        when(a2.getCheckStatus()).thenReturn(CheckStatus.REJECTED);

        Attachment a3 = mock(Attachment.class);
        when(a3.getAttachmentContentId()).thenReturn("a3");
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
        verify(connector).deleteIds(expectedIdsToDelete, AttachmentContent.class);
    }


}
