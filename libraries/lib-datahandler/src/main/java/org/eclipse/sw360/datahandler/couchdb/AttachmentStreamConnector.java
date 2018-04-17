/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2017.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.ConcatClosingInputStream;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.AttachmentInputStream;
import org.ektorp.DocumentNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;

/**
 * Created by bodet on 03/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author daniele.fognini@tngtech.com
 */
public class AttachmentStreamConnector {
    private static Logger log = Logger.getLogger(AttachmentStreamConnector.class);

    protected final DatabaseConnector connector;
    private final AttachmentContentDownloader attachmentContentDownloader;
    private final Duration downloadTimeout;

    /**
     * @param downloadTimeout timeout for downloading remote attachments
     * @throws java.net.MalformedURLException if the given database address not a valid url.
     * @todo remove this mess of constructors and use dependency injection
     */
    public AttachmentStreamConnector(Duration downloadTimeout) throws MalformedURLException {
        this(new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient().get(), DatabaseSettings.COUCH_DB_ATTACHMENTS), downloadTimeout);
    }

    public AttachmentStreamConnector(DatabaseConnector connector, Duration downloadTimeout) {
        this(connector, new AttachmentContentDownloader(), downloadTimeout);
    }

    public AttachmentStreamConnector(DatabaseConnector connector, AttachmentContentDownloader attachmentContentDownloader, Duration downloadTimeout) {
        this.connector = connector;
        this.attachmentContentDownloader = attachmentContentDownloader;
        this.downloadTimeout = downloadTimeout;
    }

    /**
     * Get an input stream to download the attachment
     * It is highly recommended to close this stream after using to avoid connection leak
     */
    public <T> InputStream getAttachmentStream(AttachmentContent attachment, User user, T context) throws TException {
        assertNotNull(attachment);
        assertNotNull(context);

        if(!PermissionUtils.makePermission(context, user)
                .isAllowedToDownload(attachment)){
            String msg =
                    "The user=["+user.getEmail()+"] tried to download attachment=["+ attachment.getId()+
                    "] without attachment permissions";
            log.warn(msg);
            throw new SW360Exception(msg);
        }

        return unsafeGetAttachmentStream(attachment);
    }

    public InputStream unsafeGetAttachmentStream(AttachmentContent attachment) throws SW360Exception {
        if (attachment.isOnlyRemote()) {
            attachment = downloadRemoteAttachmentAndUpdate(attachment);
        }

        return readAttachmentStream(attachment);
    }

    /**
     * It is highly recommended to close this stream after using to avoid connection leak
     */
    public <T> InputStream getAttachmentBundleStream(Set<AttachmentContent> attachments, User user, T context) throws IOException, SW360Exception {
        assertNotNull(context);

        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int length;

            try(ZipOutputStream zip = new ZipOutputStream(out)){
                for(AttachmentContent attachment : attachments) {
                    // TODO: handle attachments with equal name
                    ZipEntry zipEntry = new ZipEntry(attachment.getFilename());
                    zip.putNextEntry(zipEntry);

                    try(InputStream attachmentStream = getAttachmentStream(attachment, user, context)) {
                        while ((length = attachmentStream.read(buffer)) >= 0) {
                            zip.write(buffer, 0, length);
                        }
                    } catch (TException e) {
                        log.error("failed to get AttachmentStream, maybe due to permission problems", e);
                    }

                    zip.closeEntry();
                }
            } catch (IOException e) {
                log.error("failed to write zip stream", e);
            }
        }).start();

        return in;
    }

    private AttachmentContent downloadRemoteAttachmentAndUpdate(AttachmentContent attachmentContent) throws SW360Exception {
        final InputStream downloadStream;

        try {
            downloadStream = attachmentContentDownloader.download(attachmentContent, downloadTimeout);
        } catch (IOException e) {
            String msg = "Cannot download attachment " + attachmentContent.getId() + " from URL";
            log.error(msg, e);
            throw new SW360Exception(msg);
        }


        uploadAttachment(attachmentContent, downloadStream);

        attachmentContent = connector.get(AttachmentContent.class, attachmentContent.getId());
        attachmentContent.setOnlyRemote(false);
        connector.update(attachmentContent);

        return attachmentContent;
    }

    protected InputStream readAttachmentStream(AttachmentContent attachment) {
        int partsCount = -1;

        if (attachment.isSetPartsCount()) {
            partsCount = CommonUtils.toUnsignedInt(attachment.getPartsCount());
        }

        if (partsCount < 0) {
            return connector.getAttachment(attachment.getId(), attachment.getFilename());
        } else {
            return getConcatenatedAttachmentPartsStream(attachment, partsCount);
        }
    }

    protected InputStream getConcatenatedAttachmentPartsStream(final AttachmentContent attachment, final int partsCount) {
        Iterator<InputStream> streams = new Iterator<InputStream>() {
            int part = 1; // the first is part 1 not 0!

            @Override
            public boolean hasNext() {
                return part <= partsCount; // the first is part 1 not 0!
            }

            @Override
            public InputStream next() {
                String attachmentId = attachment.getId();
                String partFileName = getPartFileName(attachment, part);
                part++;
                try {
                    return connector.getAttachment(attachmentId, partFileName);
                } catch (DocumentNotFoundException e) {
                    log.error("Cannot find part " + (part - 1) + " of attachment " + attachmentId, e);
                    return null;
                }
            }

            @Override
            public void remove() {
                //this should be a no-op.
            }
        };

        return new ConcatClosingInputStream(streams);
    }

    /**
     * Upload a single part attachment using the provided metadata
     */
    public void uploadAttachment(AttachmentContent attachment, InputStream stream) throws SW360Exception {
        addAttachmentTo(attachment.getId(), attachment.getFilename(), stream);
    }

    /**
     * Upload a part of an attachment using the provided metadata
     */
    public void uploadAttachmentPart(AttachmentContent attachmentContent, int part, InputStream stream) throws SW360Exception {
        // Extract required data
        assertNotNull(attachmentContent);

        String partFileName = getPartFileName(attachmentContent, part);

        addAttachmentTo(attachmentContent.getId(), partFileName, stream);
    }

    private void addAttachmentTo(String attachmentContentId, String filename, InputStream stream) {
        String contentType = "application/octet-stream";

        AttachmentInputStream attachmentInputStream = new AttachmentInputStream(filename, stream, contentType);
        String revision = connector.getCurrentRevision(attachmentContentId);
        connector.createAttachment(attachmentContentId, revision, attachmentInputStream);
    }

    /**
     * Get an input stream to download a part of the attachment
     */
    public InputStream getAttachmentPartStream(AttachmentContent attachment, int part) throws SW360Exception {
        assertNotNull(attachment);

        return connector.getAttachment(attachment.getId(), getPartFileName(attachment, part));
    }

    private String getPartFileName(AttachmentContent attachment, int part) {
        return attachment.getFilename() + "_part" + part;
    }
}
