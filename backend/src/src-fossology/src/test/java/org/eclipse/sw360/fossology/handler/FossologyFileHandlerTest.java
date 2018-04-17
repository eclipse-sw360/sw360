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
package org.eclipse.sw360.fossology.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.FossologyStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.ssh.FossologyUploader;
import org.apache.thrift.TException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
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

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);

        when(release.getFossologyId()).thenReturn("41");
        when(release.isSetFossologyId()).thenReturn(true);
        when(release.getClearingState()).thenReturn(ClearingState.SENT_TO_FOSSOLOGY);

        spyGetFilledSourceAttachment(filledAttachment);

        when(fossologyUploader.duplicateInFossology(41, clearingTeam)).thenReturn(true);

        doNothing().when(fossologyFileHandler)
                .setFossologyStatus(eq(release), anyString(), any(FossologyStatus.class));

        doReturn(true).when(fossologyFileHandler).checkSourceAttachment(release, filledAttachment);

        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.SUCCESS));

        verify(fossologyUploader).duplicateInFossology(41, clearingTeam);
        verify(fossologyUploader).getStatusInFossology(eq(41), eq(clearingTeam));
        verify(fossologyFileHandler).setFossologyStatus(eq(release), eq(clearingTeam), eq(FossologyStatus.SENT));

        verify(componentService).updateReleaseFossology(release, user);
        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verifyNoMoreInteractions(attachmentConnector);
    }

    @Test
    public void testSendToFossologyDuplicateDoesNotUpdateTheStatusOnErrors() throws Exception {
        final FilledAttachment filledAttachment = getMockFilledAttachment("17");

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);

        when(release.getFossologyId()).thenReturn("41");
        when(release.isSetFossologyId()).thenReturn(true);

        spyGetFilledSourceAttachment(filledAttachment);

        when(fossologyUploader.duplicateInFossology(41, clearingTeam)).thenReturn(false);

        doNothing().when(fossologyFileHandler)
                .setFossologyStatus(eq(release), anyString(), any(FossologyStatus.class));
        doReturn(true).when(fossologyFileHandler).checkSourceAttachment(release, filledAttachment);

        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.FAILURE));

        verify(fossologyUploader).duplicateInFossology(41, clearingTeam);
        verify(fossologyFileHandler, never()).setFossologyStatus(eq(release), eq(clearingTeam), any(FossologyStatus.class));
        verify(fossologyUploader).getStatusInFossology(eq(41), eq(clearingTeam));

        verify(componentService, never()).updateReleaseFossology(release, user);
        verify(componentService).getReleaseById(releaseId, user);
        verifyNoMoreInteractions(attachmentConnector);
    }

    @Test
    public void testSendToFossologySendsAnAttachment() throws Exception {
        final String id = "41";
        final FilledAttachment filledAttachment = getMockFilledAttachment(id);
        final AttachmentContent attachmentContent = filledAttachment.getAttachmentContent();

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);

        spyGetFilledSourceAttachment(filledAttachment);

        final InputStream inputStream = mock(InputStream.class);
        when(release.isSetFossologyId()).thenReturn(false);
        when(release.getClearingState()).thenReturn(ClearingState.NEW_CLEARING);
        when(attachmentConnector.getAttachmentStream(attachmentContent, user, release)).thenReturn(inputStream);
        when(fossologyUploader.uploadToFossology(inputStream, attachmentContent, clearingTeam)).thenReturn(1);

        doNothing().when(fossologyFileHandler)
                .setFossologyStatus(eq(release), anyString(), eq(FossologyStatus.SENT), eq("" + 1), eq(id));
        doReturn(true).when(fossologyFileHandler).checkSourceAttachment(release, filledAttachment);

        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.SUCCESS));

        verify(inputStream).close();

        // the release should be updated
        verify(componentService).updateReleaseFossology(release, user);
        verify(fossologyFileHandler)
                .setFossologyStatus(eq(release), anyString(), eq(FossologyStatus.SENT), eq("" + 1), eq(id));

        // unimportant verifies
        verify(componentService, times(1)).getReleaseById(releaseId, user);
        verify(attachmentConnector).getAttachmentStream(attachmentContent, user, release);
        verify(fossologyUploader).uploadToFossology(inputStream, attachmentContent, clearingTeam);
    }

    private void spyGetFilledSourceAttachment(FilledAttachment filledAttachment) throws TException {
        final Attachment attachment = filledAttachment.getAttachment();
        doReturn(ImmutableSet.of(attachment)).when(fossologyFileHandler).getSourceAttachment(releaseId, user, componentService);
        doReturn(filledAttachment).when(fossologyFileHandler).fillAttachment(attachment);
    }

    @Test
    public void testAFailedSendToFossology() throws Exception {
        final String id = "41";
        final FilledAttachment filledAttachment = getMockFilledAttachment(id);
        final AttachmentContent attachmentContent = filledAttachment.getAttachmentContent();

        final Release release = mock(Release.class);
        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);

        spyGetFilledSourceAttachment(filledAttachment);

        final InputStream inputStream = mock(InputStream.class);
        when(release.isSetFossologyId()).thenReturn(false);
        when(attachmentConnector.getAttachmentStream(attachmentContent, user, release)).thenReturn(inputStream);
        when(fossologyUploader.uploadToFossology(inputStream, attachmentContent, clearingTeam)).thenReturn(-1);

        assertThat(fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam), is(RequestStatus.FAILURE));

        verify(inputStream).close();

        // unimportant verifies
        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(attachmentConnector).getAttachmentStream(attachmentContent, user, release);
        verify(fossologyUploader).uploadToFossology(inputStream, attachmentContent, clearingTeam);
    }

    @Test
    public void testGetStatusInFossology() throws Exception {
        final String id1 = "41";
        final FilledAttachment filledAttachment = getMockFilledAttachment(id1);

        final Release release = mock(Release.class);
        final Release updated = mock(Release.class);

        when(componentService.getReleaseById(releaseId, user)).thenReturn(release, updated);

        spyGetFilledSourceAttachment(filledAttachment);

        int fossologyId = 14;
        when(release.isSetFossologyId()).thenReturn(true);
        when(release.getFossologyId()).thenReturn("" + fossologyId);

        String clearingTeam2 = "anotherClearingTeam";

        when(fossologyUploader.getStatusInFossology(eq(fossologyId), eq(clearingTeam)))
                .thenReturn(FossologyStatus.IN_PROGRESS);
        when(fossologyUploader.getStatusInFossology(eq(fossologyId), eq(clearingTeam2)))
                .thenReturn(FossologyStatus.OPEN);

        doNothing().when(fossologyFileHandler)
                .setFossologyStatus(eq(release), eq(clearingTeam), eq(FossologyStatus.IN_PROGRESS), eq("" + fossologyId), eq(id1));

        doReturn(ImmutableList.of(clearingTeam, clearingTeam2))
                .when(fossologyFileHandler).getAllClearingTeams(release, clearingTeam);

        final Release resultRelease = fossologyFileHandler.getStatusInFossology(releaseId, user, clearingTeam);

        assertThat(resultRelease, is(sameInstance(release)));

        verify(fossologyFileHandler).setFossologyStatus(release, clearingTeam, FossologyStatus.IN_PROGRESS);
        verify(fossologyFileHandler).setFossologyStatus(release, clearingTeam2, FossologyStatus.OPEN);

        verify(fossologyUploader).getStatusInFossology(fossologyId, clearingTeam);
        verify(fossologyUploader).getStatusInFossology(fossologyId, clearingTeam2);

        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(componentService).updateReleaseFossology(release, user);
    }

    @Test
    public void testGetStatusInFossologyDoesNotUpdateInDatabaseOnConnectionFailures() throws Exception {
        final String id1 = "41";
        final FilledAttachment filledAttachment = getMockFilledAttachment(id1);

        final Release release = mock(Release.class);

        when(componentService.getReleaseById(releaseId, user)).thenReturn(release);

        spyGetFilledSourceAttachment(filledAttachment);

        int fossologyId = 14;
        when(release.isSetFossologyId()).thenReturn(true);
        when(release.getFossologyId()).thenReturn("" + fossologyId);

        String clearingTeam2 = "anotherClearingTeam";

        when(fossologyUploader.getStatusInFossology(eq(fossologyId), eq(clearingTeam)))
                .thenReturn(FossologyStatus.IN_PROGRESS);
        when(fossologyUploader.getStatusInFossology(eq(fossologyId), eq(clearingTeam2)))
                .thenReturn(FossologyStatus.CONNECTION_FAILED);

        doNothing().when(fossologyFileHandler)
                .setFossologyStatus(eq(release), eq(clearingTeam), eq(FossologyStatus.IN_PROGRESS), eq("" + fossologyId), eq(id1));

        doReturn(ImmutableList.of(clearingTeam, clearingTeam2))
                .when(fossologyFileHandler).getAllClearingTeams(release, clearingTeam);

        final Release resultRelease = fossologyFileHandler.getStatusInFossology(releaseId, user, clearingTeam);

        assertThat(resultRelease, is(sameInstance(release)));

        verify(fossologyFileHandler).setFossologyStatus(release, clearingTeam, FossologyStatus.IN_PROGRESS);
        verify(fossologyFileHandler).setFossologyStatus(release, clearingTeam2, FossologyStatus.CONNECTION_FAILED);

        verify(fossologyUploader).getStatusInFossology(fossologyId, clearingTeam);
        verify(fossologyUploader).getStatusInFossology(fossologyId, clearingTeam2);

        verify(componentService, atLeastOnce()).getReleaseById(releaseId, user);
        verify(componentService, never()).updateReleaseFossology(release, user);
    }

    @Test
    public void testNotUnlockingForNoClearing() throws Exception {
        Release release = mock(Release.class);
        doReturn(null).when(fossologyFileHandler).updateRelease(release, user, componentService);
        doReturn(release).when(componentService).getReleaseById(releaseId, user);

        fossologyFileHandler.getReleaseAndUnlockIt(releaseId, user, componentService);

        verify(fossologyFileHandler, never()).updateRelease(release, user, componentService);
        verify(componentService).getReleaseById(releaseId, user);
        verify(release).getClearingTeamToFossologyStatus();
        verifyNoMoreInteractions(release);
    }

    @Test
    public void testUnlocking() throws Exception {
        Release release = mock(Release.class);
        doReturn(null).when(fossologyFileHandler).updateRelease(release, user, componentService);
        doReturn(release).when(componentService).getReleaseById(releaseId, user);

        when(release.getClearingTeamToFossologyStatus()).thenReturn(ImmutableMap.of(
                "a", FossologyStatus.REJECTED,
                "b", FossologyStatus.REJECTED
        ));

        fossologyFileHandler.getReleaseAndUnlockIt(releaseId, user, componentService);

        verify(fossologyFileHandler).updateRelease(release, user, componentService);
        verify(componentService).getReleaseById(releaseId, user);
        verify(release).unsetAttachmentInFossology();
        verify(release).unsetFossologyId();
    }

    @Test
    public void testNotUnlockingIfATeamIsWorkingOnIt() throws Exception {
        Release release = mock(Release.class);
        doReturn(null).when(fossologyFileHandler).updateRelease(release, user, componentService);
        doReturn(release).when(componentService).getReleaseById(releaseId, user);

        when(release.getClearingTeamToFossologyStatus()).thenReturn(ImmutableMap.of(
                "a", FossologyStatus.OPEN,
                "b", FossologyStatus.REJECTED
        ));

        fossologyFileHandler.getReleaseAndUnlockIt(releaseId, user, componentService);

        verify(fossologyFileHandler, never()).updateRelease(release, user, componentService);
        verify(componentService).getReleaseById(releaseId, user);
        verify(release).getClearingTeamToFossologyStatus();
        verifyNoMoreInteractions(release);
    }

    @Test
    public void testGetAllClearingTeams() throws Exception {
        Release release = mock(Release.class);

        String clearingTeam2 = "team2";
        String clearingTeam3 = "yetAnotherTeam";

        when(release.getClearingTeamToFossologyStatus())
                .thenReturn(ImmutableMap.of(
                        clearingTeam, FossologyStatus.CLOSED,
                        clearingTeam2, FossologyStatus.ERROR
                ));

        assertThat(fossologyFileHandler.getAllClearingTeams(release, clearingTeam),
                containsInAnyOrder(clearingTeam, clearingTeam2));

        assertThat(fossologyFileHandler.getAllClearingTeams(release, clearingTeam3),
                containsInAnyOrder(clearingTeam, clearingTeam2, clearingTeam3));
    }

    @Test
    public void testUpdateFossologyStatusAddTheNewStatus() {
        Release release = mock(Release.class);
        final String clearingTeam2 = "team2";

        final Map<String, FossologyStatus> existingStatuses = newHashMap();
        existingStatuses.put(clearingTeam, FossologyStatus.SENT);
        existingStatuses.put(clearingTeam2, FossologyStatus.CLOSED);

        when(release.getClearingTeamToFossologyStatus()).thenReturn(existingStatuses);

        fossologyFileHandler.setFossologyStatus(release, clearingTeam, FossologyStatus.SCANNING);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, FossologyStatus> statusMap = (Map<String, FossologyStatus>) invocation.getArguments()[0];

                    assertThat(statusMap, hasEntry(clearingTeam, FossologyStatus.SCANNING));
                    assertThat(statusMap, hasEntry(clearingTeam2, FossologyStatus.CLOSED));
                    assertThat(statusMap.keySet(), containsInAnyOrder(clearingTeam, clearingTeam2));
                } catch (ClassCastException e) {
                    fail("status was supposed to be a map :-/" + e.getMessage());
                }
                return null;
            }
        }).when(release).setClearingTeamToFossologyStatus(anyMapOf(String.class, FossologyStatus.class));

        verify(release).setClearingTeamToFossologyStatus(anyMapOf(String.class, FossologyStatus.class));
    }

    @Test
    public void testUpdateFossologyStatusForTheFirstTimeCreatesTheStatusMap() {
        Release release = mock(Release.class);

        fossologyFileHandler.setFossologyStatus(release, clearingTeam, FossologyStatus.SCANNING, "13", "14");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, FossologyStatus> statusMap = (Map<String, FossologyStatus>) invocation.getArguments()[0];

                    assertThat(statusMap, hasEntry(clearingTeam, FossologyStatus.SCANNING));
                    assertThat(statusMap.keySet(), containsInAnyOrder(clearingTeam));
                } catch (ClassCastException e) {
                    fail("status was supposed to be a map :-/" + e.getMessage());
                }
                return null;
            }
        }).when(release).setClearingTeamToFossologyStatus(anyMapOf(String.class, FossologyStatus.class));

        verify(release).setClearingTeamToFossologyStatus(anyMapOf(String.class, FossologyStatus.class));

        verify(release).setFossologyId("13");
        verify(release).setAttachmentInFossology("14");
    }

    @Test
    public void testIsVisible() throws Exception {
        assertThat(FossologyFileHandler.isVisible(null), is(false));
        for (FossologyStatus fossologyStatus : FossologyStatus.values()) {
            final boolean expectedVisible;

            switch (fossologyStatus) {
                case CONNECTION_FAILED:
                case ERROR:
                case NON_EXISTENT:
                case NOT_SENT:
                case INACCESSIBLE:
                    expectedVisible = false;
                    break;
                case SENT:
                case SCANNING:
                case OPEN:
                case IN_PROGRESS:
                case CLOSED:
                case REJECTED:
                case REPORT_AVAILABLE:
                default:
                    expectedVisible = true;
                    break;
            }

            String errorMessage = "" + fossologyStatus + " should" + (expectedVisible ? "" : " not") + " be visible";
            assertThat(errorMessage, FossologyFileHandler.isVisible(fossologyStatus), is(expectedVisible));
        }
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

}