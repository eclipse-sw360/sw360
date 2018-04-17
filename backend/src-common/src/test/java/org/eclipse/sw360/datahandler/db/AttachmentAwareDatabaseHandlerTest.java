/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AttachmentAwareDatabaseHandlerTest {

    AttachmentAwareDatabaseHandler handler;

    @Before
    public void setUp() throws Exception {

        handler = new AttachmentAwareDatabaseHandler() {
            @Override
            public Set<Attachment> getAllAttachmentsToKeep(Set<Attachment> originalAttachments, Set<Attachment> changedAttachments) {
                return super.getAllAttachmentsToKeep(originalAttachments, changedAttachments);
            }
        };
    }

    @Test
    public void testGetAllAttachmentsToKeep() throws Exception {

        // Try to delete one approved and one rejected attachment.
        //  -> the approved one should not be deleted.
        Attachment attachmentAccepted = new Attachment().setAttachmentContentId("acceptedAtt").setFilename("att1.file").setCheckStatus(CheckStatus.ACCEPTED);
        Attachment attachmentRejected = new Attachment().setAttachmentContentId("rejectedAtt").setFilename("att2.file").setCheckStatus(CheckStatus.REJECTED);
        Set<Attachment> attachmentsBefore = new HashSet<>();
        attachmentsBefore.add(attachmentAccepted);
        attachmentsBefore.add(attachmentRejected);
        Set<Attachment> attachmentsAfter = new HashSet<>();

        Set<Attachment> attachmentsToKeep = handler.getAllAttachmentsToKeep(attachmentsBefore, attachmentsAfter);

        assertEquals(1, attachmentsToKeep.size());
        assertTrue(attachmentsToKeep.contains(attachmentAccepted));
        assertFalse(attachmentsToKeep.contains(attachmentRejected));

        // Change an attachment
        //  -> it should not be stored twice
        Attachment originalAttachment = new Attachment().setAttachmentContentId("att").setFilename("att.file").setCheckStatus(CheckStatus.ACCEPTED);
        attachmentsBefore = new HashSet<>();
        attachmentsBefore.add(originalAttachment);

        Attachment changedAttachment = originalAttachment.deepCopy().setCheckStatus(CheckStatus.REJECTED);
        attachmentsAfter = new HashSet<>();
        attachmentsAfter.add(changedAttachment);

        attachmentsToKeep = handler.getAllAttachmentsToKeep(attachmentsBefore, attachmentsAfter);

        assertEquals(1, attachmentsToKeep.size());
        assertTrue(attachmentsToKeep.contains(changedAttachment));
    }

    @Test
    public void testGetAllAttachmentsToKeepCanHandleNull() throws Exception {

        // Test what happens if `changedAttachments` are `null`
        //  ->  only not-accepted attachments should be deleted
        Attachment attachmentAccepted1 = new Attachment().setAttachmentContentId("acceptedAtt1").setFilename("att1.file").setCheckStatus(CheckStatus.ACCEPTED);
        Attachment attachmentRejected1 = new Attachment().setAttachmentContentId("rejectedAtt1").setFilename("att2.file").setCheckStatus(CheckStatus.REJECTED);
        Attachment attachmentRejected2 = new Attachment().setAttachmentContentId("rejectedAtt2").setFilename("att3.file").setCheckStatus(CheckStatus.NOTCHECKED);

        Set<Attachment> attachments = new HashSet<>();
        attachments.add(attachmentAccepted1);
        attachments.add(attachmentRejected1);
        attachments.add(attachmentRejected2);

        Set<Attachment> attachmentsToKeep = handler.getAllAttachmentsToKeep(attachments, null);
        assertEquals(1, attachmentsToKeep.size());
        assertTrue(attachmentsToKeep.contains(attachmentAccepted1));
        assertFalse(attachmentsToKeep.contains(attachmentRejected1));
        assertFalse(attachmentsToKeep.contains(attachmentRejected2));

        // Test what happens if `originalAttachments` are `null`  (this means adding of attachments)
        //  ->  all should be added
        attachmentsToKeep = handler.getAllAttachmentsToKeep(null, attachments);
        assertEquals(3, attachmentsToKeep.size());
        assertTrue(attachmentsToKeep.contains(attachmentAccepted1));
        assertTrue(attachmentsToKeep.contains(attachmentRejected1));
        assertTrue(attachmentsToKeep.contains(attachmentRejected2));
    }
}
