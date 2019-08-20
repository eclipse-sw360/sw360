/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.io.CharStreams;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

import static org.eclipse.sw360.datahandler.TestUtils.*;
import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author daniele.fognini@tngtech.com
 */
@RunWith(MockitoJUnitRunner.class)
public class AttachmentContentDownloaderTest {
    private final Duration downloadTimeout = durationOf(2, TimeUnit.SECONDS);
    private AttachmentContentDownloader attachmentContentDownloader;

    @Before
    public void setUp() throws Exception {
        attachmentContentDownloader = new AttachmentContentDownloader();
    }

    @Test
    public void testTheCouchDbUrl() throws Exception {
        AttachmentContent attachmentContent = mock(AttachmentContent.class);

        when(attachmentContent.getRemoteUrl()).thenReturn(DatabaseTestProperties.getCouchDbUrl());

        try (InputStream download = attachmentContentDownloader.download(attachmentContent, downloadTimeout)) {
            String read = CharStreams.toString(new InputStreamReader(download));
            assertThat(read, is(not(nullOrEmpty())));
            assertThat(read, containsString("couchdb"));
        }
    }

    @Test
    public void testABlackHoleUrl() throws Exception {
        assumeThat(getAvailableNetworkInterface(), isAvailable());

        Callable<String> downloadAttempt = () -> {
            AttachmentContent attachmentContent = mock(AttachmentContent.class);
            when(attachmentContent.getRemoteUrl()).thenReturn("http://" + BLACK_HOLE_ADDRESS + "/filename");

            try (InputStream download = attachmentContentDownloader
                    .download(attachmentContent, downloadTimeout)) {
                return CharStreams.toString(new InputStreamReader(download));
            }
        };

        ExecutorService executor = newSingleThreadExecutor();
        try {
            Future<String> future = executor.submit(downloadAttempt);

            try {
                try {
                    String read = future.get(1, TimeUnit.MINUTES);
                    fail("downloader managed to escape with '" + read + "' from the black hole! " +
                            "Try this test again with a more massive black hole");
                } catch (ExecutionException e) {
                    Throwable futureException = e.getCause();
                    assertThat(futureException, is(notNullValue()));
                    assertTrue(futureException instanceof IOException);
                }
            } catch (TimeoutException e) {
                fail("downloader got stuck on a black hole");
                throw e; // unreachable
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
