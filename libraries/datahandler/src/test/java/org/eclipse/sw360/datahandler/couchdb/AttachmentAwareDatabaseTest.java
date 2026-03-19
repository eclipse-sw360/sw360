/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.GetAttachmentOptions;
import com.ibm.cloud.cloudant.v1.model.PutAttachmentOptions;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import okhttp3.Request;
import org.eclipse.sw360.datahandler.cloudantclient.AttachmentAwareDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentAwareDatabaseTest {

    private TestAttachmentAwareDatabase database;

    private static class TestAttachmentAwareDatabase extends AttachmentAwareDatabase {
        public Request interceptedRequest;

        public TestAttachmentAwareDatabase(Cloudant client) {
            super(client);
        }

        @Override
        protected <T> ServiceCall<T> createServiceCall(Request request, ResponseConverter<T> converter) {
            this.interceptedRequest = request;
            return mock(ServiceCall.class);
        }
    }

    @Before
    public void setUp() {
        Cloudant client = new Cloudant("test", new NoAuthAuthenticator());
        client.setServiceUrl("http://localhost:5984");
        database = new TestAttachmentAwareDatabase(client);
    }

    @Test
    public void testGetAttachmentUrlEncoding() {
        String dbName = "testdb";
        String docId = "doc1";
        String attachmentName = "file[1]+test_<> name.txt";

        GetAttachmentOptions options = new GetAttachmentOptions.Builder()
                .db(dbName)
                .docId(docId)
                .attachmentName(attachmentName)
                .build();

        database.getAttachment(options);

        Request request = database.interceptedRequest;
        String requestUrl = request.url().toString();

        // Ensure that special characters '[]+_<> ' are appropriately encoded
        // file[1]+test_<> name.txt -> file%5B1%5D%2Btest_%3C%3E%20name.txt
        assertTrue(requestUrl.contains("file%5B1%5D%2Btest_%3C%3E%20name.txt") || requestUrl.contains("file%5B1%5D%2Btest_%3C%3E+name.txt"));
    }

    @Test
    public void testPutAttachmentUrlEncoding() {
        String dbName = "testdb";
        String docId = "doc2";
        String attachmentName = "another[file]+name_<> .txt";
        InputStream stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        PutAttachmentOptions options = new PutAttachmentOptions.Builder()
                .db(dbName)
                .docId(docId)
                .attachmentName(attachmentName)
                .attachment(stream)
                .contentType("text/plain")
                .build();

        database.putAttachment(options);

        Request request = database.interceptedRequest;
        String requestUrl = request.url().toString();

        assertTrue(requestUrl.contains("another%5Bfile%5D%2Bname_%3C%3E%20.txt") || requestUrl.contains("another%5Bfile%5D%2Bname_%3C%3E+.txt"));
    }
}
