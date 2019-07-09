/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.handler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.ssh.FossologyUploader;

import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FossologyFileHandlerTest {
    final String clearingTeam = "clearingId";
    final String releaseId = "id";

    FossologyFileHandler fossologyFileHandler;

    @Mock
    private AttachmentConnector attachmentConnector;
    @Mock
    private DatabaseConnector databaseConnector;
    @Mock
    private FossologyUploader fossologyUploader;
    @Mock
    private ComponentService.Iface componentService;

    private User user;

    @Before
    public void setUp() throws TException {
        ThriftClients thriftClients = TestUtils.failingMock(ThriftClients.class);
        doReturn(componentService).when(thriftClients).makeComponentClient();

        user = TestUtils.getAdminUser(getClass());

        fossologyFileHandler = spy(new FossologyFileHandler(attachmentConnector, fossologyUploader, thriftClients));
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(attachmentConnector, databaseConnector, fossologyUploader, componentService);
    }

    @Test
    public void testSendToFossologyWithNotExistentReleaseIsError() throws Exception {
        try {
            assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.FAILURE));
        } catch (SW360Exception e) {
            assertThat(e, instanceOf(SW360Exception.class));
        }
        verify(componentService).getReleaseById(releaseId, user);
    }

    @Test
    public void testSendToFossologyWithNoSourcesIsSuccess() throws Exception {
        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);

        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.FAILURE));

        verify(componentService).getReleaseById(releaseId, user);
        verify(componentService).getSourceAttachments(releaseId);
    }

    @Test
    public void testSendToFossologyDuplicates() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");
        final Attachment attachment = filledAttachment.getAttachment();
        doReturn(filledAttachment).when(fossologyFileHandler).fillAttachment(attachment);

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);
        when(componentService.getSourceAttachments(releaseId)).thenReturn(ImmutableSet.of(attachment));

        Set<ExternalToolRequest> etrs = new HashSet<>();
        etrs.add(mockExternalToolRequestInRelease(release, clearingTeam, attachment.getAttachmentContentId(),
                attachment.getSha1()));
        when(release.getExternalToolRequests()).thenReturn(etrs);
        when(release.getClearingState()).thenReturn(ClearingState.SENT_TO_FOSSOLOGY);

        when(fossologyUploader.updateStatusInFossologyRequest(any()))
                .thenReturn(ExternalToolWorkflowStatus.ACCESS_DENIED);
        when(fossologyUploader.duplicateInFossology(41, clearingTeam)).thenReturn(true);


        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.SUCCESS));


        verify(fossologyUploader)
                .updateStatusInFossologyRequest(eq(release.getExternalToolRequests().iterator().next()));
        verify(fossologyUploader).duplicateInFossology(41, clearingTeam);

        // never, because the new upload cannot affect the overall state yet (directly
        // after upload)
        verify(release, never()).setClearingState(any());

        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(componentService).getSourceAttachments(releaseId);
        verify(componentService).updateReleaseFossology(release, user);
    }

    @Test
    public void testSendToFossologyDuplicateDoesNotUpdateTheStatusOnErrors() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");
        final Attachment attachment = filledAttachment.getAttachment();
        doReturn(filledAttachment).when(fossologyFileHandler).fillAttachment(attachment);

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);
        when(componentService.getSourceAttachments(releaseId)).thenReturn(ImmutableSet.of(attachment));

        Set<ExternalToolRequest> etrs = new HashSet<>();
        etrs.add(mockExternalToolRequestInRelease(release, clearingTeam, attachment.getAttachmentContentId(),
                attachment.getSha1()));
        when(release.getExternalToolRequests()).thenReturn(etrs);

        when(fossologyUploader.updateStatusInFossologyRequest(any()))
                .thenReturn(ExternalToolWorkflowStatus.ACCESS_DENIED);
        when(fossologyUploader.duplicateInFossology(41, clearingTeam)).thenReturn(false);


        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.FAILURE));


        verify(fossologyUploader)
                .updateStatusInFossologyRequest(eq(release.getExternalToolRequests().iterator().next()));
        verify(fossologyUploader).duplicateInFossology(41, clearingTeam);

        // never, because the duplicateInFossology call fails
        verify(release, never()).setClearingState(any());

        verify(componentService).updateReleaseFossology(release, user);
        verify(componentService).getSourceAttachments(releaseId);
        verify(componentService).getReleaseById(releaseId, user);
    }

    @Test
    public void testSendToFossologySendsAnAttachment() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");
        final AttachmentContent attachmentContent = filledAttachment.getAttachmentContent();
        final Attachment attachment = filledAttachment.getAttachment();
        doReturn(filledAttachment).when(fossologyFileHandler).fillAttachment(attachment);

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);
        when(componentService.getSourceAttachments(releaseId)).thenReturn(ImmutableSet.of(attachment));

        final InputStream inputStream = mock(InputStream.class);
        when(attachmentConnector.getAttachmentStream(attachmentContent, user, release)).thenReturn(inputStream);
        when(fossologyUploader.uploadToFossology(inputStream, attachmentContent, clearingTeam)).thenReturn(1);


        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.SUCCESS));


        verify(inputStream).close();

        // the release should be updated
        verify(componentService).updateReleaseFossology(release, user);

        verify(release).addToExternalToolRequests(notNull(ExternalToolRequest.class));
        verify(release).setClearingState(any());
        // new upload does not have to check previous state
        verify(release, never()).getClearingState();

        // unimportant verifies
        verify(componentService, times(1)).getReleaseById(releaseId, user);
        verify(componentService).getSourceAttachments(releaseId);
        verify(attachmentConnector).getAttachmentStream(attachmentContent, user, release);
        verify(fossologyUploader).uploadToFossology(inputStream, attachmentContent, clearingTeam);
    }

    @Test
    public void testAFailedSendToFossology() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");
        final AttachmentContent attachmentContent = filledAttachment.getAttachmentContent();
        final Attachment attachment = filledAttachment.getAttachment();
        doReturn(filledAttachment).when(fossologyFileHandler).fillAttachment(attachment);

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);
        when(componentService.getSourceAttachments(releaseId)).thenReturn(ImmutableSet.of(attachment));

        final InputStream inputStream = mock(InputStream.class);
        when(attachmentConnector.getAttachmentStream(attachmentContent, user, release)).thenReturn(inputStream);
        when(fossologyUploader.uploadToFossology(inputStream, attachmentContent, clearingTeam)).thenReturn(-1);


        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.FAILURE));


        verify(inputStream).close();
        verify(release, never()).setClearingState(any());
        verify(componentService, never()).updateReleaseFossology(release, user);

        // unimportant verifies
        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(componentService).getSourceAttachments(releaseId);
        verify(attachmentConnector).getAttachmentStream(attachmentContent, user, release);
        verify(fossologyUploader).uploadToFossology(inputStream, attachmentContent, clearingTeam);
    }

    @Test
    public void testGetStatusInFossology() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");
        final Attachment attachment = filledAttachment.getAttachment();

        final Release release = mock(Release.class);
        final Release updated = mock(Release.class);

        when(componentService.getReleaseById(releaseId, user)).thenReturn(release, updated);
        when(componentService.getSourceAttachments(releaseId)).thenReturn(ImmutableSet.of(attachment));

        String clearingTeam2 = "anotherClearingTeam";
        // external tool id is 41
        ExternalToolRequest etr1 = mockExternalToolRequestInRelease(release, clearingTeam,
                attachment.getAttachmentContentId(), attachment.getSha1());
        etr1.setExternalToolStatus(ExternalToolStatus.IN_PROGRESS);
        ExternalToolRequest etr2 = mockExternalToolRequestInRelease(release, clearingTeam2,
                attachment.getAttachmentContentId(), attachment.getSha1());
        etr2.setExternalToolStatus(ExternalToolStatus.OPEN);
        Set<ExternalToolRequest> etrs = Sets.newHashSet(etr1, etr2);
        when(release.getExternalToolRequests()).thenReturn(etrs);

        when(fossologyUploader.updateStatusInFossologyRequest(any())).thenReturn(ExternalToolWorkflowStatus.SENT);


        final Release resultRelease = fossologyFileHandler.getStatusInFossology(releaseId, user, clearingTeam);


        assertThat(resultRelease, is(sameInstance(release)));

        verify(release).setClearingState(ClearingState.UNDER_CLEARING);

        verify(fossologyUploader).updateStatusInFossologyRequest(etr1);
        verify(fossologyUploader).updateStatusInFossologyRequest(etr2);

        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(componentService).updateReleaseFossology(release, user);
    }

    @Test
    public void testGetStatusInFossologyDoesNotUpdateInDatabaseOnConnectionFailures() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");
        final Attachment attachment = filledAttachment.getAttachment();

        final Release release = mock(Release.class);

        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);
        when(componentService.getSourceAttachments(releaseId)).thenReturn(ImmutableSet.of(attachment));

        String clearingTeam2 = "anotherClearingTeam";
        // external tool id is 41
        ExternalToolRequest etr1 = mockExternalToolRequestInRelease(release, clearingTeam,
                attachment.getAttachmentContentId(), attachment.getSha1());
        etr1.setExternalToolStatus(ExternalToolStatus.IN_PROGRESS);
        ExternalToolRequest etr2 = mockExternalToolRequestInRelease(release, clearingTeam2,
                attachment.getAttachmentContentId(), attachment.getSha1());
        etr2.setExternalToolStatus(ExternalToolStatus.OPEN);
        Set<ExternalToolRequest> etrs = Sets.newHashSet(etr1, etr2);
        when(release.getExternalToolRequests()).thenReturn(etrs);

        when(fossologyUploader.updateStatusInFossologyRequest(eq(etr1))).thenReturn(ExternalToolWorkflowStatus.SENT);
        when(fossologyUploader.updateStatusInFossologyRequest(eq(etr2)))
                .thenReturn(ExternalToolWorkflowStatus.CONNECTION_FAILED);


        final Release resultRelease = fossologyFileHandler.getStatusInFossology(releaseId, user, clearingTeam);


        assertThat(resultRelease, is(sameInstance(release)));

        // clearing state gets set, but release will not be saved
        // (updateReleaseFossology is not called)
        verify(release).setClearingState(any());

        verify(fossologyUploader).updateStatusInFossologyRequest(etr1);
        verify(fossologyUploader).updateStatusInFossologyRequest(etr2);

        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(componentService, never()).updateReleaseFossology(release, user);
    }

    @Test
    public void testNotUnlockingForNoClearing() throws Exception {
        Release release = mock(Release.class);
        doReturn(release).when(componentService).getReleaseById(releaseId, user);


        fossologyFileHandler.getReleaseAndUnlockIt(releaseId, user, componentService);


        verify(componentService).getReleaseById(releaseId, user);
        verify(componentService, never()).updateReleaseFossology(release, user);
        verify(release).getExternalToolRequests();
        verifyNoMoreInteractions(release);
    }

    @Test
    public void testUnlocking() throws Exception {
        Release release = mock(Release.class);
        doReturn(release).when(componentService).getReleaseById(releaseId, user);

        String clearingTeam2 = "anotherClearingTeam";
        // external tool id is 41
        ExternalToolRequest etr1 = mockExternalToolRequestInRelease(release, clearingTeam, "1", "2");
        etr1.setExternalToolStatus(ExternalToolStatus.REJECTED);
        ExternalToolRequest etr2 = mockExternalToolRequestInRelease(release, clearingTeam2, "1", "2");
        etr2.setExternalToolStatus(ExternalToolStatus.REJECTED);
        Set<ExternalToolRequest> etrs = Sets.newHashSet(etr1, etr2);
        when(release.getExternalToolRequests()).thenReturn(etrs);
        when(fossologyUploader.updateStatusInFossologyRequest(any())).thenReturn(ExternalToolWorkflowStatus.SENT);


        fossologyFileHandler.getReleaseAndUnlockIt(releaseId, user, componentService);


        verify(componentService).getReleaseById(releaseId, user);
        verify(componentService).updateReleaseFossology(release, user);
        verify(release).unsetExternalToolRequests();
    }

    @Test
    public void testNotUnlockingIfATeamIsWorkingOnIt() throws Exception {
        Release release = mock(Release.class);
        doReturn(release).when(componentService).getReleaseById(releaseId, user);

        String clearingTeam2 = "anotherClearingTeam";
        // external tool id is 41
        ExternalToolRequest etr1 = mockExternalToolRequestInRelease(release, clearingTeam, "1", "2");
        etr1.setExternalToolStatus(ExternalToolStatus.OPEN);
        ExternalToolRequest etr2 = mockExternalToolRequestInRelease(release, clearingTeam2, "1", "2");
        etr2.setExternalToolStatus(ExternalToolStatus.REJECTED);
        Set<ExternalToolRequest> etrs = Sets.newHashSet(etr1, etr2);
        when(release.getExternalToolRequests()).thenReturn(etrs);
        when(fossologyUploader.updateStatusInFossologyRequest(any())).thenReturn(ExternalToolWorkflowStatus.SENT);


        fossologyFileHandler.getReleaseAndUnlockIt(releaseId, user, componentService);


        verify(componentService).getReleaseById(releaseId, user);
        verify(componentService, never()).updateReleaseFossology(release, user);
        verify(release).getExternalToolRequests();
        verifyNoMoreInteractions(release);
    }

    private static FilledAttachment getMockFilledAttachment(String attachmentContentId) {
        final FilledAttachment filledAttachment = mock(FilledAttachment.class);
        final Attachment attachment = mock(Attachment.class);
        when(filledAttachment.getAttachment()).thenReturn(attachment);
        final AttachmentContent attachmentContent = mock(AttachmentContent.class);
        when(filledAttachment.getAttachmentContent()).thenReturn(attachmentContent);
        when(attachment.getAttachmentContentId()).thenReturn(attachmentContentId);
        when(attachmentContent.getId()).thenReturn(attachmentContentId);
        return filledAttachment;
    }

    private ExternalToolRequest mockExternalToolRequestInRelease(Release release, String clearingTeam,
            String attachmentContentId, String hash) {
        ExternalToolRequest etr = new ExternalToolRequest();

        etr.setExternalTool(ExternalTool.FOSSOLOGY);
        etr.setToolId("41");
        etr.setToolUserGroup(clearingTeam);
        etr.setAttachmentId(attachmentContentId);
        etr.setAttachmentHash(hash);
        etr.setExternalToolStatus(ExternalToolStatus.OPEN);

        return etr;
    }
}