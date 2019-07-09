/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.ssh;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolRequest;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolStatus;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolWorkflowStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FossologyUploaderTest {

    @Mock
    FossologySshConnector sshConnector;

    FossologyUploader fossologyUploader;

    @Before
    public void setUp() {
        fossologyUploader = new FossologyUploader(sshConnector);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(sshConnector);
    }

    @Test
    public void testUploadToFossology() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);

        when(attachment.getId()).thenReturn("id");
        when(attachment.getFilename()).thenReturn("fileName");

        String clearingTeam = "cl";

        final InputStream inputStream = mock(InputStream.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Object[] arguments = invocation.getArguments();
                String command = (String) arguments[0];
                InputStream input = (InputStream) arguments[1];
                OutputStream outputStream = (OutputStream) arguments[2];

                assertThat(input, sameInstance(inputStream));
                assertThat(command, is("./uploadFromSW360 -i 'id' -g 'cl' -f 'fileName'"));

                outputStream.write("uploadId=60".getBytes());

                return 0;
            }
        }).when(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        final long uploadId = fossologyUploader.uploadToFossology(inputStream, attachment, clearingTeam);

        verify(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        assertThat(uploadId, is((long) 60));
    }

    @Test
    public void testUploadToFossologyWithNonIntReturn() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);

        when(attachment.getId()).thenReturn("id");
        when(attachment.getFilename()).thenReturn("fileName");

        String clearingTeam = "cl";

        final InputStream inputStream = mock(InputStream.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                OutputStream outputStream = (OutputStream) invocation.getArguments()[2];

                outputStream.write("uploadId=60000000000000000".getBytes());
                return 0;
            }
        }).when(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        final long uploadId = fossologyUploader.uploadToFossology(inputStream, attachment, clearingTeam);

        verify(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        assertThat(uploadId, is((long) -1));
    }

    @Test
    public void testUploadToFossologyWithBadReturn() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);

        when(attachment.getId()).thenReturn("id");
        when(attachment.getFilename()).thenReturn("fileName");

        String clearingTeam = "cl";

        final InputStream inputStream = mock(InputStream.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                OutputStream outputStream = (OutputStream) invocation.getArguments()[2];

                outputStream.write("error".getBytes());
                return 0;
            }
        }).when(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        final long uploadId = fossologyUploader.uploadToFossology(inputStream, attachment, clearingTeam);

        verify(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        assertThat(uploadId, is((long) -1));
    }

    @Test
    public void testUploadToFossologyReportsErrors() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);

        when(attachment.getId()).thenReturn("id");
        when(attachment.getFilename()).thenReturn("fileName");

        String clearingTeam = "cl";

        final InputStream inputStream = mock(InputStream.class);

        doReturn(-1).when(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        final long uploadId = fossologyUploader.uploadToFossology(inputStream, attachment, clearingTeam);

        verify(sshConnector).runInFossologyViaSsh(anyString(), eq(inputStream), any(OutputStream.class));

        assertThat(uploadId, is((long) -1));
    }

    @Test
    public void testUploadToFossologyWithEmptyId() throws Exception {
        AttachmentContent attachment = mock(AttachmentContent.class);

        when(attachment.getId()).thenReturn(null);
        when(attachment.getFilename()).thenReturn("fileName");

        String clearingTeam = "cl";

        final InputStream inputStream = mock(InputStream.class);

        final long uploadId = fossologyUploader.uploadToFossology(inputStream, attachment, clearingTeam);

        verify(sshConnector, never()).runInFossologyViaSsh(anyString(), any(InputStream.class), any(OutputStream.class));

        assertThat(uploadId, is((long) -1));
    }

    @Test
    public void testGetStatusInFossology() throws Exception {
        int uploadId = 42;

        fossologyUploader = spy(fossologyUploader);

        String clearingTeam = "team d";

        final String output = "output result\nstatus=21 heißt in_progress\nfrom the get status script";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final OutputStream os = (OutputStream) invocation.getArguments()[1];
                os.write(output.getBytes());
                return 0;
            }
        }).when(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));

        ExternalToolRequest etr = new ExternalToolRequest();
        etr.setToolId(String.valueOf(uploadId));
        etr.setToolUserGroup(clearingTeam);

        ExternalToolWorkflowStatus etrs = fossologyUploader.updateStatusInFossologyRequest(etr);

        assertThat(etrs, is(ExternalToolWorkflowStatus.SENT));
        assertThat(etr.getExternalToolStatus(), is(ExternalToolStatus.IN_PROGRESS));

        verify(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));
    }

    @Test
    public void testGetStatusInFossologyIsConnectionErrorForServerNotAvailable() throws Exception {
        int uploadId = 43;

        String clearingTeam = "team d";

        doReturn(1).when(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));

        ExternalToolRequest etr = new ExternalToolRequest();
        etr.setToolId(String.valueOf(uploadId));
        etr.setToolUserGroup(clearingTeam);

        ExternalToolWorkflowStatus etrs = fossologyUploader.updateStatusInFossologyRequest(etr);

        assertThat(etrs, is(ExternalToolWorkflowStatus.CONNECTION_FAILED));

        verify(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));
    }

    @Test
    public void testGetStatusInFossologyIsErrorForBadUploadId() throws Exception {
        ExternalToolRequest etr = new ExternalToolRequest();
        etr.setToolId("-1");
        etr.setToolUserGroup("a");

        ExternalToolWorkflowStatus etrs = fossologyUploader.updateStatusInFossologyRequest(etr);

        assertThat(etrs, is(ExternalToolWorkflowStatus.SERVER_ERROR));
    }

    @Test
    public void testCopyToFossology() throws Exception {
        when(sshConnector.runInFossologyViaSsh(anyString(), any(InputStream.class))).thenReturn(0);

        final InputStream content = mock(InputStream.class);
        boolean success = fossologyUploader.copyToFossology("filename", content, false);

        assertThat(success, is(true));
        verify(sshConnector).runInFossologyViaSsh(eq("cat > 'filename'"), eq(content));
    }

    @Test
    public void testCopyToFossologyExecutable() throws Exception {
        when(sshConnector.runInFossologyViaSsh(anyString(), any(InputStream.class))).thenReturn(0);

        final InputStream content = mock(InputStream.class);
        boolean success = fossologyUploader.copyToFossology("filename", content, true);

        assertThat(success, is(true));
        verify(sshConnector).runInFossologyViaSsh(eq("cat > 'filename' && chmod u+x 'filename'"), eq(content));
    }

    @Test
    public void testCopyToFossologyReportsFailure() throws Exception {
        when(sshConnector.runInFossologyViaSsh(anyString(), any(InputStream.class))).thenReturn(1);

        final InputStream content = mock(InputStream.class);
        boolean success = fossologyUploader.copyToFossology("filename", content, false);

        assertThat(success, is(false));
        verify(sshConnector).runInFossologyViaSsh(eq("cat > 'filename'"), eq(content));
    }

    @Test
    public void testParseResult() {
        ExternalToolRequest etr = new ExternalToolRequest();

        ExternalToolWorkflowStatus workflowStatus = fossologyUploader.parseResultStatus("status=22", etr);

        assertThat(workflowStatus, is(ExternalToolWorkflowStatus.SENT));
        assertThat(etr.getExternalToolStatus(), is(ExternalToolStatus.CLOSED));
    }

    @Test
    public void testParseResultWithBadStatus() {
        ExternalToolRequest etr = new ExternalToolRequest();

        ExternalToolWorkflowStatus workflowStatus = fossologyUploader.parseResultStatus("status=CLOPED", etr);

        assertThat(workflowStatus, is(ExternalToolWorkflowStatus.CONNECTION_FAILED));
        assertThat(etr.getExternalToolStatus(), is(ExternalToolStatus.OPEN));
    }
}