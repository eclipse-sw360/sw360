/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.io.CharStreams;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Set;
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
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class AttachmentContentDownloaderTest {
    private final Duration downloadTimeout = durationOf(2, TimeUnit.SECONDS);
    @MockitoBean
    private AttachmentContentDownloader attachmentContentDownloader;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Autowired
    @Qualifier("COUCH_DB_URL")
    private String couchDbUrl;

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testTheCouchDbUrl() throws Exception {
        AttachmentContent attachmentContent = mock(AttachmentContent.class);

        when(attachmentContent.getRemoteUrl()).thenReturn(couchDbUrl);

        try (InputStream download = attachmentContentDownloader.download(attachmentContent)) {
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
                    .download(attachmentContent)) {
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
