/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.AttachmentInputStream;
import org.ektorp.DocumentNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentStreamConnectorTest {
    @Mock
    public DatabaseConnector connector;
    @Mock
    private AttachmentContentDownloader attachmentContentDownloader;

    private User dummyUser = new User().setEmail("dummy@some.domain");

    AttachmentStreamConnector attachmentStreamConnector;

    @Before
    public void setUp() throws Exception {
        attachmentStreamConnector = new AttachmentStreamConnector(connector, attachmentContentDownloader, durationOf(5, TimeUnit.SECONDS));
    }

    @Test
    public void testTryingToDownloadIfNotAvailable() throws Exception {
        String id = "11";
        String filename = "filename";
        AttachmentContent attachment = mock(AttachmentContent.class);
        when(attachment.isOnlyRemote()).thenReturn(true);
        when(attachment.getId()).thenReturn(id);
        when(attachment.getFilename()).thenReturn(filename);

        InputStream downloadUrlStream = mock(InputStream.class);
        InputStream returnedStream = mock(InputStream.class);

        AttachmentContent rereadAttachment = mock(AttachmentContent.class);
        when(rereadAttachment.getId()).thenReturn(id);
        when(rereadAttachment.getFilename()).thenReturn(filename);

        attachmentStreamConnector = spy(attachmentStreamConnector);
        doReturn(returnedStream).when(attachmentStreamConnector).readAttachmentStream(rereadAttachment);
        doNothing().when(attachmentStreamConnector).uploadAttachmentPart(attachment, 1, downloadUrlStream);

        when(attachmentContentDownloader.download(eq(attachment), Matchers.any(Duration.class))).thenReturn(downloadUrlStream);

        when(connector.get(AttachmentContent.class, id)).thenReturn(rereadAttachment);
        doReturn(rereadAttachment).when(rereadAttachment).setOnlyRemote(anyBoolean());

        assertThat(attachmentStreamConnector.getAttachmentStream(attachment, dummyUser,
                     new Project()
                             .setVisbility(Visibility.ME_AND_MODERATORS)
                             .setCreatedBy(dummyUser.getEmail())
                             .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(id)))),
                sameInstance(returnedStream));

        verify(attachmentContentDownloader).download(eq(attachment), Matchers.any(Duration.class));
        verify(attachmentStreamConnector).uploadAttachment(attachment, downloadUrlStream);
        verify(attachmentStreamConnector).readAttachmentStream(rereadAttachment);

        verify(rereadAttachment).setOnlyRemote(false);
        verify(connector).update(rereadAttachment);
    }

    @Test
    public void testGetFullStream() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);
        when(attachment.isOnlyRemote()).thenReturn(false);
        when(attachment.isSetPartsCount()).thenReturn(false);

        when(attachment.getFilename()).thenReturn("fil");

        String attachmentId = "id";
        when(attachment.getId()).thenReturn(attachmentId);

        AttachmentInputStream full = mock(AttachmentInputStream.class);
        when(connector.getAttachment(attachmentId, "fil")).thenReturn(full);

        when(full.read()).thenReturn(1, 2, -1);
        InputStream attachmentStream = attachmentStreamConnector.getAttachmentStream(attachment, dummyUser,
                new Project()
                        .setVisbility(Visibility.ME_AND_MODERATORS)
                        .setCreatedBy(dummyUser.getEmail())
                        .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachmentId))));

        assertThat(attachmentStream.read(), is(1));
        assertThat(attachmentStream.read(), is(2));
        assertThat(attachmentStream.read(), is(-1));
    }

    @Test
    public void testGetConcatenatedStream() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);
        when(attachment.isOnlyRemote()).thenReturn(false);

        when(attachment.isSetPartsCount()).thenReturn(true);
        when(attachment.getPartsCount()).thenReturn("2");

        when(attachment.getFilename()).thenReturn("fil");

        String attachmentId = "id";
        when(attachment.getId()).thenReturn(attachmentId);

        AttachmentInputStream part1 = mock(AttachmentInputStream.class);
        when(connector.getAttachment(attachmentId, "fil_part1")).thenReturn(part1);

        AttachmentInputStream part2 = mock(AttachmentInputStream.class);
        when(connector.getAttachment(attachmentId, "fil_part2")).thenReturn(part2);

        when(part1.read()).thenReturn(1, -1);
        when(part2.read()).thenReturn(2, -1);
        InputStream attachmentStream = attachmentStreamConnector.getAttachmentStream(attachment, dummyUser,
                new Project()
                        .setVisbility(Visibility.ME_AND_MODERATORS)
                        .setCreatedBy(dummyUser.getEmail())
                        .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachmentId))));

        verifyZeroInteractions(part2);
        assertThat(attachmentStream.read(), is(1));
        assertThat(attachmentStream.read(), is(2));
        verify(part1).close();
        assertThat(attachmentStream.read(), is(-1));
        verify(part2).close();
    }

    @Test
    public void testGetConcatenatedStreamReadThrowsOnNonExistent() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);
        when(attachment.isOnlyRemote()).thenReturn(false);

        when(attachment.isSetPartsCount()).thenReturn(true);
        when(attachment.getPartsCount()).thenReturn("2");

        when(attachment.getFilename()).thenReturn("fil");

        String attachmentId = "id";
        when(attachment.getId()).thenReturn(attachmentId);

        AttachmentInputStream part1 = mock(AttachmentInputStream.class);
        when(connector.getAttachment(attachmentId, "fil_part1")).thenReturn(part1);

        when(connector.getAttachment(attachmentId, "fil_part2")).thenThrow(new DocumentNotFoundException(""));

        when(part1.read()).thenReturn(1, -1);
        InputStream attachmentStream = attachmentStreamConnector.getAttachmentStream(attachment, dummyUser,
                new Project()
                        .setVisbility(Visibility.ME_AND_MODERATORS)
                        .setCreatedBy(dummyUser.getEmail())
                        .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachmentId))));

        assertThat(attachmentStream.read(), is(1));

        try {
            assertThat(attachmentStream.read(), is(2));
            fail("expected Exception not thrown");
        } catch (IOException ignored) {

        }

        verify(part1).close();
    }

}
