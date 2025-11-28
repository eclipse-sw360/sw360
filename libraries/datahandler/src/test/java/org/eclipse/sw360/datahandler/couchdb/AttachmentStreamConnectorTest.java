/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
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
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class AttachmentStreamConnectorTest {
    @MockitoBean
    public DatabaseConnectorCloudant connector;
    @MockitoBean
    private AttachmentContentDownloader attachmentContentDownloader;

    private User dummyUser = new User().setEmail("dummy@some.domain");

    AttachmentStreamConnector attachmentStreamConnector;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
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
        lenient().when(rereadAttachment.getId()).thenReturn(id);
        lenient().when(rereadAttachment.getFilename()).thenReturn(filename);

        attachmentStreamConnector = spy(attachmentStreamConnector);
        doReturn(returnedStream).when(attachmentStreamConnector).readAttachmentStream(rereadAttachment);
        lenient().doNothing().when(attachmentStreamConnector).uploadAttachmentPart(attachment, 1, downloadUrlStream);

        when(attachmentContentDownloader.download(eq(attachment))).thenReturn(downloadUrlStream);

        when(connector.get(AttachmentContent.class, id)).thenReturn(rereadAttachment);
        doReturn(rereadAttachment).when(rereadAttachment).setOnlyRemote(anyBoolean());

        assertThat(attachmentStreamConnector.getAttachmentStream(attachment, dummyUser,
                     new Project()
                             .setVisbility(Visibility.ME_AND_MODERATORS)
                             .setCreatedBy(dummyUser.getEmail())
                             .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(id)))),
                sameInstance(returnedStream));

        verify(attachmentContentDownloader).download(eq(attachment));
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

        InputStream full = mock(InputStream.class);
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

        InputStream part1 = mock(InputStream.class);
        when(connector.getAttachment(attachmentId, "fil_part1")).thenReturn(part1);

        InputStream part2 = mock(InputStream.class);
        when(connector.getAttachment(attachmentId, "fil_part2")).thenReturn(part2);

        when(part1.read()).thenReturn(1, -1);
        when(part2.read()).thenReturn(2, -1);
        InputStream attachmentStream = attachmentStreamConnector.getAttachmentStream(attachment, dummyUser,
                new Project()
                        .setVisbility(Visibility.ME_AND_MODERATORS)
                        .setCreatedBy(dummyUser.getEmail())
                        .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachmentId))));

        verifyNoMoreInteractions(part2);
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

        InputStream part1 = mock(InputStream.class);
        when(connector.getAttachment(attachmentId, "fil_part1")).thenReturn(part1);

        when(connector.getAttachment(attachmentId, "fil_part2")).thenThrow(
                new ServiceResponseException(404, new Response.Builder()
                        .code(404)
                        .request(new Request.Builder().url("http://example.com").build())
                        .protocol(Protocol.HTTP_1_0)
                        .message("Not Found")
                        .body(RealResponseBody.create("Not Found", MediaType.get("text/plain")))
                        .build())
        );

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

    @Test
    public void testPrintAcceptedZipEntryName() {
        assertThat(attachmentStreamConnector.printAcceptedZipEntryName("source.zip", 0), is("source (0).zip"));
        assertThat(attachmentStreamConnector.printAcceptedZipEntryName("source.JPG", 2), is("source (2).JPG"));
        assertThat(attachmentStreamConnector.printAcceptedZipEntryName("source..v1.001.tar.xz", 3), is("source..v1.001 (3).tar.xz"));
        assertThat(attachmentStreamConnector.printAcceptedZipEntryName("source", 4), is("source (4)"));
    }

    @Test
    public void testGetDeduplicatedZipEntry() {
        Map<String, Integer> fileNameUsageMap = new HashMap<>();
        ZipEntry zipEntry =  attachmentStreamConnector.getDeduplicatedZipEntry("source.zip", fileNameUsageMap);
        assertThat(zipEntry.getName(), is("source.zip"));
        fileNameUsageMap.put("source.zip", 1);
        ZipEntry zipEntry2 =  attachmentStreamConnector.getDeduplicatedZipEntry("source.zip", fileNameUsageMap);
        assertThat(zipEntry2.getName(), is("source (1).zip"));
    }

}
