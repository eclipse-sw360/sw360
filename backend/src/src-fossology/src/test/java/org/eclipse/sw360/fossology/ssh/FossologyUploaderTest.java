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
package org.eclipse.sw360.fossology.ssh;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.FossologyStatus;
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

        final String output = "output result\nfrom the get status script";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final OutputStream os = (OutputStream) invocation.getArguments()[1];
                os.write(output.getBytes());
                return 0;
            }
        }).when(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));

        FossologyStatus parseResult = FossologyStatus.IN_PROGRESS;
        doReturn(parseResult).when(fossologyUploader).parseResultStatus(output);

        FossologyStatus statusInFossology = fossologyUploader.getStatusInFossology(uploadId, clearingTeam);

        assertThat(statusInFossology, is(parseResult));

        verify(fossologyUploader).parseResultStatus(output);

        verify(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));
        verify(fossologyUploader).getStatusInFossology(uploadId, clearingTeam);
    }

    @Test
    public void testGetStatusInFossologyIsConnectionErrorForServerNotAvailable() throws Exception {
        int uploadId = 43;

        String clearingTeam = "team d";

        doReturn(1).when(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));

        FossologyStatus statusInFossology = fossologyUploader.getStatusInFossology(uploadId, clearingTeam);

        assertThat(statusInFossology, is(FossologyStatus.CONNECTION_FAILED));

        verify(sshConnector).runInFossologyViaSsh(anyString(), any(OutputStream.class));
    }

    @Test
    public void testGetStatusInFossologyIsErrorForBadUploadId() throws Exception {
        assertThat(fossologyUploader.getStatusInFossology(-1, "a"), is(FossologyStatus.ERROR));
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
        final FossologyStatus fossologyStatus = FossologyStatus.CLOSED;
        assertThat(
                fossologyUploader.parseResultStatus("status=" + fossologyStatus),
                is(fossologyStatus));
    }

    @Test
    public void testParseResultWithBadStatus() {
        assertThat(
                fossologyUploader.parseResultStatus("status=CLOPED"),
                is(FossologyStatus.CONNECTION_FAILED));
    }
}