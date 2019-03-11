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

package org.eclipse.sw360.portal.common;

import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.ektorp.DocumentNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.net.URLConnection.guessContentTypeFromName;
import static java.net.URLConnection.guessContentTypeFromStream;

/**
 * Portlet helpers
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author daniele.fognini@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class AttachmentPortletUtils extends AttachmentFrontendUtils {
    public static final String DEFAULT_ATTACHMENT_BUNDLE_NAME = "AttachmentBundle.zip";

    private static final Logger log = Logger.getLogger(AttachmentPortletUtils.class);
    private final ProjectService.Iface projectClient;
    private final ComponentService.Iface componentClient;

    public AttachmentPortletUtils() {
        this(new ThriftClients());
    }

    public AttachmentPortletUtils(ThriftClients thriftClients) {
        projectClient = thriftClients.makeProjectClient();
        componentClient = thriftClients.makeComponentClient();
    }

    public void serveFile(ResourceRequest request, ResourceResponse response) {
        serveFile(request, response, Optional.empty());
    }

    public void serveFile(ResourceRequest request, ResourceResponse response, Optional<String> downloadFileName) {
        String[] ids = request.getParameterValues(PortalConstants.ATTACHMENT_ID);

        if(ids != null && ids.length >= 1){
            serveAttachmentBundle(new HashSet<>(Arrays.asList(ids)), request, response, downloadFileName);
        }else{
            log.warn("no attachmentId was found in the request passed to serveFile");
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
        }
    }

    public void serveAttachmentBundle(Collection<String> ids, ResourceRequest request, ResourceResponse response){
        serveAttachmentBundle(ids, request, response, Optional.empty());
    }

    private Optional<Object> getContextFromRequest(ResourceRequest request, User user) {
        String contextType = request.getParameter(PortalConstants.CONTEXT_TYPE);
        String contextId = request.getParameter(PortalConstants.CONTEXT_ID);

        try {
            switch (contextType){
                case "project":
                    return Optional.ofNullable(projectClient.getProjectById(contextId, user));
                case "release":
                    return Optional.ofNullable(componentClient.getReleaseById(contextId, user));
                case "component":
                    return Optional.ofNullable(componentClient.getComponentById(contextId, user));
            }
        } catch (TException e) {
            // was not allowed to see the attachment due to missing read privileges
        }
        return Optional.empty();
    }

    public void serveAttachmentBundle(Collection<String> ids, ResourceRequest request, ResourceResponse response, Optional<String> downloadFileName){
        List<AttachmentContent> attachments = new ArrayList<>();
        try {
            for(String id : ids){
                attachments.add(attchmntClient.get().getAttachmentContent(id));
            }

            serveAttachmentBundle(attachments, request, response, downloadFileName);
        } catch (TException e) {
            log.error("Problem getting the AttachmentContents from the backend", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
        }
    }

    public void serveAttachmentBundle(List<AttachmentContent> attachments, ResourceRequest request, ResourceResponse response){
        serveAttachmentBundle(attachments, request, response, Optional.empty());
    }

    private void serveAttachmentBundle(List<AttachmentContent> attachments, ResourceRequest request, ResourceResponse response, Optional<String> downloadFileName){
        String filename;
        String contentType;
        String isAllAttachment = request.getParameter(PortalConstants.ALL_ATTACHMENTS);
        boolean downloadAllAttachmentSelected = StringUtils.isNotEmpty(isAllAttachment)
                && isAllAttachment.equalsIgnoreCase("true");
        if (attachments.size() == 1 && !downloadAllAttachmentSelected) {
            filename = attachments.get(0).getFilename();
            contentType = attachments.get(0).getContentType();
            if (contentType.equalsIgnoreCase("application/gzip")) {
                // In case of downloads with gzip file extension (e.g. *.tar.gz)
                // the client should receive the origin file.
                // Set http header 'Content-Encoding' to 'identity'
                // to prevent auto encoding content (chrome).
                response.setProperty(HttpHeaders.CONTENT_ENCODING, "identity");
            }
        } else {
            filename = downloadFileName.orElse(DEFAULT_ATTACHMENT_BUNDLE_NAME);
            contentType = "application/zip";
        }

        User user = UserCacheHolder.getUserFromRequest(request);
        try {
            Optional<Object> context = getContextFromRequest(request, user);

            if(context.isPresent()){
                try (InputStream attachmentStream = (downloadAllAttachmentSelected
                        ? getStreamToServeBundle(attachments, user, context.get())
                        : getStreamToServeAFile(attachments, user, context.get()))) {
                    PortletResponseUtil.sendFile(request, response, filename, attachmentStream, contentType);
                } catch (IOException e) {
                    log.error("cannot finish writing response", e);
                    response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
                }
            }else{
                log.warn("The user=["+user.getEmail()+"] tried to download attachment=["+
                        CommonUtils.joinStrings(attachments.stream()
                                .map(AttachmentContent::getId)
                                .collect(Collectors.toList()))+
                        "] in context=["+request.getParameter(PortalConstants.CONTEXT_ID)+"] without read permissions");
                response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
            }
        } catch (SW360Exception e) {
            log.error("Context was not set properly.", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "400");
        } catch (TException e) {
            log.error("Problem getting the attachment content from the backend", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        }
    }

    private boolean uploadAttachmentPartFromRequest(PortletRequest request, String fileUploadName) throws IOException, TException {
        final UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(request);
        final InputStream stream = uploadPortletRequest.getFileAsStream(fileUploadName);

        final ResumableUpload resumableUpload = ResumableUpload.from(uploadPortletRequest);
        AttachmentContent attachment = null;

        if (resumableUpload.isValid()) {
            final AttachmentStreamConnector attachmentStreamConnector = getConnector();

            attachment = getAttachmentContent(resumableUpload, stream);

            if (attachment != null) {
                try {
                    attachmentStreamConnector.uploadAttachmentPart(attachment, resumableUpload.getChunkNumber(), stream);
                } catch (TException e) {
                    log.error("Error saving attachment part", e);
                    return false;
                }
            }
        }

        return attachment != null;
    }

    private AttachmentContent getAttachmentContent(ResumableUpload resumableUpload, InputStream stream) throws IOException, TException {
        if (!resumableUpload.isValid()) {
            return null;
        }

        final AttachmentContent attachment = getAttachmentContent(resumableUpload);
        if (resumableUpload.getChunkNumber() == 1) {
            String fileName = resumableUpload.getFilename();

            String contentType = resumableUpload.getFileType();

            if (isNullOrEmpty(contentType)) {
                contentType = guessContentTypeFromStream(stream);
            }
            if (isNullOrEmpty(contentType)) {
                contentType = guessContentTypeFromName(fileName);
            }
            if (isNullOrEmpty(contentType)) {
                contentType = "text";
            }

            int partsCount = resumableUpload.getTotalChunks();

            attachment.setContentType(contentType)
                    .setFilename(fileName)
                    .setOnlyRemote(false)
                    .setPartsCount(Integer.toString(partsCount));

            return updateAttachmentContent(attachment);
        } else {
            return attachment;
        }
    }

    private AttachmentContent getAttachmentContent(ResumableUpload resumableUpload) {
        AttachmentContent attachment = null;
        if (resumableUpload.hasAttachmentId()) {
            try {
                attachment = attchmntClient.get().getAttachmentContent(resumableUpload.getAttachmentId());
            } catch (TException e) {
                log.error("Error retrieving attachment", e);
            }
        }
        return attachment;
    }

    public AttachmentContent createAttachmentContent(ResourceRequest request) throws IOException {
        String filename = request.getParameter("fileName");
        AttachmentContent attachmentContent = new AttachmentContent()
                .setContentType("application/octet-stream")
                .setFilename(filename);

        try {
            attachmentContent = attchmntClient.get().makeAttachmentContent(attachmentContent);
        } catch (TException e) {
            log.error("Error creating attachment", e);
            attachmentContent = null;
        }

        return attachmentContent;
    }

    public boolean uploadAttachmentPart(PortletRequest request, String fileUploadName) throws IOException {
        try {
            return uploadAttachmentPartFromRequest(request, fileUploadName);
        } catch (TException e) {
            log.error("Error getting attachment and saving it", e);
            return false;
        }
    }

    public RequestStatus cancelUpload(ResourceRequest request) {
        String attachmentId = request.getParameter(PortalConstants.ATTACHMENT_ID);
        try {
            return attchmntClient.get().deleteAttachmentContent(attachmentId);
        } catch (TException e) {
            log.error("Error deleting attachment from backend", e);
            return RequestStatus.FAILURE;
        }
    }

    public boolean checkAttachmentExistsFromRequest(ResourceRequest request) {
        ResumableUpload resumableUpload = ResumableUpload.from(request);

        final AttachmentContent attachment = getAttachmentContent(resumableUpload);

        return alreadyHavePart(attachment, resumableUpload);
    }

    private boolean alreadyHavePart(AttachmentContent attachment, ResumableUpload resumableUpload) {
        if (attachment == null || !resumableUpload.isValid()) {
            return false;
        }

        AttachmentStreamConnector attachmentStreamConnector;
        try {
            attachmentStreamConnector = getConnector();
        } catch (TException e) {
            log.error("no connector", e);
            return false;
        }

        try {
            attachmentStreamConnector.getAttachmentPartStream(attachment, resumableUpload.getChunkNumber()).close();
            return true;
        } catch (SW360Exception e) {
            log.error("cannot check if part already exists", e);
            return false;
        } catch (IOException | DocumentNotFoundException ignored) {
            return false;
        }
    }
}
