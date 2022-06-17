/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.attachments.db;

import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.AttachmentContentRepository;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;
import static org.eclipse.sw360.datahandler.common.Duration.durationOf;

/**
 * Utility to retrieve remote attachments
 *
 * @author daniele.fognini@tngtech.com
 */
public class RemoteAttachmentDownloader {
    private static final Logger log = getLogger(RemoteAttachmentDownloader.class);

    public static void main(String[] args) throws MalformedURLException {
        Duration downloadTimeout = durationOf(30, TimeUnit.SECONDS);
        retrieveRemoteAttachments(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_ATTACHMENTS, downloadTimeout);
    }

    public static int retrieveRemoteAttachments(Supplier<CloudantClient> httpClient, String dbAttachments, Duration downloadTimeout) throws MalformedURLException {
        AttachmentConnector attachmentConnector = new AttachmentConnector(httpClient, dbAttachments, downloadTimeout);
        AttachmentContentRepository attachmentContentRepository = new AttachmentContentRepository(new DatabaseConnectorCloudant(httpClient, dbAttachments));

        List<AttachmentContent> remoteAttachments = attachmentContentRepository.getOnlyRemoteAttachments();
        log.info("we have {} remote attachments to retrieve", remoteAttachments.size());

        int count = 0;

        for (AttachmentContent attachmentContent : remoteAttachments) {
            if (!attachmentContent.isOnlyRemote()) {
                log.info("skipping attachment ({}), which should already be available", attachmentContent.getId());
                continue;
            }

            String attachmentContentId = attachmentContent.getId();
            log.info("retrieving attachment ({}, filename={})", attachmentContentId, attachmentContent.getFilename());
            log.debug("url is {}", attachmentContent.getRemoteUrl());

            InputStream content = null;
            try {
                content = attachmentConnector.unsafeGetAttachmentStream(attachmentContent);
                if (content == null) {
                    log.error("null content retrieving attachment {}", attachmentContentId);
                    continue;
                }
                try {
                    long length = length(content);
                    log.info("retrieved attachment ({}), it was {} bytes long", attachmentContentId, length);
                    count++;
                } catch (IOException e) {
                    log.error("attachment was downloaded but somehow not available in database {}", attachmentContentId, e);
                }
            } catch (SW360Exception e) {
                log.error("cannot retrieve attachment {}", attachmentContentId, e);
            } finally {
                closeQuietly(content, log);
            }
        }

        return count;
    }

    protected static long length(InputStream stream) throws IOException {
        long length = 0;
        try {
            while (stream.read() >= 0) {
                ++length;
            }
        } finally {
            stream.close();
        }
        return length;
    }
}
