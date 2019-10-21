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
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360AttachmentService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @Value("${sw360.couchdb-url:http://localhost:5984}")
    private String couchdbUrl;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    private static final Logger log = Logger.getLogger(Sw360AttachmentService.class);
    private final Duration downloadTimeout = Duration.durationOf(30, TimeUnit.SECONDS);
    private AttachmentConnector attachmentConnector;

    public AttachmentInfo getAttachmentById(String id) throws TException {
        AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
        List<Attachment> attachments = attachmentClient.getAttachmentsByIds(Collections.singleton(id));
        return createAttachmentInfo(attachmentClient, attachments);
    }

    public AttachmentInfo getAttachmentBySha1(String sha1) throws TException {
        AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
        List<Attachment> attachments = attachmentClient.getAttachmentsBySha1s(Collections.singleton(sha1));
        return createAttachmentInfo(attachmentClient, attachments);
    }

    private AttachmentInfo createAttachmentInfo(AttachmentService.Iface attachmentClient, List<Attachment> attachments) throws TException {
        AttachmentInfo attachmentInfo = new AttachmentInfo(getValidAttachment(attachments));
        String attachmentId = attachmentInfo.getAttachment().getAttachmentContentId();
        attachmentInfo.setOwner(attachmentClient.getAttachmentOwnersByIds(Collections.singleton(attachmentId)).get(0));
        return attachmentInfo;
    }

    private Attachment getValidAttachment(List<Attachment> attachments) {
        if (attachments.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return attachments.get(0);
    }

    public void downloadAttachmentWithContext(Object context, String attachmentId, HttpServletResponse response, User sw360User) {
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

        String createdComment = newAttachment.getCreatedComment();
        if (createdComment != null) {
            attachment.setCreatedComment(createdComment);
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

    private AttachmentService.Iface getThriftAttachmentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/attachments/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new AttachmentService.Client(protocol);
    }

    public Resources<Resource<Attachment>> getResourcesFromList(Set<Attachment> attachmentList) {
        final List<Resource<Attachment>> attachmentResources = new ArrayList<>();
        for (final Attachment attachment : attachmentList) {
            final Attachment embeddedAttachment = restControllerHelper.convertToEmbeddedAttachment(attachment);
            final Resource<Attachment> attachmentResource = new Resource<>(embeddedAttachment);
            attachmentResources.add(attachmentResource);
        }
        return new Resources<>(attachmentResources);
    }
}
