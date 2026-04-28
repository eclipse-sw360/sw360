/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.components.ExternalTool;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FossologyHandlerTest {

    private static final String RELEASE_ID = "release-1";

    private FossologyHandler uut;

    private ThriftClients thriftClients;
    private FossologyRestClient fossologyRestClient;
    private AttachmentConnector attachmentConnector;
    private Iface componentClient;
    private User user;

    @Before
    public void setUp() {
        thriftClients = mock(ThriftClients.class);
        fossologyRestClient = mock(FossologyRestClient.class);
        attachmentConnector = mock(AttachmentConnector.class);
        componentClient = mock(Iface.class);
        user = TestUtils.getAdminUser(getClass());

        uut = new FossologyHandler(thriftClients, mock(FossologyRestConfig.class), fossologyRestClient,
                attachmentConnector);

        when(thriftClients.makeComponentClient()).thenReturn(componentClient);
    }

    @Test
    public void testProcess_returnsNull_whenMoreThanOneFossologyProcessExists() throws TException {
        Release release = createRelease(Set.of(
                createFossologyProcess("content-1", "sha-1"),
                createFossologyProcess("content-2", "sha-2")));

        when(componentClient.getReleaseById(RELEASE_ID, user)).thenReturn(release);

        ExternalToolProcess actual = uut.process(RELEASE_ID, user, "");

        assertNull(actual);
        verify(componentClient).getReleaseById(RELEASE_ID, user);
        verifyNoMoreInteractions(componentClient);
        verifyNoInteractions(fossologyRestClient, attachmentConnector);
    }

    @Test
    public void testProcess_returnsNull_whenMoreThanOneSourceAttachmentExists() throws TException {
        Release release = createRelease(Set.<ExternalToolProcess>of());
        Attachment firstSourceAttachment = createSourceAttachment("content-1", "sha-1");
        Attachment secondSourceAttachment = createSourceAttachment("content-2", "sha-2");

        when(componentClient.getReleaseById(RELEASE_ID, user)).thenReturn(release);
        when(componentClient.getSourceAttachments(RELEASE_ID))
                .thenReturn(Set.of(firstSourceAttachment, secondSourceAttachment));

        ExternalToolProcess actual = uut.process(RELEASE_ID, user, "");

        assertNull(actual);
        verify(componentClient).getReleaseById(RELEASE_ID, user);
        verify(componentClient).getSourceAttachments(RELEASE_ID);
        verifyNoMoreInteractions(componentClient);
        verifyNoInteractions(fossologyRestClient, attachmentConnector);
    }

    @Test
    public void testProcess_returnsNull_whenExistingProcessAttachmentDoesNotMatchCurrentSourceAttachment()
            throws TException {
        Release release = createRelease(Set.of(createFossologyProcess("content-1", "sha-1")));
        Attachment sourceAttachment = createSourceAttachment("content-2", "sha-2");

        when(componentClient.getReleaseById(RELEASE_ID, user)).thenReturn(release);
        when(componentClient.getSourceAttachments(RELEASE_ID)).thenReturn(Set.of(sourceAttachment));

        ExternalToolProcess actual = uut.process(RELEASE_ID, user, "");

        assertNull(actual);
        verify(componentClient).getReleaseById(RELEASE_ID, user);
        verify(componentClient).getSourceAttachments(RELEASE_ID);
        verifyNoMoreInteractions(componentClient);
        verifyNoInteractions(fossologyRestClient, attachmentConnector);
    }

    private Release createRelease(Set<ExternalToolProcess> fossologyProcesses) {
        Release release = new Release();
        release.setId(RELEASE_ID);
        release.setExternalToolProcesses(fossologyProcesses);
        return release;
    }

    private Attachment createSourceAttachment(String attachmentContentId, String sha1) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentContentId(attachmentContentId);
        attachment.setSha1(sha1);
        return attachment;
    }

    private ExternalToolProcess createFossologyProcess(String attachmentId, String attachmentHash) {
        ExternalToolProcess process = new ExternalToolProcess();
        process.setExternalTool(ExternalTool.FOSSOLOGY);
        process.setProcessStatus(ExternalToolProcessStatus.NEW);
        process.setAttachmentId(attachmentId);
        process.setAttachmentHash(attachmentHash);
        return process;
    }
}
