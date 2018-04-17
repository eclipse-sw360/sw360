/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360AttachmentService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @Value("${sw360.couchdb-url:http://localhost:5984}")
    private String couchdbUrl;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    private final Duration downloadTimeout = Duration.durationOf(30, TimeUnit.SECONDS);

    private AttachmentConnector attachmentConnector;

    public AttachmentInfo getAttachmentBySha1ForUser(String sha1, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        List<Release> releases = sw360ComponentClient.getReleaseSummary(sw360User);
        for (Release release : releases) {
            final Set<Attachment> attachments = release.getAttachments();
            if (attachments != null && attachments.size() > 0) {
                for (Attachment attachment : attachments) {
                    if (sha1.equals(attachment.getSha1())) {
                        return new AttachmentInfo(attachment, release);
                    }
                }
            }
        }
        return null;
    }

    public AttachmentInfo getAttachmentByIdForUser(String id, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        List<Release> releases = sw360ComponentClient.getReleaseSummary(sw360User);
        for (Release release : releases) {
            final Set<Attachment> attachments = release.getAttachments();
            if (attachments != null && attachments.size() > 0) {
                for (Attachment attachment : attachments) {
                    if (id.equals(attachment.getAttachmentContentId())) {
                        return new AttachmentInfo(attachment, release);
                    }
                }
            }
        }
        return null;
    }

    public void downloadAttachmentWithContext(Object context, String attachmentId, HttpServletResponse response, OAuth2Authentication oAuth2Authentication) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);

        AttachmentContent attachmentContent = getAttachmentContent(attachmentId);

        String filename = attachmentContent.getFilename();
        String contentType = attachmentContent.getContentType();

        try (InputStream attachmentStream = getStreamToAttachments(Collections.singleton(attachmentContent), sw360User, context)) {
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            FileCopyUtils.copy(attachmentStream, response.getOutputStream());
        } catch (TException|IOException e) {
            log.error(e.getMessage());
        }
    }

    public <T> InputStream getStreamToAttachments(Set<AttachmentContent> attachments, User sw360User, T context) throws IOException, TException {
        return new AttachmentFrontendUtils().getStreamToServeAFile(attachments, sw360User, context);
    }

    public Attachment uploadAttachment(MultipartFile file, Attachment newAttachment, User sw360User) throws IOException, TException {
        String fileName = file.getOriginalFilename(); // TODO: shouldn't the fileName be taken from newAttachment?
        String contentType = file.getContentType();
        final AttachmentContent attachmentContent = makeAttachmentContent(fileName, contentType);

        final AttachmentConnector attachmentConnector = getConnector();
        Attachment attachment = new AttachmentFrontendUtils().uploadAttachmentContent(attachmentContent, file.getInputStream(), sw360User);
        attachment.setSha1(attachmentConnector.getSha1FromAttachmentContentId(attachmentContent.getId()));

        AttachmentType attachmentType = newAttachment.getAttachmentType();
        if (attachmentType != null) {
            attachment.setAttachmentType(attachmentType);
        }

        CheckStatus checkStatus = newAttachment.getCheckStatus();
        if (checkStatus != null) {
            attachment.setCheckStatus(checkStatus);
        }

        return attachment;
    }

    private AttachmentContent makeAttachmentContent(String filename, String contentType) {
        AttachmentContent attachment = new AttachmentContent()
                .setContentType(contentType)
                .setFilename(filename)
                .setOnlyRemote(false);
        return makeAttachmentContent(attachment);
    }

    private AttachmentContent makeAttachmentContent(AttachmentContent content) {
        try {
            return new AttachmentFrontendUtils().makeAttachmentContent(content);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public AttachmentContent getAttachmentContent(String id) {
        try {
            return new AttachmentFrontendUtils().getAttachmentContent(id);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }

    private AttachmentConnector getConnector() throws TException {
        if (attachmentConnector == null) makeConnector();
        return attachmentConnector;
    }

    private synchronized void makeConnector() throws TException {
        if (attachmentConnector == null) {
            try {
                attachmentConnector = new AttachmentConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_ATTACHMENTS, downloadTimeout);
            } catch (MalformedURLException e) {
                log.error("Invalid database address received...", e);
                throw new TException(e);
            }
        }
    }
}
